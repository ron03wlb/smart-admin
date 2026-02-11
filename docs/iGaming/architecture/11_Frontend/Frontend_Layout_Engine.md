# Frontend Layout Engine Architecture

> **Business Requirements**: [Frontend UX Requirements](../../requirements/11_Frontend_Experience/Frontend_UX_Requirements.md)
> **Canonical Source**: [source-archive/11_Frontend_CMS/11-01](../../source-archive/11_Frontend_CMS/11-01_Frontend_Layout_Engine.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Frontend Developers

---

## 1. System Architecture

```mermaid
flowchart TD
    START[Operations Admin<br/>Layout CMS] --> EDITOR[Page Editor<br/>Drag & Drop]
    EDITOR --> COMP[Component Library]
    COMP --> ARRANGE[Drag-to-Reorder]
    ARRANGE --> CONFIG[Configure Parameters]
    CONFIG --> TARGET{Audience Targeting?}
    TARGET -->|All Users| PREVIEW[Generate Preview URL]
    TARGET -->|VIP Only| VIP_RULE[Rule: vip_level >= 3]
    TARGET -->|New Users| NEW_RULE[Rule: registration_days <= 7]
    VIP_RULE --> PREVIEW
    NEW_RULE --> PREVIEW
    PREVIEW --> PUBLISH[Publish]
    PUBLISH --> VERSION[Generate Version v1.0.1]
    VERSION --> SAVE[Save to DB<br/>layout_configs table]
    SAVE --> CDN[Push to CDN<br/>JSON Config File]
    CDN --> API[Frontend API Request<br/>GET /api/v1/layout/config]
    API --> RENDER[Vue 3 Rendering Engine]
```

## 2. JSON Configuration Schema

```json
{
  "version": "v1.0.1",
  "published_at": "2026-01-27T10:30:00Z",
  "experiment_id": null,
  "components": [
    {
      "component_id": "banner-hero",
      "type": "BANNER",
      "order": 1,
      "config": {
        "images": [
          {
            "url": "https://cdn.example.com/banner1.jpg",
            "link": "/promotions/welcome-bonus",
            "alt": "Welcome Bonus 100%"
          }
        ],
        "autoplay": true,
        "interval": 5000
      },
      "display_rules": {
        "target_users": ["ALL"]
      }
    },
    {
      "component_id": "game-grid-hot",
      "type": "GAME_GRID",
      "order": 2,
      "config": {
        "title": "Hot Games",
        "game_ids": [101, 102, 103, 104, 105],
        "columns": 5,
        "show_jackpot": true
      },
      "display_rules": {
        "target_users": ["ALL"]
      }
    },
    {
      "component_id": "vip-exclusive-banner",
      "type": "BANNER",
      "order": 3,
      "config": {
        "images": [{"url": "https://cdn.example.com/vip.jpg", "link": "/vip/benefits"}]
      },
      "display_rules": {
        "target_users": ["VIP"],
        "conditions": {"vip_level": {"operator": ">=", "value": 3}}
      }
    }
  ]
}
```

## 3. CDN Distribution

- Configuration saved to database with `status: PUBLISHED`
- Pushed to CDN (CloudFront/Cloudflare) as JSON file
- CDN TTL: 5 minutes at edge locations
- Frontend fetches via `GET /api/v1/layout/config?version=latest`
- Version checking: only download when local version is stale

## 4. Component Rendering Pipeline

```mermaid
flowchart LR
    JSON[JSON Config] --> PARSE[Parse Components]
    PARSE --> R1[Banner: Swiper.js]
    PARSE --> R2[Game Grid: Virtual Scroll]
    PARSE --> R3[Marquee: CSS Animation]
    PARSE --> R4[Deposit Button: v-if]
    R1 --> RULES{Display Rule Check}
    R2 --> RULES
    R3 --> RULES
    R4 --> RULES
    RULES -->|Match| SHOW[Render Component]
    RULES -->|No Match| HIDE[Hide Component]
    SHOW --> TRACK[Track: component_exposure event]
```

## 5. A/B Testing Integration

Layout engine supports experiment configuration:

```javascript
// Traffic splitting via MurmurHash3
function assignVariant(userId, experimentId) {
  const key = `${experimentId}:${userId}`;
  const hash = murmurHash3(key);
  const bucket = hash % 100;
  return bucket < 50 ? 'A' : 'B';
}
```

- `experiment_id` field in layout JSON
- Hash-based traffic splitting ensures consistency per user
- Exposure events auto-reported on render

## 6. Wallet Mode UI Adaptation

Frontend dynamically switches Header based on `player.wallet_mode`:

| Mode | Display | Key UI Element |
|------|---------|---------------|
| Cash | Balance (Total Cash + Bonus) | Deposit button prominent |
| Credit | Credit Limit / Used / Available | Quota details + Settlement Countdown |
| Hybrid | Both Cash + Credit | Payment selector (Cash first vs Credit) |

---

## 7. SmartAdmin Implementation

### 7.1 Layout Config Service

```java
@Service
@RequiredArgsConstructor
public class LayoutConfigService {

    private final LayoutConfigDao layoutConfigDao;
    private final LayoutPublishManager publishManager;

    /**
     * Get published layout config by tenant using Vavr Option.
     */
    public Option<LayoutConfigVO> getPublishedConfig(Long tenantId) {
        return Option.of(layoutConfigDao.selectPublishedByTenant(tenantId))
            .map(entity -> SmartBeanUtil.copy(entity, LayoutConfigVO.class));
    }

    /**
     * Publish layout configuration to CDN.
     */
    public ResponseDTO<String> publishLayout(LayoutPublishForm form) {
        return publishManager.publishToCdn(form);
    }
}
```

### 7.2 Layout Publish Manager

```java
@Component
@RequiredArgsConstructor
public class LayoutPublishManager {

    private final LayoutConfigDao layoutConfigDao;
    private final LayoutVersionDao versionDao;

    /**
     * Publish layout to CDN with version tracking.
     * @Transactional only allowed in Manager layer per SmartAdmin architecture.
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> publishToCdn(LayoutPublishForm form) {
        LayoutConfigEntity config = layoutConfigDao.selectById(form.getConfigId());
        if (config == null) {
            return ResponseDTO.userErrorParam("Layout config not found");
        }

        // Create version record
        LayoutVersionEntity version = new LayoutVersionEntity();
        version.setConfigId(form.getConfigId());
        version.setVersion(generateVersion());
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedBy(form.getOperatorId());
        versionDao.insert(version);

        // Update config status
        config.setStatus(LayoutStatus.PUBLISHED.getValue());
        config.setCdnUrl(buildCdnUrl(version));
        layoutConfigDao.updateById(config);

        return ResponseDTO.ok(config.getCdnUrl());
    }
}
```

### 7.3 Database Schema

```sql
-- Layout configuration table
CREATE TABLE t_layout_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    config_name     VARCHAR(100) NOT NULL,
    config_json     JSONB NOT NULL,
    experiment_id   BIGINT,
    status          SMALLINT NOT NULL DEFAULT 0,
    cdn_url         VARCHAR(500),
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_layout_tenant_status ON t_layout_config(tenant_id, status) WHERE deleted = FALSE;

-- Layout component library
CREATE TABLE t_layout_component (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    component_type  VARCHAR(50) NOT NULL,
    component_name  VARCHAR(100) NOT NULL,
    default_config  JSONB,
    display_rules   JSONB,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_component_tenant ON t_layout_component(tenant_id, component_type);

-- Layout version history
CREATE TABLE t_layout_version (
    id              BIGSERIAL PRIMARY KEY,
    config_id       BIGINT NOT NULL REFERENCES t_layout_config(id),
    version         VARCHAR(20) NOT NULL,
    published_at    TIMESTAMP NOT NULL,
    published_by    BIGINT NOT NULL,
    cdn_url         VARCHAR(500),
    rollback_url    VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_version_config ON t_layout_version(config_id, published_at DESC);

-- Component exposure tracking
CREATE TABLE t_component_exposure (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    component_id    VARCHAR(100) NOT NULL,
    player_id       BIGINT,
    experiment_id   BIGINT,
    variant         VARCHAR(10),
    exposed_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exposure_component ON t_component_exposure(component_id, exposed_at DESC);
```
