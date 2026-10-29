FROM postgres:18-alpine
RUN apk upgrade --no-cache \
    && rm -f /usr/local/bin/gosu
USER postgres
