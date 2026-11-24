# Gateway client proof — https://wearless-node-test.onrender.com

Date: 2026-11-24
Deploy: `dep-dc2ibu15vhsc7388m5p0` (commit `a6dac16`) — confirmed `live`, `/health` returns `ok`.

## Command attempted
```
WEARLESS_NODE_KEY=ref:wearless-key-1 scripts/gateway-client.sh wearless-node-test.onrender.com
```
(requires root for `/dev/net/tun`; host is Windows/MSYS with no Linux TUN device, so the mqvpn client cannot run locally. The Render container is the relevant server-side TUN surface.)

## Server-side result (from Render logs)
mqvpn server fails to create its TUN device on Render's free web service:

```
05:52:09.715 [ERR] TUN create failed
05:52:09.715 [ERR] open /dev/net/tun: No such file or directory
```

wstunnel server itself starts and listens on `127.0.0.1:18080`; nginx proxies `/health` and WSS on `$PORT`; `/health` returns `ok`. But without `/dev/net/tun` (no `CAP_NET_ADMIN` on Render free tier) the mqvpn server cannot bring up `mqvpn0`, so no real mqvpn session and no real traffic can be proven through this deploy.

## Verdict
BLOCKED on Render free tier — TUN device unavailable. Per standing rule, move the gateway to a provider from `PROVIDERS.md` that grants TUN. Only Fly.io documents real TUN/UDP support, and it requires a paid dedicated IPv4 (not available on the free plan) plus `flyctl` and a Fly token, neither of which is present in this environment.

## Next step required from user
Provide a Fly.io API token (and confirm paid dedicated IPv4 is acceptable) OR a host with `/dev/net/tun` + `CAP_NET_ADMIN` and `mqvpn`/`wstunnel` binaries, then re-run `scripts/gateway-client.sh` against the new URL and capture ping + download output.