# Alert Module 实现文档

本文档说明 `DriveServer` 当前已落地的告警闭环能力，覆盖：
1. 创建告警
2. 告警确认
3. 误报标注
4. 告警关闭
5. 操作日志追溯

## 1. 模块能力
实现位置：`src/main/java/com/example/demo/alert`

已实现接口：
1. `POST /api/v1/alerts`：创建告警（`ADMIN`/`OPERATOR`）
2. `POST /api/v1/alerts/{id}/confirm`：确认告警（`ADMIN`/`OPERATOR`）
3. `POST /api/v1/alerts/{id}/false-positive`：标注误报（`ADMIN`/`OPERATOR`）
4. `POST /api/v1/alerts/{id}/close`：关闭告警（`ADMIN`/`OPERATOR`）
5. `GET /api/v1/alerts/{id}/action-logs`：查询操作日志（`ADMIN`/`OPERATOR`/`VIEWER`）

统一响应：`ApiResponse`

## 2. 数据落库
核心表：
1. `alert_event`
2. `alert_action_log`

关键行为：
1. 创建告警时，`alert_event` 写入一条主记录，同时写入 `alert_action_log` 的 `CREATE` 记录。
2. 确认/误报/关闭时，更新 `alert_event.status`、`latest_action_by`、`latest_action_time`、`remark`。
3. 每次状态操作都会追加 `alert_action_log`，可完整追溯历史动作。

## 3. 状态流转规则
状态定义：
1. `0 = NEW`
2. `1 = CONFIRMED`
3. `2 = FALSE_POSITIVE`
4. `3 = CLOSED`

允许流转：
1. `NEW -> CONFIRMED/FALSE_POSITIVE/CLOSED`
2. `CONFIRMED -> FALSE_POSITIVE/CLOSED`

不允许流转：
1. `FALSE_POSITIVE` 和 `CLOSED` 为终态，不可继续流转。
2. 非法流转统一返回：
   - HTTP `400`
   - `code = 40001`
   - `message = 当前状态不允许该操作`

## 4. 操作日志
动作类型：
1. `CREATE`
2. `CONFIRM`
3. `FALSE_POSITIVE`
4. `CLOSE`

日志接口：`GET /api/v1/alerts/{id}/action-logs`
- 按 `action_time`、`id` 升序返回，便于前端按时间线展示。

当前日志项结构：
1. `id`：日志主键，便于稳定追踪与前端 key 绑定
2. `actionType`：动作枚举
3. `actionBy`：操作人用户 ID
4. `actionTime`：操作时间
5. `actionRemark`：操作备注

## 5. 测试覆盖
测试文件：`src/test/java/com/example/demo/alert/AlertModuleIntegrationTest.java`

覆盖场景：
1. 创建 -> 确认 -> 关闭，日志链路完整
2. 创建 -> 误报，日志正确写入
3. `VIEWER` 无法执行确认操作
4. `CLOSED` 与 `FALSE_POSITIVE` 终态后继续流转，统一返回固定业务错误
5. 操作日志返回稳定结构，包含 `id/actionType/actionBy/actionTime/actionRemark`

执行命令：
```bash
./mvnw test -q
```
