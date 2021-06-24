# VARS
COMMIT_ID = $(shell git rev-parse --short HEAD)
PROJECT_NAME = registry
DOCKER_REPO = district0x
DEV_IMAGE = registry-dev:local
BUILD_ENV = dev
SHELL=bash
DOCKER_VOL_PARAMS = -v ${PWD}:/build -v ${PROJECT_NAME}_vol_target:/build/target -v ${PROJECT_NAME}_vol_m2_cache:/root/.m2 -v ${PROJECT_NAME}_vol_node_modules:/build/node_modules --workdir /build
DOCKER_NET_PARAMS = --network=${PROJECT_NAME}_dev_network
.PHONY: help


# HELP
help: ## Print help
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help

# DOCKER BUILDS

# All images
build-dev-image: ## Build dev container (nodejs10, lein, openjdk_11, python)
	DOCKER_BUILDKIT=1 BUILD_ENV=${BUILD_ENV} DOCKER_REPO=${DOCKER_REPO} COMMIT_ID=${COMMIT_ID} docker build -t ${DEV_IMAGE} . -f docker-builds/base/Dockerfile

build-images: ## Build all containers in docker-compose file
	DOCKER_BUILDKIT=1 BUILD_ENV=${BUILD_ENV} DOCKER_REPO=${DOCKER_REPO} COMMIT_ID=${COMMIT_ID} docker-compose -p ${PROJECT_NAME} build --build-arg="${BUILD_ENV}" --parallel

build-images-no-cache: ## Build all containers in docker-compose file w/o cache
	DOCKER_BUILDKIT=1 BUILD_ENV=${BUILD_ENV} DOCKER_REPO=${DOCKER_REPO} COMMIT_ID=${COMMIT_ID} docker-compose -p ${PROJECT_NAME} build --build-arg="${BUILD_ENV}" --parallel --pull --no-cache

push-images: ## Push docker image from docker-compose files
	DOCKER_BUILDKIT=1 BUILD_ENV=${BUILD_ENV} DOCKER_REPO=${DOCKER_REPO} COMMIT_ID=${COMMIT_ID} docker-compose push --ignore-push-failures

build-push-images: build-images push-images ## (Re)build all containers in docker-compose file and push them

# RUN CONTAINERS
init: ## Create docker networks, volumes and build containers for this project
	DOCKER_REPO=${DOCKER_REPO} docker-compose -p ${PROJECT_NAME} up --no-start --no-build

start-containers: ## Build and start all containers ipfs, ganache, dev container)
	DOCKER_REPO=${DOCKER_REPO} docker-compose -p ${PROJECT_NAME} up -d

run-dev-shell: ## Start dev container in an interactive mode
	DOCKER_REPO=${DOCKER_REPO} docker run -ti --rm --entrypoint="" ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash

check-containers: ## Show docker-compose ps for given project
	DOCKER_REPO=${DOCKER_REPO} docker-compose -p ${PROJECT_NAME} ps

clear-all: ## Remove containers, networks and volumes
	DOCKER_REPO=${DOCKER_REPO} docker-compose -p ${PROJECT_NAME} down --remove-orphans --volumes

# TEST CODE
server-tests: ## exec tests in dev container
	docker run -ti --rm --entrypoint="" ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "lein cljsbuild once "server-tests""

# SHORTCUTS
build: build-images ## Build all containers (alias for docker-build)
up: start-containers ## Start dev environment (alias for start-containers)
rm: clear-all ## Remove containers, networks and volumes (alias for clear-all)
ps: check-containers ## Show docker-compose ps for given project (alias for check-containers)
exec: run-dev-shell ## Show docker-compose ps for given project (alias for check-containers)
