# Rule Module 实现文档

本文档说明 `DriveServer` 当前已落地的规则判定能力，重点覆盖：
1. 风险分计算与阈值判定
2. 规则管理 CRUD、版本历史与回滚
3. 冷却与分钟桶去重
4. 规则配置发布后驱动运行时加载

## 1. 目标范围
已实现内容：
1. `risk_score = max(fatigue_score, distraction_score)`
2. 疲劳分与分心分分开判阈值，支持“疲劳更严格、分心更宽松”
3. 按 `vehicleId + ruleId + channel` 维度的状态分累积与衰减
4. 按 `vehicleId + ruleId + minute_bucket` 的分钟桶去重
5. 按 `cooldown:alert:{vehicleId}:{ruleId}` 的冷却抑制
6. `/api/v1/events -> 规则判定 -> 自动创建告警` 的编排链路
7. 规则编排结果输出（触发/抑制 + 原因）

当前未接入内容（后续阶段）：
1. Redis 持久化冷却键与去重键
2. 独立 Stream 消费者异步调用规则模块
3. 规则发布后的缓存失效与热更新优化

## 2. 核心模型
核心对象位于 `src/main/java/com/example/demo/rule/model`：
1. `RuleEvent`：输入事件（`vehicleId`、`eventTime`、`fatigueScore`、`distractionScore`）
2. `RuleDefinition`：运行时规则定义（规则 ID、规则码、风险等级、阈值、持续时长、冷却时长、启用状态）
3. `RiskLevel`：风险等级枚举（`NORMAL/LOW/MID/HIGH`）
4. `RuleEvaluationResult`：判定结果（是否触发、分值、等级、抑制原因）
5. `RuleSuppressionReason`：抑制原因枚举
6. `RuleConfig` / `RuleConfigVersion`：规则管理当前态与版本快照

## 3. 默认规则
默认规则由 `RuleDefinition.defaultRiskRules()` 提供：

| 规则 | `riskScore` 展示阈值 | 疲劳阈值 | 分心阈值 | 状态分累积秒数 | 冷却时长 |
|---|---:|---:|---:|---:|---:|
| `RISK_HIGH` | `0.80` | `0.88` | `0.78` | `2s` | `60s` |
| `RISK_MID` | `0.65` | `0.72` | `0.60` | `3s` | `60s` |
| `RISK_LOW` | `0.50` | `0.58` | `0.45` | `4s` | `60s` |

## 4. 判定流程
入口：`RuleEngineService#evaluate`

1. 计算风险分（`RiskScoreCalculator`）
2. 按风险等级从高到低遍历启用规则
3. 用 `RiskStateTracker` 计算当前规则档位下的疲劳状态分与分心状态分
4. 若状态分命中阈值，再做冷却去重（`AlertCooldownDeduplicator`）
5. 输出触发结果或抑制原因

说明：
1. 若高等级规则已满足状态分阈值，但命中冷却/去重，则直接抑制，不降级触发更低等级规则。
2. 抑制原因包括：
   - `SCORE_BELOW_THRESHOLD`
   - `DEDUP_IN_MINUTE_BUCKET`
   - `IN_COOLDOWN`

### 4.1 风险分与源阈值说明
当前实现中，`risk_score` 直接取疲劳分和分心分中的较大值：

```text
risk_score = max(fatigue_score, distraction_score)
```

这样可以保证任一维度风险过高时，不会被另一项较低分数稀释。

但真正决定某条规则是否命中的，不再只是统一的 `riskScore` 阈值，而是每条规则分别判断：
1. `fatigueStateScore >= fatigueThreshold`
2. 或 `distractionStateScore >= distractionThreshold`

默认口径下：
1. 分心阈值低于同档疲劳阈值，因此分心更容易进入观察与累计
2. 疲劳阈值更高，因此疲劳驾驶只有在状态分积累得更充分时才会触发同档告警
3. `riskScore` 仍保留为统一展示分、日志分和详情分

示例：
1. `fatigueScore=0.56`、`distractionScore=0.20`，低风险疲劳状态分会缓慢上升，但很难跨过 `0.58`
2. `fatigueScore=0.20`、`distractionScore=0.46`，低风险分心状态分更容易持续累积到 `0.45`
3. `fatigueScore=0.92`、`distractionScore=0.30`，则 `riskScore=0.92`，高风险疲劳状态分会较快升高
### 4.2 状态分说明
当前实现把“持续时间”吸收到状态分模型里，不再单独按累计命中秒数判定。

规则：
1. 仍按 `vehicleId + ruleId + channel` 维度独立维护状态分
2. 原始分数会被转换成规则相关的证据分，持续高分会推动状态分上升
3. 事件间隔越长，旧状态分衰减越明显
4. 状态分一旦掉下去，不会像旧模型那样瞬间清零，而是逐步回落

运行时仅读取数据库中启用且已发布的规则配置；若没有启用规则，摄取请求仍会接收，但不会创建告警。

## 5. 去重与冷却策略
`AlertCooldownDeduplicator` 当前为内存实现：
1. 分钟桶键：`vehicleId:ruleId:epochMinute`，TTL 固定 2 分钟
2. 冷却键：`cooldown:alert:{vehicleId}:{ruleId}`，TTL 使用规则 `cooldownSeconds`
3. 判定顺序：先分钟桶，再冷却键

返回结果：
1. `ALLOWED`
2. `BLOCKED_BY_MINUTE_BUCKET`
3. `BLOCKED_BY_COOLDOWN`

## 6. 规则管理
规则管理接口位于 `/api/v1/rules`，仅 `ADMIN` 可访问：
1. `GET /api/v1/rules`：规则列表
2. `GET /api/v1/rules/{id}`：规则详情
3. `POST /api/v1/rules`：新建草稿
4. `PUT /api/v1/rules/{id}`：编辑未启用规则，编辑不会直接生效
5. `POST /api/v1/rules/{id}/publish`：发布并生成版本快照
6. `POST /api/v1/rules/{id}/toggle`：启停
7. `GET /api/v1/rules/{id}/versions`：版本历史
8. `POST /api/v1/rules/{id}/rollback`：回滚到历史版本并生成新版本

状态口径：
1. `DRAFT`
2. `PENDING_PUBLISH`
3. `ENABLED`
4. `DISABLED`
5. `ARCHIVED`

规则版本写入到 `rule_config_version`，当前态保留在 `rule_config`。
同一 `riskLevel` 同时只允许一条规则处于启用状态。

## 7. 测试覆盖
测试类位于 `src/test/java/com/example/demo/rule`：
1. `RiskScoreCalculatorTest`
2. `AlertCooldownDeduplicatorTest`
3. `RuleEngineServiceTest`
4. `RuleManagementModuleIntegrationTest`

已覆盖场景：
1. 风险分取较高值与边界值
2. 分钟桶去重与跨分钟冷却抑制
3. 触发、抑制、恢复触发的端到端路径
4. 规则管理发布、回滚、审计写入与权限收口

执行命令：
```bash
./mvnw test -q
```
