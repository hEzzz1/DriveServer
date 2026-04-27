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
1. `SUPER_ADMIN`
2. `SYS_ADMIN`
3. `RISK_ADMIN`
4. `OPERATOR`
5. `ANALYST`
6. `VIEWER`

权限注解：
1. `@SuperAdminOnly`：仅 `SUPER_ADMIN`
2. `@SystemAdminOnly`：仅 `SYS_ADMIN`
3. `@RiskAdminOnly`：仅 `RISK_ADMIN`
4. `@OperatorOnly`：仅 `OPERATOR`
5. `@AnyReadRole`：`SUPER_ADMIN` / `SYS_ADMIN` / `RISK_ADMIN` / `OPERATOR` / `ANALYST` / `VIEWER`
6. 组合注解：`@OperatorOrSuperAdmin`、`@RiskAdminOrSuperAdmin`、`@SystemAdminOrSuperAdmin`

## 3. 数据库变更
相关 Flyway 脚本：
1. `V3__init_auth_rbac.sql`
2. `V4__strengthen_user_rule_alert_audit_schema.sql`
3. `V8__expand_rbac_and_add_internal_subjects.sql`

主要变更：
1. 创建 `role` 表
2. 创建 `user_role` 表
3. 初始化并扩展默认角色到 `SUPER_ADMIN/SYS_ADMIN/RISK_ADMIN/OPERATOR/ANALYST/VIEWER`
4. 为默认用户 `admin` 绑定 `SUPER_ADMIN`
5. 为 `user_role.user_id` 与 `user_role.role_id` 增加外键约束
6. 为 `user_account.status` 增加状态值检查约束
7. 为 `user_account` 增加 `subject_type`，区分 `USER` 与 `SYSTEM`
8. 初始化内部主体 `system-auto-alert`，用于系统自动建告警

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
    "roles": ["SUPER_ADMIN"]
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
- 鉴权：`SYS_ADMIN` / `SUPER_ADMIN`

用途：用于验证 RBAC 是否生效。

## 6. JWT 载荷约定
签发时写入以下声明：
1. `sub`：用户名
2. `iss`：issuer
3. `iat`：签发时间
4. `exp`：过期时间
5. `uid`：用户 ID
6. `subjectType`：主体类型，当前支持 `USER` / `SYSTEM`
7. `roles`：角色列表

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

## 9. 认证授权日志约定
401/403 场景会记录简要原因日志，便于快速定位鉴权失败原因：
1. 401（`RestAuthenticationEntryPoint`）：`traceId` + `method` + `path` + `reason`
2. 403（`RestAccessDeniedHandler`）：`traceId` + `method` + `path` + `reason`

示例：
```text
Authentication required: traceId=trc_xxx method=GET path=/api/v1/auth/me reason=BadCredentialsException: Bad credentials
Access denied: traceId=trc_xxx method=GET path=/api/v1/auth/admin/ping reason=AccessDeniedException: Access is denied
```

全局兜底异常（`GlobalExceptionHandler`）会输出完整异常栈，并携带：
1. `traceId`
2. `method`
3. `uri`

## 10. 快速联调示例
### 10.1 登录获取 token
```bash
curl -s -X POST 'http://localhost:8080/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'
```

### 10.2 访问 `/auth/me`
```bash
curl -s 'http://localhost:8080/api/v1/auth/me' \
  -H 'Authorization: Bearer <token>'
```

### 10.3 访问管理员接口
```bash
curl -s 'http://localhost:8080/api/v1/auth/admin/ping' \
  -H 'Authorization: Bearer <token>'
```

## 11. 测试与验证
已覆盖集成测试：
1. 登录成功返回 token 和角色
2. 登录失败返回 `40101`
3. 未携带 token 访问受保护接口返回 `40101`
4. `VIEWER` 访问管理员接口返回 `40301`
5. `SYS_ADMIN` / `SUPER_ADMIN` 可访问管理员接口

执行命令：
```bash
./mvnw test -q
```
