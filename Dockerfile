FROM debian:bookworm-slim

ARG WSTUNNEL_VERSION=11.1.0
ARG MQVPN_VERSION=0.20.0
ARG TARGETARCH=amd64

ENV DEBIAN_FRONTEND=noninteractive \
    LD_LIBRARY_PATH=/opt/mqvpn/lib

RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        iproute2 \
        iptables \
        libstdc++6 \
        libevent-2.1-7 \
        nginx \
        openssl \
        procps \
        util-linux \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    case "${TARGETARCH}" in \
        amd64) WSTUNNEL_ARCH=amd64 ;; \
        arm64) WSTUNNEL_ARCH=arm64 ;; \
        *) echo "unsupported TARGETARCH: ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    curl -fsSL -o /tmp/wstunnel.tar.gz \
        "https://github.com/erebe/wstunnel/releases/download/v${WSTUNNEL_VERSION}/wstunnel_${WSTUNNEL_VERSION}_linux_${WSTUNNEL_ARCH}.tar.gz"; \
    tar -xzf /tmp/wstunnel.tar.gz -C /usr/local/bin wstunnel; \
    chmod 0755 /usr/local/bin/wstunnel; \
    rm -f /tmp/wstunnel.tar.gz

RUN set -eux; \
    case "${TARGETARCH}" in \
        amd64) MQVPN_ARCH=amd64 ;; \
        arm64) MQVPN_ARCH=arm64 ;; \
        *) echo "unsupported TARGETARCH: ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    curl -fsSL -o /tmp/mqvpn.tar.gz \
        "https://github.com/mp0rta/mqvpn/releases/download/v${MQVPN_VERSION}/mqvpn_${MQVPN_VERSION}_${MQVPN_ARCH}.tar.gz"; \
    mkdir -p /opt/mqvpn; \
    tar -xzf /tmp/mqvpn.tar.gz -C /opt/mqvpn; \
    chmod 0755 /opt/mqvpn/bin/mqvpn \
        /opt/mqvpn/lib/mqvpn/mqvpn-server-nat.sh \
        /opt/mqvpn/lib/mqvpn/mqvpn-nat-config; \
    rm -f /tmp/mqvpn.tar.gz

RUN rm -f /etc/nginx/sites-enabled/default /etc/nginx/conf.d/default.conf

COPY node/gateway/nginx.conf.template /etc/nginx/templates/gateway.conf.template
COPY node/gateway/entrypoint.sh /usr/local/bin/gateway-entrypoint
RUN chmod 0755 /usr/local/bin/gateway-entrypoint

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["/usr/local/bin/gateway-entrypoint"]
