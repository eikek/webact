FROM alpine:latest

LABEL maintainer="eikek0 <news@eknet.org>"

RUN apk add --no-cache openjdk11-jre unzip curl bash

RUN mkdir -p /opt \
  && cd /opt \
  && curl -L -o webact.zip https://github.com/eikek/webact/releases/download/v0.5.2/webact-0.5.2.zip \
  && unzip webact.zip \
  && rm webact.zip \
  && apk del unzip curl

EXPOSE 8011

ENTRYPOINT ["/opt/webact-0.5.2/bin/webact"]
