package wearless

import (
	"errors"
	"strconv"
	"sync"
	"syscall"

	"github.com/xjasonlyu/tun2socks/v2/core"
	"github.com/xjasonlyu/tun2socks/v2/core/device"
	"github.com/xjasonlyu/tun2socks/v2/core/device/fdbased"
	"github.com/xjasonlyu/tun2socks/v2/dialer"
	"github.com/xjasonlyu/tun2socks/v2/tunnel"
	"github.com/xjasonlyu/tun2socks/v2/tunnel/statistic"
	"gvisor.dev/gvisor/pkg/tcpip/stack"
)

const (
	tunMTU   = 1500
	fdOffset = 0
)

var (
	stateMu  sync.Mutex
	stackRef *stackHandle
)

type stackHandle struct {
	device device.Device
	stack  *stack.Stack
	tunnel *tunnel.Tunnel
}

// ProtectFunc is implemented by the Android service to protect outer sockets.
// gomobile cannot bind Go func fields.
type ProtectFunc interface {
	Protect(fd int) error
}

// ProxyConfig controls the userspace Wearless proxy.
type ProxyConfig struct {
	Endpoint string
	AuthKey  string
	Protect  ProtectFunc
}

// Start starts the userspace Wearless tunnel on an Android TUN fd.
func Start(endpoint string, authKey string, protect ProtectFunc, fd int) error {
	config := ProxyConfig{Endpoint: endpoint, AuthKey: authKey, Protect: protect}
	if config.Endpoint == "" || config.AuthKey == "" || fd < 0 {
		return errors.New("invalid config or fd")
	}

	stateMu.Lock()
	defer stateMu.Unlock()
	if stackRef != nil {
		return errors.New("tunnel already running")
	}

	dialer.Reset()
	if config.Protect != nil {
		dialer.RegisterSockOpt(dialer.SocketOptionFunc(func(network, address string, c syscall.RawConn) error {
			return controlRawConn(c, config.Protect)
		}))
	}

	dev, err := fdbased.Open(strconv.Itoa(fd), tunMTU, fdOffset)
	if err != nil {
		return err
	}

	p := &wearlessProxy{config: config}
	t := tunnel.New(p, statistic.DefaultManager)
	t.ProcessAsync()
	s, err := core.CreateStack(&core.Config{
		LinkEndpoint:     dev,
		TransportHandler: t,
	})
	if err != nil {
		dev.Close()
		return err
	}

	stackRef = &stackHandle{device: dev, stack: s, tunnel: t}
	return nil
}

// Stop stops the userspace tunnel.
func Stop() error {
	stateMu.Lock()
	defer stateMu.Unlock()
	h := stackRef
	if h == nil {
		return nil
	}
	stackRef = nil
	h.tunnel.Close()
	h.stack.Close()
	h.device.Close()
	return nil
}

func controlRawConn(c syscall.RawConn, protect ProtectFunc) error {
	var outer error
	if err := c.Control(func(fd uintptr) {
		outer = protect.Protect(int(fd))
	}); err != nil {
		return err
	}
	return outer
}
