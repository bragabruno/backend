.PHONY: help up down build test lint seed reset logs db redis shell app-shell

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

up: ## Start all services (Postgres, Redis, Kafka, App)
	docker compose up -d --build
	@echo "Waiting for services to be healthy..."
	@sleep 10
	@docker compose ps

down: ## Stop all services
	docker compose down

build: ## Build the application
	./gradlew build

test: ## Run unit + integration tests
	./gradlew test

lint: ## Run code quality checks
	./gradlew check

seed: ## Run Flyway migrations (seed database)
	./gradlew :app:flywayMigrate

reset: ## Clean volumes and re-create stack
	docker compose down -v
	docker compose up -d --build
	@echo "Stack recreated. Waiting for healthchecks..."
	@sleep 15
	@docker compose ps

logs: ## Follow service logs
	docker compose logs -f

logs-app: ## Follow app logs only
	docker compose logs -f app

logs-postgres: ## Follow Postgres logs only
	docker compose logs -f postgres

db: ## Connect to Postgres via psql
	docker compose exec postgres psql -U fraud_user -d fraud_db

redis-cli: ## Connect to Redis CLI
	docker compose exec redis redis-cli

shell: ## Open a shell in the app container
	docker compose exec app sh

app-shell: ## Open a bash shell in the app container (if available)
	docker compose exec app bash || docker compose exec app sh

status: ## Show service status
	docker compose ps

restart: ## Restart all services
	docker compose restart

restart-app: ## Restart the app only
	docker compose restart app

clean: ## Remove all containers, volumes, and images
	docker compose down -v --rmi local
	./gradlew clean

deps: ## Check dependency tree
	./gradlew :app:dependencies
