# 接口设计文档（API SPEC）

## 1. 接口规范
### 1.1 基础信息
- Base URL：`/api/v1`
- 协议：HTTP/HTTPS（生产环境建议仅HTTPS）
- 编码：`UTF-8`
- 数据格式：`application/json`
- 时间格式：`ISO-8601`，统一使用 UTC 存储

### 1.2 鉴权约定
- 管理接口：`Authorization: Bearer <jwt_token>`
- 设备上报接口：`X-Device-Token: <device_token>`

### 1.3 幂等与追踪
- 事件上报使用 `eventId` 做幂等键。
- 每个响应返回 `traceId` 用于日志追踪。

### 1.4 统一响应结构
```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "traceId": "trc_20260407_xxx"
}
```

## 2. 错误码定义
| code | 含义 | HTTP状态码 |
|---|---|---|
| 0 | 成功 | 200 |
| 40001 | 请求参数不合法 | 400 |
| 40002 | 幂等冲突（重复事件） | 409 |
| 40101 | 未授权或token失效 | 401 |
| 40301 | 无权限访问 | 403 |
| 40401 | 资源不存在 | 404 |
| 42901 | 请求过于频繁 | 429 |
| 50001 | 内部服务器错误 | 500 |
| 50002 | 依赖服务不可用 | 503 |

## 3. 鉴权接口
### 3.1 用户登录
- 方法：`POST`
- 路径：`/auth/login`
- 权限：匿名

请求：
```json
{
  "username": "admin",
  "password": "123456"
}
```

响应：
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

## 4. 事件接入接口
### 4.1 单条事件上报
- 方法：`POST`
- 路径：`/events`
- 权限：设备Token

请求字段：
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| eventId | string(64) | 是 | 事件唯一ID，幂等键 |
| fleetId | string(64) | 是 | 车队标识 |
| vehicleId | string(64) | 是 | 车辆标识 |
| driverId | string(64) | 是 | 司机标识 |
| eventTime | string | 是 | ISO-8601时间 |
| fatigueScore | number | 是 | 范围0~1 |
| distractionScore | number | 是 | 范围0~1 |
| perclos | number | 否 | 范围0~1 |
| blinkRate | number | 否 | 眨眼率 |
| yawnCount | integer | 否 | 哈欠次数 |
| headPose | string | 否 | 头姿态枚举 |
| algorithmVer | string(32) | 否 | 算法版本 |

请求示例：
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

响应示例：
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

## 5. 告警管理接口
### 5.1 告警分页查询
- 方法：`GET`
- 路径：`/alerts`
- 权限：`ADMIN`、`OPERATOR`、`VIEWER`

查询参数：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | int | 否 | 默认1 |
| size | int | 否 | 默认20，最大100 |
| fleetId | string | 否 | 车队 |
| vehicleId | string | 否 | 车辆 |
| driverId | string | 否 | 司机 |
| riskLevel | int | 否 | 1低2中3高 |
| status | int | 否 | 0新建1确认2误报3关闭 |
| startTime | string | 否 | 开始时间 |
| endTime | string | 否 | 结束时间 |

响应示例：
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
        "alertNo": "ALT202604070001",
        "fleetId": "fleet_01",
        "vehicleId": "veh_001",
        "driverId": "drv_001",
        "riskLevel": 3,
        "fatigueScore": 0.84,
        "distractionScore": 0.67,
        "status": 0,
        "triggerTime": "2026-04-07T10:01:16Z"
      }
    ]
  },
  "traceId": "trc_xxx"
}
```

### 5.2 告警详情
- 方法：`GET`
- 路径：`/alerts/{id}`
- 权限：`ADMIN`、`OPERATOR`、`VIEWER`

### 5.3 告警确认
- 方法：`POST`
- 路径：`/alerts/{id}/confirm`
- 权限：`ADMIN`、`OPERATOR`

请求：
```json
{
  "remark": "已电话提醒司机"
}
```

### 5.4 告警误报标注
- 方法：`POST`
- 路径：`/alerts/{id}/false-positive`
- 权限：`ADMIN`、`OPERATOR`

请求：
```json
{
  "remark": "遮挡导致误报"
}
```

### 5.5 告警关闭
- 方法：`POST`
- 路径：`/alerts/{id}/close`
- 权限：`ADMIN`、`OPERATOR`

请求：
```json
{
  "remark": "风险已解除"
}
```

## 6. 实时与统计接口
### 6.1 实时总览
- 方法：`GET`
- 路径：`/realtime/overview`
- 权限：`ADMIN`、`OPERATOR`、`VIEWER`

响应字段：
- 当前在线车辆数
- 当前告警车辆数
- 最近5分钟告警数量
- 告警等级分布

### 6.2 趋势统计
- 方法：`GET`
- 路径：`/stats/trend`
- 权限：`ADMIN`、`OPERATOR`、`VIEWER`

查询参数：
| 参数 | 示例 | 说明 |
|---|---|---|
| fleetId | fleet_01 | 车队维度 |
| granularity | 1m/5m/1h/1d | 时间粒度 |
| startTime | 2026-04-01T00:00:00Z | 开始 |
| endTime | 2026-04-07T00:00:00Z | 结束 |

### 6.3 风险排行
- 方法：`GET`
- 路径：`/stats/ranking`
- 权限：`ADMIN`、`OPERATOR`、`VIEWER`

查询参数：
| 参数 | 示例 | 说明 |
|---|---|---|
| dimension | driver/vehicle | 排行维度 |
| topN | 10 | 返回数量 |
| startTime | 2026-04-01T00:00:00Z | 开始 |
| endTime | 2026-04-07T00:00:00Z | 结束 |

## 7. 规则管理接口
### 7.1 查询规则
- 方法：`GET`
- 路径：`/rules`
- 权限：`ADMIN`、`OPERATOR`

### 7.2 新增规则
- 方法：`POST`
- 路径：`/rules`
- 权限：`ADMIN`

请求示例：
```json
{
  "ruleCode": "RISK_HIGH",
  "ruleName": "高风险规则",
  "riskThreshold": 0.80,
  "durationSeconds": 3,
  "cooldownSeconds": 60,
  "enabled": true
}
```

### 7.3 更新规则
- 方法：`PUT`
- 路径：`/rules/{id}`
- 权限：`ADMIN`

### 7.4 启停规则
- 方法：`POST`
- 路径：`/rules/{id}/toggle`
- 权限：`ADMIN`

## 8. WebSocket 规范
- 地址：`/ws/alerts`
- 鉴权：握手时携带 JWT
- 推送消息类型：
  - `ALERT_CREATED`
  - `ALERT_UPDATED`
  - `SYSTEM_NOTICE`

推送示例：
```json
{
  "type": "ALERT_CREATED",
  "timestamp": "2026-04-07T10:01:16Z",
  "payload": {
    "alertId": 1001,
    "riskLevel": 3,
    "vehicleId": "veh_001",
    "driverId": "drv_001"
  }
}
```

## 9. 限流与幂等策略
1. 设备上报默认限流：每设备 `50 req/s`。
2. 管理接口默认限流：每用户 `20 req/s`。
3. 幂等校验窗口：`eventId` 保留24小时。

## 10. 审计要求
以下操作必须写入审计日志：
1. 登录成功/失败。
2. 规则新增/更新/启停。
3. 告警确认/误报/关闭。
4. 权限变更。

