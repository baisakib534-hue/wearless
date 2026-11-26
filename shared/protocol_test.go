package protocol

import (
	"bytes"
	"testing"
)

func TestEncodeDecodeRoundTrip(t *testing.T) {
	original := Frame{
		Version:   ProtocolVersion,
		Type:      FrameData,
		RequestID: 0x0102030405060708,
		Payload:   []byte("example.com:443"),
	}
	decoded, err := DecodeFrame(EncodeFrame(original))
	if err != nil {
		t.Fatal(err)
	}
	if decoded.Version != original.Version || decoded.Type != original.Type ||
		decoded.RequestID != original.RequestID || !bytes.Equal(decoded.Payload, original.Payload) {
		t.Fatalf("decoded = %+v, want %+v", decoded, original)
	}
}

func TestDecodeRejectsBadVersion(t *testing.T) {
	frame := EncodeFrame(Frame{Type: FramePing})
	frame[0] = ProtocolVersion + 1
	if _, err := DecodeFrame(frame); err == nil {
		t.Fatal("expected bad-version error")
	}
}
