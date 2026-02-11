# Localization Requirements

> **Canonical Source**: [source-archive/11_Frontend_CMS/11-07](../../source-archive/11_Frontend_CMS/11-07_i18n_Localization.md), [11-08](../../source-archive/11_Frontend_CMS/11-08_Dynamic_Content_Localization.md), [11-09](../../source-archive/11_Frontend_CMS/11-09_Localization_Workflow.md), [11-10](../../source-archive/11_Frontend_CMS/11-10_Localization_API.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Localization Managers, Operations Team
> **Related Doc**: [i18n Architecture](../../architecture/11_Frontend/i18n_Localization.md), [Dynamic Content Localization](../../architecture/11_Frontend/Dynamic_Content_Localization.md), [Localization Workflow](../../architecture/11_Frontend/Localization_Workflow.md), [Localization API](../../architecture/11_Frontend/Localization_API.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This feature delivers value by:
- Enabling market expansion across 20 languages including P0 markets (English, Traditional/Simplified Chinese) and P1 growth markets (Thai, Vietnamese, Indonesian, Portuguese)
- Supporting RTL languages (Arabic, Hebrew) for Middle East market entry without layout rework
- Reducing operational cost through automated meta template engine for 5,000-10,000 translation keys
- Accelerating content localization through Crowdin integration with webhook-triggered auto-import
- Ensuring translation quality through role-based workflow (Translator → Reviewer → Publisher) with state machine lifecycle
- Protecting user experience through multi-tier fallback (User preference → Browser → GeoIP → English)

---

## 1. Multi-Language Support Requirements

### 1.1 Supported Languages

| Language Code | Language | Market | RTL | Priority |
|--------------|----------|--------|-----|----------|
| `en` | English | Global | No | P0 |
| `zh-TW` | Traditional Chinese | Taiwan, Hong Kong | No | P0 |
| `zh-CN` | Simplified Chinese | China, Singapore | No | P0 |
| `th` | Thai | Thailand | No | P1 |
| `vi` | Vietnamese | Vietnam | No | P1 |
| `id` | Bahasa Indonesia | Indonesia | No | P1 |
| `pt-BR` | Portuguese | Brazil | No | P1 |
| `ar` | Arabic | Middle East | Yes | P2 |
| `he` | Hebrew | Israel | Yes | P2 |

### 1.2 Core Principle

All user-facing text must use Translation Keys. Hardcoded strings are strictly prohibited.

**Namespace Convention**: `module.component.key`
- Example: `common.button.submit`, `game.slot.freespin_won`, `error.wallet.insufficient`

### 1.3 Scale Expectations

| Metric | Expected Value |
|--------|---------------|
| Supported languages | 20 |
| Total translation keys | 5,000 - 10,000 |
| File size per language | ~500 KB |
| Peak API throughput | 5,000 req/s |

---

## 2. Regional Format Standards

Beyond text translation, the platform must handle format differences:

| Category | Item | US Format | Vietnam Format | India Format |
|----------|------|-----------|----------------|--------------|
| **Currency** | Symbol & position | $100.00 | 100.000 VND | Rs.100.00 |
| **Numbers** | Thousands/decimal | 1,234.56 | 1.234,56 | 1,234.56 |
| **Date** | Format | MM/DD/YYYY | DD/MM/YYYY | DD/MM/YYYY |
| **Timezone** | Default display | UTC-4 | UTC+7 | UTC+5.5 |

**Mandatory**: Use standard internationalization libraries (Intl API, dayjs). Custom regex for formatting is strictly prohibited.

---

## 3. RTL Language Support

### 3.1 RTL Requirements

When supporting Arabic or Hebrew markets:
- Navigation bar direction reversal (right-to-left)
- Form field order reversal
- Icon mirroring (back arrow flips direction)
- Numbers and English text remain LTR
- Scrollbar position adjustment

### 3.2 RTL Testing Checklist

- Navigation direction correct
- Form fields reordered
- Icons properly mirrored
- Numbers maintain LTR
- Scrollbar adjusted

---

## 4. Dynamic Content Localization

### 4.1 Content Types Requiring Localization

| Content Type | Fields | Example |
|-------------|--------|---------|
| Games | name, description, rules | Game names, gameplay instructions |
| Banners | title, subtitle, cta_text | Banner titles, call-to-action text |
| Notifications | title, body | In-site notification content |
| FAQs | question, answer | Frequently asked questions |
| VIP Tiers | tier_name, benefits | VIP level names and perks |
| Payment Methods | display_name, instructions | Payment method names and guides |

### 4.2 Language Fallback Priority

1. User preference language (from profile)
2. Browser Accept-Language header
3. GeoIP detected language
4. Platform default language (English)

### 4.3 API Response Modes

- **Single-language response** (for mobile apps): Return only the user's language
- **Multi-language response** (for CMS backend): Return all language versions for editing

### 4.4 Translation Completeness Tracking

The CMS must show translation completeness dashboards per content type and language, highlighting missing translations.

---

## 5. Translation Workflow Requirements

### 5.1 Missing Key Detection

When the frontend attempts to display a missing translation key, the system must automatically report it to the backend for tracking and resolution.

### 5.2 Translation Lifecycle

| State | Description | Allowed Actions |
|-------|-------------|-----------------|
| **Draft** | Translator is editing | Edit, Submit for review |
| **In Review** | Awaiting reviewer approval | Approve, Reject, Flag |
| **Approved** | Approved, pending publish | Publish |
| **Published** | Live on CDN, visible to players | Archive |
| **Flagged** | Issue found, needs correction | Edit, Resubmit |
| **Archived** | Deprecated, no longer in use | Delete |

### 5.3 Role-Based Access

| Role | Permissions | Responsibilities |
|------|------------|-----------------|
| Translator | Edit draft, submit for review | Translate content |
| Reviewer | Approve/reject submissions | Ensure quality |
| Publisher | Publish approved translations to CDN | Release management |
| Admin | All operations | System administration |

### 5.4 Batch Import/Export

- Support JSON, CSV, and XLIFF export formats
- Support bulk import with upsert or overwrite modes
- CSV format compatible with Excel and third-party translation tools
- XLIFF format compatible with CAT (Computer-Assisted Translation) tools

### 5.5 Crowdin Integration

- Push missing translation tasks to Crowdin platform
- Receive Webhook notifications when translations are completed
- Auto-import completed translations
- Scheduled sync jobs for regular synchronization

---

## 6. Image and Media Localization

### 6.1 Localized Image Naming

File naming format: `{resource_name}_{lang}.{ext}`

Example: `new_year_promo_en.jpg`, `new_year_promo_zh-TW.jpg`, `new_year_promo_th.jpg`

### 6.2 Best Practices

- Use pure graphic backgrounds with CSS/HTML text overlays (translatable)
- Avoid embedding text directly in images (cannot be translated)
- QA process should use OCR to detect hardcoded text in images

---

## 7. SLA Requirements

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| API Response Time (P99) | < 50ms | > 200ms |
| Cache Hit Rate | > 95% | < 85% |
| Translation Coverage | 100% | < 95% |
| Missing Key Rate | 0% | > 1% |

---

## 8. Implementation Roadmap

| Phase | Duration | Scope |
|-------|----------|-------|
| Phase 1 | Week 1-2 | Database, API, frontend i18n integration |
| Phase 2 | Week 3-4 | Translation management UI, batch upload, online editor |
| Phase 3 | Week 5-6 | Translation state machine, RBAC, Crowdin integration |
| Phase 4 | Week 7-8 | CDN distribution, RTL support, monitoring |

**Total estimated effort**: 6-8 weeks

---

## 9. Acceptance Criteria

1. All user-facing text uses translation keys (zero hardcoded strings)
2. Language fallback chain works correctly across all scenarios
3. RTL languages render correctly with proper layout mirroring
4. Dynamic content (banners, promotions, games) supports multi-language editing
5. Translation workflow supports draft -> review -> approve -> publish lifecycle
6. Batch import/export works in JSON, CSV, and XLIFF formats
7. Missing keys are automatically detected, reported, and tracked
8. API response time < 50ms at P99, cache hit rate > 95%
