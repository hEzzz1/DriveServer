# Redis 设计文档（缓存 + 流处理）

## 1. 目标
1. 提供低延迟实时状态读写。
2. 承担事件异步流转，解耦接入与处理。
3. 提供告警去重与冷却控制能力。

## 2. Key 设计规范
- 前缀统一：`业务域:资源:标识[:子标识]`
- 示例：`realtime:vehicle:veh_001`
- 统一设置 TTL，避免脏数据长期滞留（流数据除外）

## 3. 数据结构规划
| Key | 类型 | TTL | 说明 |
|---|---|---|---|
| `stream:events` | Stream | 无 | 事件主流 |
| `stream:deadletter` | Stream | 无 | 死信流 |
| `cooldown:alert:{vehicleId}:{ruleId}` | String | 30~120s | 告警冷却去重 |
| `realtime:vehicle:{vehicleId}` | Hash | 5~15m | 车辆实时状态 |
| `realtime:driver:{driverId}` | Hash | 5~15m | 司机实时状态 |
| `cache:stats:trend:{fleet}:{window}` | String | 30~120s | 趋势缓存 |
| `cache:stats:ranking:{dim}:{window}` | String | 30~120s | 排行缓存 |
| `ws:online_users` | Set | 无 | 在线用户集合 |

## 4. Stream 消费模型
### 4.1 消费组
- Group A：`rule-engine-group`
- Group B：`timeseries-group`
- Group C：`audit-group`（可选）

### 4.2 消费流程
1. API 侧 `XADD stream:events`。
2. 消费者 `XREADGROUP` 拉取消息。
3. 处理成功后 `XACK`。
4. 失败重试，超阈值写入 `stream:deadletter`。

### 4.3 待处理消息（PEL）治理
1. 定时扫描 PEL 超时消息。
2. 使用 `XCLAIM` 回收孤儿消息。
3. 超重试阈值后标记死信并告警。

## 5. 缓存策略
1. Cache Aside 模式：查询先读缓存，未命中回源 DB 并回填。
2. 热点接口设置短 TTL，降低雪崩影响。
3. 配置随机过期时间抖动，避免同一时刻大量失效。
4. 规则变更后主动删除相关缓存键。

## 6. 去重与冷却设计
1. 告警触发前检查 `cooldown` 键是否存在。
2. 存在则跳过创建，仅更新实时状态。
3. 不存在则创建告警并设置冷却键。

## 7. 高可用建议
1. 开发环境：单实例 Redis。
2. 生产环境：Redis Sentinel 或 Redis Cluster。
3. 开启 AOF 持久化，保障重启恢复能力。

## 8. 监控指标
1. Stream 长度与积压量。
2. 消费延迟、重试次数、死信数量。
3. 缓存命中率与回源次数。
4. 内存使用率与淘汰键数量。

