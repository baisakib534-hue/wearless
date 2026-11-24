# Wearless provider inbound networking research

Research for the provider-agnostic gateway node package (`WSS on 443 -> wstunnel -> localhost UDP mqvpn`).  
Scope: public inbound networking rules only, from official provider documentation. Nothing below is guessed; items that could not be confirmed from fetched docs are marked `TO VERIFY` with the reason.

## Gateway node requirement (from `node/gateway` spec)

- One container artifact, deployable unchanged on any free HTTP/WebSocket provider.
- Inbound: HTTPS/WSS on 443 only. No UDP, no custom ports on the public edge.
- Inside the container: mqvpn server on `127.0.0.1:4433` UDP (localhost UDP is unrestricted); wstunnel server unwraps WSS -> `127.0.0.1:4433` with `--restrict-to 127.0.0.1:4433`; an HTTP listener on `$PORT` exposes `/health` and proxies everything else to wstunnel with WebSocket Upgrade headers.

## Provider comparison

| Provider | Public inbound protocols | Custom port on public edge | Free-tier sleep / idle | Real UDP door? | Gateway deploys unchanged? | Verdict |
|---|---|---|---|---|---|---|
| Render | HTTPS + WebSocket (WSS). No public UDP/TCP. Private network allows any protocol, but free web services cannot receive private network traffic. | One public HTTP port via `PORT` (default `10000`); reserved ports `18012`, `18013`, `19099` blocked. Multiple ports only over private network. | Free web service spins down after 15 minutes with no inbound HTTP/WebSocket traffic; spin-up ~1 minute; 750 free instance hours/workspace/month. | No public UDP. | Yes. WSS on 443 is supported. | Suitable. Use Render free web service with WSS gateway. |
| Koyeb | HTTP, HTTP/2, WebSocket, gRPC on web services; raw TCP via TCP Proxy (preview). No UDP documented. | Exposed ports configurable (port + `HTTP`/`HTTP/2` + path). TCP Proxy assigns a random persistent proxy port. | Free instance scales to zero after 1 hour idle (cannot disable on free instance). Held WebSocket/HTTP/2 connections prevent scale-to-zero. | No UDP documented. | Yes. WebSocket is supported on web services. | Suitable, with 1-hour idle sleep; a held WSS connection keeps it awake. |
| Railway | HTTP/1.1 and HTTP/2 over HTTPS; WebSocket over HTTP/1.1; raw TCP via TCP Proxy. No UDP (provider says UDP not supported platform-wide). | TCP Proxy: specify internal port, Railway generates domain + random port. HTTP edge port handling: TO VERIFY exact default/override. | Free plan services sleep after 5-10 minutes without outbound traffic; wake on next request. WebSocket connections are exempt from edge idle limits but service sleep is based on outbound packets. | No public UDP. | Yes via WSS on 443. TCP Proxy cannot carry UDP. | Suitable for WSS gateway; expect cold starts unless outbound traffic is maintained. |
| Hugging Face Spaces | Inbound is HTTPS through the Space URL. Docker Spaces expose one external port (`app_port`, default `7860`); internal ports are reachable only inside the container. Outbound restricted to ports 80, 443, 8080. | `app_port` in README YAML (default `7860`). Only that port is exposed externally; reverse proxy dispatches from a single port. | CPU Basic spaces sleep after 48 hours of inactivity; upgraded hardware sleep time configurable; manual pause available. | No UDP documented. | TO VERIFY: WebSocket upgrade through the HF proxy is not explicitly documented for Docker Spaces. Also, creating Gradio/Docker Spaces requires a paid plan (PRO/Team/Enterprise) for personal/org accounts, so free-tier deploy is blocked. | Not suitable for free-tier unchanged deploy; verify WebSocket support and plan requirement before use. |
| Fly.io | HTTP/HTTPS and WebSocket on `[http_service]` (ports 80/443). Raw TCP and UDP supported via `[[services]]` with `protocol = "tcp"` or `"udp"`. | Fully custom: external ports 1-65535, port ranges, multiple services/internal ports. UDP requires binding to `fly-global-services` on the same external port. | Autostop/autostart configurable (`stop`/`suspend`/`off`); scale to zero possible with `min_machines_running = 0`. Free plan: up to 3 machines, no performance machines. | Yes, real UDP is supported, but requires a dedicated IPv4 address, which is not available on the free plan. UDP works over IPv6 private networking. | On free tier: no (use WSS gateway). With paid dedicated IPv4: yes, real UDP is a better door than WebSocket. | Best UDP door if paid; on free tier use the WSS gateway. |

## Per-provider evidence

### Render
- Source: `https://render.com/docs/websocket` — "Render web services can accept inbound WebSocket connections from the public internet."
- Source: `https://render.com/docs/web-services` — "Render forwards inbound requests to your web service at this port"; "Render's load balancer terminates SSL for inbound HTTPS requests"; reserved ports `18012`, `18013`, `19099`.
- Source: `https://render.com/docs/free` — "Render spins down a Free web service that goes 15 minutes without receiving any inbound traffic. This includes both HTTP requests and WebSocket messages from existing connections."; "Render grants 750 Free instance hours to each workspace per calendar month".
- UDP: no public UDP. Private network allows any protocol, but free web services cannot receive private network traffic (`https://render.com/docs/private-network`).

### Koyeb
- Source: `https://www.koyeb.com/docs/reference/services` — Web Service runs "HTTP, HTTP/2, WebSocket, or gRPC applications".
- Source: `https://www.koyeb.com/docs/build-and-deploy/exposing-your-service` — public protocol choices are `HTTP` or `HTTP/2`; ports and routes configurable.
- Source: `https://www.koyeb.com/docs/run-and-scale/tcp-proxy` — raw TCP via TCP Proxy (preview); "TCP Proxy is not compatible with Scale-to-Zero".
- Source: `https://www.koyeb.com/docs/run-and-scale/scale-to-zero` — free instance scales to zero after 1 hour idle; "No held connection (e.g. websocket or HTTP/2 stream) from the Internet" is required for scale-down; "You can wake a Service up using a WebSocket connection, but that connection may only live for a few minutes."
- Source: `https://www.koyeb.com/docs/faqs/pricing` — one free web Service: 512MB RAM, 0.1 vCPU, 2GB SSD.
- UDP: no UDP support found in fetched docs. TO VERIFY whether Koyeb supports UDP at all (no UDP doc found).

### Railway
- Source: `https://docs.railway.com/networking/public-networking/specs-and-limits` — "We support HTTP/1.1 and HTTP/2 traffic"; "Support for websockets over HTTP/1.1."; "Websocket connections are exempt from these duration and inactivity limits, and can stay open indefinitely, even while idle."
- Source: `https://docs.railway.com/networking/tcp-proxy` — raw TCP via TCP Proxy; custom domain hostname replacement only, port stays Railway-assigned.
- Source: `https://docs.railway.com/deployments/serverless` — sleep after 5-10 minutes without outbound traffic; inbound requests are not measured directly but responses count; wake on next request.
- Source: `https://docs.railway.com/pricing/free-plan` — $1/month credit, no card; 5-10 minute sleep; limited trial has restricted outbound network and only limited ports.
- Source: `https://blog.railway.com/p/launch-week-01-scale-to-zero` — "We'd also like to support UDP, but this is something we'd have to support for the whole platform..."; "QUIC/HTTP/3" not supported end to end.
- UDP: no public UDP. TO VERIFY exact HTTP service port default/override (not explicitly stated in fetched docs).

### Hugging Face Spaces
- Source: `https://huggingface.co/docs/hub/spaces-sdks-docker` — "you can change the default exposed port `7860` by setting `app_port: 7860`"; "Internally you could have as many open ports as you want... reverse proxy like Nginx to dispatch requests from the broader internet (on a single port) to different internal ports."
- Source: `https://huggingface.co/docs/hub/spaces-overview` — "Static Spaces are free for everyone. Gradio and Docker Spaces run on compute and require a paid plan to create" (PRO/Team/Enterprise); free personal accounts in good standing can host up to 2 Gradio Spaces on ZeroGPU.
- Source: `https://huggingface.co/docs/hub/spaces-gpus` — CPU Basic: 2 vCPU, 16 GB, 50 GB disk, free; "Spaces running on free hardware are suspended automatically if they are not used for an extended period of time (e.g. two days)."
- Source: `https://huggingface.co/docs/hub/spaces-overview` (networking) — outbound requests allowed only on ports 80, 443, 8080.
- Source: `https://huggingface.co/docs/hub/spaces-gpus` — cpu-basic sleep after 48 hours inactivity.
- UDP: no UDP documented. WebSocket: TO VERIFY through official Docker Spaces docs (not explicitly stated in fetched pages).

### Fly.io
- Source: `https://docs.fly.io/networking/udp-and-tcp` — real UDP supported; requires dedicated IPv4; bind to `fly-global-services`; same external/internal port; no per-flow load balancing; QUIC not supported.
- Source: `https://docs.fly.io/networking/services` and `https://docs.fly.io/reference/configuration` — custom ports 1-65535, port ranges, multiple services, handlers `http`/`tls`/etc.; `[http_service]` on 80/443.
- Source: `https://docs.fly.io/networking/websockets` — WebSocket works without extra config; idle connections stay open; autostop can close with code 1012.
- Source: `https://docs.fly.io/about/free-plan` — no dedicated IPv4 on free plan ("Dedicated IPv4 addresses and static egress IP addresses" not available); up to 3 machines; $1 one-time credit (7 days) or $5/month with card.
- Source: `https://docs.fly.io/launch/autostop-autostart` — scale to zero with `auto_stop_machines = "stop"`, `auto_start_machines = true`, `min_machines_running = 0`.
- UDP door proof: `https://docs.fly.io/networking/udp-and-tcp` confirms real UDP, but free tier lacks the required dedicated IPv4. TO VERIFY whether a paid dedicated IPv4 is acceptable for this project's cost model.

## wstunnel authoritative client/server flag syntax

Source: `https://raw.githubusercontent.com/erebe/wstunnel/main/README.md` (default branch `main`, pushed 2026-10-19; same content at `master`).  
Repo: `https://github.com/erebe/wstunnel`.

Client tunnel spec:
```
-L, --local-to-remote <{tcp,udp,socks5,stdio,unix}://[BIND:]PORT:HOST:PORT>
```
UDP example with timeout control:
```
'udp://1212:1.1.1.1:53?timeout_sec=10'    timeout_sec on udp force close the tunnel after 10sec. Set it to 0 to disable the timeout [default: 30]
```
Client command shape:
```
wstunnel client -L 'udp://51820:localhost:51820?timeout_sec=0' wss://my.server.com:443
```
Mandatory for Wearless gateway: `timeout_sec=0` (disables the 30s default UDP tunnel timeout).

Server restriction:
```
--restrict-to <DEST:PORT>
    Server will only allow tunneling to the specified destination (host:port).
    Restricts where clients can forward traffic to. Can be specified multiple times.
    Example: --restrict-to "google.com:443" --restrict-to "localhost:22"
```
Wearless gateway usage:
```
wstunnel server wss://[::]:443 --restrict-to 127.0.0.1:22
...
wstunnel server --restrict-to localhost:51820 wss://[::]:443
```
For the gateway node, use `--restrict-to 127.0.0.1:4433` (localhost UDP mqvpn). Server also supports `-r/--restrict-http-upgrade-path-prefix` (env `WSTUNNEL_RESTRICT_HTTP_UPGRADE_PATH_PREFIX`) and `--restrict-config`.

Additional relevant flags:
- `--websocket-ping-frequency <DURATION>` (client and server; default `30s`; set to zero to disable) — useful for keepalive.
- Client: `--connection-retry-max-backoff`, `--reverse-tunnel-connection-retry-max-backoff` (default `1s`) for auto-reconnect.
- Server: `--enable-webtransport` / `wts://` opens a UDP port (WebTransport over QUIC/HTTP/3) — requires end-to-end UDP; not usable on providers without UDP.

## URLs actually fetched

- https://render.com/docs/free
- https://render.com/docs/web-services
- https://render.com/docs/websocket
- https://render.com/docs/private-network
- https://docs.railway.com/pricing/free-plan
- https://docs.railway.com/networking/public-networking/specs-and-limits
- https://docs.railway.com/networking/tcp-proxy
- https://docs.railway.com/deployments/serverless
- https://blog.railway.com/p/launch-week-01-scale-to-zero
- https://huggingface.co/docs/hub/spaces-overview
- https://huggingface.co/docs/hub/spaces-sdks-docker
- https://huggingface.co/docs/hub/spaces-gpus
- https://www.koyeb.com/docs
- https://www.koyeb.com/docs/reference/services
- https://www.koyeb.com/docs/build-and-deploy/exposing-your-service
- https://www.koyeb.com/docs/run-and-scale/tcp-proxy
- https://www.koyeb.com/docs/run-and-scale/scale-to-zero
- https://www.koyeb.com/docs/faqs/pricing
- https://www.koyeb.com/docs/reference/instances
- https://docs.fly.io/about/free-plan
- https://docs.fly.io/networking/udp-and-tcp
- https://docs.fly.io/networking/services
- https://docs.fly.io/networking/websockets
- https://docs.fly.io/reference/configuration
- https://docs.fly.io/launch/autostop-autostart
- https://raw.githubusercontent.com/erebe/wstunnel/main/README.md

## TO VERIFY (unverified, with reason)

- Koyeb: whether UDP is supported at all (no UDP documentation found in fetched pages).
- Koyeb: whether WebSocket wake-ups have the "few minutes" lifetime limit in practice for a long-lived WSS tunnel.
- Railway: exact default/override HTTP service port and whether `PORT` is injected (not stated in fetched docs).
- Hugging Face Spaces: whether WebSocket upgrade is proxied for Docker Spaces (not explicitly documented in fetched pages).
- Hugging Face Spaces: whether any free tier can run Docker Spaces (fetched docs say Gradio/Docker Spaces require a paid plan; TO VERIFY current ZeroGPU/free exceptions).
- Fly.io: whether paying for a dedicated IPv4 is acceptable for this project's cost model (pricing not in fetched docs).
- Render: whether WebSocket messages count as inbound traffic for the 15-minute spin-down timer (doc says inbound traffic includes WebSocket messages from existing connections, but message-frequency threshold is not quantified).

## Fly.io and Oracle Always Free (TUN-capable options)

### Fly.io dedicated IPv4 (for real UDP/TUN)
- Source: https://fly.io/docs/about/pricing — dedicated IPv4 addresses are billed at **$2/month per address** (as of the pricing page). Shared IPv4 (via fly-proxy) is free but cannot carry raw UDP/TUN.
- To get TUN+UDP on Fly.io you need a paid dedicated IPv4, which is not on the free plan (`https://docs.fly.io/about/free-plan`).
- flyctl CLI + a Fly account token required to deploy.

### Oracle Cloud Always Free (signup requirements)
- Source: https://www.oracle.com/cloud/free/ and https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Signup requires a **credit card for verification** (no charge; Oracle places a small temporary authorization).
- Always Free tier includes: 2 AMD Compute VMs (1/8 OCPU, 1 GB RAM each) OR up to 4 ARM Ampere A1 instances (up to 4 OCPU, 24 GB RAM total), 200 GB block storage, 10 TB/month outbound data transfer.
- OCI Compute instances grant full root and support `/dev/net/tun` + `CAP_NET_ADMIN`, so mqvpn can create its TUN device there. This is the cheapest TUN-capable route if Fly.io cost is unacceptable.

## Userspace (no-TUN) server exit — research conclusion (B)

- mqvpn (`github.com/mp0rta/mqvpn`) requires `/dev/net/tun` + `CAP_NET_ADMIN` on the server; its server mode calls `open("/dev/net/tun")` and fails with `TUN create failed / open /dev/net/tun: No such file or directory` on Render. No no-TUN or userspace-socket mode is documented in mqvpn's README or server config (`server.conf.example` uses `TunName`).
- Surveyed equivalent MASQUE/QUIC stacks (cloudflare/quiche, msquic, quic-go, aioquic): all are QUIC libraries, not VPN exits. None terminate a multipath tunnel and dial real TCP/UDP sockets without a TUN device. A userspace exit would require reimplementing the entire MASQUE CONNECT-IP handler plus a TCP/IP stack (smoltcp/lwIP) — out of scope for a bounded research round.
- Verdict: **cannot be done on Render without TUN.** Do not fake it. Move to Oracle Always Free or Fly.io paid dedicated IPv4.
