# Pulse: the Listen Together server (MetrolistGroup/metroserver, GPL-3.0), built at a pinned
# commit with a policy that lets Pulse host rooms. The upstream code is used unchanged.
FROM golang:1.25.6-alpine AS builder

RUN apk add --no-cache git protobuf-dev bash

ARG METROSERVER_REPO=https://github.com/MetrolistGroup/metroserver
ARG METROSERVER_COMMIT=9da6f9da1f3ec92183a26dc8f6fbf9c8b3f0ede8

WORKDIR /build
RUN git clone "$METROSERVER_REPO" . && \
    git checkout "$METROSERVER_COMMIT" && \
    git submodule update --init --depth 1

RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.36.11
ENV PATH="/root/go/bin:${PATH}"

RUN bash scripts/generate_proto.sh && \
    CGO_ENABLED=0 GOOS=linux go build -o metroserver -ldflags="-w -s" ./cmd/metroserver

FROM alpine:3.20

RUN apk add --no-cache ca-certificates && \
    addgroup -g 1000 metrolist && \
    adduser -D -u 1000 -G metrolist metrolist

WORKDIR /app
COPY --from=builder /build/metroserver .
COPY ua_policy.json /app/ua_policy.json
RUN mkdir /app/data && chown -R metrolist:metrolist /app
USER metrolist

ENV PORT=8080 \
    UA_POLICY_FILE=/app/ua_policy.json \
    DATABASE_FILE=/app/data/metroserver.db

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider "http://localhost:${PORT}/health" || exit 1

CMD ["./metroserver"]
