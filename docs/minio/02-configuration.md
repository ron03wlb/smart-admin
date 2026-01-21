# MinIO 配置参考

## Docker Compose 配置

### 环境变量

| 变量名 | 默认值 | 说明 | 环境 |
|--------|--------|------|------|
| `MINIO_ROOT_USER` | smartadmin | 管理员账号 | dev/test |
| `MINIO_ROOT_PASSWORD` | SmartAdmin@2024 | 管理员密码 | dev/test |
| `MINIO_API_PORT` | 9000 | S3 API 端口 | 所有 |
| `MINIO_CONSOLE_PORT` | 9001 | Web 控制台端口 | dev/test |
| `MINIO_REGION_NAME` | us-east-1 | 区域名称 | 所有 |

### Volume 配置

```yaml
volumes:
  minio_data:
    name: sa21-minio-data
    driver: local
```

## 应用配置详解

### 开发环境（dev）

```yaml
file:
  storage:
    mode: cloud
    cloud:
      endpoint: localhost:9000
      bucket-name: dev-smart-admin
      access-key: smartadmin
      secret-key: SmartAdmin@2024
      url-prefix: http://localhost:9000/dev-smart-admin/
```

### 生产环境（prod）

```yaml
file:
  storage:
    mode: cloud
    cloud:
      endpoint: minio.yourdomain.com
      bucket-name: prod-smart-admin
      access-key: ${PROD_MINIO_ACCESS_KEY}
      secret-key: ${PROD_MINIO_SECRET_KEY}
      url-prefix: https://minio.yourdomain.com/prod-smart-admin/
```

## 存储桶结构设计

```
{env}-smart-admin/
├── public/                 # 公共可读（匿名访问）
│   ├── images/            # 产品图片、头像
│   └── documents/         # 公开文档
├── private/               # 私有文件（需预签名 URL）
│   ├── contracts/         # 合同文档
│   └── reports/           # 业务报表
└── temp/                  # 临时文件（24小时自动删除）
```

## 详细配置说明

请参阅完整的配置参考文档以了解所有配置选项。
