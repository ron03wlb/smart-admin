# ADR-011: MinIO 作為 S3 兼容對象存儲

**狀態**: ✅ 已接受

**日期**: 2026-01-20

**作者**: 基礎設施團隊

**審核者**: 財務團隊（KYC 要求）、CTO

**相關文檔**: [P2-19: MinIO 文件存儲](../technical-specs/P2-enhancements/19-minio-file-storage.md), [P0-04: KYC/AML 自動化](../technical-specs/P0-critical/04-kyc-aml-automation.md)

---

## 背景

iGaming 平台需要對象存儲來處理各種文件類型：

**用例**：
1. **KYC 文檔**（P0-04）：護照掃描、地址證明、身份證（7 年保留期、GDPR 合規）
2. **CMS 媒體**（P1-10）：遊戲標誌、橫幅、促銷圖片（高流量、CDN 集成）
3. **玩家頭像**：個人資料圖片（可選、低優先級）
4. **合規導出**：監管機構日報（MGA、Curacao）

**數據特徵**：
- **容量**: 每月 100K+ KYC 文檔 × 2MB = 200GB/月 = 2.4TB/年
- **訪問模式**: KYC 文檔（不頻繁、長期保留）、CMS 媒體（頻繁、短期保留）
- **安全性**: KYC 文檔包含 PII（GDPR 第 32 條靜態加密要求）
- **合規性**: 博彩監管機構要求 7 年保留期（MGA）

**當前狀態**：
- backend_project.md 提到文件存儲但未指定具體技術
- 未部署對象存儲基礎設施
- 文件目前存儲在 PostgreSQL BLOB 中（不可擴展）

**約束條件**：
- 成本: 10TB 存儲 <$500/月
- 安全性: 靜態加密、傳輸加密（HTTPS）
- 可用性: 99.9% 正常運行時間（KYC 上傳在註冊關鍵路徑中）
- GDPR: 歐盟數據駐留（對歐盟玩家）

**成功標準**：
- <500ms p95 文件上傳延遲（<5MB 文件）
- 相比 AWS S3 降低 70% 成本（$0.023/GB vs AWS $0.08/GB）
- S3 兼容 API（如需要可移植到 AWS）

---

## 決策

**我們將使用 MinIO 作為自託管、S3 兼容的對象存儲解決方案。**

### 核心組件

#### 圖 11.1: MinIO 分佈式對象存儲架構與多租戶隔離

> **說明**: 此圖展示 MinIO 4 節點分佈式集群架構，包括紐刪碼（EC:2）容錯機制、多租戶存儲桶策略、CDN 集成和安全加密，演示如何實現高可用、低成本的對象存儲（相比 AWS S3 降低 70% 成本）。

```mermaid
graph TB
    subgraph "應用層"
        A[Spring Boot 應用<br/>FileStorageManager]
        B[Strapi CMS<br/>媒體上傳]
        C[KYC 服務<br/>文檔上傳]
    end

    subgraph "MinIO 集群（分佈式模式）"
        D[MinIO 節點 1<br/>4 × 4TB NVMe SSD]
        E[MinIO 節點 2<br/>4 × 4TB NVMe SSD]
        F[MinIO 節點 3<br/>4 × 4TB NVMe SSD]
        G[MinIO 節點 4<br/>4 × 4TB NVMe SSD]
    end

    subgraph "存儲桶策略"
        H[kyc-docs 存儲桶<br/>租戶前綴隔離<br/>kyc-docs/tenant_a/...]
        I[cms-media 存儲桶<br/>90 天生命週期策略<br/>自動刪除過期媒體]
        J[player-avatars 存儲桶<br/>低優先級<br/>標準存儲類]
    end

    subgraph "安全層"
        K[MinIO KMS<br/>AES-256 靜態加密]
        L[TLS 1.3<br/>傳輸加密]
        M[IAM 策略<br/>tenant_id 訪問控制]
    end

    subgraph "CDN 層"
        N[CloudFlare CDN<br/>CMS 媒體緩存<br/>7 天 TTL]
    end

    subgraph "備份層"
        O[S3 Glacier<br/>每日冷備份<br/>KYC 文檔 7 年保留]
    end

    A -->|S3 API<br/>PutObject/GetObject| D
    B -->|上傳橫幅圖片| D
    C -->|上傳 KYC 文檔| D

    D <-->|紐刪碼 EC:2<br/>容忍 2 盤故障| E
    E <-->|數據分片<br/>16 盤總計| F
    F <-->|3x 副本<br/>高可用| G

    D --> H
    D --> I
    D --> J

    H --> K
    I --> K
    J --> K

    D -.->|HTTPS| L
    H -.->|IAM 策略驗證| M

    I -->|Presigned URL<br/>7 天過期| N
    N -->|媒體 URL| B

    H -->|每日備份<br/>10TB KYC 文檔| O

    classDef app fill:#74c0fc,stroke:#339af0,color:#000
    classDef minio fill:#ff6b6b,stroke:#c92a2a,color:#fff
    classDef bucket fill:#ffd93d,stroke:#f59f00,color:#000
    classDef security fill:#51cf66,stroke:#37b24d,color:#fff
    classDef cdn fill:#e599f7,stroke:#9c36b5,color:#000
    classDef backup fill:#868e96,stroke:#495057,color:#fff

    class A,B,C app
    class D,E,F,G minio
    class H,I,J bucket
    class K,L,M security
    class N cdn
    class O backup
```

**1. 架構**:
```
應用 → MinIO 集群（4 節點）→ 磁盤存儲（NVMe SSD）
                 ↓
          Presigned URL → CDN（CloudFlare）→ 最終用戶
```

**2. MinIO 部署**:
- **分佈式模式**: 4 節點 × 4 磁盤 = 16 總磁盤
- **紐刪碼**: EC:2（可容忍 2 盤故障而不丟失數據）
- **容量**: 4 節點 × 4TB = 16TB 原始 = 8TB 可用（EC:2 後）
- **副本**: 3x 副本跨節點（高可用）

**3. 存儲桶策略**:
```
多租戶選項：
A. 每租戶一個存儲桶（<100 租戶）：tenant-001-kyc、tenant-002-kyc
B. 基於前綴（>100 租戶）：kyc-docs/{tenant_id}/{file_id}
```

**4. 安全性**:
- **靜態加密**: MinIO KMS（AES-256）
- **傳輸加密**: TLS 1.3（HTTPS）
- **訪問控制**: IAM 策略（租戶 A 無法訪問租戶 B 的文件）
- **Presigned URL**: 7 天過期以實現安全下載

#### 圖 11.2: 文件上傳與 Presigned URL 下載完整流程

> **說明**: 此時序圖展示完整的文件生命週期管理，包括 KYC 文檔上傳（租戶隔離、靜態加密）、Presigned URL 生成（7 天過期）、CMS 媒體 CDN 緩存，以及生命週期策略自動清理（90 天刪除 CMS 媒體）。

```mermaid
sequenceDiagram
    participant U as 玩家/營銷團隊
    participant C as Spring Boot Controller<br/>FileController
    participant M as FileStorageManager<br/>業務邏輯
    participant MI as MinIO 集群<br/>4 節點分佈式
    participant DB as PostgreSQL<br/>文件元數據
    participant CDN as CloudFlare CDN<br/>媒體緩存
    participant LC as 生命週期策略<br/>定時清理

    rect rgb(240, 250, 255)
        Note over U,DB: Phase 1: 文件上傳（KYC 文檔 + 租戶隔離）
        U->>C: POST /api/files/upload<br/>X-Tenant-ID: tenant_a<br/>Content-Type: multipart/form-data<br/>文件: passport.pdf (2MB)
        C->>M: uploadFile(file, category="KYC", uploadedBy)
        M->>M: 驗證租戶身份<br/>TenantContextHolder.getTenantId()

        M->>M: 生成對象鍵<br/>kyc-docs/tenant_a/2026/01/uuid-passport.pdf

        M->>MI: PutObject API<br/>bucket: kyc-docs<br/>key: tenant_a/2026/01/uuid-passport.pdf<br/>stream: file.getInputStream()
        MI->>MI: 紐刪碼處理<br/>數據分片到 16 盤（EC:2）
        MI->>MI: AES-256 靜態加密<br/>MinIO KMS
        MI-->>M: 返回 ETag（文件校驗和）

        M->>DB: INSERT INTO file_metadata<br/>(tenant_id, object_key, size, mime_type, etag)
        DB-->>M: 返回 file_id = 42

        M-->>C: 返回文件元數據<br/>{file_id: 42, object_key: "...", size: 2097152}
        C-->>U: 響應 201 Created（p95 <500ms）
    end

    rect rgb(255, 250, 240)
        Note over U,DB: Phase 2: 文件下載（Presigned URL 生成）
        U->>C: GET /api/files/42/download<br/>X-Tenant-ID: tenant_a
        C->>M: getDownloadUrl(fileId=42)
        M->>DB: SELECT * FROM file_metadata<br/>WHERE id = 42<br/>AND tenant_id = 'tenant_a'

        alt 租戶隔離驗證失敗
            DB-->>M: 空結果（文件屬於其他租戶）
            M-->>C: 拋出 ForbiddenException
            C-->>U: 403 Forbidden（租戶隔離生效）
        else 租戶驗證通過
            DB-->>M: 返回 object_key

            M->>MI: GetPresignedObjectUrl API<br/>bucket: kyc-docs<br/>key: tenant_a/2026/01/uuid-passport.pdf<br/>expiry: 7 天
            MI-->>M: 返回 Presigned URL<br/>https://minio.example.com/kyc-docs/...<br/>?X-Amz-Signature=...&X-Amz-Expires=604800

            M-->>C: 返回 Presigned URL
            C-->>U: 響應 {url: "...", expires_at: "2026-01-30"}

            U->>MI: GET Presigned URL（直接下載，不經過應用）
            MI->>MI: 驗證簽名<br/>檢查過期時間
            MI-->>U: 返回文件流（p95 <200ms）
        end
    end

    rect rgb(250, 255, 240)
        Note over U,DB: Phase 3: CMS 媒體上傳與 CDN 集成
        U->>C: POST /api/files/upload<br/>X-Tenant-ID: tenant_a<br/>category: "CMS_BANNER"<br/>文件: valentine-promo.jpg (500KB)
        C->>M: uploadFile(file, category="CMS_BANNER")
        M->>M: 生成對象鍵<br/>cms-media/tenant_a/2026/01/uuid-valentine.jpg

        M->>MI: PutObject API<br/>bucket: cms-media<br/>Content-Type: image/jpeg
        MI-->>M: 返回 ETag

        M->>DB: INSERT INTO file_metadata<br/>設置 lifecycle_policy = "DELETE_AFTER_90_DAYS"
        DB-->>M: 返回 file_id = 123

        M->>CDN: 清除緩存（如果舊版本存在）<br/>PURGE /valentine-promo.jpg
        CDN-->>M: 緩存已清除

        M-->>C: 返回 CDN URL<br/>{url: "https://cdn.example.com/valentine.jpg"}
        C-->>U: 響應（前端可直接使用 CDN URL）

        U->>CDN: GET /valentine.jpg（首次請求）
        CDN->>MI: 回源請求（緩存未命中）
        MI-->>CDN: 返回圖片
        CDN->>CDN: 緩存圖片（7 天 TTL）
        CDN-->>U: 返回圖片（後續請求 <50ms）
    end

    rect rgb(255, 240, 245)
        Note over U,DB: Phase 4: 生命週期策略（自動清理過期媒體）
        LC->>MI: 每日定時任務<br/>ListObjects API<br/>bucket: cms-media<br/>filter: created_at < NOW() - 90 days
        MI-->>LC: 返回 1,000 個過期文件

        loop 遍歷每個過期文件
            LC->>MI: DeleteObject API<br/>key: cms-media/tenant_x/2025/10/old-banner.jpg
            MI-->>LC: 刪除成功
            LC->>DB: UPDATE file_metadata<br/>SET deleted = true<br/>WHERE object_key = '...'
            DB-->>LC: 更新成功
        end

        LC->>LC: 記錄日誌<br/>Deleted 1,000 expired CMS media files
    end

    rect rgb(250, 240, 255)
        Note over U,DB: Phase 5: 災難恢復（每日備份到 S3 Glacier）
        LC->>MI: 每日 00:00 UTC<br/>ListObjects API<br/>bucket: kyc-docs<br/>filter: created_at = TODAY
        MI-->>LC: 返回當日新增 KYC 文檔（100 個）

        loop 遍歷每個 KYC 文檔
            LC->>MI: GetObject API（讀取文件）
            MI-->>LC: 返回文件流
            LC->>LC: 上傳到 AWS S3 Glacier<br/>深度歸檔（$0.00099/GB/月）
            LC->>LC: 驗證校驗和（ETag 匹配）
        end

        LC->>LC: 記錄日誌<br/>Backed up 100 KYC docs to S3 Glacier<br/>7 年保留期開始
    end
```

**5. 集成示例**:
```java
@Service
@RequiredArgsConstructor
public class FileStorageManager {
    private final MinioClient minioClient;

    @Transactional(rollbackFor = Exception.class)
    public FileMetadata uploadFile(MultipartFile file, String category, Long uploadedBy) {
        String tenantId = TenantContextHolder.getTenantId();

        // 生成對象鍵：{tenant_id}/{category}/{year}/{month}/{uuid}-{filename}
        String objectKey = generateObjectKey(tenantId, category, file.getOriginalFilename());

        // 上傳到 MinIO
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(defaultBucket)
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(detectMimeType(file))
                .build()
        );

        // 保存元數據到 PostgreSQL
        FileMetadata metadata = new FileMetadata();
        metadata.setObjectKey(objectKey);
        fileMetadataDao.insert(metadata);

        return metadata;
    }
}
```

### 實施方法

1. **部署 MinIO 集群**（Kubernetes 上 4 節點）
2. **配置存儲桶**（kyc-docs、cms-media、player-avatars）
3. **設置生命週期策略**（刪除 CMS 媒體 >90 天、軟刪除 KYC >7 年）
4. **與應用集成**（MinIO Java SDK）
5. **CDN 集成**（CloudFlare 用於 CMS 媒體）

---

## 後果

### 正面影響

- ✅ **降低 70% 成本**: $0.023/GB（自託管）vs AWS S3 $0.08/GB = 每月節省 $420
- ✅ **S3 兼容性**: AWS S3 的直接替代品（相同 API、易於遷移）
- ✅ **數據主權**: 在歐盟託管以符合 GDPR（AWS S3 在美國）
- ✅ **無出口費用**: 無數據傳輸費用（AWS 收取 $0.09/GB 出口費）
- ✅ **高性能**: NVMe SSD + 本地網絡 = <100ms 延遲（vs S3 200-500ms）
- ✅ **加密**: 內置靜態加密、傳輸加密（TLS 1.3）

### 負面影響

- ❌ **運維負擔**: 自託管需要運維團隊（備份、監控、打補丁）
- ❌ **無全球 CDN**: 無內置 CDN（AWS S3 + CloudFront 集成），必須使用單獨的 CDN
- ❌ **磁盤管理**: 必須監控磁盤使用情況、手動更換故障磁盤
- ❌ **有限生態系統**: 比 AWS S3 集成更少（例如無 Lambda 觸發器）

### 風險

- ⚠️ **數據丟失**: 如果 >2 個磁盤同時故障（EC:2 容錯）
  - **緩解措施**: 每日備份到 S3 Glacier（冷存儲）、磁盤故障監控警報

- ⚠️ **性能下降**: 高併發（1000+ 上傳/秒）使網絡飽和
  - **緩解措施**: MinIO Pod 自動擴展（HPA）、專用 10Gbps 存儲網絡

- ⚠️ **GDPR 違規**: 未經授權訪問 KYC 文檔（PII 洩露）
  - **緩解措施**: IAM 策略（嚴格訪問控制）、審計日誌（誰訪問了什麼）、靜態加密

### 指標

- **上傳延遲 p95**: <500ms（<5MB 文件）
- **下載延遲 p95**: <200ms（使用 CDN 緩存）
- **存儲成本**: $0.023/GB（自託管）vs $0.08/GB（AWS S3）
- **可用性**: 99.9%（EC:2 容忍 2 盤故障）

---

## 考慮的替代方案

### 替代方案 1: AWS S3

**描述**: 使用 AWS S3 托管對象存儲

**優點**:
- ✅ **完全托管**: 無運維負擔（AWS 處理備份、副本、打補丁）
- ✅ **全球 CDN**: CloudFront 集成（一鍵 CDN 設置）
- ✅ **持久性**: 99.999999999%（11 個 9）持久性
- ✅ **生態系統**: Lambda 觸發器、生命週期策略、Athena 查詢

**缺點**:
- ❌ **高成本**: $0.08/GB 存儲 + $0.09/GB 出口 = 10TB + 傳輸每月 $800（vs $200/月 MinIO）
- ❌ **數據出口費用**: 導出 KYC 文檔進行審計每 10TB 成本 $900
- ❌ **供應商鎖定**: 難以遷離 AWS（專有功能）
- ❌ **延遲**: 從歐盟到 us-east-1 200-500ms（vs <100ms 本地 MinIO）

**拒絕原因**:
成本高 4 倍（$800/月 vs $200/月）。GDPR 數據駐留要求（歐盟玩家的 KYC 文檔必須留在歐盟，AWS eu-central-1 更昂貴）。數據出口費用高昂（導出 10TB 進行審計需 $900）。

---

### 替代方案 2: Ceph

**描述**: 使用 Ceph 分佈式存儲（像 MinIO 一樣開源）

**優點**:
- ✅ **開源**: Apache 2.0 許可證（無供應商鎖定）
- ✅ **塊 + 對象 + 文件存儲**: 統一存儲平台（MinIO 僅對象）
- ✅ **成熟**: CERN、DigitalOcean 使用（大規模驗證）

**缺點**:
- ❌ **運維複雜性**: 需要專門的存儲團隊（OSD、MON、MGR 守護程序）
- ❌ **S3 兼容性**: 有限的 S3 API 支持（RadosGW 缺少功能）
- ❌ **性能**: 對象存儲比 MinIO 慢（針對塊存儲優化）
- ❌ **資源密集**: 每個 OSD 節點需要 16GB+ RAM（vs MinIO 4GB）

**拒絕原因**:
Ceph 針對塊存儲（VM、Kubernetes PVC）優化，而非對象存儲。MinIO 專門用於 S3 兼容對象存儲（更好的性能、更簡單的運維）。團隊缺乏 Ceph 專業知識（6 個月學習曲線）。

---

### 替代方案 3: PostgreSQL BLOB

**描述**: 將文件作為 bytea/BLOB 存儲在 PostgreSQL 中

**優點**:
- ✅ **無新基礎設施**: 重用現有 PostgreSQL 集群
- ✅ **事務性**: 文件 + 元數據在同一 ACID 事務中
- ✅ **更簡單的技術棧**: 無對象存儲層

**缺點**:
- ❌ **數據庫膨脹**: 10TB 文件使 PostgreSQL 膨脹（備份慢、副本滯後）
- ❌ **內存壓力**: 大文件加載到 shared_buffers（內存不足）
- ❌ **無 CDN 集成**: 無法通過 CDN 提供文件（PostgreSQL 不是 HTTP 服務器）
- ❌ **備份開銷**: pg_dump 包含所有文件（100GB+ 備份需要數小時）

**拒絕原因**:
PostgreSQL 為結構化數據設計，而非文件存儲。10TB BLOB 會使數據庫飽和、降低事務性能。對象存儲是文件的行業標準（關注點分離）。

---

## 相關決策

- [ADR-001: 雙式記帳](./001-double-entry-ledger-accounting.md) - 元數據存儲在 PostgreSQL，文件在 MinIO
- [ADR-006: 多租戶隔離](./006-multi-tenant-row-level-isolation.md) - 對象鍵路徑中的 tenant_id

---

## 實施說明

### 時間表

- **提議**: 2026-01-20
- **接受**: 2026-01-22
- **實施開始**: 2026-02-24（第 11 週）
- **目標完成**: 2026-03-03（第 12 週）

### 受影響組件

- **MinIO 集群**: 在 Kubernetes 上部署 4 個節點（16TB 總容量）
- **FileStorageManager**: 用於上傳/下載操作的新服務
- **KYC 服務**: 從 PostgreSQL BLOB 遷移到 MinIO
- **CMS 服務**: 在 MinIO 中存儲媒體文件
- **CDN**: CloudFlare 集成用於 CMS 媒體

### 遷移策略

1. **階段 1: 部署 MinIO**（第 11 週）：
   - 部署 4 節點集群
   - 創建存儲桶（kyc-docs、cms-media）
   - 配置加密、生命週期策略

2. **階段 2: 遷移現有文件**（第 12 週）：
   - 從 PostgreSQL BLOB 導出 KYC 文檔
   - 上傳到 MinIO（保留元數據）
   - 驗證校驗和（100% 數據完整性）

3. **回滾計劃**：
   - 如果 MinIO 失敗，回退到 PostgreSQL BLOB（接受性能下降）
   - 保持雙寫 30 天（PostgreSQL + MinIO）以確保安全

---

## 參考資料

- [MinIO 文檔](https://min.io/docs/)
- [MinIO vs AWS S3 成本比較](https://blog.min.io/minio-vs-s3-cost-comparison/)
- [P2-19: MinIO 文件存儲](../technical-specs/P2-enhancements/19-minio-file-storage.md)
- [GDPR 第 32 條：處理安全](https://gdpr-info.eu/art-32-gdpr/)

---

## 審核歷史

| 日期 | 審核者 | 評論 | 結果 |
|------|--------|------|------|
| 2026-01-21 | 財務團隊 | 確認 7 年保留期滿足 MGA 要求 | ✅ 批准 |
| 2026-01-22 | CTO | 批准附帶條件：每日備份到 S3 Glacier | ✅ 批准 |
| 2026-01-22 | 安全團隊 | 驗證靜態加密 + TLS 1.3 | ✅ 批准 |

---

## 備註

**成本分解（10TB、1 年）**：
- MinIO（自託管）：4 節點 × $50/月 = $200/月 = $2,400/年
- AWS S3：10TB × $0.08/GB × 12 = $9,600/年
- **節省**: $7,200/年（降低 75%）

**未來遷移路徑**: 如果運維負擔過重，可以遷移到 AWS S3（S3 兼容性 = 直接替代）。MinIO 作為對沖供應商鎖定的工具。

---

**版本**: 2.0

**變更日誌**:
- v2.0 (2026-01-23): 完整翻譯為繁體中文，添加 2 個 Mermaid 圖表（MinIO 分佈式架構與文件生命週期管理流程）
- v1.0 (2026-01-20): 初始英文版本
