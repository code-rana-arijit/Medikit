#!/usr/bin/env bash
set -euo pipefail

# Medikit local development helper
# Usage: ./scripts/dev.sh [infra|all|build|stop|logs]

CMD="${1:-infra}"

case "$CMD" in
  infra)
    echo "Starting infrastructure (postgres, redis, kafka, elasticsearch, mailhog)..."
    docker compose --profile infra up -d
    ;;
  all)
    echo "Starting everything..."
    docker compose --profile all up -d --build
    ;;
  build)
    echo "Building all service images..."
    docker compose --profile all build
    ;;
  stop)
    echo "Stopping all containers..."
    docker compose down
    ;;
  logs)
    echo "Streaming service logs..."
    docker compose logs -f
    ;;
  *)
    echo "Usage: $0 [infra|all|build|stop|logs]"
    exit 1
    ;;
esac
