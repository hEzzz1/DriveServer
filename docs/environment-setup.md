# DriveServer 环境安装与部署（Spring Boot 3.5.x）

本文档用于在当前项目目录快速搭建后端开发环境。

## 1. 基础软件
建议版本：
1. JDK 17
2. Maven 3.9+
3. Docker Desktop（含 Docker Compose）
4. IntelliJ IDEA

macOS 安装示例：
```bash
brew install --cask temurin17
brew install maven
brew install --cask docker
```

验证：
```bash
java -version
mvn -v
docker -v
docker compose version
```

## 2. 启动服务端依赖
项目根目录已提供 [`compose.yaml`](/Users/m1ngyangg/Documents/DriveServer/compose.yaml)。

启动：
```bash
docker compose up -d
```

查看状态：
```bash
docker compose ps
```

停止：
```bash
docker compose down
```

## 3. 依赖服务说明
1. MySQL 8.4
1. 端口：`3306`
2. 库名：`drive_server`
3. root 密码：`123456`
2. Redis 7.2
1. 端口：`6379`
2. 已开启 AOF 持久化
3. InfluxDB 3 Core
1. 端口：`8181`
2. 本地文件存储目录：`./docker-data/influxdb3`

## 4. 连接验证命令
MySQL：
```bash
docker exec -it driveserver-mysql mysql -uroot -p123456 -e "SHOW DATABASES;"
```

Redis：
```bash
docker exec -it driveserver-redis redis-cli ping
```

InfluxDB 3：
```bash
docker exec -it driveserver-influxdb3 influxdb3 --version
```

## 5. 默认初始化数据
Flyway 会自动执行 `V1`、`V2` 脚本，包含：
1. 核心表结构
2. 默认管理员账号
3. 默认风险规则（高/中/低）

默认管理员：
1. 用户名：`admin`
2. 密码：`123456`

## 6. Spring Boot 连接配置模板
如果你在 IDEA 新建 `Spring Boot 3.5.x` 模块，可使用以下 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/drive_server?useSSL=false&serverTimezone=UTC
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

## 7. 建议的下一步
1. 在当前仓库新增 `backend/` 模块（Spring Boot 3.5.x + Maven）。
2. 创建 Flyway 首版脚本（用户、规则、告警、审计核心表）。
3. 优先打通 MVP 链路：`/auth/login`、`/events`、`/alerts`。
