# Realtime Module 实现文档

本文档说明 `DriveServer` 当前已落地的实时告警推送能力，覆盖：
1. WebSocket 连接与订阅
2. 告警创建/更新实时推送
3. 握手鉴权约定
4. 实时总览接口

## 1. 模块能力
实现位置：`src/main/java/com/example/demo/realtime`

已实现内容：
1. WebSocket STOMP 端点：`/ws/alerts`
2. 广播主题：`/topic/alerts`
3. 推送类型：`ALERT_CREATED`、`ALERT_UPDATED`
4. 标准消息体：`eventType + traceId + data`
5. 告警事务提交后推送（`AFTER_COMMIT`）
6. 握手支持 JWT（请求头 `Authorization` 或查询参数 `token`）
7. 总览接口：`GET /api/v1/realtime/overview`

## 2. 协议说明
### 2.1 连接与订阅
1. 连接地址：`ws://{host}:{port}/ws/alerts`
2. 订阅主题：`/topic/alerts`
3. 协议：STOMP over WebSocket

### 2.2 推送消息结构
```json
{
  "eventType": "ALERT_CREATED",
  "traceId": "trc_20260424_001",
  "data": {
    "alertId": 1001,
    "alertNo": "ALT202604071001161234",
    "status": 0,
    "riskLevel": 3,
    "riskScore": 0.89,
    "fatigueScore": 0.91,
    "distractionScore": 0.86,
    "triggerTime": "2026-04-07T10:01:16Z",
    "fleetId": 1001,
    "vehicleId": 2001,
    "driverId": 3001,
    "latestActionBy": 1,
    "latestActionTime": "2026-04-07T10:01:16Z",
    "remark": "系统自动创建"
  }
}
```

字段说明：
1. `eventType`：消息类型（`ALERT_CREATED` 或 `ALERT_UPDATED`）
2. `traceId`：链路追踪 ID，可关联 REST 请求日志与推送日志
3. `data.alertId`：告警主键
4. `data.alertNo`：告警编号
5. `data.status`：告警状态（`0=NEW`、`1=CONFIRMED`、`2=FALSE_POSITIVE`、`3=CLOSED`）
6. `data.riskLevel`：风险等级（`1/2/3`）
7. `data.riskScore/fatigueScore/distractionScore`：风险分与来源分
8. `data.triggerTime`：告警触发时间（UTC）
9. `data.fleetId/vehicleId/driverId`：业务维度
10. `data.latestActionBy/latestActionTime`：最近操作人和最近操作时间
11. `data.remark`：告警当前备注，与 REST 详情字段保持一致

## 3. 推送时机
推送由告警服务发布领域事件，实时模块监听后广播：
1. `POST /api/v1/alerts` 成功创建告警后，推送 `ALERT_CREATED`
2. 告警状态变更成功后，推送 `ALERT_UPDATED`
   - `POST /api/v1/alerts/{id}/confirm`
   - `POST /api/v1/alerts/{id}/false-positive`
   - `POST /api/v1/alerts/{id}/close`
3. 监听器使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，保证事务提交后才推送，避免回滚脏消息

## 4. 鉴权约定
WebSocket 握手沿用现有 JWT 鉴权链路：
1. 优先读取请求头：`Authorization: Bearer <jwt>`
2. 若请求头缺失，读取查询参数：`?token=<jwt>`

说明：
1. STOMP 客户端（如 `stompjs`）建议走 `Authorization` 请求头。
2. 浏览器原生 `WebSocket` 不方便自定义请求头时，可使用 `token` 查询参数。

## 5. 关键代码
1. WebSocket 配置：`src/main/java/com/example/demo/realtime/config/WebSocketConfig.java`
2. 推送监听器：`src/main/java/com/example/demo/realtime/listener/AlertRealtimeEventListener.java`
3. 推送 DTO：
   - `src/main/java/com/example/demo/realtime/dto/AlertRealtimeMessage.java`
   - `src/main/java/com/example/demo/realtime/dto/AlertRealtimeData.java`
4. 告警事件模型：`src/main/java/com/example/demo/alert/event/AlertRealtimeEvent.java`
5. 告警事件发布：`src/main/java/com/example/demo/alert/service/AlertService.java`
6. JWT 令牌解析入口：`src/main/java/com/example/demo/auth/security/JwtAuthenticationFilter.java`

## 6. 测试覆盖
集成测试类：`src/test/java/com/example/demo/realtime/RealtimeModuleIntegrationTest.java`

已覆盖场景：
1. 登录获取 JWT，建立 WebSocket 连接并订阅 `/topic/alerts`
2. 创建告警后收到标准 `ALERT_CREATED` 消息
3. 确认、误报、关闭三类状态变更后均收到 `ALERT_UPDATED`
4. 校验 `traceId`、`data.status`、`data.latestActionTime` 与业务动作一致
5. 校验消息体字段语义与 REST 告警详情一致

执行命令：
```bash
./mvnw -q -Dtest=RealtimeModuleIntegrationTest test
./mvnw -q test
```

## 7. 实时刷新联调检查清单
1. WebSocket 连接成功后，前端连接状态显示为“已连接”。
2. 创建新告警后，收到 `ALERT_CREATED`，且消息体包含 `eventType/traceId/data`。
3. `data.alertId/status/riskLevel/riskScore/fatigueScore/distractionScore/triggerTime` 与 REST 详情一致。
4. 执行 `confirm` 后，收到 `ALERT_UPDATED`，`data.status=1`，`latestActionTime` 前进。
5. 执行 `false-positive` 后，收到 `ALERT_UPDATED`，`data.status=2`。
6. 执行 `close` 后，收到 `ALERT_UPDATED`，`data.status=3`。
7. 当前列表页只做受影响行更新，不做整页重载。
8. 当前详情页查看同一 `alertId` 时，只局部刷新状态、最新操作信息和日志。
9. 重复消息以前端幂等键 `eventType + alertId + latestActionTime` 去重。
10. 连接断开后前端自动重连，失败时提示用户可手动刷新。

## 8. 当前边界
1. 当前使用 Spring Simple Broker（单节点内存广播）。
2. 多实例横向扩容时，建议接入 Redis Pub/Sub 做跨节点消息同步。
3. `GET /api/v1/realtime/overview` 当前由 `alert_event` 聚合得到：
   - 最近 5 分钟指标：告警数据驱动
   - 最新告警流：告警数据驱动
   - 风险分布：告警数据驱动
4. 驾驶员在线数、活跃车辆数、设备心跳等严格实时状态目前尚无独立数据源，不应将总览页口径等同于实时在线监控。

## 9. 总览接口说明
- 路径：`GET /api/v1/realtime/overview`
- 鉴权：`OPERATOR` / `ANALYST` / `VIEWER` / `RISK_ADMIN` / `SYS_ADMIN` / `SUPER_ADMIN`
- 可选参数：`fleetId`

返回能力：
1. 最近 5 分钟告警数
2. 最近 5 分钟高风险告警数
3. 最近 5 分钟已处理告警数
4. 最新 5 条告警流
5. 最近 5 分钟风险等级分布

说明：
1. 当前总览页是“告警数据驱动”的近实时视图。
2. 如果前端需要严格实时在线状态，应在后续接入设备心跳或时序指标数据源后单独扩展接口。
