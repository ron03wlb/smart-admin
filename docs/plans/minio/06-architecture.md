# MinIO 架构设计说明

> 深入理解 SmartAdmin 文件存储架构与 MinIO 集成方案

## 📐 整体架构

### 三层架构设计

```
┌─────────────────────────────────────────┐
│       Presentation Layer                │
│  Controller (REST API)                  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│        Business Layer                    │
│  IFileStorageService 接口抽象            │
│  ├─ FileStorageLocalServiceImpl         │
│  └─ FileStorageCloudServiceImpl (MinIO) │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│       Persistence Layer                  │
│  ├─ FileDao (元数据)                     │
│  └─ MinIO / S3 (实际文件)                │
└─────────────────────────────────────────┘
```

### 存储模式切换机制

```java
@Configuration
public class FileConfig {
    
    @Bean
    @ConditionalOnProperty(name = "file.storage.mode", havingValue = "local")
    public IFileStorageService initLocalFileService() {
        return new FileStorageLocalServiceImpl();
    }
    
    @Bean
    @ConditionalOnProperty(name = "file.storage.mode", havingValue = "cloud")
    public IFileStorageService initCloudFileService() {
        return new FileStorageCloudServiceImpl();
    }
}
```

**优势**：
- ✅ 零代码修改：切换存储只需修改配置
- ✅ 接口统一：业务代码无需关心底层实现
- ✅ 易于测试：可 mock IFileStorageService 接口

## 🔌 MinIO 集成架构

### S3 兼容层

```
┌──────────────────────────────────┐
│  SmartAdmin Application          │
│  FileStorageCloudServiceImpl     │
│       ↓                          │
│  AWS SDK for Java                │
└──────────────────────────────────┘
           ↓ S3 API
┌──────────────────────────────────┐
│       MinIO Server               │
│  - S3 兼容 API 层                │
└──────────────────────────────────┘
           ↓
┌──────────────────────────────────┐
│  File System (Volume)            │
└──────────────────────────────────┘
```

### 关键技术点

**S3 API 兼容性**：
- ✅ 基础操作：PUT/GET/DELETE
- ✅ 预签名 URL：Presigned URL
- ✅ 存储桶策略：Bucket Policy
- ✅ 对象元数据：User Metadata

## 🗂️ 数据存储架构

### 双层存储模型

```
┌───────────────────────────────┐
│  PostgreSQL (元数据)          │
│  t_file 表                    │
│  ├─ file_key: 唯一标识        │
│  ├─ file_name: 原始文件名     │
│  ├─ file_size: 文件大小       │
│  └─ ...                       │
└───────────────────────────────┘
            ↕ (关联)
┌───────────────────────────────┐
│  MinIO (实际文件数据)          │
│  file_key → 二进制数据         │
└───────────────────────────────┘
```

## 🔐 访问控制架构

### 公共 vs 私有文件策略

```
File Upload
    ↓
判断文件夹前缀
    ↓
┌──────────┴──────────┐
↓                     ↓
public/*         其他前缀
↓                     ↓
PUBLIC_READ      PRIVATE
↓                     ↓
匿名可读          需预签名 URL
```

### 预签名 URL 生成流程

```
1. 请求 GET /support/file/url?fileKey=xxx
    ↓
2. 检查 Redis 缓存
    ↓ (未命中)
3. 查询数据库元数据
    ↓
4. 使用 S3Presigner 生成临时 URL
    ↓
5. 缓存 URL 到 Redis (3595秒)
    ↓
6. 返回预签名 URL
```

## 🚀 性能优化架构

### 缓存策略

```
┌─────────────────────────────┐
│  L1: JVM 本地缓存 (Caffeine) │
│  - 文件元数据对象            │
│  - TTL: 5 分钟              │
└─────────────────────────────┘
          ↓ (未命中)
┌─────────────────────────────┐
│  L2: Redis 分布式缓存         │
│  - 预签名 URL               │
│  - TTL: 3595 秒             │
└─────────────────────────────┘
          ↓ (未命中)
┌─────────────────────────────┐
│  L3: PostgreSQL 数据库        │
└─────────────────────────────┘
```

## 🔄 高可用架构

### 单机模式（开发/测试）

```
Application Server
    ↓ HTTP
MinIO (Single Node)
    ↓
Docker Volume
```

### 集群模式（生产环境）

```
        Load Balancer
             ↓
    ┌────────┼────────┐
    ↓        ↓        ↓
MinIO-1  MinIO-2  MinIO-3
    ↓        ↓        ↓
Disk-1   Disk-2   Disk-3
```

**Erasure Coding (纠删码)**：
- 容错能力：N/2 - 1 个节点故障仍可读写
- 存储效率：1.5x 冗余

## 📊 架构演进路线

### Phase 1: 当前架构（已实现）
- ✅ 本地存储 + 云存储双模式
- ✅ S3 兼容 API（MinIO/阿里云 OSS）
- ✅ 公共/私有文件访问控制
- ✅ 预签名 URL 缓存优化

### Phase 2: 增强功能（规划中）
- ⏳ 分片上传（大文件 > 100MB）
- ⏳ 断点续传
- ⏳ 图片处理管道
- ⏳ CDN 集成

### Phase 3: 企业级特性（未来）
- 🔮 多云存储混合
- 🔮 智能存储分层
- 🔮 内容审核
- 🔮 全文检索

## 📖 参考资料

- [SmartAdmin 架构规范](../../CLAUDE.md)
- [AWS S3 API Reference](https://docs.aws.amazon.com/AmazonS3/latest/API/)
- [MinIO Architecture Guide](https://min.io/docs/minio/linux/operations/concepts.html)
