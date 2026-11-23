# TEST-ONLY image: runs the Wearless node wrapper smoke checks and serves a
# tiny health endpoint. The production node is NATIVE under systemd on a VPS;
# this image exists only to smoke-test the wrapper on Render.
FROM ubuntu:22.04

RUN apt-get update \
 && apt-get install -y --no-install-recommends bash ca-certificates socat \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/wearless
COPY node/ /opt/wearless/node/
COPY scripts/render-smoke.sh /opt/wearless/scripts/render-smoke.sh
COPY render-test/health-server.sh /opt/wearless/health-server.sh
RUN chmod +x /opt/wearless/node/*.sh /opt/wearless/scripts/*.sh /opt/wearless/health-server.sh

ENV PORT=10000
EXPOSE 10000

# Run the real node wrapper smoke checks on boot, then serve /health with the
# actual result. If the smoke checks fail, the process exits non-zero so Render
# marks the deploy unhealthy.
CMD ["/bin/bash", "-c", "/opt/wearless/scripts/render-smoke.sh && exec /opt/wearless/health-server.sh"]