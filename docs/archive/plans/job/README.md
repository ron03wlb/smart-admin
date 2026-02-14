# Snail-Job 分布式任务调度集成文档

> SmartAdmin 任务调度系统 - Snail-Job 集成完整指南

## 📚 文档导航

### 快速开始

- [**5 分钟快速开始**](./01-quick-start.md) ⚡
  - 启动 Snail-Job 服务
  - 配置应用连接
  - 创建第一个定时任务

### 配置与使用

- [**配置参考手册**](./02-configuration.md) ⚙️
  - Docker Compose 配置详解
  - 应用配置项说明
  - 多环境配置策略
  - 安全配置建议

- [**API 使用示例**](./03-api-usage.md) 💻
  - 创建定时任务（CRON）
  - 实现分布式重试
  - DAG 工作流编排
  - 任务监听器与钩子

### 故障排查与迁移

- [**故障排查手册**](./04-troubleshooting.md) 🔧
  - 常见问题诊断
  - 连接失败处理
  - 任务执行失败排查
  - 性能问题优化

- [**从 SmartJob 迁移**](./07-migration.md) 🔄
  - SmartJob vs Snail-Job 对比
  - API 迁移指南
  - 数据迁移方案
  - 迁移最佳实践

### 进阶主题

- [**架构设计说明**](./05-architecture.md) 🏗️
  - 分布式架构设计
  - 任务调度原理
  - 重试机制设计
  - 高可用部署方案

- [**监控运维指南**](./06-monitoring.md) 📊
  - 任务执行监控
  - 告警规则配置
  - 性能指标分析
  - 日志管理与审计

- [**DAG 工作流编排**](./08-dag-workflows.md) 🔗
  - DAG 概念与原理
  - 工作流设计模式
  - 复杂流程示例
  - 依赖管理与错误处理

---

## 🎯 快速链接

| 场景 | 推荐文档 |
|------|----------|
| 🆕 新项目搭建 | [快速开始](./01-quick-start.md) → [配置参考](./02-configuration.md) |
| 💼 生产环境部署 | [配置参考](./02-configuration.md) → [监控运维](./06-monitoring.md) |
| 🔄 从 SmartJob 迁移 | [迁移指南](./07-migration.md) → [API 使用](./03-api-usage.md) |
| 🐛 遇到问题 | [故障排查](./04-troubleshooting.md) → [架构设计](./05-architecture.md) |
| 📖 深入学习 | [架构设计](./05-architecture.md) → [DAG 工作流](./08-dag-workflows.md) |

---

## 💡 核心特性

### 为什么选择 Snail-Job？

✅ **强大的分布式重试系统**
- 持久化重试状态管理
- 多种退避策略（固定、指数、抖动）
- 重试风暴预防机制
- 多通道告警（邮件、钉钉、企业微信）

✅ **灵活的任务调度**
- CRON 表达式支持
- 固定频率/延迟调度
- 手动触发与 OpenAPI 调用
- 多种执行模式（集群、广播、分片、MapReduce）

✅ **DAG 工作流编排**
- 可视化工作流设计
- 任务依赖关系管理
- 条件分支与并行执行
- 失败重试与补偿机制

✅ **现代化管理控制台**
- 实时任务执行监控
- 可视化配置管理
- 执行日志实时查看
- 性能指标仪表盘

✅ **Spring Boot 3 原生支持**
- 无缝集成 Spring Boot 3.x
- 基于 JDK 17+ 开发
- 支持 Jakarta EE 规范
- 与 SmartAdmin 技术栈完美匹配

✅ **企业级安全特性**
- 客户端 Token 认证
- 细粒度权限控制
- 多租户支持
- 敏感数据加密存储

---

## 🚀 快速体验

### 1. 启动 Snail-Job 服务

```bash
cd docker
docker compose up -d snail-job-server snail-job-init
```

### 2. 访问管理控制台

打开浏览器：http://localhost:8082

- 用户名：`admin`
- 密码：`SmartAdmin@2024`

### 3. 配置应用

```yaml
# sa-admin/src/main/resources/dev/sa-base.yaml
snail-job:
  enabled: true
  server:
    host: localhost
    port: 1788
    namespace: dev
    group-name: smart-admin
    token: ""  # 开发环境无需 token
  client:
    port: 1789
```

### 4. 创建定时任务

```java
@Component
public class DailyReportJob extends BaseSnailJobExecutor {
    @Override
    protected String doExecute(String params) {
        // 业务逻辑：生成日报
        return "Report generated successfully";
    }
}
```

**在控制台配置**:
- CRON: `0 0 8 * * ?` (每天早上 8 点)
- 执行器: `DailyReportJob`

---

## 📊 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Snail-Job | 1.8.1 | 分布式任务调度平台 |
| Spring Boot | 3.5.4 | SmartAdmin 应用框架 |
| JDK | 21 | Java 运行环境 |
| PostgreSQL | 16 | 任务元数据存储 |
| Redis | 7 | 分布式锁与缓存 |
| Netty | 4.x | 高性能通信框架 |

---

## 🏗️ 架构概览

```
┌─────────────────────────────────────────────────┐
│         SmartAdmin Application                  │
│                                                 │
│  ┌─────────────────────────────────────┐       │
│  │  BaseSnailJobExecutor               │       │
│  │  - DailyReportJob                   │       │
│  │  - DataCleanupJob                   │       │
│  │  - EmailNotificationJob             │       │
│  └─────────────────────────────────────┘       │
│              ↓ Snail-Job Client                │
└─────────────────────────────────────────────────┘
                 ↓ Netty RPC (Port 1788)
┌─────────────────────────────────────────────────┐
│         Snail-Job Server                        │
│                                                 │
│  ┌──────────────┐  ┌──────────────┐           │
│  │   Scheduler  │  │ Retry Engine │           │
│  │   Engine     │  │              │           │
│  └──────────────┘  └──────────────┘           │
│                                                 │
│  ┌──────────────┐  ┌──────────────┐           │
│  │ DAG Workflow │  │   Admin      │           │
│  │   Engine     │  │   Console    │           │
│  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────┘
         ↓                           ↓
┌──────────────┐          ┌──────────────┐
│  PostgreSQL  │          │    Redis     │
│  (元数据)     │          │  (分布式锁)   │
└──────────────┘          └──────────────┘
```

---

## 🔐 安全特性

### 开发环境

- ✅ 简单配置（便于开发）
- ✅ 无需 Token 认证
- ✅ HTTP 协议
- ✅ 本地网络访问

### 生产环境

- 🔒 强制 Token 认证
- 🔒 HTTPS 加密传输
- 🔒 最小权限原则（IAM）
- 🔒 审计日志记录
- 🔒 网络隔离部署
- 🔒 定期 Token 轮换

详见 [配置参考 - 安全配置](./02-configuration.md#安全配置)

---

## 📈 性能指标

### 开发环境（单节点）

| 指标 | 性能 |
|------|------|
| 任务调度延迟 | < 100ms |
| 任务执行 QPS | 100-200 req/s |
| 重试处理能力 | 50-100 req/s |
| 并发任务数 | 100+ |

### 生产环境（多节点集群）

| 指标 | 性能 |
|------|------|
| 任务调度延迟 | < 50ms |
| 任务执行 QPS | 2000+ req/s |
| 重试处理能力 | 500+ req/s |
| 并发任务数 | 10000+ |

详见 [架构设计 - 性能优化](./05-architecture.md#性能优化)

---

## 🛠️ 运维工具

### Web 管理控制台

访问：http://localhost:8082

**功能**:
- 📋 任务管理（创建、编辑、删除、启用/禁用）
- 📊 实时监控（执行状态、成功率、耗时统计）
- 📝 执行日志（实时日志、历史记录）
- ⚙️ 配置管理（执行器、命名空间、分组）
- 👥 权限管理（用户、角色、权限）
- 🔔 告警配置（邮件、钉钉、企业微信）

### 命令行工具

**健康检查**:
```bash
# 服务器健康检查
curl http://localhost:8082/actuator/health

# 客户端注册状态
docker logs sa21-snail-job-server | grep "client registered"
```

**日志查看**:
```bash
# 查看服务器日志
docker logs -f sa21-snail-job-server

# 查看初始化日志
docker logs sa21-snail-job-init
```

**性能监控**:
```bash
# 导出 Prometheus 指标
curl http://localhost:8082/actuator/prometheus
```

---

## 🆘 获取帮助

### 遇到问题？

1. **检查文档**：[故障排查手册](./04-troubleshooting.md)
2. **查看日志**：`docker logs -f sa21-snail-job-server`
3. **健康检查**：`curl http://localhost:8082/actuator/health`
4. **社区支持**：[Snail-Job GitHub Issues](https://github.com/aizuda/snail-job/issues)

### 常见问题快速导航

| 问题 | 文档链接 |
|------|----------|
| 服务器启动失败 | [故障排查 #1](./04-troubleshooting.md#1-服务器启动失败) |
| 客户端连接失败 | [故障排查 #2](./04-troubleshooting.md#2-客户端连接失败) |
| 任务执行失败 | [故障排查 #3](./04-troubleshooting.md#3-任务执行失败) |
| 任务未触发 | [故障排查 #4](./04-troubleshooting.md#4-任务未按预期触发) |
| 重试不生效 | [故障排查 #5](./04-troubleshooting.md#5-分布式重试不生效) |
| 性能问题 | [故障排查 #6](./04-troubleshooting.md#6-性能问题) |

---

## 📚 扩展阅读

### 官方资源

- [Snail-Job 官方文档](https://snailjob.opensnail.com/)
- [Snail-Job GitHub](https://github.com/aizuda/snail-job)
- [快速开始指南](https://snailjob.opensnail.com/docs/quickstart/quick_start.html)
- [SmartAdmin 项目文档](../../README.md)

### 相关技术

- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot 文档](https://docs.spring.io/spring-boot/docs/current/reference/)
- [Netty 文档](https://netty.io/wiki/index.html)

### 任务调度对比

| 特性 | Snail-Job | XXL-Job | PowerJob |
|------|-----------|---------|----------|
| 分布式重试 | ✅ 内置 | ❌ 无 | ⚠️ 基础 |
| DAG 工作流 | ✅ 支持 | ❌ 无 | ✅ 支持 |
| Spring Boot 3 | ✅ 原生 | ✅ 兼容 | ✅ 兼容 |
| 多语言客户端 | ✅ Java/Go/Python | ❌ Java only | ✅ 多语言 |
| 内存占用 | 中等 | 低 | 高 |
| 学习曲线 | 中等 | 低 | 中等 |

---

## 🔄 版本历史

### v1.0.0 (2026-01-22)

- ✨ 初始版本发布
- 📝 完整文档体系（8 文件）
- 🐳 Docker Compose 集成
- 🔐 生产级安全配置
- 📊 监控告警方案
- 🏗️ 架构设计文档
- 🔄 SmartJob 迁移指南
- 🔗 DAG 工作流示例

---

## 📝 文档贡献

发现文档错误或有改进建议？

1. Fork 项目仓库
2. 创建特性分支：`git checkout -b docs/job-improvements`
3. 提交修改：`git commit -m "docs(job): improve troubleshooting guide"`
4. 推送分支：`git push origin docs/job-improvements`
5. 提交 Pull Request

---

## 📄 许可证

本文档遵循 [MIT License](../../LICENSE)

---

**Happy Coding! 🚀**

如有问题，请参阅 [故障排查手册](./04-troubleshooting.md) 或访问 [Snail-Job 官方文档](https://snailjob.opensnail.com/)。
