FROM clojure:openjdk-11-lein-2.9.5 AS builder
ARG BUILD_ENV
ENV BUILD_ENV=${BUILD_ENV}
ENV DISTRICT_REGISTRY_ENV=${BUILD_ENV}

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN curl -fsSL https://deb.nodesource.com/setup_10.x | bash -
RUN apt-get install -y --no-install-recommends \
        build-essential \
        nodejs \
        python3.7 \
        python3-pip libc6 sqlite3 libsqlite3-dev

WORKDIR /app
