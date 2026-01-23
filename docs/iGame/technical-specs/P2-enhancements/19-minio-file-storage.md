# P2-19: MinIO File Storage

## Document Control

| Attribute | Value |
|-----------|-------|
| Document ID | P2-19 |
| Title | MinIO File Storage |
| Version | 1.0.0 |
| Status | Draft |
| Author | SmartAdmin Architecture Team |
| Created | 2026-01-23 |
| Last Updated | 2026-01-23 |
| Related Docs | [backend_project.md](../../backend_project.md), [P0-04](../P0-critical/04-kyc-aml-automation.md), [P1-10](../P1-important/10-headless-cms-integration.md) |

---

## 1. Background

### 1.1 Purpose

This document provides comprehensive architecture and implementation guidance for integrating MinIO object storage as a S3-compatible file storage solution for the iGaming platform.

**Core Objectives**:
1. **S3-Compatible API**: Seamless integration with AWS SDK and standard S3 tools
2. **Multi-Tenant Isolation**: Secure file storage with tenant-based access control
3. **Presigned URLs**: Temporary access URLs for secure direct uploads/downloads
4. **High Availability**: Distributed storage with erasure coding (N/2 data loss tolerance)
5. **Cost Efficiency**: Self-hosted storage reduces cloud costs by 70% compared to AWS S3

### 1.2 Scope

**In Scope**:
- MinIO cluster deployment (distributed mode with 4+ nodes)
- File upload/download API with presigned URLs
- Multi-tenant isolation strategies (bucket-per-tenant vs prefix-based)
- Image optimization and thumbnail generation
- File metadata tracking in PostgreSQL
- Integration with KYC document storage (P0-04)
- Integration with CMS media library (P1-10)
- Access control and lifecycle policies

**Out of Scope**:
- CDN integration (CloudFront, Cloudflare) - deployment-specific
- Video transcoding and streaming
- Full-text search within documents
- Backup and disaster recovery (infrastructure concern)

### 1.3 Strategic Alignment

Aligns with igame_str.md first principles:
- **Zero Marginal Cost**: Self-hosted MinIO eliminates per-GB storage fees
- **Data Sovereignty**: Compliance with GDPR data residency requirements
- **Control**: Full control over data retention and deletion policies

### 1.4 File Storage Use Cases

| Use Case | File Types | Retention | Access Pattern |
|----------|------------|-----------|----------------|
| KYC Documents | PDF, JPG, PNG | 7 years (MGA requirement) | Write-once, read-rarely |
| Player Avatars | JPG, PNG | Until account deletion | Read-frequently |
| CMS Media (Logos, Banners) | JPG, PNG, SVG | Until replaced | Read-very-frequently (cached) |
| Game Provider Logos | PNG, SVG | Permanent | Read-frequently |
| Monthly Reports | PDF, Excel | 7 years | Write-once, read-occasionally |
| Promotional Materials | JPG, PNG, MP4 | Until campaign ends | Read-frequently |

---

## 2. Architecture

### 2.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   Application Layer (Spring Boot)                │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              FileStorageController                      │    │
│  │  POST /api/files/upload (multipart file upload)       │    │
│  │  GET  /api/files/{fileId}/download (presigned URL)    │    │
│  │  DELETE /api/files/{fileId} (soft delete)             │    │
│  └────────────────────────────────────────────────────────┘    │
│                           │                                      │
│  ┌────────────────────────▼───────────────────────────────┐    │
│  │              FileStorageManager                         │    │
│  │  - Upload file to MinIO                                │    │
│  │  - Generate presigned URLs (7-day expiry)              │    │
│  │  - Track file metadata in PostgreSQL                   │    │
│  │  - Apply multi-tenant isolation                        │    │
│  │  - Generate thumbnails for images                      │    │
│  └────────────────────────────────────────────────────────┘    │
│                           │                                      │
└───────────────────────────┼──────────────────────────────────────┘
                            │ MinIO SDK (AWS S3 API)
┌───────────────────────────┼──────────────────────────────────────┐
│                      MinIO Cluster (Distributed)                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  Node 1  │  │  Node 2  │  │  Node 3  │  │  Node 4  │        │
│  │  (Data)  │  │  (Data)  │  │  (Data)  │  │  (Data)  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                  │
│  Erasure Coding: EC:2 (tolerates 2 disk failures)              │
│  Total Capacity: 4 nodes × 1TB = 4TB raw (2TB usable)          │
└─────────────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                      PostgreSQL Database                         │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              file_metadata Table                        │    │
│  │  - id, tenant_id, file_name, file_size, mime_type     │    │
│  │  - bucket_name, object_key, upload_by, created_at     │    │
│  │  - deleted (soft delete flag)                          │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Multi-Tenant Isolation Strategies

#### Strategy 1: Bucket-Per-Tenant (Recommended for <100 tenants)

```
MinIO Buckets:
├── tenant-merchant-a
│   ├── kyc/
│   │   ├── player-123-passport.pdf
│   │   └── player-456-id-card.jpg
│   ├── avatars/
│   │   └── player-123-avatar.jpg
│   └── cms/
│       └── logo.png
├── tenant-merchant-b
│   ├── kyc/
│   │   └── player-789-passport.pdf
│   └── avatars/
│       └── player-789-avatar.jpg
└── shared
    └── game-provider-logos/
        ├── evolution-logo.png
        └── pragmatic-play-logo.png
```

**Pros**:
- Strong isolation (IAM policies per bucket)
- Easy to monitor storage usage per tenant
- Simple backup/restore per tenant

**Cons**:
- MinIO limit: ~10,000 buckets per cluster
- Not scalable for >1000 tenants

#### Strategy 2: Prefix-Based (Recommended for >100 tenants)

```
MinIO Buckets:
├── igaming-files
│   ├── merchant-a/
│   │   ├── kyc/player-123-passport.pdf
│   │   ├── avatars/player-123-avatar.jpg
│   │   └── cms/logo.png
│   ├── merchant-b/
│   │   ├── kyc/player-789-passport.pdf
│   │   └── avatars/player-789-avatar.jpg
│   └── shared/
│       └── game-provider-logos/evolution-logo.png
```

**Pros**:
- Scales to unlimited tenants
- Simpler bucket management

**Cons**:
- Application-enforced isolation (no MinIO IAM per tenant)
- Requires careful object key prefixing

### 2.3 File Upload Flow

```
1. Client: POST /api/files/upload (multipart/form-data)
   ↓
2. FileStorageController: Validate file (size, MIME type)
   ↓
3. FileStorageManager: Generate unique object key
   ↓
4. MinIO SDK: Upload file to bucket
   ↓
5. PostgreSQL: Insert file metadata record
   ↓
6. Response: { fileId: 123, downloadUrl: "https://..." }
```

### 2.4 Presigned URL Flow

```
1. Client: GET /api/files/{fileId}/download
   ↓
2. FileStorageManager: Query file metadata
   ↓
3. Validate access (tenant_id match + soft delete check)
   ↓
4. MinIO SDK: Generate presigned URL (7-day expiry)
   ↓
5. Response: { downloadUrl: "https://minio.example.com/...?X-Amz-Expires=604800" }
   ↓
6. Client: Direct download from MinIO (no proxy)
```

---

## 3. Implementation

### 3.1 MinIO Configuration

#### 3.1.1 Docker Compose Deployment

**docker-compose.yml** (4-node distributed cluster):
```yaml
version: '3.8'

services:
  minio1:
    image: minio/minio:RELEASE.2024-01-18T22-51-28Z
    container_name: minio1
    command: server http://minio{1...4}/data{1...2} --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
      MINIO_STORAGE_CLASS_STANDARD: EC:2  # Erasure coding (2 data, 2 parity)
    volumes:
      - minio1-data1:/data1
      - minio1-data2:/data2
    networks:
      - minio-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

  minio2:
    image: minio/minio:RELEASE.2024-01-18T22-51-28Z
    container_name: minio2
    command: server http://minio{1...4}/data{1...2} --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
      MINIO_STORAGE_CLASS_STANDARD: EC:2
    volumes:
      - minio2-data1:/data1
      - minio2-data2:/data2
    networks:
      - minio-network

  minio3:
    image: minio/minio:RELEASE.2024-01-18T22-51-28Z
    container_name: minio3
    command: server http://minio{1...4}/data{1...2} --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
      MINIO_STORAGE_CLASS_STANDARD: EC:2
    volumes:
      - minio3-data1:/data1
      - minio3-data2:/data2
    networks:
      - minio-network

  minio4:
    image: minio/minio:RELEASE.2024-01-18T22-51-28Z
    container_name: minio4
    command: server http://minio{1...4}/data{1...2} --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
      MINIO_STORAGE_CLASS_STANDARD: EC:2
    volumes:
      - minio4-data1:/data1
      - minio4-data2:/data2
    networks:
      - minio-network

  # Nginx load balancer
  nginx:
    image: nginx:alpine
    container_name: minio-lb
    ports:
      - "9000:9000"   # S3 API
      - "9001:9001"   # Console UI
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - minio1
      - minio2
      - minio3
      - minio4
    networks:
      - minio-network

volumes:
  minio1-data1:
  minio1-data2:
  minio2-data1:
  minio2-data2:
  minio3-data1:
  minio3-data2:
  minio4-data1:
  minio4-data2:

networks:
  minio-network:
    driver: bridge
```

**nginx.conf** (Load Balancer):
```nginx
upstream minio_s3 {
    least_conn;
    server minio1:9000;
    server minio2:9000;
    server minio3:9000;
    server minio4:9000;
}

upstream minio_console {
    least_conn;
    server minio1:9001;
    server minio2:9001;
    server minio3:9001;
    server minio4:9001;
}

server {
    listen 9000;

    location / {
        proxy_pass http://minio_s3;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Allow large uploads (500MB)
        client_max_body_size 500M;
    }
}

server {
    listen 9001;

    location / {
        proxy_pass http://minio_console;
        proxy_set_header Host $http_host;
    }
}
```

**Start MinIO Cluster**:
```bash
# Start cluster
docker-compose up -d

# Check cluster status
docker exec -it minio1 mc admin info local

# Create initial bucket
docker exec -it minio1 mc mb local/igaming-files
```

### 3.2 Spring Boot Integration

#### 3.2.1 Dependencies

**build.gradle**:
```gradle
dependencies {
    // MinIO (S3-compatible client)
    implementation 'io.minio:minio:8.5.7'

    // Image processing (thumbnails)
    implementation 'org.imgscalr:imgscalr-lib:4.2'

    // Apache Tika (MIME type detection)
    implementation 'org.apache.tika:tika-core:2.9.1'
}
```

#### 3.2.2 MinIO Configuration

**MinIOConfig.java**:
```java
package net.lab1024.sa.base.module.support.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO configuration
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Configuration
public class MinIOConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }
}
```

**application.yml**:
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin123
  default-bucket: igaming-files
```

### 3.3 File Storage Manager

**FileStorageManager.java**:
```java
package net.lab1024.sa.base.module.support.file.manager;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.file.dao.FileMetadataDao;
import net.lab1024.sa.base.module.support.file.domain.entity.FileMetadata;
import net.lab1024.sa.base.module.support.tenant.context.TenantContextHolder;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * File storage manager
 *
 * Responsibilities:
 * - Upload files to MinIO
 * - Generate presigned URLs for secure downloads
 * - Track file metadata in PostgreSQL
 * - Apply multi-tenant isolation
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageManager {

    private final MinioClient minioClient;
    private final FileMetadataDao fileMetadataDao;
    private final Tika tika = new Tika();

    @Value("${minio.default-bucket}")
    private String defaultBucket;

    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB
    private static final int PRESIGNED_URL_EXPIRY_DAYS = 7;

    /**
     * Upload file to MinIO
     *
     * @param file Multipart file
     * @param category File category (kyc, avatars, cms, etc.)
     * @param uploadedBy User ID who uploaded the file
     * @return File metadata
     */
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata uploadFile(MultipartFile file, String category, Long uploadedBy) {
        String tenantId = TenantContextHolder.getTenantId();

        try {
            // 1. Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ServiceException("File size exceeds 500MB limit");
            }

            // 2. Detect MIME type (don't trust client-provided MIME type)
            String mimeType;
            try (InputStream inputStream = file.getInputStream()) {
                mimeType = tika.detect(inputStream, file.getOriginalFilename());
            }

            // 3. Generate unique object key
            String objectKey = generateObjectKey(tenantId, category, file.getOriginalFilename());

            // 4. Upload to MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(defaultBucket)
                        .object(objectKey)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(mimeType)
                        .build()
                );
            }

            // 5. Save metadata to PostgreSQL
            FileMetadata metadata = new FileMetadata();
            metadata.setTenantId(tenantId);
            metadata.setFileName(file.getOriginalFilename());
            metadata.setFileSize(file.getSize());
            metadata.setMimeType(mimeType);
            metadata.setBucketName(defaultBucket);
            metadata.setObjectKey(objectKey);
            metadata.setCategory(category);
            metadata.setUploadedBy(uploadedBy);
            metadata.setDeleted(false);
            metadata.setCreatedAt(LocalDateTime.now());

            fileMetadataDao.insert(metadata);

            log.info("File uploaded: id={}, name={}, size={}, tenant={}",
                metadata.getId(), metadata.getFileName(), metadata.getFileSize(), tenantId);

            return metadata;

        } catch (Exception e) {
            log.error("File upload failed: name={}, tenant={}", file.getOriginalFilename(), tenantId, e);
            throw new ServiceException("File upload failed: " + e.getMessage());
        }
    }

    /**
     * Generate presigned download URL (valid for 7 days)
     *
     * @param fileId File ID
     * @return Presigned URL
     */
    public String getPresignedDownloadUrl(Long fileId) {
        String tenantId = TenantContextHolder.getTenantId();

        // 1. Query file metadata
        FileMetadata metadata = fileMetadataDao.selectById(fileId);
        if (metadata == null) {
            throw new ServiceException("File not found");
        }

        // 2. Validate tenant access
        if (!tenantId.equals(metadata.getTenantId())) {
            throw new ServiceException("Access denied");
        }

        // 3. Check soft delete
        if (metadata.getDeleted()) {
            throw new ServiceException("File has been deleted");
        }

        try {
            // 4. Generate presigned URL
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(metadata.getBucketName())
                    .object(metadata.getObjectKey())
                    .expiry(PRESIGNED_URL_EXPIRY_DAYS, TimeUnit.DAYS)
                    .build()
            );

            log.info("Presigned URL generated: fileId={}, expiry={}d", fileId, PRESIGNED_URL_EXPIRY_DAYS);

            return url;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL: fileId={}", fileId, e);
            throw new ServiceException("Failed to generate download URL");
        }
    }

    /**
     * Soft delete file (mark as deleted in metadata, keep MinIO object)
     *
     * @param fileId File ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        String tenantId = TenantContextHolder.getTenantId();

        FileMetadata metadata = fileMetadataDao.selectById(fileId);
        if (metadata == null || !tenantId.equals(metadata.getTenantId())) {
            throw new ServiceException("File not found or access denied");
        }

        // Soft delete (mark as deleted, keep MinIO object for audit/recovery)
        metadata.setDeleted(true);
        metadata.setDeletedAt(LocalDateTime.now());
        fileMetadataDao.updateById(metadata);

        log.info("File soft deleted: id={}, name={}", fileId, metadata.getFileName());
    }

    /**
     * Generate unique object key
     *
     * Format: {tenant_id}/{category}/{year}/{month}/{uuid}-{filename}
     * Example: merchant-a/kyc/2026/01/abc123-passport.pdf
     */
    private String generateObjectKey(String tenantId, String category, String originalFilename) {
        LocalDateTime now = LocalDateTime.now();
        String uuid = UUID.randomUUID().toString();

        return String.format("%s/%s/%d/%02d/%s-%s",
            tenantId,
            category,
            now.getYear(),
            now.getMonthValue(),
            uuid,
            sanitizeFilename(originalFilename)
        );
    }

    /**
     * Sanitize filename (remove special characters)
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
```

### 3.4 File Upload Controller

**FileStorageController.java**:
```java
package net.lab1024.sa.base.module.support.file.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.module.support.file.domain.entity.FileMetadata;
import net.lab1024.sa.base.module.support.file.domain.vo.FileUploadVO;
import net.lab1024.sa.base.module.support.file.manager.FileStorageManager;
import net.lab1024.sa.base.module.support.operatelog.annoation.OperateLog;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * File storage controller
 *
 * Endpoints:
 * - POST /api/files/upload - Upload file
 * - GET /api/files/{fileId}/download - Get presigned download URL
 * - DELETE /api/files/{fileId} - Soft delete file
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageManager fileStorageManager;

    /**
     * Upload file
     *
     * @param file Multipart file
     * @param category File category (kyc, avatars, cms, etc.)
     */
    @PostMapping("/upload")
    @OperateLog(module = "File Storage", content = "Upload file")
    public ResponseDTO<FileUploadVO> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "category", defaultValue = "general") String category
    ) {
        Long uploadedBy = RequestUser.getUserId();  // Get from Sa-Token context

        FileMetadata metadata = fileStorageManager.uploadFile(file, category, uploadedBy);

        FileUploadVO vo = FileUploadVO.builder()
            .fileId(metadata.getId())
            .fileName(metadata.getFileName())
            .fileSize(metadata.getFileSize())
            .mimeType(metadata.getMimeType())
            .downloadUrl(fileStorageManager.getPresignedDownloadUrl(metadata.getId()))
            .build();

        return ResponseDTO.ok(vo);
    }

    /**
     * Get presigned download URL
     *
     * @param fileId File ID
     */
    @GetMapping("/{fileId}/download")
    public ResponseDTO<String> getDownloadUrl(@PathVariable Long fileId) {
        String url = fileStorageManager.getPresignedDownloadUrl(fileId);
        return ResponseDTO.ok(url);
    }

    /**
     * Soft delete file
     *
     * @param fileId File ID
     */
    @DeleteMapping("/{fileId}")
    @OperateLog(module = "File Storage", content = "Delete file")
    public ResponseDTO<Void> deleteFile(@PathVariable Long fileId) {
        fileStorageManager.deleteFile(fileId);
        return ResponseDTO.ok();
    }
}
```

### 3.5 Image Thumbnail Generation

**ThumbnailService.java**:
```java
package net.lab1024.sa.base.module.support.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Image thumbnail generation service
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    /**
     * Generate thumbnail (200x200 max size, maintain aspect ratio)
     *
     * @param originalImage Original image input stream
     * @param format Image format (JPEG, PNG)
     * @return Thumbnail image as byte array
     */
    public byte[] generateThumbnail(InputStream originalImage, String format) throws Exception {
        BufferedImage image = ImageIO.read(originalImage);

        // Resize to 200x200 (maintain aspect ratio)
        BufferedImage thumbnail = Scalr.resize(
            image,
            Scalr.Method.QUALITY,
            Scalr.Mode.FIT_TO_WIDTH,
            200,
            200,
            Scalr.OP_ANTIALIAS
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, format, outputStream);

        return outputStream.toByteArray();
    }
}
```

---

## 4. Testing

### 4.1 Integration Tests

**FileStorageManagerTest.java**:
```java
@SpringBootTest
@Testcontainers
class FileStorageManagerTest {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
        .withUserName("minioadmin")
        .withPassword("minioadmin123");

    @Autowired
    private FileStorageManager fileStorageManager;

    @Test
    void testUploadFile_Success() throws Exception {
        // Given: Mock multipart file
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            "Test PDF content".getBytes()
        );

        // When: Upload file
        TenantContextHolder.setTenantId("merchant-a");
        FileMetadata metadata = fileStorageManager.uploadFile(file, "kyc", 123L);

        // Then: File uploaded and metadata saved
        assertNotNull(metadata.getId());
        assertEquals("test-document.pdf", metadata.getFileName());
        assertTrue(metadata.getObjectKey().contains("merchant-a/kyc/"));
    }

    @Test
    void testGetPresignedDownloadUrl_Success() {
        // Given: File uploaded
        FileMetadata metadata = uploadTestFile();

        // When: Get presigned URL
        String url = fileStorageManager.getPresignedDownloadUrl(metadata.getId());

        // Then: URL contains MinIO endpoint and expiry
        assertTrue(url.contains("minio"));
        assertTrue(url.contains("X-Amz-Expires"));
    }

    @Test
    void testDeleteFile_SoftDelete() {
        // Given: File uploaded
        FileMetadata metadata = uploadTestFile();

        // When: Delete file
        fileStorageManager.deleteFile(metadata.getId());

        // Then: File marked as deleted in metadata
        FileMetadata deleted = fileMetadataDao.selectById(metadata.getId());
        assertTrue(deleted.getDeleted());
        assertNotNull(deleted.getDeletedAt());
    }
}
```

---

## 5. Operations

### 5.1 Monitoring

**Prometheus Metrics** (MinIO built-in):
```promql
# Storage capacity usage
minio_cluster_capacity_usable_total_bytes

# Object count
minio_bucket_objects_total{bucket="igaming-files"}

# Upload/download throughput
rate(minio_s3_requests_total{api="PutObject"}[5m])
rate(minio_s3_requests_total{api="GetObject"}[5m])
```

### 5.2 Lifecycle Policies

**Delete soft-deleted files after 90 days**:
```bash
# Install mc (MinIO Client)
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc

# Configure alias
mc alias set myminio http://localhost:9000 minioadmin minioadmin123

# Set lifecycle policy (delete objects with "deleted=true" tag after 90 days)
mc ilm add myminio/igaming-files --expiry-days 90 --tags "deleted=true"
```

---

## 6. Appendices

### 6.1 File Category Reference

| Category | Description | Examples | Retention |
|----------|-------------|----------|-----------|
| kyc | KYC/AML documents | Passport, ID card, proof of address | 7 years |
| avatars | Player profile pictures | JPG, PNG (max 5MB) | Until deletion |
| cms | CMS media library | Logos, banners, promotional images | Until replaced |
| game-logos | Game provider logos | PNG, SVG | Permanent |
| reports | Monthly/annual reports | PDF, Excel | 7 years |
| promotions | Marketing materials | JPG, PNG, MP4 | Until campaign ends |

### 6.2 Related Documentation

- [P0-04: KYC/AML Automation](../P0-critical/04-kyc-aml-automation.md) - KYC document storage
- [P1-10: Headless CMS Integration](../P1-important/10-headless-cms-integration.md) - CMS media library
- [backend_project.md](../../backend_project.md) - Overall platform architecture

---

## Document End

**Version**: 1.0.0
**Status**: Draft
**Next Review**: After MinIO cluster deployment testing
**Feedback**: Security team review required for presigned URL expiry policy