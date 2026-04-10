# DriveServer 全量环境部署手册（Spring Boot 3.5.x）

本文档覆盖“从零安装到可联调运行”的完整步骤，适用于本项目当前代码与配置。

## 1. 部署目标
完成以下环境可用性：
1. 应用运行：Spring Boot 3.5.x（Java 17）
2. 关系库：MySQL 8.4
3. 缓存：Redis 7.2
4. 时序库：InfluxDB 3 Core
5. 数据迁移：Flyway
6. 本地测试：Maven + JUnit

## 2. 目录与关键文件
项目根目录：
1. [`compose.yaml`](/Users/m1ngyangg/Documents/DriveServer/compose.yaml)
2. [`pom.xml`](/Users/m1ngyangg/Documents/DriveServer/pom.xml)
3. [`src/main/resources/application.yaml`](/Users/m1ngyangg/Documents/DriveServer/src/main/resources/application.yaml)
4. [`src/main/resources/application-local.yaml`](/Users/m1ngyangg/Documents/DriveServer/src/main/resources/application-local.yaml)
5. [`src/main/resources/logback-spring.xml`](/Users/m1ngyangg/Documents/DriveServer/src/main/resources/logback-spring.xml)
6. [`src/main/resources/db/migration/V1__init_core_tables.sql`](/Users/m1ngyangg/Documents/DriveServer/src/main/resources/db/migration/V1__init_core_tables.sql)
7. [`src/main/resources/db/migration/V2__seed_default_data.sql`](/Users/m1ngyangg/Documents/DriveServer/src/main/resources/db/migration/V2__seed_default_data.sql)
8. [`src/main/resources/db/migration/V3__init_auth_rbac.sql`](/Users/m1ngyangg/Documents/DriveServer/src/main/resources/db/migration/V3__init_auth_rbac.sql)
9. [`src/main/resources/db/migration/V4__strengthen_user_rule_alert_audit_schema.sql`](/Users/m1ngyangg/Documents/DriveServer/src/main/resources/db/migration/V4__strengthen_user_rule_alert_audit_schema.sql)
10. [`src/test/resources/application.yaml`](/Users/m1ngyangg/Documents/DriveServer/src/test/resources/application.yaml)

## 3. 软件安装
建议版本：
1. JDK 17
2. Maven 3.9+
3. Docker + Docker Compose
4. IntelliJ IDEA

macOS 安装示例：
```bash
brew install --cask temurin17
brew install maven
brew install --cask docker
```

如果 `docker` 命令找不到：
```bash
touch ~/.zshrc
grep -qxF 'export PATH="/opt/homebrew/bin:$PATH"' ~/.zshrc || echo 'export PATH="/opt/homebrew/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

验证：
```bash
java -version
mvn -v
docker -v
docker compose version
```

## 4. 启动依赖服务（MySQL / Redis / InfluxDB）
在项目根目录执行：
```bash
cd /Users/m1ngyangg/Documents/DriveServer
docker compose up -d
docker compose ps
```

期望状态：
1. `driveserver-mysql` 为 `healthy`
2. `driveserver-redis` 为 `healthy`
3. `driveserver-influxdb3` 为 `up`

停止服务：
```bash
docker compose down
```

清空数据并重建（谨慎）：
```bash
docker compose down -v
rm -rf docker-data
docker compose up -d
```

## 5. 服务连通性检查
MySQL：
```bash
docker exec -i driveserver-mysql mysql -uroot -p123456 -e "SHOW DATABASES;"
```

Redis：
```bash
docker exec -i driveserver-redis redis-cli ping
```

InfluxDB：
```bash
docker exec -i driveserver-influxdb3 influxdb3 --version
```

## 6. 应用配置说明
`application.yaml`：默认配置（可通过环境变量覆盖）  
`application-local.yaml`：本地 profile（启用 Docker Compose 集成）  
`logback-spring.xml`：控制台 + 文件滚动日志（`logs/app.log`）

关键变量：
1. `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`
2. `REDIS_HOST` / `REDIS_PORT`
3. `SERVER_PORT`

日志配置要点：
1. `application.yaml` 与 `application-local.yaml` 统一包含 `traceId` 日志 pattern。
2. `logback-spring.xml` 开启滚动文件日志，默认落盘：`logs/app.log`。
3. 发生兜底异常时会输出完整堆栈，并带 `traceId + method + uri`。
4. 请求日志保持“每请求一行”，至少包含 `status`、`durationMs`、`traceId`、`path`。

## 7. 启动应用（本地开发）
```bash
cd /Users/m1ngyangg/Documents/DriveServer
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

期望日志关键字：
1. `Started DemoApplication`
2. `Successfully applied 4 migrations`（首次启动空库时）
3. `Tomcat started on port 8080`
4. `HTTP_REQUEST method=... path=... status=... durationMs=... traceId=...`

日志文件检查：
```bash
cd /Users/m1ngyangg/Documents/DriveServer
ls -lh logs
tail -n 100 logs/app.log
```

健康检查：
```bash
curl http://localhost:8080/actuator/health
```

## 8. Flyway 与初始化数据
应用首次启动会自动执行：
1. `V1__init_core_tables.sql`：`user/rule/alert/audit` 核心表结构
2. `V2__seed_default_data.sql`：默认管理员与默认规则
3. `V3__init_auth_rbac.sql`：RBAC 表结构与默认角色初始化
4. `V4__strengthen_user_rule_alert_audit_schema.sql`：补充外键与检查约束

默认管理员：
1. 用户名：`admin`
2. 密码：`123456`

检查迁移执行记录：
```bash
docker exec -i driveserver-mysql mysql -uroot -p123456 -D drive_server -e "SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank;"
```

## 9. 测试环境执行
运行单元/集成基础测试：
```bash
cd /Users/m1ngyangg/Documents/DriveServer
./mvnw -q test
```

说明：
1. 测试使用 H2（见 `src/test/resources/application.yaml`）
2. 测试不依赖本地 MySQL/Redis 容器

## 10. Stage/Prod-like 推荐部署方式
Stage（单机）：
1. 使用与本地相同镜像版本
2. 改为独立 `.env` 注入密码和密钥
3. 接入 Nginx 与 HTTPS

Prod-like（多实例）：
1. `app` 至少 2 实例（无状态）
2. MySQL 主从或托管服务
3. Redis 高可用（哨兵或托管）
4. 指标：Prometheus + Grafana
5. 日志：结构化日志 + 集中检索

## 11. 常见问题排查
1. `docker: command not found`
1. 检查 `PATH` 是否包含 `/opt/homebrew/bin`
2. `cannot connect to docker.sock`
1. 确认 Docker Desktop 或 Colima 已启动
3. Flyway 启动失败
1. 检查 `DB_URL` 和账号密码
2. 用 `docker compose ps` 确认 MySQL 为 `healthy`
4. 端口冲突（3306/6379/8181/8080）
1. 关闭占用进程或改 `compose.yaml` 端口映射

## 12. 一键验收命令清单
```bash
cd /Users/m1ngyangg/Documents/DriveServer

docker compose up -d
docker compose ps

docker exec -i driveserver-mysql mysql -uroot -p123456 -e "SHOW DATABASES;"
docker exec -i driveserver-redis redis-cli ping
docker exec -i driveserver-influxdb3 influxdb3 --version

./mvnw -q test
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
