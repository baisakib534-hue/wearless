# syntax=docker/dockerfile:1
FROM golang:1.23-alpine AS build
WORKDIR /src
COPY shared/go.mod /src/shared/
COPY node/gateway/userspace/go.mod node/gateway/userspace/go.sum /src/node/gateway/userspace/
RUN cd /src/node/gateway/userspace && go mod download
COPY shared /src/shared
COPY node/gateway/userspace /src/node/gateway/userspace
RUN cd /src/node/gateway/userspace && CGO_ENABLED=0 GOOS=linux go build -trimpath -o /out/wearless-userspace

FROM alpine:3.20
RUN adduser -D -H -u 10001 wearless
COPY --from=build /out/wearless-userspace /usr/local/bin/wearless-userspace
USER 10001
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/wearless-userspace"]