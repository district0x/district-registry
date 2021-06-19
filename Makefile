# VARS
COMMIT_ID = $(shell git rev-parse --short HEAD)
PROJECT_NAME = registry
NODEJS_IMAGE = node:14-stretch
DEV_IMAGE = node14-lein:local
BUILD_ENV = qa
SHELL=bash
DOCKER_VOL_PARAMS = -v ${PWD}:/build/ -v ${PROJECT_NAME}_vol_m2_cache:/root/.m2 -v ${PROJECT_NAME}_vol_node_modules:/build/node_modules --workdir /build
DOCKER_NET_PARAMS = --network=${PROJECT_NAME}_dev_network
.PHONY: help


# HELP
help: ## Print help
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help

# DOCKER BUILDS

# All images
build-images: ## Build all containers in docker-compose file
	DOCKER_BUILDKIT=1 BUILD_ENV=${BUILD_ENV} COMMIT_ID=${COMMIT_ID} docker-compose -p ${PROJECT_NAME} build --build-arg="${BUILD_ENV}" --parallel

build-images-no-cache: # Build base docker image with node11.14, yarn, clojure, lein, truffle
	DOCKER_BUILDKIT=1 BUILD_ENV=${BUILD_ENV} COMMIT_ID=${COMMIT_ID} docker-compose -p ${PROJECT_NAME} build --build-arg="${BUILD_ENV}" --parallel --pull --no-cache

push-images:
	DOCKER_BUILDKIT=1 BUILD_ENV=${BUILD_ENV} COMMIT_ID=${COMMIT_ID} docker-compose push --ignore-push-failures

build-push-images: build-images push-images ## (Re)build all containers in docker-compose file and push them

# RUN CONTAINERS
start-containers: dev-image ## Build and start containers ((ipfs, ganache, dev container)
	docker-compose -p ${PROJECT_NAME} up -d

run-dev-shell: ## Start container in interactive mode
	docker run -ti --rm --entrypoint="" ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash

check-containers: ## Show docker-compose ps for given project
	docker-compose -p ${PROJECT_NAME} ps

clear-all: ## Remove containers, networks and volumes
	docker-compose -p ${PROJECT_NAME} down

# TEST CODE
deps: ## Install/update deps
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "yarn deps"

lint: dev-image deps  ## mount code inside dev container and lint
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} sh -c "yarn lint"

# SHORTCUTS
build: build-images ## Build all containers (alias for docker-build)
up: start-containers ## Start dev environment (alias for start-containers)
rm: clear-all ## Remove containers, networks and volumes (alias for clear-all)
ps: check-containers ## Show docker-compose ps for given project (alias for check-containers)
exec: run-dev-shell ## Show docker-compose ps for given project (alias for check-containers)
