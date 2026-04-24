# Ingest Module 实现文档

本文档说明 `DriveServer` 当前已落地的事件接入能力，重点覆盖：
1. `/api/v1/events` 参数校验
2. 基于 `eventId` 的幂等控制
3. 自动告警编排

## 1. 目标范围
已实现内容：
1. 单条事件接入接口 `POST /api/v1/events`
2. 请求体字段校验（必填、长度、分值范围）
3. 幂等键 `eventId` 去重（默认窗口 24 小时）
4. 设备鉴权（`X-Device-Token`）
5. Redis Stream 入流（`XADD stream:events`）
6. 调用规则模块做风险判定、状态分控制与冷却控制
7. 命中规则后自动创建告警（`alert_event` / `alert_action_log`）
8. 统一错误码返回（`40001` / `40002`）

## 2. 接口说明
### 2.1 事件上报
- 方法：`POST`
- 路径：`/api/v1/events`
- 鉴权：`X-Device-Token: <device_token>`

成功响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accepted": true
  },
  "traceId": "trc_xxx"
}
```

说明：
1. `accepted=true` 表示事件已通过鉴权、幂等校验并被系统接收。
2. 事件接收成功后，会继续进入规则判定链路。
3. 当状态分达到规则阈值且未命中冷却/去重时，系统会自动创建一条告警。

## 3. 参数校验规则
请求 DTO：`IngestEventRequest`

| 字段 | 必填 | 规则 |
|---|---|---|
| `eventId` | 是 | `@NotBlank`，最大 64 |
| `fleetId` | 否 | 最大 64，兼容边缘端未绑定车队的场景 |
| `vehicleId` | 是 | `@NotBlank`，最大 64 |
| `driverId` | 否 | 最大 64，兼容边缘端未绑定司机的场景 |
| `eventTime` | 是 | `@NotNull`，`OffsetDateTime`，兼容别名 `eventTimeUtc` |
| `fatigueScore` | 是 | `0.0 ~ 1.0` |
| `distractionScore` | 是 | `0.0 ~ 1.0` |
| `perclos` | 否 | `0.0 ~ 1.0` |
| `blinkRate` | 否 | `>= 0` |
| `yawnCount` | 否 | `>= 0` 整数 |
| `headPose` | 否 | 最大 32 |
| `algorithmVer` | 否 | 最大 32 |
| `riskLevel` | 否 | 最大 32，边缘端风险等级透传 |
| `dominantRiskType` | 否 | 最大 32，边缘端主导风险类型透传 |
| `triggerReasons` | 否 | 字符串数组，边缘端触发原因透传 |
| `windowStartMs` | 否 | `>= 0`，事件窗口开始毫秒时间戳 |
| `windowEndMs` | 否 | `>= 0`，事件窗口结束毫秒时间戳 |
| `createdAtMs` | 否 | `>= 0`，边缘端事件创建毫秒时间戳 |

参数不合法时统一返回：
1. HTTP `400`
2. `code = 40001`

## 4. 幂等设计
### 4.1 幂等键
`eventId` 作为全局幂等键。

### 4.2 默认存储（Redis）
1. key 前缀：`ingest:idempotency:event:{eventId}`
2. 操作：`SETNX + TTL`
3. TTL：默认 `24h`

当 `SETNX` 失败（同 `eventId` 已存在）时返回：
1. HTTP `409`
2. `code = 40002`

### 4.3 可切换存储策略
通过配置切换幂等存储：
1. `redis`（默认，生产推荐）
2. `memory`（测试环境）

## 5. 配置说明
`application.yaml`：

```yaml
ingest:
  security:
    device-tokens: ${INGEST_DEVICE_TOKENS:dev-device-token}
  idempotency:
    store: ${INGEST_IDEMPOTENCY_STORE:redis}
    ttl: ${INGEST_IDEMPOTENCY_TTL:24h}
  stream:
    producer: ${INGEST_STREAM_PRODUCER:redis}
    key: ${INGEST_STREAM_KEY:stream:events}
```

`src/test/resources/application.yaml`：

```yaml
ingest:
  security:
    device-tokens: test-device-token
  idempotency:
    store: memory
    ttl: 24h
  stream:
    producer: noop
    key: stream:events
```

## 6. 自动告警行为
`/api/v1/events` 接收成功后，当前实现会继续执行以下步骤：
1. 将 `vehicleId/eventTime/fatigueScore/distractionScore` 转换为规则输入
2. 计算 `riskScore`
3. 按高、中、低风险规则依次累积疲劳状态分和分心状态分
4. 判定状态分是否达到阈值
5. 校验分钟桶去重与冷却窗口
6. 命中后自动创建一条告警，并通过现有告警实时推送链路广播

当前 `riskScore` 计算方式为：

```text
risk_score = max(fatigue_score, distraction_score)
```

说明：
1. `riskScore` 用于统一展示与日志输出
2. 告警触发条件默认按疲劳状态分与分心状态分分别判定，当前口径是“分心更宽松、疲劳更严格”
3. 规则内部会把原始分数转换成会累积、会衰减的状态分，再比较各档阈值

## 7. 接口与告警关系
当前 `/api/v1/events` 与 `/api/v1/alerts` 的关系是：
1. `/api/v1/events` 上报的是边缘端检测事件
2. 事件先进入服务端规则判定链路
3. 只有在满足疲劳/分心状态分阈值和冷却条件时，才会自动生成 `/api/v1/alerts` 中的告警记录
4. 若边缘端仅完成事件上报，但未携带自动建告警所需的业务 ID（如 `fleetId`、`driverId`），服务端仍会接收并透传事件，只跳过自动建告警
4. 因此 `/alerts` 中的记录不是简单复制 `/events`，而是服务端规则命中后的结果

## 8. 错误码
1. `40001`：请求参数不合法
2. `40002`：幂等冲突（重复事件）

## 9. 测试覆盖
集成测试类：`IngestModuleIntegrationTest`

已覆盖场景：
1. 合法请求返回 `code=0` 且 `accepted=true`
2. 分值越界返回 `40001`
3. 重复 `eventId` 返回 `40002`（HTTP 409）
4. 缺失或错误 `X-Device-Token` 返回 `40101`
5. 连续高风险事件推动状态分跨阈值后自动创建告警

执行命令：
```bash
./mvnw test -q
```
