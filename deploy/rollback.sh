#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-}"
TARGET_REF="${2:-}"
SYSTEMD_SERVICE="${SYSTEMD_SERVICE:-driveserver}"
ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health}"
APP_PORT="${APP_PORT:-8080}"

usage() {
  cat <<'EOF'
Usage:
  ./deploy/rollback.sh systemd <git-ref>
  ./deploy/rollback.sh compose <git-ref>

Examples:
  ./deploy/rollback.sh systemd HEAD~1
  ./deploy/rollback.sh compose a1b2c3d

Environment variables:
  SYSTEMD_SERVICE=driveserver
  ENV_FILE=.env.prod
  COMPOSE_FILE=compose.prod.yaml
  HEALTH_PATH=/actuator/health
  APP_PORT=8080
EOF
}

log() {
  printf '[rollback] %s\n' "$1"
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
  log "Checking health: $url"
  curl --fail --silent "$url"
  printf '\n'
}

ensure_clean_worktree() {
  if [[ -n "$(cd "$ROOT_DIR" && git status --porcelain)" ]]; then
    printf 'Working tree has local changes. Commit or stash them before rollback.\n' >&2
    exit 1
  fi
}

checkout_ref() {
  run bash -lc "cd \"$ROOT_DIR\" && git fetch --all --tags"
  run bash -lc "cd \"$ROOT_DIR\" && git checkout \"$TARGET_REF\""

  if ! (cd "$ROOT_DIR" && git symbolic-ref -q HEAD >/dev/null); then
    log "Repository is now in detached HEAD state at $TARGET_REF"
    log "When you want to resume normal releases, switch back to your deploy branch first"
  fi
}

rollback_systemd() {
  require_cmd git
  require_cmd docker
  require_cmd java
  require_cmd sudo

  ensure_clean_worktree
  checkout_ref
  run compose_cmd -f "$ROOT_DIR/compose.yaml" up -d
  run "$ROOT_DIR/mvnw" -f "$ROOT_DIR/pom.xml" clean package -DskipTests
  run sudo systemctl restart "$SYSTEMD_SERVICE"
  run sudo systemctl status "$SYSTEMD_SERVICE" --no-pager
  check_http_health "http://127.0.0.1:${APP_PORT}${HEALTH_PATH}"
}

rollback_compose() {
  require_cmd git
  require_cmd docker

  if [[ ! -f "$ROOT_DIR/$ENV_FILE" ]]; then
    printf 'Missing env file: %s/%s\n' "$ROOT_DIR" "$ENV_FILE" >&2
    exit 1
  fi

  ensure_clean_worktree
  checkout_ref
  compose_up_build --env-file "$ROOT_DIR/$ENV_FILE" -f "$ROOT_DIR/$COMPOSE_FILE" up -d --build
  run compose_cmd --env-file "$ROOT_DIR/$ENV_FILE" -f "$ROOT_DIR/$COMPOSE_FILE" ps
  check_http_health "http://127.0.0.1${HEALTH_PATH}"
}

main() {
  if [[ -z "$MODE" || -z "$TARGET_REF" ]]; then
    usage
    exit 1
  fi

  case "$MODE" in
    systemd)
      rollback_systemd
      ;;
    compose)
      rollback_compose
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
