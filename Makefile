.PHONY: help up down build test lint seed reset logs db redis shell app-shell

# Doppler integration (consistent with root Makefile)
DOPPLER := $(shell command -v doppler 2>/dev/null)
ifdef DOPPLER
  RUN := doppler run --
else
  RUN :=
endif

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

up: ## Start all services (Postgres, Redis, Kafka, App)
	$(RUN) docker compose up -d --build
	@echo "Waiting for services to be healthy..."
	@sleep 10
	@$(RUN) docker compose ps

down: ## Stop all services
	$(RUN) docker compose down

build: ## Build the application
	$(RUN) ./gradlew build

test: ## Run unit + integration tests
	$(RUN) ./gradlew test

lint: ## Run code quality checks
	$(RUN) ./gradlew check

seed: ## Run Flyway migrations (seed database)
	$(RUN) ./gradlew :app:flywayMigrate

reset: ## Clean volumes and re-create stack
	$(RUN) docker compose down -v
	$(RUN) docker compose up -d --build
	@echo "Stack recreated. Waiting for healthchecks..."
	@sleep 15
	@$(RUN) docker compose ps

logs: ## Follow service logs
	$(RUN) docker compose logs -f

logs-app: ## Follow app logs only
	$(RUN) docker compose logs -f app

logs-postgres: ## Follow Postgres logs only
	$(RUN) docker compose logs -f postgres

db: ## Connect to Postgres via psql
	$(RUN) docker compose exec postgres psql -U fraud_user -d fraud_db

redis-cli: ## Connect to Redis CLI
	$(RUN) docker compose exec redis redis-cli

shell: ## Open a shell in the app container
	$(RUN) docker compose exec app sh

app-shell: ## Open a bash shell in the app container (if available)
	$(RUN) docker compose exec app bash || $(RUN) docker compose exec app sh

status: ## Show service status
	$(RUN) docker compose ps

restart: ## Restart all services
	$(RUN) docker compose restart

restart-app: ## Restart the app only
	$(RUN) docker compose restart app

clean: ## Remove all containers, volumes, and images
	$(RUN) docker compose down -v --rmi local
	$(RUN) ./gradlew clean

deps: ## Check dependency tree
	$(RUN) ./gradlew :app:dependencies
