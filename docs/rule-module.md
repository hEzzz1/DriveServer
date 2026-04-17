# Rule Module 实现文档

本文档说明 `DriveServer` 当前已落地的规则判定能力，重点覆盖：
1. 风险公式计算
2. 持续时长判定
3. 冷却与分钟桶去重

## 1. 目标范围
已实现内容：
1. `risk_score = max(fatigue_score, distraction_score)`
2. 按 `vehicleId + ruleId` 维度的连续时长判定
3. 按 `vehicleId + ruleId + minute_bucket` 的分钟桶去重
4. 按 `cooldown:alert:{vehicleId}:{ruleId}` 的冷却抑制
5. `/api/v1/events -> 规则判定 -> 自动创建告警` 的编排链路
6. 规则编排结果输出（触发/抑制 + 原因）

当前未接入内容（后续阶段）：
1. Redis 持久化冷却键与去重键
2. 独立 Stream 消费者异步调用规则模块

## 2. 核心模型
核心对象位于 `src/main/java/com/example/demo/rule/model`：
1. `RuleEvent`：输入事件（`vehicleId`、`eventTime`、`fatigueScore`、`distractionScore`）
2. `RuleDefinition`：规则配置（阈值、持续时长、冷却时长、风险等级）
3. `RiskLevel`：风险等级枚举（`NORMAL/LOW/MID/HIGH`）
4. `RuleEvaluationResult`：判定结果（是否触发、分值、等级、抑制原因）
5. `RuleSuppressionReason`：抑制原因枚举

## 3. 默认规则
默认规则由 `RuleDefinition.defaultRiskRules()` 提供：

| 规则 | 阈值 | 持续时长 | 冷却时长 |
|---|---:|---:|---:|
| `RISK_HIGH` | `0.80` | `3s` | `60s` |
| `RISK_MID` | `0.65` | `5s` | `60s` |
| `RISK_LOW` | `0.50` | `8s` | `60s` |

## 4. 判定流程
入口：`RuleEngineService#evaluate`

1. 计算风险分（`RiskScoreCalculator`）
2. 按风险等级从高到低遍历启用规则
3. 先判阈值，再判持续时长（`DurationJudge`）
4. 若满足持续时长，再做冷却去重（`AlertCooldownDeduplicator`）
5. 输出触发结果或抑制原因

说明：
1. 若高等级规则已满足阈值和持续时长，但命中冷却/去重，则直接抑制，不降级触发更低等级规则。
2. 抑制原因包括：
   - `SCORE_BELOW_THRESHOLD`
   - `DURATION_NOT_MET`
   - `DEDUP_IN_MINUTE_BUCKET`
   - `IN_COOLDOWN`

### 4.1 风险分说明
当前实现中，`risk_score` 直接取疲劳分和分心分中的较大值：

```text
risk_score = max(fatigue_score, distraction_score)
```

这样可以保证任一维度风险过高时，不会被另一项较低分数稀释。

示例：
1. `fatigueScore=0.92`、`distractionScore=0.30`，则 `riskScore=0.92`
2. `fatigueScore=0.31`、`distractionScore=0.91`，则 `riskScore=0.91`

## 5. 去重与冷却策略
`AlertCooldownDeduplicator` 当前为内存实现：
1. 分钟桶键：`vehicleId:ruleId:epochMinute`，TTL 固定 2 分钟
2. 冷却键：`cooldown:alert:{vehicleId}:{ruleId}`，TTL 使用规则 `cooldownSeconds`
3. 判定顺序：先分钟桶，再冷却键

返回结果：
1. `ALLOWED`
2. `BLOCKED_BY_MINUTE_BUCKET`
3. `BLOCKED_BY_COOLDOWN`

## 6. 测试覆盖
测试类位于 `src/test/java/com/example/demo/rule`：
1. `RiskScoreCalculatorTest`
2. `DurationJudgeTest`
3. `AlertCooldownDeduplicatorTest`
4. `RuleEngineServiceTest`

已覆盖场景：
1. 风险分取较高值与边界值
2. 持续时长临界秒触发
3. 分钟桶去重与跨分钟冷却抑制
4. 触发、抑制、恢复触发的端到端路径

执行命令：
```bash
./mvnw test -q
```
