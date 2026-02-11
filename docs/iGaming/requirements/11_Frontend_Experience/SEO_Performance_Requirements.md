# SEO & Performance Requirements

> **Canonical Source**: [source-archive/11_Frontend_CMS/11-03](../../source-archive/11_Frontend_CMS/11-03_SEO_and_Performance.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, SEO Specialists, Performance Engineers
> **Related Architecture**: [SEO Performance Architecture](../../architecture/11_Frontend/SEO_Performance.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This feature delivers value by:
- Maximizing organic traffic acquisition through SEO-friendly URLs for 3000+ game pages without paid advertising costs
- Automating meta tag generation for thousands of games via template engine (reducing manual content management effort)
- Preventing duplicate content penalties through proper hreflang configuration across multi-country operations
- Meeting Core Web Vitals targets (LCP < 2.5s, FID < 100ms, CLS < 0.1) to improve Google search ranking
- Reducing page load times through CDN edge caching (60s HTML, 7 days static assets) improving user retention
- Enabling continuous performance monitoring through Lighthouse CI integration blocking regressions before deployment

---

## 1. SEO Requirements

### 1.1 Organic Traffic Optimization

The iGaming industry is highly dependent on organic traffic. The platform must maximize SEO effectiveness to ensure thousands of slot/game pages are effectively indexed by search engines.

**Business Goals**:
- Every game must have its own unique, static, SEO-friendly URL
- URL structure: `/games/{provider}/{game-slug}` (e.g., `/games/pg-soft/mahjong-ways-2`)
- Query parameter-based routing is prohibited for game pages

### 1.2 Automated Meta Data

With 3000+ games, manual meta tag maintenance is not feasible. An automated Meta Template Engine is required.

**Title Template**: `"{GameName} Slot - Play Free Demo & RTP {RTP}% | {SiteName}"`

**Description Template**: `"Play {GameName} by {Provider}. Features: {Volatility} volatility, {MaxWin}x max win. Try the free demo now!"`

**Structured Data**: Each game page must include Schema.org JSON-LD markup with `aggregateRating`, `operatingSystem`, and `applicationCategory`.

### 1.3 Sitemap Automation

- Dynamic generation: Daily synchronize game library, automatically write active game URLs to `sitemap-games.xml`
- Chunking: If URLs exceed 50,000, auto-split into multiple sitemap files
- Sitemap must include hreflang annotations for all supported languages

### 1.4 Multi-Language SEO (Hreflang)

For multi-country operations, hreflang tags must be correctly configured to prevent duplicate content penalties.

**URL Strategy**: Subdirectory model (`casino.com/th/games`) is recommended, balancing SEO friendliness and implementation cost.

| Model | SEO Friendliness | Implementation Complexity |
|-------|-------------------|--------------------------|
| Subdomain (`th.casino.com`) | Best | High (multiple SSL certs) |
| Subdirectory (`casino.com/th/`) | Recommended | Medium (routing config) |
| Query Parameter (`?lang=th`) | Not recommended | Low (SEO unfriendly) |

---

## 2. Performance Requirements

### 2.1 Core Web Vitals Targets

| Metric | Target | Description |
|--------|--------|-------------|
| **LCP** (Largest Contentful Paint) | < 2.5s | Largest element render time |
| **FID** (First Input Delay) | < 100ms | First interaction response time |
| **CLS** (Cumulative Layout Shift) | < 0.1 | Visual stability score |

### 2.2 Lighthouse Performance Budget

- Performance score must be >= 90 (error threshold)
- First Contentful Paint must be < 2000ms
- LCP must be < 2500ms
- CLS must be < 0.1

### 2.3 Resource Loading Standards

- All images must use WebP/AVIF format
- Game thumbnails must use lazy loading with blur placeholders
- Game pre-loading: On hover, pre-establish TCP connection (preconnect) to game server
- Clicking "Start Game" must show skeleton screen, never a blank page

### 2.4 CDN Requirements

- Static resources (JS/CSS/Images) must be distributed via global CDN
- HTML pages (especially homepage and game detail pages) must be edge-cached with TTL 60 seconds
- Cache invalidation must be triggered when game information is updated

---

## 3. Mobile Performance Targets

- Cold start: < 2 seconds
- Hot start: < 0.5 seconds
- Image cache must not cause out-of-memory issues

---

## 4. Monitoring Requirements

### 4.1 Real User Monitoring (RUM)

- Core Web Vitals must be tracked via analytics integration
- Performance data must be available in dashboard for trend analysis

### 4.2 Automated Performance Testing

- Lighthouse CI must run on every pull request
- Performance budget violations must block merges

---

## 5. Acceptance Criteria

1. All game pages have unique, SEO-friendly URLs with proper meta tags
2. Sitemap auto-generates daily and includes hreflang annotations
3. Core Web Vitals meet targets: LCP < 2.5s, FID < 100ms, CLS < 0.1
4. Lighthouse score >= 90 on all critical pages
5. Game thumbnails lazy-load with blur placeholders
6. CDN edge cache configured with TTL 60s for HTML, 7 days for static assets
7. Hreflang tags properly configured for all supported languages
