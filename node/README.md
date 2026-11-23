# Wearless node

Wearless runs the app plus this native node only. There is no website, account system, cloud control plane, or Docker anywhere in this design.

## Install

Run on Ubuntu 22.04 or Debian 12 as root:

```sh
sudo env \
  WEARLESS_NODE_KEY='<new-node-key>' \
  WEARLESS_SERVER_ADDRESS='<public-node-address>' \
  MQVPN_URL='<verified-official-direct-binary-url>' \
  sh node/install.sh
```

`MQVPN_URL` is intentionally not set by this repository. The exact official release URL was not present in the provided sources, so the installer's default is `TO VERIFY — official release URL`; it refuses to invent one. Supply the exact official direct binary URL after verifying it, or supply `MQVPN_BINARY=/path/to/mqvpn`.

The installer creates the dedicated `wearless-node` system user, writes `/etc/wearless/node.conf` as root-only mode `0600`, installs the native systemd service, opens UDP 443, and enables start-on-boot. The service uses systemd credentials so the unprivileged service receives the configuration without making the root-only file world-readable.

## Verify

```sh
systemctl status wearless-node.service
sudo ss -lunp | grep ':443 '
sudo wearless-control status
sudo wearless-control stats
sudo wearless-stats
sudo tail -n 1 /var/log/wearless/node-stats.log
```

The node listens on UDP 443. Check the hosting provider's external firewall too; a local rule cannot open a provider-side block.

## Users and revocation

The mqvpn Control API is JSON over TCP on `127.0.0.1:9090`. It has **no authentication** and must never be exposed remotely.

```sh
sudo wearless-control list
printf '%s\n' '<new-user-key>' | sudo wearless-control add '<user-name>'
sudo wearless-control remove '<user-name>'
```

Removing a user disconnects its active sessions. To revoke the node key, rerun `install.sh` with the same server address and a new `WEARLESS_NODE_KEY`; the installer replaces the root-only configuration and restarts the service.

## Local stats

`sudo wearless-stats` appends one root-only line to `/var/log/wearless/node-stats.log` with a UTC timestamp, sessions, per-user bytes, per-path RTT/loss when the API exposes them, and node RSS. Fields the API does not expose are recorded as `NOT_MEASURED`; they are never estimated.

## Memory

The measured mqvpn v0.19.2 server idle RSS is approximately 36 MB, and it remained healthy under a hard 48 MB memory cap in the Wearless lab [D11]. That measurement was idle only: RSS under real bonded traffic has **not** been measured on a real VPS. Treat 128 MB as a practical floor and 256 MB as the comfortable recommendation until the under-load number exists. The service does not impose a hard 48 MB production cap because under-load behavior is not yet measured.

## Security boundaries

- Node configuration is outside the repository at `/etc/wearless/node.conf`, mode `0600`.
- The Control API is local-only and unauthenticated; never forward or bind it remotely.
- Stats are local-only and root-only.
- No Docker, custom kernel, paid service, website, accounts, or cloud control plane.
