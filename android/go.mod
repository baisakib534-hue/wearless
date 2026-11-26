module github.com/baisakib534-hue/wearless/android

go 1.27.1

require (
	github.com/baisakib534-hue/wearless/shared v0.0.0
	github.com/gorilla/websocket v1.5.3
	github.com/xjasonlyu/tun2socks/v2 v2.7.0
)

replace github.com/baisakib534-hue/wearless/shared => ../shared

require (
	github.com/google/btree v1.1.3 // indirect
	github.com/google/uuid v1.6.0 // indirect
	go.uber.org/atomic v1.12.0 // indirect
	go.uber.org/multierr v1.11.0 // indirect
	go.uber.org/zap v1.28.0 // indirect
	golang.org/x/exp v0.0.0-20261011100332-faecfb24eb49 // indirect
	golang.org/x/mobile v0.0.0-20261117202452-47699113848a // indirect
	golang.org/x/sys v0.50.0 // indirect
	golang.org/x/time v0.16.0 // indirect
	gvisor.dev/gvisor v0.0.0-20261010064756-cd110d9dec04 // indirect
)
