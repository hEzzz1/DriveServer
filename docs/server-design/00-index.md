# 驾驶员疲劳与分心驾驶实时监测系统（服务器端）文档总览

## 1. 文档范围
本套文档用于构建一个“可长期演进的个人开发成熟案例”，仅覆盖服务器端设计，技术栈固定为：

- Spring Boot
- MySQL
- Redis
- InfluxDB 3 Core

不包含边缘设备硬件选型与设备端算法实现。

## 2. 阅读顺序
1. [01-requirements.md](./01-requirements.md)
2. [02-architecture.md](./02-architecture.md)
3. [03-api-spec.md](./03-api-spec.md)
4. [04-mysql-design.md](./04-mysql-design.md)
5. [05-influxdb-design.md](./05-influxdb-design.md)
6. [06-redis-design.md](./06-redis-design.md)
7. [07-rule-engine.md](./07-rule-engine.md)
8. [08-deployment-ops.md](./08-deployment-ops.md)
9. [09-security-compliance.md](./09-security-compliance.md)
10. [10-test-acceptance.md](./10-test-acceptance.md)
11. [11-project-plan.md](./11-project-plan.md)

## 3. 交付清单映射
- 需求规格说明书：`01-requirements.md`
- 总体设计说明书：`02-architecture.md`
- 详细设计说明书：`03~07`
- 部署运维手册：`08-deployment-ops.md`
- 安全设计说明：`09-security-compliance.md`
- 测试报告模板：`10-test-acceptance.md`
- 路线图与里程碑：`11-project-plan.md`
