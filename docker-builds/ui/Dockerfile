
FROM clojure:openjdk-11-lein-slim-bullseye AS builder
ARG BUILD_ENV=qa
ENV BUILD_ENV=${BUILD_ENV}
ENV DISTRICT_REGISTRY_ENV=${BUILD_ENV}

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN curl -fsSL https://deb.nodesource.com/setup_10.x | bash -
RUN apt-get install -y --no-install-recommends \
        build-essential \
        nodejs \
        python3 \
        python3-pip libc6 sqlite3 libsqlite3-dev

WORKDIR /app
COPY . /app/

RUN lein less4clj once
RUN lein cljsbuild once "ui"
RUN lein deps
RUN lein npm install
RUN npx truffle compile --all

FROM nginx:stable-alpine

# replace nginx config
COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY docker-builds/ui/default /etc/nginx/conf.d/default

# nginx config
COPY docker-builds/ui/registry_io.conf /etc/nginx/sites-enabled/registry_io.conf

# setup error page
ADD --chown=nginx:nginx https://raw.githubusercontent.com/district0x/X0X/master/X0X.html /usr/share/nginx/html/X0X.html

# get compiled JS
COPY --from=builder --chown=nginx:nginx /app/resources/public /registry/resources/public/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
