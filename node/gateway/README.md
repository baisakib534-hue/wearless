# Wearless gateway node

Provider-agnostic container image. It exposes one public HTTP/WebSocket port
(`$PORT`) and carries mqvpn UDP traffic through wstunnel over that port.

## Environment variables

- `PORT` (required): public listening port assigned by the provider. The
  container listens only on this port.
- `WEARLESS_NODE_KEY` (required): mqvpn per-user key used as the node key. It is
  written to the runtime mqvpn config under `/run/wearless` with mode `0600`.
- `WSTUNNEL_PORT` (optional, default `18080`): internal plain WebSocket port
  used between nginx and wstunnel. It is not published and must differ from
  `PORT`.
- `WSTUNNEL_RESTRICT_HTTP_UPGRADE_PATH_PREFIX` (optional): wstunnel server path
  prefix secret. wstunnel reads this variable itself.
- `WSTUNNEL_DNS_PREFER_IPV4` (optional): wstunnel DNS preference. wstunnel
  reads this variable itself.
- `RUST_LOG` (optional): wstunnel log level. wstunnel reads this variable itself.

No other configuration files are required. No secret is stored in the image.

## Build

```sh
docker build -f Dockerfile.gateway -t wearless-gateway .
```

## Run locally

```sh
docker run --rm \
  -p 8080:8080 \
  -e PORT=8080 \
  -e WEARLESS_NODE_KEY='<new-node-key>' \
  --cap-add NET_ADMIN \
  --device /dev/net/tun \
  wearless-gateway
```

## Deploy

Set `PORT` to the value required by the provider and `WEARLESS_NODE_KEY` to the
node key. The same image is used on every provider; only those environment
variables change.

```sh
docker run -d \
  -e PORT="$PORT" \
  -e WEARLESS_NODE_KEY="$WEARLESS_NODE_KEY" \
  --cap-add NET_ADMIN \
  --device /dev/net/tun \
  wearless-gateway
```

Health check:

```sh
curl -fsS "http://127.0.0.1:${PORT}/health"
```

Expected response: `ok`.

## Runtime requirements

- `/dev/net/tun` and `NET_ADMIN` are required by the mqvpn server. Providers or
  runtimes that cannot grant them cannot run this node.
- Outbound internet access is required for mqvpn client traffic NAT.
- The container is the artifact; no Docker daemon runs inside it.

## Client

```sh
WEARLESS_NODE_KEY='<node-key>' \
  scripts/gateway-client.sh <service-host-or-url>
```

This runs:

```sh
wstunnel client -L 'udp://127.0.0.1:4433:127.0.0.1:4433?timeout_sec=0' wss://<service>
mqvpn --mode client --server 127.0.0.1:4433 --auth-key "$WEARLESS_NODE_KEY" --insecure
```

`timeout_sec=0` is mandatory for the UDP mapping. The mqvpn client uses
`--insecure` because the node serves a self-signed mqvpn certificate.

## Verified flag sources

- wstunnel command syntax, `--restrict-to`, `timeout_sec=0`, and the
  `udp://BIND:PORT:HOST:PORT` mapping:
  https://github.com/erebe/wstunnel/blob/main/README.md
- mqvpn server/client flags, `--mode`, `--listen`, `--server`, `--auth-key`,
  `--insecure`, and config keys:
  https://docs.mqvpn.org/guide/getting-started
  https://docs.mqvpn.org/guide/configuration

## Secret scan

No secret values are committed. Configuration is supplied only through
`WEARLESS_NODE_KEY`, `PORT`, and optional `WSTUNNEL_*` variables at runtime.
