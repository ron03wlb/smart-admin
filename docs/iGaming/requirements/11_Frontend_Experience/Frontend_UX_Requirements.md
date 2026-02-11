# Frontend UX Requirements

> **Canonical Source**: [source-archive/11_Frontend_CMS/11-01](../../source-archive/11_Frontend_CMS/11-01_Frontend_Layout_Engine.md), [11-02](../../source-archive/11_Frontend_CMS/11-02_Banner_and_Announcement.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, UX Designers, Operations Team
> **Related Doc**: [Frontend Layout Engine Architecture](../../architecture/11_Frontend/Frontend_Layout_Engine.md), [Banner Announcement Architecture](../../architecture/11_Frontend/Banner_Announcement.md)
> **Last Synced**: 2026-02-09

---

## 1. Layout Configuration Requirements

### 1.1 No-Code Page Editor

Operations staff must be able to adjust frontend homepage layout through a drag-and-drop interface without engineering involvement.

**Business Rules**:
- Theme switching: Platform provides multiple pre-built themes (dark, light, holiday-limited)
- Merchants can switch entire site color scheme with one click
- Component library must include: Banner carousel, Marquee, Game Grid (3/4/5 column configurable), Deposit guide button
- Page editor supports drag-to-reorder modules and customizable module titles
- Navigation bar (Header/Footer) supports custom menu ordering with internal page or external link jumps

### 1.2 Multi-Terminal Support

- Layout configuration must be adaptive across Web, H5, and App terminals
- Each terminal may have independent component visibility settings

### 1.3 Wallet Mode UI Adaptation

Frontend must automatically switch Header information display based on player wallet mode:

| Wallet Mode | Display Content | Key Features |
|-------------|----------------|--------------|
| **Cash Mode** | Balance (Total Cash + Bonus) | Emphasize "Deposit" button |
| **Credit Mode** | Credit Limit, Used, Available | Hide deposit button; show "Quota" details page; display Settlement Countdown |
| **Hybrid Mode** | Both Cash Balance and Available Credit | Payment selector: "Deduct Cash first" or "Use Credit" |

---

## 2. Approval and Publishing Requirements

### 2.1 Preview Before Publish

- Editing completion must generate a Preview URL for internal testing
- Preview must be viewable without affecting production users

### 2.2 Publishing Workflow

- "Publish" action generates a version number (e.g., v1.0.1)
- Configuration pushes to CDN for global distribution
- Frontend pulls latest configuration through API
- Version checking: only download new config when local version is expired

### 2.3 Approval Mandate

All banner content must go through mandatory approval:
- **Operations Specialist**: Submit Banner, edit drafts
- **Operations Supervisor**: Review Banner content and target links, approve publishing
- **CTO**: Emergency takedown authority for critical errors

**Rationale**: Incorrect promotional content (e.g., "Deposit 100 get 1000") could cause massive financial losses.

---

## 3. Banner and Announcement Requirements

### 3.1 Banner Types and Attributes

| Banner Type | Attributes |
|------------|------------|
| PC Homepage Carousel | Image (multi-language), Jump link (Deep Link), Effective time range, Sort order |
| H5 Homepage Carousel | Same as PC with mobile-responsive dimensions |
| Pop-up Advertisement | Same attributes with display frequency control |

### 3.2 Multi-Language Banner Management

**Language Matrix**:

| Language | Required | Priority |
|----------|----------|----------|
| zh-CN (Simplified Chinese) | Mandatory | P0 |
| en-US (English) | Mandatory | P0 |
| vi-VN (Vietnamese) | Mandatory | P1 |
| th-TH (Thai) | Mandatory | P1 |
| pt-BR (Portuguese) | Optional | P2 |
| ja-JP (Japanese) | Optional | P2 |

**Fallback Logic**: User language -> English -> Simplified Chinese (platform default) -> Default image

### 3.3 Marquee (Scrolling Announcements)

- **System Auto-Generated**: Congratulations messages (e.g., "Player xxx won $10,000 on game yyy")
- **Manual Publishing**: Platform maintenance notices, new payment channel announcements
- **Playback Configuration**: Speed, color, loop count all configurable

### 3.4 Inbox Messages (Station Mail)

- **Target Audience**: Broadcast (all users), Segment (e.g., "all VIP3+ players"), Individual
- **Template Support**: HTML format with image and button insertion

---

## 4. Banner Display Rules (Targeting)

### 4.1 User Segment Targeting

| Segment | Criteria | Banner Type |
|---------|----------|-------------|
| New Players | Registration < 7 days | First deposit promotion |
| Active Players | Login in last 7 days | Game recommendation |
| Inactive Players | No login for 30 days | Return bonus |
| High Rollers | Monthly deposit > $10,000 | VIP exclusive events |

### 4.2 VIP Level Targeting

| VIP Level | Visible Banners |
|-----------|----------------|
| Bronze | Basic activity banners |
| Silver | Standard activity + weekly cashback |
| Gold | VIP events + birthday bonus |
| Platinum | VIP exclusive + custom service |
| Diamond | Top-tier events + personal account manager |

### 4.3 Device Targeting

- **Desktop Only**: Large banners for desktop sites only
- **Mobile Only**: Mobile-adapted small screen banners
- **Both**: Responsive design for all platforms

### 4.4 Geo Targeting

- Banners can be restricted by country/region
- Payment-specific banners by geography (e.g., UnionPay for China, PromptPay for Thailand)

### 4.5 Time-Based Rules

- **Fixed Time Window**: e.g., "Night deposit bonus" shown 18:00-23:00 daily
- **Weekend Exclusive**: Only shown on Saturday/Sunday
- **Holiday Events**: Chinese New Year, Christmas period-specific banners

---

## 5. Banner Analytics Requirements

### 5.1 Core Metrics

| Metric | Definition |
|--------|-----------|
| Impressions | Number of times banner was loaded |
| Clicks | Number of times banner was clicked |
| CTR (Click-Through Rate) | Clicks / Impressions x 100% |
| Conversions | Completed target action after click (e.g., deposit) |
| CVR (Conversion Rate) | Conversions / Clicks x 100% |

### 5.2 Attribution Logic

- Banner click must be tracked with conversion attribution
- Support for A/B testing with variant comparison

---

## 6. Image Requirements

### 6.1 Technical Specifications

| Device Type | Max File Size | Recommended Size (px) |
|------------|---------------|----------------------|
| Desktop Banner | 200 KB | 1920x600 |
| Mobile Banner | 150 KB | 750x400 |
| Pop-up Ad | 100 KB | 600x600 |
| Marquee Icon | 20 KB | 32x32 |

### 6.2 Format Requirements

- WebP/PNG preferred format
- Responsive images required for multi-device support
- Lazy loading for all non-critical images

---

## 7. Acceptance Criteria

- [ ] Operations can create, edit, preview, and publish page layouts without developer assistance
- [ ] Banner system supports multi-language with proper fallback (User language → English → zh-CN → default image)
- [ ] All banners go through mandatory approval workflow (Specialist → Supervisor) before publishing
- [ ] Targeting rules correctly filter banners by user segment, VIP level, device, geo, and time
- [ ] Analytics dashboard shows real-time CTR and conversion metrics with attribution tracking
- [ ] Wallet mode UI correctly adapts Header display based on player configuration (Cash/Credit/Hybrid)
- [ ] Theme switching allows one-click site-wide color scheme change across all page components
- [ ] Multi-terminal support provides adaptive layout for Web, H5, and App with independent component visibility
- [ ] Preview URL generation allows internal testing without affecting production users
- [ ] Version control generates version numbers on publish and pushes configuration to CDN
