# Auth Module 实现文档

本文档说明 `DriveServer` 当前已落地的认证与授权能力，包括登录、JWT 签发与 RBAC 鉴权。

## 1. 目标范围
已实现内容：
1. 用户名密码登录（`/api/v1/auth/login`）
2. JWT 签发与解析（Bearer Token）
3. 基于角色的访问控制（RBAC）
4. 统一错误码与统一响应结构

## 2. 角色模型
系统内置角色：
1. `ADMIN`
2. `OPERATOR`
3. `VIEWER`

权限注解：
1. `@AdminOnly`：仅 `ADMIN`
2. `@AdminOrOperator`：`ADMIN` 或 `OPERATOR`
3. `@AnyUserRole`：`ADMIN` / `OPERATOR` / `VIEWER`

## 3. 数据库变更
新增 Flyway 脚本：`V3__init_auth_rbac.sql`

主要变更：
1. 创建 `role` 表
2. 创建 `user_role` 表
3. 初始化默认角色 `ADMIN/OPERATOR/VIEWER`
4. 为默认用户 `admin` 绑定 `ADMIN` 角色

## 4. 配置说明
`application.yaml` 新增：

```yaml
auth:
  jwt:
    secret: ${JWT_SECRET:DriveServerDevSecretDriveServerDevSecret1234}
    expire-seconds: ${JWT_EXPIRE_SECONDS:7200}
    issuer: ${JWT_ISSUER:DriveServer}
```

配置项说明：
1. `auth.jwt.secret`：JWT HMAC 密钥，至少 32 字节
2. `auth.jwt.expire-seconds`：Token 过期秒数，默认 7200 秒
3. `auth.jwt.issuer`：签发者标识

生产环境必须通过环境变量覆盖默认 `JWT_SECRET`。

## 5. 接口说明
### 5.1 登录
- 方法：`POST`
- 路径：`/api/v1/auth/login`
- 鉴权：匿名

请求：
```json
{
  "username": "admin",
  "password": "123456"
}
```

成功响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "<jwt>",
    "expireAt": "2026-04-07T12:00:00Z",
    "roles": ["ADMIN"]
  },
  "traceId": "trc_xxx"
}
```

失败场景：
1. 参数缺失：`40001`
2. 用户名或密码错误：`40101`
3. 账号禁用：`40101`
4. 账号未分配角色：`40301`

### 5.2 当前用户信息
- 方法：`GET`
- 路径：`/api/v1/auth/me`
- 鉴权：任意已登录角色

用途：用于前端在登录后获取当前用户身份与角色。

### 5.3 管理员示例接口
- 方法：`GET`
- 路径：`/api/v1/auth/admin/ping`
- 鉴权：`ADMIN`

用途：用于验证 RBAC 是否生效。

## 6. JWT 载荷约定
签发时写入以下声明：
1. `sub`：用户名
2. `iss`：issuer
3. `iat`：签发时间
4. `exp`：过期时间
5. `uid`：用户 ID
6. `roles`：角色列表

## 7. 鉴权流程
1. 客户端在 `Authorization` 头传入 `Bearer <token>`
2. `JwtAuthenticationFilter` 解析并校验签名与过期时间
3. 校验成功后，将用户信息注入 `SecurityContext`
4. 控制器方法通过注解执行角色鉴权

## 8. 统一返回与错误码
统一结构：
```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "traceId": "trc_xxx"
}
```

认证授权相关错误码：
1. `40101`：未授权或 token 失效
2. `40301`：无权限访问

## 9. 快速联调示例
### 9.1 登录获取 token
```bash
curl -s -X POST 'http://localhost:8080/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'
```

### 9.2 访问 `/auth/me`
```bash
curl -s 'http://localhost:8080/api/v1/auth/me' \
  -H 'Authorization: Bearer <token>'
```

### 9.3 访问管理员接口
```bash
curl -s 'http://localhost:8080/api/v1/auth/admin/ping' \
  -H 'Authorization: Bearer <token>'
```

## 10. 测试与验证
已覆盖集成测试：
1. 登录成功返回 token 和角色
2. 登录失败返回 `40101`
3. 未携带 token 访问受保护接口返回 `40101`
4. `VIEWER` 访问管理员接口返回 `40301`
5. `ADMIN` 可访问管理员接口

执行命令：
```bash
./mvnw test -q
```
