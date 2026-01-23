# MinIO 快速开始指南

> 5 分钟内完成 MinIO 本地开发环境搭建

## 📋 前置条件

- Docker 20.10+
- Docker Compose 2.0+
- 可用端口：9000（API）、9001（Console）

## 🚀 启动步骤

### 1. 启动 MinIO 服务

```bash
cd docker
docker compose up -d minio minio-init
```

**预期输出**：
```
✔ Container sa21-minio       Started
✔ Container sa21-minio-init  Started
```

### 2. 验证服务状态

```bash
# 检查容器运行状态
docker ps | grep minio

# 查看初始化日志
docker logs sa21-minio-init
```

**成功标志**：
```
MinIO initialization completed!
✔ Bucket created: dev-smart-admin
✔ Public policy set
```

### 3. 访问管理控制台

打开浏览器访问：**http://localhost:9001**

**登录凭证**：
- 用户名：`smartadmin`
- 密码：`SmartAdmin@2024`

### 4. 配置应用连接

编辑 `sa-admin/src/main/resources/dev/sa-base.yaml`：

```yaml
file:
  storage:
    mode: cloud
    cloud:
      endpoint: localhost:9000
      bucket-name: dev-smart-admin
      access-key: smartadmin
      secret-key: SmartAdmin@2024
```

### 5. 启动后端应用

```bash
cd ../smart-admin-api-java21-springboot3
./gradlew :sa-admin:bootRun
```

**验证连接成功**：查看启动日志，应无 S3 相关异常。

### 6. 测试文件上传

#### 方式 1：Swagger UI

1. 访问 http://localhost:1024/swagger-ui.html
2. 找到 `/support/file/upload` 接口
3. 上传测试文件（folder 参数填 `public/test/`）

#### 方式 2：curl 命令

```bash
curl -X POST http://localhost:1024/support/file/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.jpg" \
  -F "folder=public/images/"
```

**成功响应**：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "fileKey": "public/images/abc123_20240121120000.jpg",
    "fileUrl": "http://localhost:9000/dev-smart-admin/public/images/abc123_20240121120000.jpg",
    "fileName": "abc123_20240121120000.jpg",
    "fileSize": 102400,
    "fileType": "jpg"
  }
}
```

### 7. 验证文件访问

在浏览器中打开 `fileUrl`，应能正常显示图片。

## 🎉 完成！

现在您已经成功搭建了 MinIO 开发环境，可以开始文件存储相关的开发工作。

## 📚 下一步

- [配置参考](./02-configuration.md) - 了解详细配置选项
- [API 使用示例](./04-api-usage.md) - 学习如何在代码中使用
- [故障排查](./05-troubleshooting.md) - 遇到问题时查阅
