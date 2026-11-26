package protocol

import (
	"encoding/binary"
	"errors"
	"fmt"
)

const (
	ProtocolVersion byte = 1
	HandshakePath   = "/wearless/v1"
	maxFrame        = 32 * 1024
	headerLength    = 10
)

type FrameType byte

const (
	FrameInvalid FrameType = iota
	FrameConnectRequest
	FrameConnectResponse
	FrameData
	FrameClose
	FramePing
	FramePong
	FrameError
)

func (t FrameType) String() string {
	switch t {
	case FrameConnectRequest:
		return "CONNECT_REQUEST"
	case FrameConnectResponse:
		return "CONNECT_RESPONSE"
	case FrameData:
		return "DATA"
	case FrameClose:
		return "CLOSE"
	case FramePing:
		return "PING"
	case FramePong:
		return "PONG"
	case FrameError:
		return "ERROR"
	default:
		return fmt.Sprintf("INVALID_%d", byte(t))
	}
}

type Frame struct {
	Version   byte
	Type      FrameType
	RequestID uint64
	Payload   []byte
}

func EncodeFrame(frame Frame) []byte {
	buf := make([]byte, headerLength+len(frame.Payload))
	buf[0] = ProtocolVersion
	buf[1] = byte(frame.Type)
	binary.BigEndian.PutUint64(buf[2:headerLength], frame.RequestID)
	copy(buf[headerLength:], frame.Payload)
	return buf
}

func DecodeFrame(input []byte) (Frame, error) {
	if len(input) == 0 {
		return Frame{}, errors.New("protocol: empty frame")
	}
	if len(input) < headerLength {
		return Frame{}, fmt.Errorf("protocol: short header: got %d bytes, want %d", len(input), headerLength)
	}
	if input[0] != ProtocolVersion {
		return Frame{}, fmt.Errorf("protocol: unsupported version %d", input[0])
	}
	if len(input) > maxFrame {
		return Frame{}, fmt.Errorf("protocol: frame too large: %d bytes, max %d", len(input), maxFrame)
	}
	frameType := FrameType(input[1])
	if frameType == FrameInvalid {
		return Frame{}, errors.New("protocol: invalid frame type 0")
	}
	payload := make([]byte, len(input)-headerLength)
	copy(payload, input[headerLength:])
	return Frame{
		Version:   input[0],
		Type:      frameType,
		RequestID: binary.BigEndian.Uint64(input[2:headerLength]),
		Payload:   payload,
	}, nil
}

func MaxFrame() int { return maxFrame }
