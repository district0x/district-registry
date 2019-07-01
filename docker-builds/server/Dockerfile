FROM node:11-stretch-slim
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# ENV variables
ENV CONFIG /configs/registry.config.edn

# get compiled JS
COPY server /registry/server
COPY node_modules /registry/node_modules
COPY resources /registry/resources

### --- DEBUG
RUN ls registry/
RUN ls registry/server/

WORKDIR registry

ENTRYPOINT ["node", "server/district-registry.js"]
CMD ["--max-old-space-size=2048"]
