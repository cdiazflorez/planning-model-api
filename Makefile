
DATABASE_CONTAINER=planning-model-api-mysql
DATABASE_IMAGE=${DATABASE_CONTAINER}:0.1.0

all: help

.PHONY: help
help: ## Display this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(firstword $(MAKEFILE_LIST)) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: build
build: ## Run gradle task to compile the application
	./gradlew -DjvmArgs=  -Dorg.gradle.daemon=false build --refresh-dependencies

.PHONY: test
test: ## Run tests
	./gradlew -DjvmArgs=  -Dorg.gradle.daemon=false test jacocoTestReport

.PHONY: run
run: ## Run application locally at localhost:8080
	./gradlew bootRun --args='--spring.profiles.active=test'

.PHONY: db-build
db-build: ## Build a mysql docker image locally
	docker build -f Dockerfile.mysql -t ${DATABASE_IMAGE} .

.PHONY: db-run
db-run: ## Run the mysql docker container at localhost:3306
	-docker rm -f ${DATABASE_CONTAINER}
	docker run -d -p 3306:3306 -p 33060:33060 --name ${DATABASE_CONTAINER} ${DATABASE_IMAGE}

.PHONY: db-stop
db-stop: ## Stops mysql docker container
	docker stop ${DATABASE_CONTAINER}
