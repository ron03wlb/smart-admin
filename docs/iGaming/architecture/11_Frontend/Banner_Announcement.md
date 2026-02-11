# Banner & Announcement Architecture

> **Business Requirements**: [Frontend UX Requirements](../../requirements/11_Frontend_Experience/Frontend_UX_Requirements.md)
> **Canonical Source**: [source-archive/11_Frontend_CMS/11-02](../../source-archive/11_Frontend_CMS/11-02_Banner_and_Announcement.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Frontend Developers, Backend Developers

---

## 1. Multi-Language Banner Data Structure

```json
{
  "banner_id": "banner_001",
  "title": {
    "zh-CN": "New Year Promotion",
    "en-US": "New Year Promotion",
    "vi-VN": "Khuyen mai Nam moi"
  },
  "images": {
    "zh-CN": "https://cdn.platform.com/banners/cny_zh.webp",
    "en-US": "https://cdn.platform.com/banners/cny_en.webp",
    "vi-VN": "https://cdn.platform.com/banners/cny_vi.webp"
  },
  "link_url": {
    "zh-CN": "/zh/promotions/cny",
    "en-US": "/en/promotions/new-year",
    "vi-VN": "/vi/khuyen-mai/nam-moi"
  }
}
```

## 2. Language Fallback Logic

```typescript
function getBannerImage(banner: Banner, userLanguage: string): string {
    if (banner.images[userLanguage]) {
        return banner.images[userLanguage];
    }
    if (banner.images['en-US']) {
        return banner.images['en-US'];
    }
    return banner.images['zh-CN'] || banner.default_image;
}
```

## 3. Device Targeting

```typescript
const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);

const visibleBanners = allBanners.filter(banner => {
    if (banner.device_target === 'desktop' && isMobile) return false;
    if (banner.device_target === 'mobile' && !isMobile) return false;
    return true;
});
```

## 4. Approval Workflow

```mermaid
graph TD
    A[Operations Submit Banner] --> B{Auto Check}
    B -->|Format/Size Error| C[Return for Edit]
    B -->|Pass| D[Supervisor Review]
    D -->|Content Issue| C
    D -->|Approved| E[Schedule Publish]
    E --> F{Effective Time Reached?}
    F -->|Yes| G[Auto Publish]
    F -->|No| H[Pending]
```

## 5. CDN Integration

### 5.1 Image Upload Pipeline

```
Upload Flow:
1. Operations upload Banner image -> S3 Bucket (s3://platform-banners/)
2. Auto-trigger Lambda -> Generate multiple sizes (1920x600, 750x400, 600x600)
3. Convert to WebP format (30% higher compression)
4. Push to CDN -> https://cdn.platform.com/banners/{banner_id}_{size}.webp
```

### 5.2 Responsive Images

```html
<picture>
    <source
        srcset="https://cdn.platform.com/banners/banner_001_1920x600.webp"
        media="(min-width: 1024px)" type="image/webp">
    <source
        srcset="https://cdn.platform.com/banners/banner_001_1024x512.webp"
        media="(min-width: 768px)" type="image/webp">
    <source
        srcset="https://cdn.platform.com/banners/banner_001_750x400.webp"
        media="(max-width: 767px)" type="image/webp">
    <img src="https://cdn.platform.com/banners/banner_001_750x400.jpg"
        alt="Promotion" loading="lazy">
</picture>
```

### 5.3 Cache Configuration

```nginx
location /banners/ {
    add_header Cache-Control "public, max-age=86400, s-maxage=86400";
    add_header ETag $1;
    gzip on;
    gzip_types image/webp image/jpeg image/png;
}
```

## 6. A/B Testing Configuration

```json
{
    "experiment_id": "exp_001",
    "name": "Homepage Banner Color Test",
    "variants": [
        {"variant_id": "A", "name": "Red Version", "banner_id": "banner_red", "traffic_allocation": 50},
        {"variant_id": "B", "name": "Blue Version", "banner_id": "banner_blue", "traffic_allocation": 50}
    ],
    "start_time": "2026-01-27T00:00:00Z",
    "end_time": "2026-02-03T23:59:59Z"
}
```

## 7. Analytics Tracking

| Metric | Event | Formula |
|--------|-------|---------|
| Impressions | `impression_event` | COUNT(impression_event) |
| Clicks | `click_event` | COUNT(click_event) |
| CTR | - | (Clicks / Impressions) x 100% |
| Conversions | `conversion_event` | COUNT(conversion_event) |
| CVR | - | (Conversions / Clicks) x 100% |

---

## 8. SmartAdmin Implementation

### 8.1 Banner Service

```java
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerDao bannerDao;
    private final BannerPublishManager publishManager;

    /**
     * Get active banners for display using Vavr Option.
     */
    public List<BannerVO> getActiveBanners(Long tenantId, String deviceType, String lang) {
        List<BannerEntity> banners = bannerDao.selectActiveByTenant(tenantId, deviceType);
        return banners.stream()
            .map(entity -> SmartBeanUtil.copy(entity, BannerVO.class))
            .collect(Collectors.toList());
    }

    /**
     * Submit banner for approval workflow.
     */
    public ResponseDTO<Void> submitForApproval(BannerApprovalForm form) {
        return publishManager.submitForApproval(form);
    }
}
```

### 8.2 Banner Publish Manager

```java
@Component
@RequiredArgsConstructor
public class BannerPublishManager {

    private final BannerDao bannerDao;
    private final BannerApprovalDao approvalDao;

    /**
     * Submit banner for approval with validation.
     * @Transactional only allowed in Manager layer per SmartAdmin architecture.
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> submitForApproval(BannerApprovalForm form) {
        BannerEntity banner = bannerDao.selectById(form.getBannerId());
        if (banner == null) {
            return ResponseDTO.userErrorParam("Banner not found");
        }

        // Create approval record
        BannerApprovalEntity approval = new BannerApprovalEntity();
        approval.setBannerId(form.getBannerId());
        approval.setSubmittedBy(form.getOperatorId());
        approval.setStatus(ApprovalStatus.PENDING.getValue());
        approval.setCreatedAt(LocalDateTime.now());
        approvalDao.insert(approval);

        // Update banner status
        banner.setStatus(BannerStatus.PENDING_APPROVAL.getValue());
        bannerDao.updateById(banner);

        return ResponseDTO.ok();
    }
}
```

### 8.3 Database Schema

```sql
-- Banner configuration table
CREATE TABLE t_banner (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    banner_name     VARCHAR(100) NOT NULL,
    title           JSONB NOT NULL,
    images          JSONB NOT NULL,
    link_url        JSONB NOT NULL,
    device_target   VARCHAR(20) NOT NULL DEFAULT 'all',
    display_order   INTEGER NOT NULL DEFAULT 0,
    status          SMALLINT NOT NULL DEFAULT 0,
    effective_start TIMESTAMP,
    effective_end   TIMESTAMP,
    experiment_id   BIGINT,
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_banner_tenant_status ON t_banner(tenant_id, status, device_target) WHERE deleted = FALSE;
CREATE INDEX idx_banner_effective ON t_banner(effective_start, effective_end) WHERE status = 1 AND deleted = FALSE;

-- Banner approval workflow
CREATE TABLE t_banner_approval (
    id              BIGSERIAL PRIMARY KEY,
    banner_id       BIGINT NOT NULL REFERENCES t_banner(id),
    submitted_by    BIGINT NOT NULL,
    reviewed_by     BIGINT,
    status          SMALLINT NOT NULL DEFAULT 0,
    rejection_reason VARCHAR(500),
    submitted_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMP
);

CREATE INDEX idx_approval_status ON t_banner_approval(status, submitted_at DESC);

-- Banner analytics tracking
CREATE TABLE t_banner_analytics (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    banner_id       BIGINT NOT NULL,
    event_date      DATE NOT NULL,
    impressions     BIGINT NOT NULL DEFAULT 0,
    clicks          BIGINT NOT NULL DEFAULT 0,
    conversions     BIGINT NOT NULL DEFAULT 0,
    unique_viewers  BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_banner_analytics UNIQUE (banner_id, event_date)
);

CREATE INDEX idx_banner_analytics_date ON t_banner_analytics(tenant_id, event_date DESC);

-- A/B test experiment configuration
CREATE TABLE t_banner_experiment (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    experiment_name VARCHAR(100) NOT NULL,
    variants        JSONB NOT NULL,
    traffic_split   JSONB NOT NULL,
    status          SMALLINT NOT NULL DEFAULT 0,
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_experiment_status ON t_banner_experiment(tenant_id, status);
```
