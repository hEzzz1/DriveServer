#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-}"
SKIP_PULL="${SKIP_PULL:-0}"
FRONTEND_ROOT="${FRONTEND_ROOT:-$(cd "$ROOT_DIR/.." && pwd)/DriveWeb}"
SYSTEMD_SERVICE="${SYSTEMD_SERVICE:-driveserver}"
ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health}"
APP_PORT="${APP_PORT:-8080}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-2}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-5}"

usage() {
  cat <<'EOF'
Usage:
  ./deploy/release.sh systemd
  ./deploy/release.sh compose

Environment variables:
  SKIP_PULL=1            Skip git pull
  SYSTEMD_SERVICE=name   Override systemd service name
  ENV_FILE=.env.prod     Override compose env file
  COMPOSE_FILE=compose.prod.yaml
  HEALTH_PATH=/actuator/health
  APP_PORT=8080
  HEALTH_RETRIES=30
  HEALTH_INTERVAL_SECONDS=2
  HEALTH_TIMEOUT_SECONDS=5
EOF
}

log() {
  printf '[release] %s\n' "$1"
}

run() {
  log "$*"
  "$@"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    printf 'Missing command: %s\n' "$1" >&2
    exit 1
  }
}

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi

  printf 'Missing docker compose command. Install docker compose plugin or docker-compose.\n' >&2
  exit 1
}

compose_up_build() {
  log "DOCKER_BUILDKIT=${DOCKER_BUILDKIT:-1} COMPOSE_DOCKER_CLI_BUILD=${COMPOSE_DOCKER_CLI_BUILD:-1} compose up -d --build"
  DOCKER_BUILDKIT="${DOCKER_BUILDKIT:-1}" COMPOSE_DOCKER_CLI_BUILD="${COMPOSE_DOCKER_CLI_BUILD:-1}" \
    compose_cmd "$@"
}

check_http_health() {
  local url="$1"
  require_cmd curl
  local attempt=1

  while [[ "$attempt" -le "$HEALTH_RETRIES" ]]; do
    log "Checking health: $url (attempt ${attempt}/${HEALTH_RETRIES})"
    if curl --fail --silent --show-error --max-time "$HEALTH_TIMEOUT_SECONDS" "$url" >/dev/null; then
      log "Health check passed: $url"
      return 0
    fi

    if [[ "$attempt" -lt "$HEALTH_RETRIES" ]]; then
      sleep "$HEALTH_INTERVAL_SECONDS"
    fi
    attempt=$((attempt + 1))
  done

  log "Health check failed after ${HEALTH_RETRIES} attempts: $url"
  return 1
}

git_pull_if_needed() {
  if [[ "$SKIP_PULL" == "1" ]]; then
    log "Skipping git pull"
    return
  fi

  run bash -lc "cd \"$ROOT_DIR\" && git pull --ff-only"
  if [[ -d "$FRONTEND_ROOT/.git" ]]; then
    run bash -lc "cd \"$FRONTEND_ROOT\" && git pull --ff-only"
  else
    log "Skipping frontend pull: $FRONTEND_ROOT is not a git repository"
  fi
}

deploy_systemd() {
  require_cmd git
  require_cmd docker
  require_cmd java
  require_cmd sudo

  git_pull_if_needed
  run compose_cmd -f "$ROOT_DIR/compose.yaml" up -d
  run "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" clean package -DskipTests
  run sudo systemctl restart "$SYSTEMD_SERVICE"
  run sudo systemctl status "$SYSTEMD_SERVICE" --no-pager
  check_http_health "http://127.0.0.1:${APP_PORT}${HEALTH_PATH}"
}

deploy_compose() {
  require_cmd git
  require_cmd docker

  if [[ ! -f "$ROOT_DIR/$ENV_FILE" ]]; then
    printf 'Missing env file: %s/%s\n' "$ROOT_DIR" "$ENV_FILE" >&2
    printf 'Create it from .env.prod.example first.\n' >&2
    exit 1
  fi

  git_pull_if_needed
  compose_up_build --env-file "$ROOT_DIR/$ENV_FILE" -f "$ROOT_DIR/$COMPOSE_FILE" up -d --build
  run compose_cmd --env-file "$ROOT_DIR/$ENV_FILE" -f "$ROOT_DIR/$COMPOSE_FILE" ps
  if ! check_http_health "http://127.0.0.1${HEALTH_PATH}"; then
    run compose_cmd --env-file "$ROOT_DIR/$ENV_FILE" -f "$ROOT_DIR/$COMPOSE_FILE" ps || true
    run compose_cmd --env-file "$ROOT_DIR/$ENV_FILE" -f "$ROOT_DIR/$COMPOSE_FILE" logs app --tail=200 || true
    exit 1
  fi
}

main() {
  if [[ -z "$MODE" ]]; then
    usage
    exit 1
  fi

  case "$MODE" in
    systemd)
      deploy_systemd
      ;;
    compose)
      deploy_compose
      ;;
    -h|--help|help)
      usage
      ;;
    *)
      printf 'Unknown mode: %s\n\n' "$MODE" >&2
      usage
      exit 1
      ;;
  esac
}

main
