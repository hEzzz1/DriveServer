# Realtime Module 实现文档

本文档说明 `DriveServer` 当前已落地的实时告警推送能力，覆盖：
1. WebSocket 连接与订阅
2. 告警创建/更新实时推送
3. 握手鉴权约定

## 1. 模块能力
实现位置：`src/main/java/com/example/demo/realtime`

已实现内容：
1. WebSocket STOMP 端点：`/ws/alerts`
2. 广播主题：`/topic/alerts`
3. 推送类型：`ALERT_CREATED`、`ALERT_UPDATED`
4. 告警事务提交后推送（`AFTER_COMMIT`）
5. 握手支持 JWT（请求头 `Authorization` 或查询参数 `token`）

## 2. 协议说明
### 2.1 连接与订阅
1. 连接地址：`ws://{host}:{port}/ws/alerts`
2. 订阅主题：`/topic/alerts`
3. 协议：STOMP over WebSocket

### 2.2 推送消息结构
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
1. `type`：消息类型（`ALERT_CREATED` 或 `ALERT_UPDATED`）
2. `timestamp`：告警最近动作时间（UTC）
3. `payload.alertId`：告警主键
4. `payload.alertNo`：告警编号
5. `payload.fleetId/vehicleId/driverId`：业务维度
6. `payload.riskLevel`：风险等级（`1/2/3`）
7. `payload.status`：告警状态（`0=NEW`、`1=CONFIRMED`、`2=FALSE_POSITIVE`、`3=CLOSED`）
8. `payload.actionType`：触发动作（`CREATE`、`CONFIRM`、`FALSE_POSITIVE`、`CLOSE`）

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
   - `src/main/java/com/example/demo/realtime/dto/AlertPushPayload.java`
4. 告警事件模型：`src/main/java/com/example/demo/alert/event/AlertRealtimeEvent.java`
5. 告警事件发布：`src/main/java/com/example/demo/alert/service/AlertService.java`
6. JWT 令牌解析入口：`src/main/java/com/example/demo/auth/security/JwtAuthenticationFilter.java`

## 6. 测试覆盖
集成测试类：`src/test/java/com/example/demo/realtime/RealtimeModuleIntegrationTest.java`

已覆盖场景：
1. 登录获取 JWT，建立 WebSocket 连接并订阅 `/topic/alerts`
2. 创建告警后收到 `ALERT_CREATED`
3. 确认告警后收到 `ALERT_UPDATED`
4. 校验 `payload.alertId/actionType/status` 与业务动作一致

执行命令：
```bash
./mvnw -q -Dtest=RealtimeModuleIntegrationTest test
./mvnw -q test
```

## 7. 当前边界
1. 当前使用 Spring Simple Broker（单节点内存广播）。
2. 多实例横向扩容时，建议接入 Redis Pub/Sub 做跨节点消息同步。
