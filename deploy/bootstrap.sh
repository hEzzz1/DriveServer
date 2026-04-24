#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-}"
SYSTEMD_SERVICE="${SYSTEMD_SERVICE:-driveserver}"
ENV_FILE="${ENV_FILE:-.env.prod}"
ENV_TARGET="${ENV_TARGET:-/etc/driveserver/driveserver.env}"
NGINX_SITE="${NGINX_SITE:-}"
APP_PORT="${APP_PORT:-8080}"
SERVER_NAME="${SERVER_NAME:-_}"
INSTALL_PACKAGES="${INSTALL_PACKAGES:-1}"
DOCKER_COMPOSE_VERSION="${DOCKER_COMPOSE_VERSION:-2.29.7}"
JAVA_DIST_DIR="${JAVA_DIST_DIR:-/opt/java}"
JAVA_DOWNLOAD_URL="${JAVA_DOWNLOAD_URL:-https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk}"

usage() {
  cat <<'EOF'
Usage:
  ./deploy/bootstrap.sh systemd
  ./deploy/bootstrap.sh compose

Environment variables:
  SYSTEMD_SERVICE=driveserver
  ENV_FILE=.env.prod
  ENV_TARGET=/etc/driveserver/driveserver.env
  NGINX_SITE=/etc/nginx/conf.d/driveserver.conf
  APP_PORT=8080
  SERVER_NAME=_
  INSTALL_PACKAGES=1
  DOCKER_COMPOSE_VERSION=2.29.7
  JAVA_DIST_DIR=/opt/java
  JAVA_DOWNLOAD_URL=https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk
EOF
}

log() {
  printf '[bootstrap] %s\n' "$1"
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

detect_package_manager() {
  if command -v apt-get >/dev/null 2>&1; then
    printf 'apt'
    return
  fi

  if command -v yum >/dev/null 2>&1; then
    printf 'yum'
    return
  fi

  printf 'unknown'
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

detect_nginx_site() {
  if [[ -n "$NGINX_SITE" ]]; then
    printf '%s' "$NGINX_SITE"
    return
  fi

  if [[ -d /etc/nginx/conf.d ]]; then
    printf '/etc/nginx/conf.d/driveserver.conf'
    return
  fi

  printf '/etc/nginx/sites-available/driveserver'
}

install_docker_compose_standalone() {
  local os arch target
  require_cmd sudo
  require_cmd curl

  if command -v docker-compose >/dev/null 2>&1; then
    return
  fi

  os="$(uname -s)"
  arch="$(uname -m)"
  target="/usr/local/bin/docker-compose"

  run sudo curl -L \
    "https://github.com/docker/compose/releases/download/v${DOCKER_COMPOSE_VERSION}/docker-compose-${os}-${arch}" \
    -o "$target"
  run sudo chmod +x "$target"
}

install_java_17_from_tarball() {
  local tmp_dir archive_path extracted_dir java_home
  require_cmd sudo
  require_cmd curl
  require_cmd tar

  tmp_dir="$(mktemp -d)"
  archive_path="${tmp_dir}/jdk17.tar.gz"

  run sudo mkdir -p "$JAVA_DIST_DIR"
  run curl -L "$JAVA_DOWNLOAD_URL" -o "$archive_path"
  run sudo tar -xzf "$archive_path" -C "$JAVA_DIST_DIR"

  extracted_dir="$(tar -tzf "$archive_path" | head -n 1 | cut -d/ -f1)"
  java_home="${JAVA_DIST_DIR}/${extracted_dir}"

  run sudo ln -sfn "${java_home}" "${JAVA_DIST_DIR}/jdk17"
  run sudo ln -sfn "${JAVA_DIST_DIR}/jdk17/bin/java" /usr/local/bin/java
  run sudo ln -sfn "${JAVA_DIST_DIR}/jdk17/bin/javac" /usr/local/bin/javac
}

ensure_java_17_apt() {
  require_cmd sudo
  run sudo apt install -y openjdk-17-jdk
}

ensure_java_17_yum() {
  if rpm -q java-17-openjdk-devel >/dev/null 2>&1 || rpm -q java-17-openjdk >/dev/null 2>&1; then
    return
  fi

  if sudo yum install -y java-17-openjdk-devel; then
    return
  fi

  if sudo yum install -y java-17-openjdk; then
    return
  fi

  log "Yum repo does not provide Java 17, falling back to Temurin 17 tarball"
  install_java_17_from_tarball
}

ensure_packages_apt() {
  require_cmd sudo
  run sudo apt update
  run sudo apt install -y docker.io docker-compose-plugin nginx curl git
  ensure_java_17_apt
  run sudo systemctl enable --now docker
}

ensure_packages_yum() {
  require_cmd sudo
  run sudo yum install -y curl git nginx

  if ! command -v docker >/dev/null 2>&1; then
    run sudo yum install -y yum-utils device-mapper-persistent-data lvm2
    run sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    run sudo yum install -y docker-ce docker-ce-cli containerd.io
  fi

  run sudo systemctl enable --now docker

  if ! docker compose version >/dev/null 2>&1 && ! command -v docker-compose >/dev/null 2>&1; then
    install_docker_compose_standalone
  fi
}

ensure_base_packages() {
  local package_manager

  if [[ "$INSTALL_PACKAGES" != "1" ]]; then
    log "Skipping package installation"
    return
  fi

  package_manager="$(detect_package_manager)"

  case "$package_manager" in
    apt)
      ensure_packages_apt
      ;;
    yum)
      ensure_packages_yum
      ;;
    *)
      printf 'Unsupported package manager. Set INSTALL_PACKAGES=0 and install dependencies manually.\n' >&2
      exit 1
      ;;
  esac
}

ensure_java_17() {
  local package_manager

  package_manager="$(detect_package_manager)"

  case "$package_manager" in
    apt)
      ensure_java_17_apt
      ;;
    yum)
      ensure_java_17_yum
      ;;
    *)
      printf 'Unsupported package manager for Java 17 installation.\n' >&2
      exit 1
      ;;
  esac
}

ensure_compose_env() {
  if [[ -f "$ROOT_DIR/$ENV_FILE" ]]; then
    log "Keeping existing env file: $ROOT_DIR/$ENV_FILE"
    return
  fi

  run cp "$ROOT_DIR/.env.prod.example" "$ROOT_DIR/$ENV_FILE"
  log "Created $ROOT_DIR/$ENV_FILE from template. Fill in real secrets before release."
}

write_systemd_env_template() {
  require_cmd sudo
  run sudo mkdir -p "$(dirname "$ENV_TARGET")"

  if sudo test -f "$ENV_TARGET"; then
    log "Keeping existing env file: $ENV_TARGET"
    return
  fi

  log "Writing env template to $ENV_TARGET"
  sudo tee "$ENV_TARGET" >/dev/null <<EOF
DB_URL=jdbc:mysql://127.0.0.1:3306/drive_server?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=change-me-strong-password
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
SERVER_PORT=${APP_PORT}
JWT_SECRET=change-me-to-a-long-random-secret
JWT_EXPIRE_SECONDS=7200
JWT_ISSUER=DriveServer
EOF

  run sudo chmod 600 "$ENV_TARGET"
}

write_systemd_unit() {
  require_cmd sudo
  log "Writing systemd unit: /etc/systemd/system/${SYSTEMD_SERVICE}.service"
  sudo tee "/etc/systemd/system/${SYSTEMD_SERVICE}.service" >/dev/null <<EOF
[Unit]
Description=DriveServer Spring Boot Service
After=network.target docker.service
Requires=docker.service

[Service]
Type=simple
User=root
WorkingDirectory=${ROOT_DIR}
EnvironmentFile=${ENV_TARGET}
Environment=PATH=/usr/local/bin:/usr/bin:/bin
ExecStart=/bin/sh -c 'exec java -jar ${ROOT_DIR}/target/*.jar'
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

  run sudo systemctl daemon-reload
  run sudo systemctl enable "$SYSTEMD_SERVICE"
}

write_nginx_site() {
  local nginx_site

  require_cmd sudo
  nginx_site="$(detect_nginx_site)"
  run sudo mkdir -p "$(dirname "$nginx_site")"
  log "Writing nginx site: $nginx_site"
  sudo tee "$nginx_site" >/dev/null <<EOF
server {
    listen 80;
    server_name ${SERVER_NAME};

    location / {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
    }
}
EOF

  if [[ -d /etc/nginx/sites-enabled ]]; then
    run sudo ln -sf "$nginx_site" /etc/nginx/sites-enabled/driveserver
  fi

  run sudo nginx -t
  run sudo systemctl enable --now nginx
  run sudo systemctl reload nginx
}

bootstrap_systemd() {
  ensure_base_packages
  ensure_java_17
  require_cmd docker
  require_cmd git
  require_cmd sudo
  write_systemd_env_template
  write_systemd_unit
  write_nginx_site
  run compose_cmd -f "$ROOT_DIR/compose.yaml" up -d
  log "Bootstrap finished. Edit $ENV_TARGET with real secrets, then run ./deploy/release.sh systemd"
}

bootstrap_compose() {
  ensure_base_packages
  require_cmd docker
  require_cmd git
  ensure_compose_env
  log "Bootstrap finished. Edit $ROOT_DIR/$ENV_FILE with real secrets, then run ./deploy/release.sh compose"
}

main() {
  if [[ -z "$MODE" ]]; then
    usage
    exit 1
  fi

  case "$MODE" in
    systemd)
      bootstrap_systemd
      ;;
    compose)
      bootstrap_compose
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
