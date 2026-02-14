# MinIO 安全最佳实践

> 本指南涵盖从开发到生产的全方位安全策略

## 🔐 认证安全

### 开发环境

```yaml
MINIO_ROOT_USER: smartadmin
MINIO_ROOT_PASSWORD: SmartAdmin@2024
```

### 生产环境

**强制要求**：
- 使用专用应用账号（禁止使用 ROOT）
- 密码最少 16 字符
- 包含大小写字母、数字、特殊字符
- 每 90 天轮换一次密钥

**示例：创建应用专用账号**

```bash
# 创建应用专用账号
mc admin user add prod-minio smartadmin-app [GENERATED_PASSWORD]
mc admin policy set prod-minio readwrite user=smartadmin-app
```

## 🛡️ 网络隔离

### 生产环境网络架构

```
Internet
    ↓
[Nginx/CloudFront CDN]
    ↓ (HTTPS only)
[Application Server]
    ↓ (Internal network only)
[MinIO Server] ← NO direct public access
```

**Docker Compose 配置**：

```yaml
# 生产环境 MinIO
minio:
  ports:
    - "127.0.0.1:9000:9000"  # 仅本地访问
  networks:
    - backend-internal

networks:
  backend-internal:
    driver: bridge
    internal: true  # 隔离外部网络
```

## 🔑 IAM 策略（最小权限原则）

**应用账号权限示例**：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::prod-smart-admin/*"
    }
  ]
}
```

## 🔒 传输加密（HTTPS）

### 配置 MinIO

```yaml
minio:
  volumes:
    - /etc/letsencrypt/live/minio.yourdomain.com/fullchain.pem:/root/.minio/certs/public.crt:ro
    - /etc/letsencrypt/live/minio.yourdomain.com/privkey.pem:/root/.minio/certs/private.key:ro
  environment:
    MINIO_SERVER_URL: "https://minio.yourdomain.com"
```

## 💾 静态加密

**服务端加密（SSE-S3）**：

```yaml
minio:
  environment:
    MINIO_KMS_SECRET_KEY: "your-32-byte-long-secret-master-key"
```

## 📊 审计日志

```yaml
minio:
  environment:
    MINIO_AUDIT_LOGGER_ENABLE_console: "on"
    MINIO_AUDIT_WEBHOOK_ENDPOINT_audit_log: "http://logstash:5044"
```

## 🔍 安全检查清单

### 生产环境

- [ ] 使用专用应用账号（非 ROOT）
- [ ] 密码 16+ 字符，复杂度要求
- [ ] 密钥存储在密钥管理系统
- [ ] 强制 HTTPS
- [ ] 启用静态加密（SSE）
- [ ] 启用审计日志
- [ ] 网络完全隔离
- [ ] Console 端口不对外暴露
- [ ] 配置防火墙规则
- [ ] 每 90 天轮换密钥
