# 接口设计文档（API SPEC）

本文档整理 `DriveServer` 当前可对接的接口能力，按“已实现接口”与“规划中接口”区分，避免联调时混淆。

## 1. 文档范围
### 1.1 已实现接口
当前代码已落地以下能力：
1. 认证与鉴权
2. 设备事件上报
3. 告警管理与闭环
4. WebSocket 告警实时推送

### 1.2 尚未落地的能力
以下内容目前仍属于设计阶段，仓库中暂无对应 Controller / Service 实现：
1. 实时总览接口 `GET /api/v1/realtime/overview`
2. 趋势统计接口 `GET /api/v1/stats/trend`
3. 风险排行接口 `GET /api/v1/stats/ranking`
4. 规则管理接口 `GET /api/v1/rules`、`POST /api/v1/rules`
5. 服务器主动向边缘端下发告警/指令接口

## 2. 通用约定
### 2.1 Base URL
- REST Base URL：`/api/v1`
- WebSocket 端点：`/ws/alerts`

### 2.2 协议与编码
- 协议：HTTP/HTTPS、WebSocket
- 编码：`UTF-8`
- 数据格式：`application/json`
- 时间格式：ISO-8601
- 存储时区：统一 UTC

### 2.3 鉴权约定
- 管理端 REST 接口：`Authorization: Bearer <jwt>`
- 设备上报接口：`X-Device-Token: <device_token>`
- WebSocket 握手：
  1. 优先读取请求头 `Authorization: Bearer <jwt>`
  2. 若请求头缺失，可使用查询参数 `?token=<jwt>`

### 2.4 统一响应结构
除 WebSocket 外，REST 接口统一返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "traceId": "trc_20260407_xxx"
}
```

说明：
1. `code`：业务状态码
2. `message`：业务消息
3. `data`：业务数据
4. `traceId`：请求链路追踪 ID

### 2.5 错误码
| code | 含义 | HTTP 状态码 |
|---|---|---|
| 0 | 成功 | 200 |
| 40001 | 请求参数不合法 | 400 |
| 40002 | 幂等冲突（重复事件） | 409 |
| 40101 | 未授权或 token 失效 | 401 |
| 40301 | 无权限访问 | 403 |
| 40401 | 资源不存在 | 404 |
| 40501 | 请求方法不支持 | 405 |
| 50001 | 内部服务器错误 | 500 |

### 2.6 角色说明
系统内置角色：
1. `ADMIN`
2. `OPERATOR`
3. `VIEWER`

权限范围：
1. `ADMIN`：管理类能力
2. `OPERATOR`：业务操作类能力
3. `VIEWER`：只读查询类能力

## 3. 接口总览
### 3.1 REST 接口
| 模块 | 方法 | 路径 | 鉴权 |
|---|---|---|---|
| 认证 | `POST` | `/auth/login` | 匿名 |
| 认证 | `GET` | `/auth/me` | 任意登录角色 |
| 认证 | `GET` | `/auth/admin/ping` | `ADMIN` |
| 事件接入 | `POST` | `/events` | 设备 Token |
| 告警 | `POST` | `/alerts` | `ADMIN` / `OPERATOR` |
| 告警 | `GET` | `/alerts` | `ADMIN` / `OPERATOR` / `VIEWER` |
| 告警 | `GET` | `/alerts/{id}` | `ADMIN` / `OPERATOR` / `VIEWER` |
| 告警 | `POST` | `/alerts/{id}/confirm` | `ADMIN` / `OPERATOR` |
| 告警 | `POST` | `/alerts/{id}/false-positive` | `ADMIN` / `OPERATOR` |
| 告警 | `POST` | `/alerts/{id}/close` | `ADMIN` / `OPERATOR` |
| 告警 | `GET` | `/alerts/{id}/action-logs` | `ADMIN` / `OPERATOR` / `VIEWER` |

### 3.2 WebSocket 接口
| 模块 | 协议 | 端点 | 订阅主题 | 鉴权 |
|---|---|---|---|---|
| 实时告警 | STOMP over WebSocket | `/ws/alerts` | `/topic/alerts` | JWT |

## 4. 认证接口
### 4.1 用户登录
- 方法：`POST`
- 路径：`/api/v1/auth/login`
- 鉴权：匿名

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

请求字段：
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码 |

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

响应字段：
| 字段 | 类型 | 说明 |
|---|---|---|
| `token` | string | JWT 访问令牌 |
| `expireAt` | string | 过期时间 |
| `roles` | string[] | 用户角色列表 |

失败场景：
1. 参数缺失或格式错误：`40001`
2. 用户名或密码错误：`40101`
3. 账号禁用：`40101`
4. 用户未分配角色：`40301`

### 4.2 当前用户信息
- 方法：`GET`
- 路径：`/api/v1/auth/me`
- 鉴权：`ADMIN` / `OPERATOR` / `VIEWER`

请求头：

```http
Authorization: Bearer <jwt>
```

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": 1,
    "username": "admin",
    "roles": ["ADMIN"]
  },
  "traceId": "trc_xxx"
}
```

### 4.3 管理员连通性检查
- 方法：`GET`
- 路径：`/api/v1/auth/admin/ping`
- 鉴权：`ADMIN`

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "message": "admin-ok"
  },
  "traceId": "trc_xxx"
}
```

## 5. 设备事件接入接口
### 5.1 单条事件上报
- 方法：`POST`
- 路径：`/api/v1/events`
- 鉴权：`X-Device-Token`
- 幂等键：`eventId`

请求头：

```http
X-Device-Token: <device_token>
Content-Type: application/json
```

请求体：

```json
{
  "eventId": "evt_20260407_0001",
  "fleetId": "fleet_01",
  "vehicleId": "veh_001",
  "driverId": "drv_001",
  "eventTime": "2026-04-07T10:01:15Z",
  "fatigueScore": 0.82,
  "distractionScore": 0.64,
  "perclos": 0.41,
  "blinkRate": 0.28,
  "yawnCount": 2,
  "headPose": "DOWN",
  "algorithmVer": "v1.0.3"
}
```

请求字段：
| 字段 | 类型 | 必填 | 规则 / 说明 |
|---|---|---|---|
| `eventId` | string | 是 | 最大 64，幂等键 |
| `fleetId` | string | 是 | 最大 64 |
| `vehicleId` | string | 是 | 最大 64 |
| `driverId` | string | 是 | 最大 64 |
| `eventTime` | string | 是 | ISO-8601 时间 |
| `fatigueScore` | number | 是 | `0.0 ~ 1.0` |
| `distractionScore` | number | 是 | `0.0 ~ 1.0` |
| `perclos` | number | 否 | `0.0 ~ 1.0` |
| `blinkRate` | number | 否 | `>= 0` |
| `yawnCount` | integer | 否 | `>= 0` |
| `headPose` | string | 否 | 最大 32 |
| `algorithmVer` | string | 否 | 最大 32 |

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accepted": true
  },
  "traceId": "trc_20260407_100115_001"
}
```

失败场景：
1. 参数校验失败：`40001`
2. 重复 `eventId`：`40002`
3. 未携带或错误设备 Token：`40101`

说明：
1. 事件写入接入链路后返回 `accepted=true`
2. 事件接收成功后，会继续进入服务端规则判定链路
3. 当前 `riskScore` 计算方式为 `max(fatigueScore, distractionScore)`
4. 只有在满足阈值、持续时长且未命中冷却/去重时，才会自动生成 `/alerts` 告警
5. 当前仅支持“边缘端上报到服务器”，不支持服务器主动回推到边缘端

## 6. 告警管理接口
### 6.1 创建告警
- 方法：`POST`
- 路径：`/api/v1/alerts`
- 鉴权：`ADMIN` / `OPERATOR`

请求体：

```json
{
  "fleetId": 1001,
  "vehicleId": 2001,
  "driverId": 3001,
  "ruleId": 4001,
  "riskLevel": 3,
  "riskScore": 0.91,
  "fatigueScore": 0.88,
  "distractionScore": 0.36,
  "triggerTime": "2026-04-07T10:01:16Z",
  "remark": "连续闭眼触发高风险告警"
}
```

请求字段：
| 字段 | 类型 | 必填 | 规则 / 说明 |
|---|---|---|---|
| `fleetId` | long | 是 | 车队 ID |
| `vehicleId` | long | 是 | 车辆 ID |
| `driverId` | long | 是 | 司机 ID |
| `ruleId` | long | 是 | 规则 ID |
| `riskLevel` | int | 是 | 取值 `1~3` |
| `riskScore` | decimal | 是 | `0.0 ~ 1.0` |
| `fatigueScore` | decimal | 是 | `0.0 ~ 1.0` |
| `distractionScore` | decimal | 是 | `0.0 ~ 1.0` |
| `triggerTime` | string | 是 | ISO-8601 时间 |
| `remark` | string | 否 | 最大 255 |

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1001,
    "alertNo": "ALT202604071001161234",
    "status": 0,
    "latestActionBy": 1,
    "latestActionTime": "2026-04-07T10:01:16Z",
    "actionType": "CREATE"
  },
  "traceId": "trc_xxx"
}
```

### 6.2 告警分页查询
- 方法：`GET`
- 路径：`/api/v1/alerts`
- 鉴权：`ADMIN` / `OPERATOR` / `VIEWER`

查询参数：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `page` | int | 否 | 默认 `1` |
| `size` | int | 否 | 默认 `20`，最大 `100` |
| `fleetId` | long | 否 | 车队 ID |
| `vehicleId` | long | 否 | 车辆 ID |
| `driverId` | long | 否 | 司机 ID |
| `riskLevel` | int | 否 | `1=LOW`、`2=MEDIUM`、`3=HIGH` |
| `status` | int | 否 | `0=NEW`、`1=CONFIRMED`、`2=FALSE_POSITIVE`、`3=CLOSED` |
| `startTime` | string | 否 | 触发时间开始，ISO-8601 |
| `endTime` | string | 否 | 触发时间结束，ISO-8601 |

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 128,
    "page": 1,
    "size": 20,
    "items": [
      {
        "id": 1001,
        "alertNo": "ALT202604071001161234",
        "fleetId": 1001,
        "vehicleId": 2001,
        "driverId": 3001,
        "riskLevel": 3,
        "fatigueScore": 0.88,
        "distractionScore": 0.36,
        "status": 0,
        "triggerTime": "2026-04-07T10:01:16Z"
      }
    ]
  },
  "traceId": "trc_xxx"
}
```

说明：
1. 结果按 `triggerTime DESC, id DESC` 排序
2. 若 `startTime > endTime`，返回 `40001`

### 6.3 告警详情
- 方法：`GET`
- 路径：`/api/v1/alerts/{id}`
- 鉴权：`ADMIN` / `OPERATOR` / `VIEWER`

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1001,
    "alertNo": "ALT202604071001161234",
    "fleetId": 1001,
    "vehicleId": 2001,
    "driverId": 3001,
    "ruleId": 4001,
    "riskLevel": 3,
    "riskScore": 0.91,
    "fatigueScore": 0.88,
    "distractionScore": 0.36,
    "triggerTime": "2026-04-07T10:01:16Z",
    "status": 0,
    "latestActionBy": 1,
    "latestActionTime": "2026-04-07T10:01:16Z",
    "remark": "连续闭眼触发高风险告警"
  },
  "traceId": "trc_xxx"
}
```

失败场景：
1. 指定 `id` 不存在：`40401`

### 6.4 告警确认
- 方法：`POST`
- 路径：`/api/v1/alerts/{id}/confirm`
- 鉴权：`ADMIN` / `OPERATOR`

请求体：

```json
{
  "remark": "已电话提醒司机"
}
```

成功响应结构与“创建告警”一致，`actionType` 为 `CONFIRM`，`status` 变为 `1`。

### 6.5 告警误报标注
- 方法：`POST`
- 路径：`/api/v1/alerts/{id}/false-positive`
- 鉴权：`ADMIN` / `OPERATOR`

请求体：

```json
{
  "remark": "遮挡导致误报"
}
```

成功响应结构与“创建告警”一致，`actionType` 为 `FALSE_POSITIVE`，`status` 变为 `2`。

### 6.6 告警关闭
- 方法：`POST`
- 路径：`/api/v1/alerts/{id}/close`
- 鉴权：`ADMIN` / `OPERATOR`

请求体：

```json
{
  "remark": "风险已解除"
}
```

成功响应结构与“创建告警”一致，`actionType` 为 `CLOSE`，`status` 变为 `3`。

### 6.7 告警操作日志
- 方法：`GET`
- 路径：`/api/v1/alerts/{id}/action-logs`
- 鉴权：`ADMIN` / `OPERATOR` / `VIEWER`

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "alertId": 1001,
    "items": [
      {
        "actionType": "CREATE",
        "actionBy": 1,
        "actionTime": "2026-04-07T10:01:16Z",
        "actionRemark": "连续闭眼触发高风险告警"
      },
      {
        "actionType": "CONFIRM",
        "actionBy": 2,
        "actionTime": "2026-04-07T10:02:10Z",
        "actionRemark": "已电话提醒司机"
      }
    ]
  },
  "traceId": "trc_xxx"
}
```

说明：
1. 日志按 `actionTime ASC, id ASC` 返回
2. 适合前端直接渲染时间线

### 6.8 告警状态与动作枚举
状态枚举：
1. `0 = NEW`
2. `1 = CONFIRMED`
3. `2 = FALSE_POSITIVE`
4. `3 = CLOSED`

动作枚举：
1. `CREATE`
2. `CONFIRM`
3. `FALSE_POSITIVE`
4. `CLOSE`

### 6.9 状态流转规则
允许流转：
1. `NEW -> CONFIRMED`
2. `NEW -> FALSE_POSITIVE`
3. `NEW -> CLOSED`
4. `CONFIRMED -> FALSE_POSITIVE`
5. `CONFIRMED -> CLOSED`

不允许流转：
1. `FALSE_POSITIVE` 为终态
2. `CLOSED` 为终态
3. 非法状态流转返回 `40001`

## 7. WebSocket 告警推送
### 7.1 连接信息
- 协议：STOMP over WebSocket
- 连接地址：`ws://{host}:{port}/ws/alerts`
- 订阅主题：`/topic/alerts`

### 7.2 鉴权方式
WebSocket 握手支持两种方式：
1. 请求头：`Authorization: Bearer <jwt>`
2. 查询参数：`ws://{host}:{port}/ws/alerts?token=<jwt>`

说明：
1. 浏览器原生 `WebSocket` 不方便自定义请求头时，可使用 `token` 查询参数
2. STOMP 客户端建议优先使用请求头

### 7.3 消息类型
当前支持两类广播消息：
1. `ALERT_CREATED`
2. `ALERT_UPDATED`

### 7.4 消息体结构

```json
{
  "type": "ALERT_CREATED",
  "timestamp": "2026-04-07T10:01:16Z",
  "payload": {
    "alertId": 1001,
    "alertNo": "ALT202604071001161234",
    "fleetId": 1001,
    "vehicleId": 2001,
    "driverId": 3001,
    "riskLevel": 3,
    "status": 0,
    "actionType": "CREATE"
  }
}
```

字段说明：
| 字段 | 类型 | 说明 |
|---|---|---|
| `type` | string | `ALERT_CREATED` / `ALERT_UPDATED` |
| `timestamp` | string | 告警最近动作时间 |
| `payload.alertId` | long | 告警主键 |
| `payload.alertNo` | string | 告警编号 |
| `payload.fleetId` | long | 车队 ID |
| `payload.vehicleId` | long | 车辆 ID |
| `payload.driverId` | long | 司机 ID |
| `payload.riskLevel` | int | 风险等级 |
| `payload.status` | int | 告警状态 |
| `payload.actionType` | string | 业务动作 |

### 7.5 推送时机
推送由告警领域事件触发，且在事务提交成功后发送：
1. 创建告警成功后，推送 `ALERT_CREATED`
2. 告警确认后，推送 `ALERT_UPDATED`
3. 告警误报标注后，推送 `ALERT_UPDATED`
4. 告警关闭后，推送 `ALERT_UPDATED`

## 8. 联调示例
### 8.1 登录获取 JWT
```bash
curl -s -X POST 'http://localhost:8080/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'
```

### 8.2 上报设备事件
```bash
curl -s -X POST 'http://localhost:8080/api/v1/events' \
  -H 'Content-Type: application/json' \
  -H 'X-Device-Token: dev-device-token' \
  -d '{
    "eventId":"evt_20260407_0001",
    "fleetId":"fleet_01",
    "vehicleId":"veh_001",
    "driverId":"drv_001",
    "eventTime":"2026-04-07T10:01:15Z",
    "fatigueScore":0.82,
    "distractionScore":0.64
  }'
```

### 8.3 查询告警列表
```bash
curl -s 'http://localhost:8080/api/v1/alerts?page=1&size=20' \
  -H 'Authorization: Bearer <jwt>'
```

### 8.4 确认告警
```bash
curl -s -X POST 'http://localhost:8080/api/v1/alerts/1001/confirm' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <jwt>' \
  -d '{"remark":"已电话提醒司机"}'
```

## 9. 对接边界说明
1. 当前已支持“设备向服务器上报事件”
2. 当前已支持“服务器向在线管理端实时推送告警”
3. 当前未提供“服务器主动向边缘端推送告警”的专用下行接口
4. 若后续需要边缘端下行能力，建议单独设计 MQTT / WebSocket / HTTP callback 方案
