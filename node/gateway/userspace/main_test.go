package main

import (
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	protocol "github.com/baisakib534-hue/wearless/shared"
)

func TestTunnelConnectsAndRelaysTCP(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	go func() {
		for {
			conn, err := listener.Accept()
			if err != nil {
				return
			}
			go func(conn net.Conn) {
				defer conn.Close()
				buf := make([]byte, 1024)
				for {
					conn.SetReadDeadline(time.Now().Add(2 * time.Second))
					n, err := conn.Read(buf)
					if err != nil {
						return
					}
					if _, err := conn.Write(buf[:n]); err != nil {
						return
					}
				}
			}(conn)
		}
	}()

	s := &server{key: "test-key", allowPrivate: true}
	mux := http.NewServeMux()
	mux.HandleFunc(protocol.HandshakePath, s.handleTunnel)
	httpServer := httptest.NewServer(mux)
	defer httpServer.Close()

	wsURL := "ws" + strings.TrimPrefix(httpServer.URL, "http") + protocol.HandshakePath
	header := http.Header{"Authorization": []string{"Bearer test-key"}}
	ws, _, err := websocket.DefaultDialer.Dial(wsURL, header)
	if err != nil {
		t.Fatal(err)
	}
	defer ws.Close()
	ws.SetReadDeadline(time.Now().Add(2 * time.Second))

	address := listener.Addr().String()
	if err := ws.WriteMessage(websocket.BinaryMessage, protocol.EncodeFrame(protocol.Frame{
		Version: protocol.ProtocolVersion, Type: protocol.FrameConnectRequest, Payload: []byte(address),
	})); err != nil {
		t.Fatal(err)
	}
	if err := ws.WriteMessage(websocket.BinaryMessage, protocol.EncodeFrame(protocol.Frame{
		Version: protocol.ProtocolVersion, Type: protocol.FrameData, Payload: []byte("ping"),
	})); err != nil {
		t.Fatal(err)
	}

	_, response, err := ws.ReadMessage()
	if err != nil {
		t.Fatal(err)
	}
	connectFrame, err := protocol.DecodeFrame(response)
	if err != nil {
		t.Fatal(err)
	}
	if connectFrame.Type != protocol.FrameConnectResponse {
		t.Fatalf("first frame type = %s, want %s", connectFrame.Type, protocol.FrameConnectResponse)
	}

	_, data, err := ws.ReadMessage()
	if err != nil {
		t.Fatal(err)
	}
	dataFrame, err := protocol.DecodeFrame(data)
	if err != nil {
		t.Fatal(err)
	}
	if dataFrame.Type != protocol.FrameData || string(dataFrame.Payload) != "ping" {
		t.Fatalf("echo frame = %+v", dataFrame)
	}

	if err := ws.WriteMessage(websocket.BinaryMessage, protocol.EncodeFrame(protocol.Frame{
		Version: protocol.ProtocolVersion, Type: protocol.FrameClose,
	})); err != nil {
		t.Fatal(err)
	}
}

func TestTunnelRejectsBadKey(t *testing.T) {
	s := &server{key: "test-key", allowPrivate: true}
	mux := http.NewServeMux()
	mux.HandleFunc(protocol.HandshakePath, s.handleTunnel)
	httpServer := httptest.NewServer(mux)
	defer httpServer.Close()

	wsURL := "ws" + strings.TrimPrefix(httpServer.URL, "http") + protocol.HandshakePath
	header := http.Header{"Authorization": []string{"Bearer wrong"}}
	_, response, err := websocket.DefaultDialer.Dial(wsURL, header)
	if err == nil {
		_ = response.Body.Close()
		t.Fatal("expected dial to fail")
	}
	if response == nil || response.StatusCode != http.StatusUnauthorized {
		t.Fatalf("response = %v, want HTTP 401", response)
	}
}
