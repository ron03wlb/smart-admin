# MinIO 对象存储集成文档

> SmartAdmin 文件服务 MinIO 集成完整指南

## 📚 文档导航

### 快速开始

- [**5 分钟快速开始**](./01-quick-start.md) ⚡
  - 启动 MinIO 服务
  - 配置应用连接
  - 验证文件上传

### 配置与使用

- [**配置参考手册**](./02-configuration.md) ⚙️
  - Docker Compose 配置详解
  - 应用配置项说明
  - 多环境配置策略
  - 存储桶结构设计

- [**API 使用示例**](./04-api-usage.md) 💻
  - Controller 层调用
  - Service 层使用
  - HTTP 测试示例
  - 前端集成代码

### 安全与运维

- [**安全最佳实践**](./03-security.md) 🔐
  - 生产环境认证策略
  - 网络隔离配置
  - IAM 权限设计
  - 传输与静态加密
  - 审计日志配置

- [**监控运维指南**](./07-monitoring.md) 📊
  - Prometheus 指标采集
  - Grafana 仪表盘
  - 告警规则配置
  - 容量管理与备份
  - 性能调优建议

### 进阶主题

- [**架构设计说明**](./06-architecture.md) 🏗️
  - 三层架构设计
  - S3 兼容层原理
  - 双层存储模型
  - 访问控制架构
  - 高可用部署方案

- [**故障排查手册**](./05-troubleshooting.md) 🔧
  - 常见问题诊断
  - 错误码解析
  - 性能问题排查
  - 高级诊断工具

---

## 🎯 快速链接

| 场景 | 推荐文档 |
|------|----------|
| 🆕 新项目搭建 | [快速开始](./01-quick-start.md) → [配置参考](./02-configuration.md) |
| 💼 生产环境部署 | [安全实践](./03-security.md) → [监控运维](./07-monitoring.md) |
| 🐛 遇到问题 | [故障排查](./05-troubleshooting.md) → [配置参考](./02-configuration.md) |
| 📖 深入学习 | [架构设计](./06-architecture.md) → [API 使用](./04-api-usage.md) |

---

## 💡 核心特性

### 为什么选择 MinIO？

✅ **S3 API 完全兼容**
- 无需修改代码即可从阿里云 OSS/AWS S3 切换
- 开发环境与生产环境 API 行为一致

✅ **本地开发零成本**
- Docker 一键部署
- 无需外网依赖
- 避免云存储费用

✅ **企业级性能**
- 支持每秒数千次并发请求
- 低延迟（P95 < 50ms）
- 可线性扩展

✅ **高可用架构**
- 纠删码（Erasure Coding）容错
- 多节点集群部署
- 自动故障恢复

---

## 🚀 快速体验

### 1. 启动 MinIO 服务

```bash
cd docker
docker compose up -d minio minio-init
```

### 2. 访问管理控制台

打开浏览器：http://localhost:9001

- 用户名：`smartadmin`
- 密码：`SmartAdmin@2024`

### 3. 配置应用

```yaml
# sa-admin/src/main/resources/dev/sa-base.yaml
file:
  storage:
    mode: cloud
    cloud:
      endpoint: localhost:9000
      bucket-name: dev-smart-admin
      access-key: smartadmin
      secret-key: SmartAdmin@2024
```

### 4. 测试文件上传

```bash
curl -X POST http://localhost:1024/support/file/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.jpg" \
  -F "folder=public/images/"
```

---

## 📊 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| MinIO | LTS (2023-12-23) | 对象存储服务 |
| AWS SDK | 2.20.0 | S3 API 客户端 |
| Docker | 20.10+ | 容器化部署 |
| PostgreSQL | 16 | 文件元数据存储 |
| Redis | 7 | 预签名 URL 缓存 |

---

## 🏗️ 架构概览

```
┌────────────────────────────────────────────┐
│         SmartAdmin Application            │
│  FileStorageCloudServiceImpl              │
│           ↓ AWS S3 SDK                    │
└────────────────────────────────────────────┘
                 ↓ HTTP/HTTPS
┌────────────────────────────────────────────┐
│            MinIO Server                    │
│  - S3 兼容 API                             │
│  - 存储桶管理                               │
│  - 预签名 URL 生成                         │
└────────────────────────────────────────────┘
                 ↓
┌────────────────────────────────────────────┐
│        File System (Volume)                │
│  dev-smart-admin/                          │
│  ├─ public/   (匿名可读)                   │
│  ├─ private/  (需签名 URL)                 │
│  └─ temp/     (24小时自动清理)             │
└────────────────────────────────────────────┘
```

---

## 🔐 安全特性

### 开发环境

- ✅ 简单密码（便于开发）
- ✅ HTTP 协议（无需证书）
- ✅ 本地网络访问

### 生产环境

- 🔒 专用应用账号（非 ROOT）
- 🔒 强密码策略（16+ 字符）
- 🔒 HTTPS 强制加密
- 🔒 IAM 最小权限控制
- 🔒 审计日志记录
- 🔒 网络隔离部署

详见 [安全最佳实践](./03-security.md)

---

## 📈 性能指标

### 开发环境（单节点）

| 指标 | 性能 |
|------|------|
| 上传 QPS | 100-200 req/s |
| 下载 QPS | 200-500 req/s |
| P95 延迟 | < 100ms |
| 并发连接 | 500+ |

### 生产环境（4 节点集群）

| 指标 | 性能 |
|------|------|
| 上传 QPS | 2000+ req/s |
| 下载 QPS | 5000+ req/s |
| P95 延迟 | < 50ms |
| 并发连接 | 10000+ |

详见 [架构设计](./06-architecture.md#性能优化架构)

---

## 🛠️ 运维工具

### 命令行工具 (mc)

```bash
# 查看存储桶列表
docker exec sa21-minio mc ls local/

# 查看文件统计
docker exec sa21-minio mc du --recursive local/dev-smart-admin

# 设置生命周期规则
docker exec sa21-minio mc ilm add --expiry-days 1 local/dev-smart-admin/temp

# 导出监控指标
docker exec sa21-minio mc admin prometheus metrics local/
```

### Web 管理控制台

访问：http://localhost:9001

功能：
- 存储桶管理
- 文件浏览与操作
- 用户与权限管理
- 监控指标查看
- 日志审计

---

## 🆘 获取帮助

### 遇到问题？

1. **检查文档**：[故障排查手册](./05-troubleshooting.md)
2. **查看日志**：`docker logs -f sa21-minio`
3. **健康检查**：`curl http://localhost:9000/minio/health/live`
4. **社区支持**：[MinIO GitHub Issues](https://github.com/minio/minio/issues)

### 常见问题快速导航

| 问题 | 文档链接 |
|------|----------|
| 连接失败 (Access Denied) | [故障排查 #1](./05-troubleshooting.md#1-连接失败access-denied-403) |
| 存储桶不存在 | [故障排查 #2](./05-troubleshooting.md#2-存储桶不存在-nosuchbucket) |
| 私有文件无法访问 | [故障排查 #4](./05-troubleshooting.md#4-私有文件-url-无法访问-403-forbidden) |
| 上传速度慢 | [故障排查 #9](./05-troubleshooting.md#9-性能问题上传下载速度慢) |
| 配置 HTTPS | [安全实践 - 传输加密](./03-security.md#传输加密https) |

---

## 📚 扩展阅读

### 官方资源

- [MinIO 官方文档](https://min.io/docs/minio/linux/index.html)
- [AWS S3 API Reference](https://docs.aws.amazon.com/AmazonS3/latest/API/)
- [SmartAdmin 项目文档](../../README.md)

### 相关技术

- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Prometheus 监控](https://prometheus.io/docs/)
- [Grafana 可视化](https://grafana.com/docs/)

---

## 🔄 版本历史

### v1.0.0 (2024-01-21)

- ✨ 初始版本发布
- 📝 完整文档体系
- 🐳 Docker Compose 集成
- 🔐 生产级安全策略
- 📊 监控告警方案
- 🏗️ 架构设计文档

---

## 📝 文档贡献

发现文档错误或有改进建议？

1. Fork 项目仓库
2. 创建特性分支：`git checkout -b docs/minio-improvements`
3. 提交修改：`git commit -m "docs(minio): improve troubleshooting guide"`
4. 推送分支：`git push origin docs/minio-improvements`
5. 提交 Pull Request

---

## 📄 许可证

本文档遵循 [MIT License](../../LICENSE)

---

**Happy Coding! 🚀**

如有问题，请参阅 [故障排查手册](./05-troubleshooting.md) 或联系团队技术支持。
