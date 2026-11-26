package main

import (
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	protocol "github.com/baisakib534-hue/wearless/shared"
)

const readHeaderTimeout = 15 * time.Second

var upgrader = websocket.Upgrader{
	ReadBufferSize:  32 * 1024,
	WriteBufferSize: 32 * 1024,
	CheckOrigin:     func(*http.Request) bool { return true },
}

type server struct {
	key           string
	allowedHosts  map[string]struct{}
	allowPrivate  bool
	activeConns   int64
	activeConnsMu sync.Mutex
	totalConns    uint64
	totalConnsMu  sync.Mutex
}

func (s *server) active() int64 {
	s.activeConnsMu.Lock()
	defer s.activeConnsMu.Unlock()
	return s.activeConns
}

func (s *server) incActive() {
	s.activeConnsMu.Lock()
	s.activeConns++
	s.activeConnsMu.Unlock()
}

func (s *server) decActive() {
	s.activeConnsMu.Lock()
	s.activeConns--
	s.activeConnsMu.Unlock()
}

func (s *server) incTotal() uint64 {
	s.totalConnsMu.Lock()
	s.totalConns++
	n := s.totalConns
	s.totalConnsMu.Unlock()
	return n
}

func (s *server) authenticate(r *http.Request) error {
	if s.key == "" {
		return errors.New("server key is not configured")
	}
	auth := r.Header.Get("Authorization")
	token := strings.TrimSpace(strings.TrimPrefix(auth, "Bearer "))
	if token == "" {
		token = strings.TrimSpace(r.Header.Get("X-Wearless-Key"))
	}
	if token != s.key {
		return errors.New("invalid node key")
	}
	return nil
}

func (s *server) authorizeAddress(address string) (string, error) {
	host, portText, err := net.SplitHostPort(address)
	if err != nil {
		return "", fmt.Errorf("invalid CONNECT address %q: %w", address, err)
	}
	port, err := strconv.Atoi(portText)
	if err != nil || port < 1 || port > 65535 {
		return "", fmt.Errorf("invalid CONNECT port %q", portText)
	}
	host = strings.TrimSpace(host)
	if host == "" {
		return "", errors.New("empty CONNECT host")
	}
	if len(s.allowedHosts) > 0 {
		if _, ok := s.allowedHosts[strings.ToLower(host)]; !ok {
			return "", fmt.Errorf("host not allowed: %s", host)
		}
		return net.JoinHostPort(host, portText), nil
	}
	if !s.allowPrivate {
		ip := net.ParseIP(host)
		if ip == nil {
			ips, err := net.LookupIP(host)
			if err != nil || len(ips) == 0 {
				return "", fmt.Errorf("CONNECT host does not resolve: %s", host)
			}
			ip = ips[0]
		}
		if ip == nil || ip.IsLoopback() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() ||
			ip.IsMulticast() || ip.IsUnspecified() || ip.IsPrivate() {
			return "", fmt.Errorf("private or invalid CONNECT host rejected: %s", host)
		}
	}
	return net.JoinHostPort(host, portText), nil
}

func (s *server) handleHealth(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, _ = io.WriteString(w, "ok\n")
}

func (s *server) handleReady(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	if s.key == "" {
		w.WriteHeader(http.StatusServiceUnavailable)
		_, _ = io.WriteString(w, "not ready: WEARLESS_NODE_KEY is empty\n")
		return
	}
	w.WriteHeader(http.StatusOK)
	_, _ = fmt.Fprintf(w, "ready active=%d total=%d\n", s.active(), s.totalConns)
}

func (s *server) handleTunnel(w http.ResponseWriter, r *http.Request) {
	if err := s.authenticate(r); err != nil {
		log.Printf("auth failed remote=%s: %v", r.RemoteAddr, err)
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("websocket upgrade failed remote=%s: %v", r.RemoteAddr, err)
		return
	}
	defer conn.Close()

	if err := s.runConnection(conn); err != nil {
		var closeErr *websocket.CloseError
		if !errors.As(err, &closeErr) {
			log.Printf("tunnel ended remote=%s: %v", r.RemoteAddr, err)
		}
	}
}

func (s *server) runConnection(conn *websocket.Conn) error {
	conn.SetReadLimit(int64(protocol.MaxFrame()))
	var connectionID uint64
	var downstream net.Conn

	for {
		messageType, message, err := conn.ReadMessage()
		if err != nil {
			if errors.Is(err, io.EOF) || websocket.IsCloseError(err, websocket.CloseNormalClosure, websocket.CloseGoingAway) {
				return nil
			}
			return err
		}
		if messageType != websocket.BinaryMessage {
			return fmt.Errorf("expected binary WebSocket message, got type %d", messageType)
		}
		frame, err := protocol.DecodeFrame(message)
		if err != nil {
			return err
		}

		switch frame.Type {
		case protocol.FrameConnectRequest:
			if downstream != nil {
				return errors.New("CONNECT_REQUEST after connection already established")
			}
			address := string(frame.Payload)
			authorized, err := s.authorizeAddress(address)
			if err != nil {
				_ = writeFrame(conn, protocol.Frame{Version: protocol.ProtocolVersion, Type: protocol.FrameError, RequestID: frame.RequestID, Payload: []byte(err.Error())})
				return err
			}
			downstream, err = net.DialTimeout("tcp", authorized, 10*time.Second)
			if err != nil {
				_ = writeFrame(conn, protocol.Frame{Version: protocol.ProtocolVersion, Type: protocol.FrameConnectResponse, RequestID: frame.RequestID, Payload: []byte("ERROR " + err.Error())})
				return fmt.Errorf("CONNECT %s failed: %w", authorized, err)
			}
			connectionID = s.incTotal()
			s.incActive()
			log.Printf("connected id=%d remote=%s destination=%s", connectionID, conn.RemoteAddr().String(), authorized)
			if err := writeFrame(conn, protocol.Frame{Version: protocol.ProtocolVersion, Type: protocol.FrameConnectResponse, RequestID: frame.RequestID}); err != nil {
				s.closeDownstream(downstream, &connectionID)
				return err
			}
			go s.copyDownstream(conn, downstream, connectionID)

		case protocol.FrameData:
			if downstream == nil {
				return errors.New("DATA before CONNECT_REQUEST")
			}
			if len(frame.Payload) == 0 {
				return errors.New("DATA frame has empty payload")
			}
			if _, err := downstream.Write(frame.Payload); err != nil {
				return fmt.Errorf("downstream write failed: %w", err)
			}

		case protocol.FramePing:
			if err := writeFrame(conn, protocol.Frame{Version: protocol.ProtocolVersion, Type: protocol.FramePong, RequestID: frame.RequestID, Payload: frame.Payload}); err != nil {
				return err
			}

		case protocol.FrameClose:
			s.closeDownstream(downstream, &connectionID)
			_ = conn.WriteControl(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, ""), time.Now().Add(time.Second))
			return nil

		default:
			return fmt.Errorf("unexpected frame type %s", frame.Type)
		}
	}
}

func (s *server) copyDownstream(ws *websocket.Conn, downstream net.Conn, connectionID uint64) {
	defer s.closeDownstream(downstream, &connectionID)
	buf := make([]byte, 24*1024)
	for {
		n, err := downstream.Read(buf)
		if n > 0 {
			payload := make([]byte, n)
			copy(payload, buf[:n])
			frame := protocol.Frame{Version: protocol.ProtocolVersion, Type: protocol.FrameData, RequestID: connectionID, Payload: payload}
			if writeErr := writeFrame(ws, frame); writeErr != nil {
				return
			}
		}
		if err != nil {
			return
		}
	}
}

func (s *server) closeDownstream(conn net.Conn, connectionID *uint64) {
	if conn == nil {
		return
	}
	_ = conn.Close()
	if connectionID != nil && *connectionID != 0 {
		s.decActive()
		*connectionID = 0
	}
}

func writeFrame(conn *websocket.Conn, frame protocol.Frame) error {
	conn.SetWriteDeadline(time.Now().Add(30 * time.Second))
	return conn.WriteMessage(websocket.BinaryMessage, protocol.EncodeFrame(frame))
}

func envFlag(name string) bool {
	value := strings.ToLower(strings.TrimSpace(os.Getenv(name)))
	return value == "1" || value == "true" || value == "yes"
}

func main() {
	log.SetFlags(log.LstdFlags | log.LUTC | log.Lmicroseconds)
	port := strings.TrimSpace(os.Getenv("PORT"))
	if port == "" {
		port = "8080"
	}
	key := os.Getenv("WEARLESS_NODE_KEY")
	allowedHosts := map[string]struct{}{}
	if raw := strings.TrimSpace(os.Getenv("WEARLESS_ALLOWED_HOSTS")); raw != "" {
		for _, rawHost := range strings.Split(raw, ",") {
			host := strings.ToLower(strings.TrimSpace(rawHost))
			if host != "" {
				allowedHosts[host] = struct{}{}
			}
		}
	}
	s := &server{
		key:          key,
		allowedHosts: allowedHosts,
		allowPrivate: envFlag("WEARLESS_EGRESS_ALLOW_PRIVATE"),
	}
	mux := http.NewServeMux()
	mux.HandleFunc("/health", s.handleHealth)
	mux.HandleFunc("/ready", s.handleReady)
	mux.HandleFunc(protocol.HandshakePath, s.handleTunnel)

	server := &http.Server{
		Addr:              ":" + port,
		Handler:           mux,
		ReadHeaderTimeout: readHeaderTimeout,
	}
	log.Printf("userspace gateway listening port=%s allowed_hosts=%d private_egress=%t", port, len(allowedHosts), s.allowPrivate)
	if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("userspace gateway failed: %v", err)
	}
}
