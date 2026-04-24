# DriveServer 云服务器部署手册

本文档面向“把当前仓库部署到一台云服务器并可稳定运行”的场景，适合 `Ubuntu 22.04/24.04` 和 `CentOS 7.9 64位` 单机部署。

说明：
1. `CentOS 7.9` 已停止维护，若是新环境，长期建议迁移到更新系统
2. 仓库中的 `deploy/bootstrap.sh` 已兼容 `apt` 和 `yum`
3. 在 `CentOS 7.9` 上，脚本会优先使用 `docker compose`，若插件不可用则回退为 `docker-compose`

仓库内已补充以下生产部署模板：
1. [`Dockerfile`](/Users/m1ngyangg/Documents/DriveServer/Dockerfile)
2. [`compose.prod.yaml`](/Users/m1ngyangg/Documents/DriveServer/compose.prod.yaml)
3. [`.env.prod.example`](/Users/m1ngyangg/Documents/DriveServer/.env.prod.example)
4. [`deploy/nginx/default.conf`](/Users/m1ngyangg/Documents/DriveServer/deploy/nginx/default.conf)
5. [`deploy/release.sh`](/Users/m1ngyangg/Documents/DriveServer/deploy/release.sh)
6. [`deploy/bootstrap.sh`](/Users/m1ngyangg/Documents/DriveServer/deploy/bootstrap.sh)
7. [`deploy/rollback.sh`](/Users/m1ngyangg/Documents/DriveServer/deploy/rollback.sh)

## 1. 两条部署路径
本文档包含两条可选部署路线，你可以二选一：

### 方式 A：`jar + systemd + Nginx`
适合：
1. 希望应用进程由宿主机直接托管
2. 想把数据库、缓存、应用分开管理
3. 更习惯传统 Linux 服务部署方式

组成：
1. `Docker Compose` 启动依赖服务：`MySQL`、`Redis`、`InfluxDB 3`
2. `Spring Boot` 打包为 `jar`，由 `systemd` 托管运行
3. `Nginx` 对外提供 `HTTP/HTTPS` 和反向代理

### 方式 B：`Docker Compose` 整套容器化
适合：
1. 希望整套服务统一用容器托管
2. 需要更简单的迁移、复制和重建方式
3. 想减少宿主机上的 Java 和 systemd 配置

组成：
1. 使用 [`compose.prod.yaml`](/Users/m1ngyangg/Documents/DriveServer/compose.prod.yaml) 统一启动 `app + mysql + redis + influxdb3 + nginx`
2. 使用 [`Dockerfile`](/Users/m1ngyangg/Documents/DriveServer/Dockerfile) 构建应用镜像
3. 使用 [`.env.prod.example`](/Users/m1ngyangg/Documents/DriveServer/.env.prod.example) 管理生产环境变量

建议：
1. 想先快速上线单机环境，优先选方式 B
2. 想更贴近传统生产机运维方式，优先选方式 A

## 2. 服务器准备
建议配置：
1. 操作系统：`Ubuntu 22.04/24.04` 或 `CentOS 7.9 64位`
2. CPU：`2 核` 起步
3. 内存：`4 GB` 起步
4. 磁盘：`40 GB` 起步

安全组/防火墙建议开放：
1. `22`：SSH
2. `80`：HTTP
3. `443`：HTTPS

不建议直接对公网开放：
1. `3306`：MySQL
2. `6379`：Redis
3. `8181`：InfluxDB
4. `8080`：Spring Boot 应用端口

## 3. 安装基础软件
Ubuntu：
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk docker.io docker-compose-plugin nginx curl git
sudo systemctl enable --now docker
```

CentOS 7.9：
```bash
sudo yum install -y curl git nginx
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io
sudo systemctl enable --now docker
```

如果你的 `CentOS 7.9` 仓库里没有 `java-17-openjdk`，说明系统源本身不提供 Java 17。此时分两种情况：

1. 如果你走 `compose` 路线：可以先不装 Java 17，宿主机不需要它
2. 如果你走 `systemd + jar` 路线：改用 Temurin 17 二进制安装

`systemd + jar` 路线可执行：
```bash
sudo mkdir -p /opt/java
curl -L "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk" -o /tmp/jdk17.tar.gz
sudo tar -xzf /tmp/jdk17.tar.gz -C /opt/java
sudo ln -sfn /opt/java/$(tar -tzf /tmp/jdk17.tar.gz | head -n 1 | cut -d/ -f1) /opt/java/jdk17
sudo ln -sfn /opt/java/jdk17/bin/java /usr/local/bin/java
sudo ln -sfn /opt/java/jdk17/bin/javac /usr/local/bin/javac
java -version
```

如果 `CentOS 7.9` 上没有 `docker compose` 子命令，可安装独立版 `docker-compose`：
```bash
sudo curl -L "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

验证：
```bash
java -version
docker -v
docker compose version || docker-compose version
nginx -v
git --version
curl --version
```

## 4. 上传代码
推荐部署目录：
```bash
sudo mkdir -p /opt/DriveServer
sudo chown -R $USER:$USER /opt/DriveServer
git clone <你的仓库地址> /opt/DriveServer
cd /opt/DriveServer
```

如果不是通过 Git，也可以将本地代码打包上传后解压到 `/opt/DriveServer`。

## 5. 方式 A：`jar + systemd + Nginx`
这一方式下，数据库和缓存通过 Docker 运行，应用本体通过宿主机 Java 运行。

### 5.1 首次初始化
首次部署时可以直接运行：

```bash
cd /opt/DriveServer
chmod +x deploy/bootstrap.sh deploy/release.sh
./deploy/bootstrap.sh systemd
```

脚本会尝试完成：
1. 安装 `Java 17`、`Docker`、`Docker Compose`、`Nginx`、`curl`
2. 写入 `/etc/driveserver/driveserver.env` 模板
3. 写入 `systemd` 服务文件
4. 写入 `Nginx` 站点配置
5. 启动 `compose.yaml` 中的依赖服务

在 `CentOS 7.9` 上：
1. `Nginx` 配置默认会写到 `/etc/nginx/conf.d/driveserver.conf`
2. 如果没有 `docker compose` 插件，脚本会尝试安装独立版 `docker-compose`
3. 如果系统源没有 Java 17，脚本会自动回退到 Temurin 17 二进制安装

初始化完成后：
1. 打开 `/etc/driveserver/driveserver.env`
2. 修改真实的 `DB_PASSWORD` 和 `JWT_SECRET`
3. 再执行 `./deploy/release.sh systemd`

可选环境变量：
```bash
SERVER_NAME=api.example.com ./deploy/bootstrap.sh systemd
APP_PORT=8080 ./deploy/bootstrap.sh systemd
INSTALL_PACKAGES=0 ./deploy/bootstrap.sh systemd
```

### 5.2 启动依赖服务
项目根目录已有 [`compose.yaml`](/Users/m1ngyangg/Documents/DriveServer/compose.yaml)，可直接启动：

```bash
cd /opt/DriveServer
docker compose up -d
docker compose ps
```

期望状态：
1. `driveserver-mysql` 为 `healthy`
2. `driveserver-redis` 为 `healthy`
3. `driveserver-influxdb3` 为 `running`

必要时检查：
```bash
docker compose logs mysql --tail=100
docker compose logs redis --tail=100
docker compose logs influxdb3 --tail=100
```

### 5.3 构建应用
项目使用 `Maven` 和 `Java 17`，直接执行：

```bash
cd /opt/DriveServer
chmod +x mvnw
./mvnw clean package -DskipTests
```

构建完成后，产物通常位于：
```bash
target/*.jar
```

### 5.4 生产环境变量
当前项目会从环境变量读取数据库、Redis 和鉴权配置，关键项如下：

```bash
DB_URL=jdbc:mysql://127.0.0.1:3306/drive_server?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=请替换为生产密码
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
SERVER_PORT=8080
JWT_SECRET=请替换为足够长的生产密钥
JWT_EXPIRE_SECONDS=7200
JWT_ISSUER=DriveServer
```

建议单独创建环境变量文件：
```bash
sudo mkdir -p /etc/driveserver
sudo tee /etc/driveserver/driveserver.env >/dev/null <<'EOF'
DB_URL=jdbc:mysql://127.0.0.1:3306/drive_server?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=请替换为生产密码
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
SERVER_PORT=8080
JWT_SECRET=请替换为足够长的生产密钥
JWT_EXPIRE_SECONDS=7200
JWT_ISSUER=DriveServer
EOF

sudo chmod 600 /etc/driveserver/driveserver.env
```

### 5.5 首次手动启动验证
在交给 `systemd` 托管前，建议先手动验证一次：

```bash
cd /opt/DriveServer
set -a
source /etc/driveserver/driveserver.env
set +a
java -jar target/*.jar
```

启动成功后，检查：
```bash
curl http://127.0.0.1:8080/actuator/health
```

说明：
1. 首次启动会自动执行 `Flyway` migration
2. 如数据库为空，会初始化默认表结构和种子数据
3. 如果这里启动失败，不要先配 Nginx，应先解决应用本身问题

### 5.6 使用 systemd 托管应用
创建服务文件：

```bash
sudo tee /etc/systemd/system/driveserver.service >/dev/null <<'EOF'
[Unit]
Description=DriveServer Spring Boot Service
After=network.target docker.service
Requires=docker.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/DriveServer
EnvironmentFile=/etc/driveserver/driveserver.env
ExecStart=/bin/sh -c '/usr/bin/java -jar /opt/DriveServer/target/*.jar'
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

启动并设置开机自启：
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now driveserver
sudo systemctl status driveserver
```

常用命令：
```bash
sudo systemctl restart driveserver
sudo systemctl stop driveserver
sudo journalctl -u driveserver -n 200 --no-pager
```

### 5.7 配置 Nginx 反向代理
创建配置文件：

```bash
sudo tee /etc/nginx/sites-available/driveserver >/dev/null <<'EOF'
server {
    listen 80;
    server_name 你的域名或公网 IP;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
EOF
```

启用配置：
```bash
sudo ln -sf /etc/nginx/sites-available/driveserver /etc/nginx/sites-enabled/driveserver
sudo nginx -t
sudo systemctl reload nginx
```

如果后续绑定域名，建议再接入 `Let's Encrypt` 证书。

### 5.8 上线验收
依次执行：

```bash
docker compose ps
sudo systemctl status driveserver
curl http://127.0.0.1:8080/actuator/health
curl http://你的域名或公网IP/actuator/health
```

重点检查：
1. 应用健康检查返回 `UP`
2. MySQL、Redis、InfluxDB 容器正常运行
3. Nginx 能成功转发到应用
4. 登录接口、业务接口、WebSocket 能正常访问

### 5.9 一键发布
仓库内已提供一键发布脚本：

```bash
cd /opt/DriveServer
chmod +x deploy/release.sh
./deploy/release.sh systemd
```

脚本会按顺序执行：
1. `git pull --ff-only`
2. `docker compose up -d`
3. `./mvnw clean package -DskipTests`
4. `sudo systemctl restart driveserver`
5. `curl http://127.0.0.1:8080/actuator/health`

可选环境变量：
```bash
SKIP_PULL=1 ./deploy/release.sh systemd
SYSTEMD_SERVICE=driveserver ./deploy/release.sh systemd
APP_PORT=8080 ./deploy/release.sh systemd
```

### 5.10 一键回滚
仓库内已提供一键回滚脚本：

```bash
cd /opt/DriveServer
chmod +x deploy/rollback.sh
./deploy/rollback.sh systemd HEAD~1
```

也可以回滚到指定提交：
```bash
./deploy/rollback.sh systemd <git提交号>
```

脚本会按顺序执行：
1. 检查工作区是否干净
2. `git fetch --all --tags`
3. `git checkout <目标版本>`
4. `docker compose up -d`
5. `./mvnw clean package -DskipTests`
6. `sudo systemctl restart driveserver`
7. `curl http://127.0.0.1:8080/actuator/health`

可选环境变量：
```bash
SYSTEMD_SERVICE=driveserver ./deploy/rollback.sh systemd HEAD~1
APP_PORT=8080 ./deploy/rollback.sh systemd HEAD~1
```

注意：
1. 如果回滚目标是具体提交号而不是分支名，Git 可能进入 detached HEAD
2. 后续恢复正常发布前，记得先切回你的发布分支，再执行 `./deploy/release.sh systemd`

## 6. 方式 B：`Docker Compose` 整套容器化
这一方式下，应用、数据库、缓存和 Nginx 都由 Docker Compose 统一托管。

### 6.1 首次初始化
首次部署时可以直接运行：

```bash
cd /opt/DriveServer
chmod +x deploy/bootstrap.sh deploy/release.sh
./deploy/bootstrap.sh compose
```

脚本会尝试完成：
1. 安装 `Java 17`、`Docker`、`Docker Compose`、`Nginx`、`curl`
2. 从 `.env.prod.example` 生成 `.env.prod`

在 `CentOS 7.9` 上：
1. 脚本会优先安装 `docker-ce`
2. `docker compose` 不可用时会回退为独立版 `docker-compose`
3. `compose` 路线不依赖宿主机 Java 17

初始化完成后：
1. 打开 `.env.prod`
2. 修改真实的 `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD`、`JWT_SECRET`
3. 再执行 `./deploy/release.sh compose`

可选环境变量：
```bash
ENV_FILE=.env.prod ./deploy/bootstrap.sh compose
INSTALL_PACKAGES=0 ./deploy/bootstrap.sh compose
```

### 6.2 生产模板说明
仓库内已提供：
1. [`Dockerfile`](/Users/m1ngyangg/Documents/DriveServer/Dockerfile)
2. [`compose.prod.yaml`](/Users/m1ngyangg/Documents/DriveServer/compose.prod.yaml)
3. [`.env.prod.example`](/Users/m1ngyangg/Documents/DriveServer/.env.prod.example)
4. [`deploy/nginx/default.conf`](/Users/m1ngyangg/Documents/DriveServer/deploy/nginx/default.conf)

如果你希望直接构建生产镜像，也可以在项目根目录执行：

```bash
docker build -t driveserver-app:latest .
```

### 6.3 准备环境变量

```bash
cd /opt/DriveServer
cp .env.prod.example .env.prod
```

然后编辑 `.env.prod`，至少修改：
1. `MYSQL_ROOT_PASSWORD`
2. `DB_PASSWORD`
3. `JWT_SECRET`

### 6.4 启动整套服务
```bash
cd /opt/DriveServer
docker compose --env-file .env.prod -f compose.prod.yaml up -d --build
docker compose --env-file .env.prod -f compose.prod.yaml ps
```

### 6.5 查看日志
```bash
docker compose --env-file .env.prod -f compose.prod.yaml logs app --tail=100
docker compose --env-file .env.prod -f compose.prod.yaml logs nginx --tail=100
```

### 6.6 上线验收
```bash
docker compose --env-file .env.prod -f compose.prod.yaml ps
curl http://127.0.0.1/actuator/health
curl http://你的域名或公网IP/actuator/health
```

重点检查：
1. `app`、`mysql`、`redis`、`influxdb3`、`nginx` 均正常运行
2. 健康检查返回 `UP`
3. Nginx 能成功转发到应用
4. 登录接口、业务接口、WebSocket 能正常访问

如果已经采用这一方式，一般不需要再单独创建 `driveserver.service`。

### 6.7 一键发布
仓库内已提供一键发布脚本：

```bash
cd /opt/DriveServer
chmod +x deploy/release.sh
./deploy/release.sh compose
```

脚本会按顺序执行：
1. `git pull --ff-only`
2. `docker compose --env-file .env.prod -f compose.prod.yaml up -d --build`
3. `docker compose --env-file .env.prod -f compose.prod.yaml ps`
4. `curl http://127.0.0.1/actuator/health`

可选环境变量：
```bash
SKIP_PULL=1 ./deploy/release.sh compose
ENV_FILE=.env.prod ./deploy/release.sh compose
COMPOSE_FILE=compose.prod.yaml ./deploy/release.sh compose
```

### 6.8 一键回滚
仓库内已提供一键回滚脚本：

```bash
cd /opt/DriveServer
chmod +x deploy/rollback.sh
./deploy/rollback.sh compose HEAD~1
```

也可以回滚到指定提交：
```bash
./deploy/rollback.sh compose <git提交号>
```

脚本会按顺序执行：
1. 检查工作区是否干净
2. `git fetch --all --tags`
3. `git checkout <目标版本>`
4. `docker compose --env-file .env.prod -f compose.prod.yaml up -d --build`
5. `curl http://127.0.0.1/actuator/health`

可选环境变量：
```bash
ENV_FILE=.env.prod ./deploy/rollback.sh compose HEAD~1
COMPOSE_FILE=compose.prod.yaml ./deploy/rollback.sh compose HEAD~1
```

注意：
1. 如果回滚目标是具体提交号而不是分支名，Git 可能进入 detached HEAD
2. 后续恢复正常发布前，记得先切回你的发布分支，再执行 `./deploy/release.sh compose`

## 7. 发布更新
代码更新后的常规发布流程：

### 7.1 方式 A 更新流程
```bash
cd /opt/DriveServer
git pull
./mvnw clean package -DskipTests
sudo systemctl restart driveserver
sudo systemctl status driveserver
```

如果依赖服务配置有变化，再执行：
```bash
docker compose up -d
```

### 7.2 方式 B 更新流程
```bash
cd /opt/DriveServer
git pull
docker compose --env-file .env.prod -f compose.prod.yaml up -d --build
docker compose --env-file .env.prod -f compose.prod.yaml ps
```

## 8. 回滚思路
如果新版本启动失败，优先使用仓库内的 `deploy/rollback.sh`。

手工回滚时可参考：
1. 先查看 `journalctl`、应用日志或 `docker compose logs`
2. 找到上一个稳定 Git 提交
3. 切回该提交并重新部署

方式 A 手工回滚：
```bash
cd /opt/DriveServer
git log --oneline -n 5
git checkout <上一个稳定提交>
./mvnw clean package -DskipTests
sudo systemctl restart driveserver
```

方式 B 手工回滚：
```bash
cd /opt/DriveServer
git log --oneline -n 5
git checkout <上一个稳定提交>
docker compose --env-file .env.prod -f compose.prod.yaml up -d --build
```

如果数据库 migration 已执行，回滚前需要先确认是否包含破坏性变更。

## 9. 常见问题
### 9.1 应用启动失败
排查顺序：
1. `sudo journalctl -u driveserver -n 200 --no-pager`
2. 检查 `/etc/driveserver/driveserver.env`
3. 检查 MySQL/Redis 容器是否存活
4. 检查 `target/*.jar` 是否存在

### 9.2 数据库连接失败
检查：
1. `DB_URL` 是否指向 `127.0.0.1:3306`
2. `DB_USERNAME` / `DB_PASSWORD` 是否正确
3. `docker compose ps` 中 MySQL 是否为 `healthy`

### 9.3 Redis 连接失败
检查：
1. `REDIS_HOST` 是否为 `127.0.0.1`
2. `REDIS_PORT` 是否为 `6379`
3. Redis 容器是否健康

### 9.4 WebSocket 不通
检查：
1. Nginx 是否配置了 `Upgrade` 和 `Connection` 头
2. 前端连接路径是否与后端端点一致
3. 反向代理是否走了正确域名和端口

### 9.5 Docker 镜像构建失败
检查：
1. 服务器是否已安装并启动 Docker
2. 项目根目录是否包含完整源码、`pom.xml` 和 `.mvn`
3. 是否因网络原因无法拉取基础镜像

## 10. 生产环境建议
上线前建议至少完成以下项：
1. 修改 MySQL 默认 root 密码
2. 修改默认 `JWT_SECRET`
3. 限制数据库和 Redis 仅内网访问
4. 配置 HTTPS
5. 配置自动备份
6. 配置日志留存和监控告警

如果后面需要进一步规范化部署，下一步建议是补齐：
1. `Dockerfile`
2. 生产专用 `compose.prod.yaml`
3. CI/CD 发布脚本
4. 自动备份与监控方案
