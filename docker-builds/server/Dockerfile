FROM clojure:openjdk-11-lein-2.9.5 AS builder
ARG BUILD_ENV=qa
ENV BUILD_ENV=${BUILD_ENV}
ENV DISTRICT_REGISTRY_ENV=${BUILD_ENV}

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN curl -fsSL https://deb.nodesource.com/setup_10.x | bash -
RUN apt-get install -y --no-install-recommends \
        build-essential \
        nodejs \
        python3.7 \
        python3-pip

WORKDIR /app
COPY . /app/

RUN lein cljsbuild once "server"
RUN lein deps
RUN lein npm install
RUN npx truffle compile --all


FROM node:11-stretch-slim
MAINTAINER "Filip Bielejec" <filip@district0x.io>
ARG BUILD_ENV=qa
ENV BUILD_ENV=${BUILD_ENV}
ENV DISTRICT_REGISTRY_ENV=${BUILD_ENV}


# ENV variables
ENV CONFIG /configs/registry.config.edn

# get compiled JS
COPY --from=builder /app/server /registry/server
COPY --from=builder /app/node_modules /registry/node_modules
COPY --from=builder /app/resources /registry/resources

WORKDIR /registry

ENTRYPOINT ["node", "server/district-registry.js"]
CMD ["--max-old-space-size=2048"]
