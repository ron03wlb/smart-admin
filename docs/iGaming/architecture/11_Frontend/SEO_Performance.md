# SEO & Performance Architecture

> **Business Requirements**: [SEO Performance Requirements](../../requirements/11_Frontend_Experience/SEO_Performance_Requirements.md)
> **Canonical Source**: [source-archive/11_Frontend_CMS/11-03](../../source-archive/11_Frontend_CMS/11-03_SEO_and_Performance.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Frontend Developers

---

## 1. SSR/ISR Rendering Strategy

### 1.1 Next.js ISR Configuration

```javascript
// pages/games/[provider]/[slug].tsx
export async function getStaticPaths() {
  const topGames = await fetchTopGames(100);
  return {
    paths: topGames.map(game => ({
      params: { provider: game.provider, slug: game.slug }
    })),
    fallback: 'blocking'
  };
}

export async function getStaticProps({ params }) {
  const game = await fetchGameBySlug(params.provider, params.slug);
  return {
    props: { game },
    revalidate: 3600 // Regenerate every hour
  };
}
```

### 1.2 Canonical URL

```html
<link rel="canonical" href="https://www.casino.com/games/pg-soft/mahjong-ways-2" />
```

### 1.3 Open Graph Tags

```html
<meta property="og:title" content="Mahjong Ways 2 - Play Free Demo | Casino" />
<meta property="og:description" content="Play Mahjong Ways 2 by PG Soft. 96.81% RTP." />
<meta property="og:image" content="https://cdn.casino.com/games/mahjong-ways-2.jpg" />
<meta property="og:url" content="https://www.casino.com/games/pg-soft/mahjong-ways-2" />
```

## 2. Hreflang Configuration

```html
<link rel="alternate" hreflang="en" href="https://www.casino.com/games/slot1" />
<link rel="alternate" hreflang="th" href="https://www.casino.com/th/games/slot1" />
<link rel="alternate" hreflang="vi" href="https://www.casino.com/vi/games/slot1" />
<link rel="alternate" hreflang="x-default" href="https://www.casino.com/games/slot1" />
```

**Sitemap Integration**:
```xml
<url>
  <loc>https://www.casino.com/games/slot1</loc>
  <xhtml:link rel="alternate" hreflang="en" href="https://www.casino.com/games/slot1" />
  <xhtml:link rel="alternate" hreflang="th" href="https://www.casino.com/th/games/slot1" />
</url>
```

## 3. Structured Data (Schema.org)

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "VideoGame",
  "name": "Mahjong Ways 2",
  "description": "Exciting slot game with Mahjong theme",
  "gamePlatform": ["Web Browser", "iOS", "Android"],
  "applicationCategory": "Casino Game",
  "offers": {
    "@type": "Offer", "price": "0", "priceCurrency": "USD"
  },
  "aggregateRating": {
    "@type": "AggregateRating",
    "ratingValue": "4.7", "ratingCount": "1234"
  },
  "provider": {"@type": "Organization", "name": "PG Soft"}
}
</script>
```

## 4. Multi-Layer Cache Architecture

```
+--------------------------------------------+
|  Cloudflare CDN (Edge Cache)               |
|  - HTML: TTL 60s                           |
|  - JS/CSS: TTL 7 days (immutable)          |
|  - Images: TTL 30 days                     |
+----------------+---------------------------+
                 |
+----------------v---------------------------+
|  Origin Server (Next.js)                   |
|  - ISR Cache: 3600s                        |
|  - API Cache: Redis 300s                   |
+----------------+---------------------------+
                 |
+----------------v---------------------------+
|  Database (MySQL)                          |
+--------------------------------------------+
```

### Cache-Control Headers

```nginx
# Static resources (versioned)
location /static/ {
    add_header Cache-Control "public, max-age=31536000, immutable";
}

# HTML pages
location / {
    add_header Cache-Control "public, max-age=60, s-maxage=60, stale-while-revalidate=120";
}
```

### Cache Invalidation

```javascript
async function purgeGameCache(gameSlug) {
  await cloudflarePurge([`/games/*/${gameSlug}`, `/api/games/${gameSlug}`]);
  await redisDel(`game:${gameSlug}`);
}
```

## 5. Image Optimization

```html
<picture>
  <source srcset="/games/slot1.avif" type="image/avif" />
  <source srcset="/games/slot1.webp" type="image/webp" />
  <img src="/games/slot1.jpg" alt="Fortune Tiger Slot" loading="lazy" />
</picture>
```

**CDN Image Transformation**:
```
https://cdn.casino.com/games/slot1.jpg?w=400&h=300&fit=cover&fm=webp&q=85
```

## 6. Performance Monitoring

### 6.1 Core Web Vitals Tracking

```javascript
import { getCLS, getFID, getLCP } from 'web-vitals';

function sendToAnalytics({ name, value, id }) {
  gtag('event', name, {
    event_category: 'Web Vitals',
    value: Math.round(name === 'CLS' ? value * 1000 : value),
    event_label: id, non_interaction: true,
  });
}

getCLS(sendToAnalytics);
getFID(sendToAnalytics);
getLCP(sendToAnalytics);
```

### 6.2 Lighthouse CI

```json
{
  "ci": {
    "assert": {
      "assertions": {
        "categories:performance": ["error", {"minScore": 0.9}],
        "first-contentful-paint": ["error", {"maxNumericValue": 2000}],
        "largest-contentful-paint": ["error", {"maxNumericValue": 2500}],
        "cumulative-layout-shift": ["error", {"maxNumericValue": 0.1}]
      }
    }
  }
}
```
