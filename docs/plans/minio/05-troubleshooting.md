# MinIO 故障排查手册

> 遇到问题？本指南帮助您快速诊断和解决常见问题

## 🔍 快速诊断清单

```bash
# 1. 检查 MinIO 容器状态
docker ps | grep minio
docker logs sa21-minio --tail 50

# 2. 检查网络连接
curl -I http://localhost:9000/minio/health/live

# 3. 检查存储桶
docker exec sa21-minio mc ls local/
```

## ❌ 常见问题及解决方案

### 1. 连接失败：Access Denied (403)

**症状**：
```
software.amazon.awssdk.services.s3.model.S3Exception: Access Denied
```

**解决步骤**：

```bash
# 验证 MinIO 凭证
docker exec sa21-minio printenv | grep MINIO_ROOT

# 检查应用配置
grep -A 5 "access-key" sa-admin/src/main/resources/dev/sa-base.yaml
```

### 2. 存储桶不存在 (NoSuchBucket)

**症状**：
```
NoSuchBucketException: The specified bucket does not exist
```

**解决步骤**：

```bash
# 检查存储桶是否存在
docker exec sa21-minio mc ls local/

# 手动创建存储桶
docker exec sa21-minio mc mb local/dev-smart-admin

# 设置公共读策略
docker exec sa21-minio mc anonymous set download local/dev-smart-admin/public
```

### 3. 连接超时 (Connection Timeout)

**症状**：
```
java.net.ConnectException: Connection timed out
```

**解决步骤**：

```bash
# 确认 MinIO 正在运行
docker ps | grep minio

# 检查端口映射
docker port sa21-minio

# 测试端口连通性
curl -v http://localhost:9000/minio/health/live
```

### 4. 私有文件 URL 无法访问

**症状**：访问预签名 URL 返回 403

**解决步骤**：

```bash
# 重新生成预签名 URL
curl -X GET "http://localhost:1024/support/file/url?fileKey=private/test.pdf" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 检查服务器时间同步
date  # 宿主机时间
docker exec sa21-minio date  # MinIO 容器时间
```

### 5. Docker 容器无法启动

**解决步骤**：

```bash
# 检查端口占用
lsof -i :9000
lsof -i :9001

# 查看详细错误日志
docker compose logs minio
```

## 🔧 高级诊断工具

### 启用 MinIO 调试日志

```bash
docker exec -it sa21-minio mc admin trace local/ --verbose
```

### 检查对象元数据

```bash
docker exec sa21-minio mc stat local/dev-smart-admin/path/to/file.pdf
```

## 📞 获取帮助

收集诊断信息：

```bash
# 1. 系统信息
docker --version
docker compose version

# 2. 容器日志
docker logs sa21-minio --tail 100 > minio-logs.txt

# 3. 配置信息（脱敏）
cat sa-admin/src/main/resources/dev/sa-base.yaml | grep -A 15 "file:" > config.txt
```

## 📖 相关文档

- [快速开始](./01-quick-start.md) - 重新搭建环境
- [配置参考](./02-configuration.md) - 检查配置项
- [安全最佳实践](./03-security.md) - 权限和安全问题
