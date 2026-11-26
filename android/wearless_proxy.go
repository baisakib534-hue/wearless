package wearless

import (
	"context"
	"errors"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"syscall"
	"time"

	"github.com/gorilla/websocket"

	M "github.com/xjasonlyu/tun2socks/v2/metadata"
	"github.com/xjasonlyu/tun2socks/v2/proxy"

	protocol "github.com/baisakib534-hue/wearless/shared"
)

var _ proxy.Proxy = (*wearlessProxy)(nil)

// wearlessProxy carries every gVisor TCP flow through the authenticated
// Wearless WebSocket tunnel. It never dials the destination directly.
type wearlessProxy struct {
	config ProxyConfig
}

func (p *wearlessProxy) DialContext(ctx context.Context, metadata *M.Metadata) (net.Conn, error) {
	if metadata == nil {
		return nil, errors.New("wearless: nil metadata")
	}
	destination := metadata.DestinationAddress()
	if destination == "" {
		return nil, errors.New("wearless: destination address is empty")
	}
	return newTunnelConn(ctx, p.config, destination)
}

func (*wearlessProxy) DialUDP(*M.Metadata) (net.PacketConn, error) {
	return nil, errors.New("wearless: UDP through tunnel is not implemented yet")
}

type tunnelConn struct {
	config        ProxyConfig
	destination   string
	ws            *websocket.Conn
	readMu        sync.Mutex
	readBuf       []byte
	writeMu       sync.Mutex
	closeMu       sync.Mutex
	closed        bool
	readDeadline  time.Time
	writeDeadline time.Time
}

func newTunnelConn(ctx context.Context, config ProxyConfig, destination string) (net.Conn, error) {
	endpoint := normalizeEndpoint(config.Endpoint)
	header := http.Header{}
	header.Set("Authorization", "Bearer "+config.AuthKey)

	dialer := &websocket.Dialer{
		HandshakeTimeout: 10 * time.Second,
		NetDialContext:   protectDialContext(config.Protect),
	}
	ws, _, err := dialer.DialContext(ctx, endpoint, header)
	if err != nil {
		return nil, fmt.Errorf("wearless: websocket dial %s failed: %w", safeEndpoint(endpoint), err)
	}
	conn := &tunnelConn{config: config, destination: destination, ws: ws}
	if err := conn.connect(); err != nil {
		_ = conn.Close()
		return nil, err
	}
	return conn, nil
}

func (c *tunnelConn) connect() error {
	c.ws.SetReadLimit(int64(protocol.MaxFrame()))
	request := protocol.Frame{
		Version: protocol.ProtocolVersion,
		Type:    protocol.FrameConnectRequest,
		Payload: []byte(c.destination),
	}
	if err := c.ws.WriteMessage(websocket.BinaryMessage, protocol.EncodeFrame(request)); err != nil {
		return fmt.Errorf("wearless: CONNECT send failed: %w", err)
	}
	_ = c.ws.SetReadDeadline(time.Now().Add(10 * time.Second))
	messageType, message, err := c.ws.ReadMessage()
	if err != nil {
		return fmt.Errorf("wearless: CONNECT response read failed: %w", err)
	}
	if messageType != websocket.BinaryMessage {
		return fmt.Errorf("wearless: CONNECT response type = %d, want binary", messageType)
	}
	frame, err := protocol.DecodeFrame(message)
	if err != nil {
		return fmt.Errorf("wearless: CONNECT response decode failed: %w", err)
	}
	if frame.Type != protocol.FrameConnectResponse {
		if frame.Type == protocol.FrameError {
			return fmt.Errorf("wearless: gateway rejected CONNECT %s: %s", c.destination, string(frame.Payload))
		}
		return fmt.Errorf("wearless: CONNECT response type = %s", frame.Type)
	}
	if len(frame.Payload) != 0 {
		return fmt.Errorf("wearless: gateway CONNECT %s failed: %s", c.destination, string(frame.Payload))
	}
	_ = c.ws.SetReadDeadline(time.Time{})
	return nil
}

func (c *tunnelConn) Read(b []byte) (int, error) {
	c.readMu.Lock()
	defer c.readMu.Unlock()
	if len(c.readBuf) > 0 {
		n := copy(b, c.readBuf)
		c.readBuf = c.readBuf[n:]
		return n, nil
	}
	if !c.readDeadline.IsZero() {
		_ = c.ws.SetReadDeadline(c.readDeadline)
	} else {
		_ = c.ws.SetReadDeadline(time.Time{})
	}
	messageType, message, err := c.ws.ReadMessage()
	if err != nil {
		return 0, err
	}
	if messageType != websocket.BinaryMessage {
		return 0, fmt.Errorf("wearless: downstream frame type = %d, want binary", messageType)
	}
	frame, err := protocol.DecodeFrame(message)
	if err != nil {
		return 0, err
	}
	switch frame.Type {
	case protocol.FrameData:
		if len(frame.Payload) == 0 {
			return 0, nil
		}
		n := copy(b, frame.Payload)
		if n < len(frame.Payload) {
			c.readBuf = append([]byte(nil), frame.Payload[n:]...)
		}
		return n, nil
	case protocol.FrameClose:
		return 0, net.ErrClosed
	case protocol.FrameError:
		return 0, errors.New(string(frame.Payload))
	default:
		return 0, fmt.Errorf("wearless: unexpected downstream frame %s", frame.Type)
	}
}

func (c *tunnelConn) Write(b []byte) (int, error) {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	if !c.writeDeadline.IsZero() {
		_ = c.ws.SetWriteDeadline(c.writeDeadline)
	} else {
		_ = c.ws.SetWriteDeadline(time.Time{})
	}
	frame := protocol.Frame{
		Version: protocol.ProtocolVersion,
		Type:    protocol.FrameData,
		Payload: append([]byte(nil), b...),
	}
	if err := c.ws.WriteMessage(websocket.BinaryMessage, protocol.EncodeFrame(frame)); err != nil {
		return 0, err
	}
	return len(b), nil
}

func (c *tunnelConn) Close() error {
	c.closeMu.Lock()
	defer c.closeMu.Unlock()
	if c.closed {
		return nil
	}
	c.closed = true
	_ = c.ws.SetWriteDeadline(time.Now().Add(time.Second))
	_ = c.ws.WriteMessage(websocket.BinaryMessage, protocol.EncodeFrame(protocol.Frame{
		Version: protocol.ProtocolVersion,
		Type:    protocol.FrameClose,
	}))
	return c.ws.Close()
}

func (c *tunnelConn) LocalAddr() net.Addr  { return tunnelAddr("wearless-local") }
func (c *tunnelConn) RemoteAddr() net.Addr { return tunnelAddr(c.destination) }

func (c *tunnelConn) SetDeadline(t time.Time) error {
	c.readDeadline = t
	c.writeDeadline = t
	return nil
}
func (c *tunnelConn) SetReadDeadline(t time.Time) error  { c.readDeadline = t; return nil }
func (c *tunnelConn) SetWriteDeadline(t time.Time) error { c.writeDeadline = t; return nil }

type tunnelAddr string

func (a tunnelAddr) Network() string { return "wearless" }
func (a tunnelAddr) String() string  { return string(a) }

func normalizeEndpoint(endpoint string) string {
	trimmed := strings.TrimSpace(endpoint)
	if trimmed == "" {
		return protocol.HandshakePath
	}
	if !strings.Contains(trimmed, "://") {
		trimmed = "wss://" + trimmed
	}
	if !strings.HasSuffix(trimmed, protocol.HandshakePath) {
		trimmed = strings.TrimRight(trimmed, "/") + protocol.HandshakePath
	}
	return trimmed
}

func safeEndpoint(endpoint string) string {
	if parsed, err := url.Parse(endpoint); err == nil && parsed.Host != "" {
		return parsed.Host + parsed.Path
	}
	return endpoint
}

func protectDialContext(protect ProtectFunc) func(context.Context, string, string) (net.Conn, error) {
	dialer := &net.Dialer{Timeout: 10 * time.Second}
	return func(ctx context.Context, network, address string) (net.Conn, error) {
		conn, err := dialer.DialContext(ctx, network, address)
		if err != nil {
			return nil, err
		}
		if protect == nil {
			return conn, nil
		}
		rawConn, ok := conn.(syscall.Conn)
		if !ok {
			return conn, nil
		}
		raw, rawErr := rawConn.SyscallConn()
		if rawErr != nil {
			_ = conn.Close()
			return nil, rawErr
		}
		var protectErr error
		if err := raw.Control(func(fd uintptr) {
			protectErr = protect.Protect(int(fd))
		}); err != nil {
			_ = conn.Close()
			return nil, err
		}
		if protectErr != nil {
			_ = conn.Close()
			return nil, protectErr
		}
		return conn, nil
	}
}
