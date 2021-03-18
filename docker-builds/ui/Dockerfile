
FROM clojure:openjdk-11-lein-2.9.5 AS builder
ARG BUILD_ENV=qa
ENV BUILD_ENV=${BUILD_ENV}
ENV DISTRICT_REGISTRY_ENV=${BUILD_ENV}

WORKDIR /app
COPY . /app/

RUN lein less4clj once
RUN lein cljsbuild once "ui"


FROM nginx:stable
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# replace nginx config
COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY docker-builds/ui/default /etc/nginx/conf.d/default

# nginx config
COPY docker-builds/ui/registry.io /etc/nginx/sites-enabled/registry.io

# setup error page
ADD https://raw.githubusercontent.com/district0x/X0X/master/X0X.html /usr/share/nginx/html/X0X.html

# get compiled JS
COPY --from=builder /app/resources/public /registry/resources/public/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
