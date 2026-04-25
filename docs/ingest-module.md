# Ingest Module 实现文档

本文档说明 `DriveServer` 当前已落地的边缘警告接入能力，重点覆盖：
1. `/api/v1/events` 参数校验
2. 基于 `eventId` 的幂等控制
3. 边缘警告直转与规则兜底编排

## 1. 目标范围
已实现内容：
1. 单条边缘警告接入接口 `POST /api/v1/events`
2. 请求体字段校验（必填、长度、分值范围）
3. 幂等键 `eventId` 去重（默认窗口 24 小时）
4. 设备鉴权（`X-Device-Token`）
5. Redis Stream 入流（`XADD stream:events`）
6. 边缘上报直转告警（`alert_event` / `alert_action_log`）
7. 边缘风险字段缺失时，规则引擎兜底计算 `riskScore` / `ruleId`
8. 统一错误码返回（`40001` / `40002`）

## 2. 接口说明
### 2.1 边缘警告上报
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
1. `accepted=true` 表示边缘警告已通过鉴权、幂等校验并被系统接收。
2. 接收成功后，系统会立即落一条统一告警记录到 `alert_event`。
3. 若边缘端提供了 `riskLevel` / `dominantRiskType` / `triggerReasons`，优先按边缘结果落告警。
4. 若这些字段缺失，则保留原规则引擎作为兜底路径，补算 `riskScore` / `ruleId`。

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
1. 完成设备鉴权和 `eventId` 幂等校验
2. 将原始载荷继续写入事件流，保留现有接入链路
3. 直接组装 `CreateAlertRequest` 并创建统一告警记录
4. 若边缘端已提供 `riskLevel` / `dominantRiskType` / `triggerReasons`，则以边缘风险结果为主
5. 若边缘风险字段不完整，则调用规则引擎兜底计算 `riskScore` / `ruleId`
6. 告警创建后沿用现有实时推送链路广播

当前 `riskScore` 计算方式为：

```text
risk_score = max(fatigue_score, distraction_score)
```

说明：
1. `riskScore` 继续用于统一展示与日志输出
2. 边缘直转场景下，`riskLevel` 优先采用边缘结果，`ruleId` 仍映射到现有高/中/低风险规则，便于兼容历史模型
3. 规则兜底场景下，仍复用原有规则模块的风险计算逻辑

## 7. 接口与告警关系
当前 `/api/v1/events` 与 `/api/v1/alerts` 的关系是：
1. `/api/v1/events` 是边缘警告上报入口
2. 接入成功后会立即创建 `/api/v1/alerts` 统一告警记录
3. `alert_event` 仍然是系统内唯一告警主表，不新增 `warning_event`
4. 对外可以逐步把文案切到“警告”，对内实体仍保留 `Alert` 命名以降低改动面
5. 若边缘端未绑定 `fleetId` / `driverId`，当前实现会以 `0` 作为兼容占位值落库，避免丢失告警记录

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
5. 单次上报即创建统一告警记录
6. 边缘元数据会随 `/api/v1/events` 一并持久化

执行命令：
```bash
./mvnw test -q
```
