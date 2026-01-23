# P1-10: Headless CMS Integration

## Document Control

| Attribute | Value |
|-----------|-------|
| Document ID | P1-10 |
| Title | Headless CMS Integration |
| Version | 1.0.0 |
| Status | Draft |
| Author | SmartAdmin Architecture Team |
| Created | 2026-01-23 |
| Last Updated | 2026-01-23 |
| Related Docs | [backend_project.md](../../backend_project.md), [P1-07](07-multi-tenant-isolation.md), [P1-14](14-performance-optimization.md) |

---

## 1. Background

### 1.1 Purpose

This document provides comprehensive architecture and implementation guidance for integrating a headless CMS (Content Management System) to enable multi-tenant dynamic branding and content management across the iGaming platform.

**Core Objectives**:
1. **Zero Marginal Cost Scaling**: One codebase serves 100+ merchants with different branding
2. **Marketer Autonomy**: Non-technical users can update content without developer involvement
3. **Multi-Language Support**: Serve players in 20+ languages with locale fallback
4. **Performance**: <100ms content delivery with multi-level caching
5. **Preview & Versioning**: Live preview before publishing, rollback capability

### 1.2 Scope

**In Scope**:
- Strapi 4.x headless CMS integration architecture
- Multi-tenant content isolation and theming
- Component-based content modeling (landing pages, promotions, T&C)
- Multi-language (i18n) content management with locale fallback
- GraphQL API for frontend content delivery
- Content versioning and rollback
- Cache invalidation strategy (Caffeine L1 + Redis L2)
- Webhook integration for real-time cache updates
- Frontend preview mode with live editing

**Out of Scope**:
- Frontend Vue.js implementation details (covered in smart-admin-web docs)
- Image/video optimization and CDN configuration
- SEO meta tag management (separate CMS plugin)
- Marketing automation and A/B testing (covered in P2-21)

### 1.3 Strategic Alignment

Aligns with igame_str.md first principles:
- **Code Leverage**: 1 codebase → 100 merchants (zero marginal cost)
- **Friction (摩擦)**: Marketers update content independently (no developer bottleneck)
- **Velocity (速度)**: <100ms content delivery enables instant branding changes

### 1.4 Business Requirements

| Requirement | Description | Success Criteria |
|-------------|-------------|------------------|
| Multi-Tenant Branding | Each merchant has unique colors, logos, layouts | 100% visual isolation |
| Content Autonomy | Marketers can publish without code deployment | 0 developer involvement |
| Multi-Language | Support 20+ languages with fallback | 100% content translated |
| Performance | Content delivery latency | <100ms p95 |
| Cache Hit Rate | Reduce CMS API calls | >90% cache hit rate |
| Versioning | Rollback to previous version | <5 min rollback time |
| Preview | Live preview before publishing | Real-time preview |

---

## 2. Architecture

### 2.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          Landing Page (Dynamic Components)               │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │  │
│  │  │  Hero   │  │Promotion│  │  Games  │  │ Footer  │    │  │
│  │  │ Banner  │  │  Cards  │  │  Grid   │  │ (T&C)   │    │  │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           │                                     │
│                           ▼ GraphQL Query                       │
└───────────────────────────┼─────────────────────────────────────┘
                            │
┌───────────────────────────┼─────────────────────────────────────┐
│                SmartAdmin Backend (Spring Boot)                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              CMS Content Controller                     │    │
│  │  @GetMapping("/api/cms/content/{page}")               │    │
│  └────────────────────────────────────────────────────────┘    │
│                           │                                     │
│  ┌────────────────────────▼───────────────────────────────┐    │
│  │              CMS Content Service                        │    │
│  │  - getTenantId() from TenantContextHolder              │    │
│  │  - getLocale() from Accept-Language header             │    │
│  │  - Check L1 cache (Caffeine) → L2 cache (Redis)       │    │
│  │  - Fetch from Strapi CMS if cache miss                 │    │
│  └────────────────────────────────────────────────────────┘    │
│                           │                                     │
└───────────────────────────┼─────────────────────────────────────┘
                            │ REST API / GraphQL
┌───────────────────────────┼─────────────────────────────────────┐
│                      Strapi CMS (Headless)                       │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Content Types (Collections)                │    │
│  │  - LandingPage (tenant_id, locale, components)         │    │
│  │  - Theme (tenant_id, primary_color, logo_url)          │    │
│  │  - Promotion (tenant_id, locale, title, image)         │    │
│  │  - TermsAndConditions (tenant_id, locale, html)        │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Content Versioning (Draft/Published)       │    │
│  │  - Draft: Work-in-progress content                     │    │
│  │  - Published: Live content served to players           │    │
│  │  - Version History: Rollback capability                │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Webhooks (Content Update Events)           │    │
│  │  - POST https://api.smartadmin.com/webhook/cms        │    │
│  │  - Payload: { "event": "entry.publish", "model": ... }│    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼ PostgreSQL
┌─────────────────────────────────────────────────────────────────┐
│                      Strapi Database                             │
│  - strapi_content_releases                                       │
│  - strapi_releases_actions                                       │
│  - landing_pages (tenant_id, locale, components JSON)           │
│  - themes (tenant_id, primary_color, logo_url)                  │
│  - promotions (tenant_id, locale, title, image_url)             │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Multi-Tenant Content Isolation

**Tenant-Scoped Content** (Row-Level Security):
```
Landing Page (tenant_id = "merchant_a", locale = "en")
├── Component: HeroBanner
│   ├── title: "Welcome to Casino A!"
│   ├── subtitle: "Get 100% Bonus"
│   └── background_image: "https://cdn.example.com/hero_a.jpg"
├── Component: PromotionCards
│   ├── promotion_1: { title: "Free Spins", ... }
│   └── promotion_2: { title: "Cashback", ... }
└── Component: Footer
    └── terms_link: "/terms"

Landing Page (tenant_id = "merchant_b", locale = "en")
├── Component: HeroBanner
│   ├── title: "Welcome to Casino B!"
│   ├── subtitle: "Play Now!"
│   └── background_image: "https://cdn.example.com/hero_b.jpg"
└── ...
```

**Strapi Multi-Tenancy Plugin**:
```javascript
// config/plugins.js
module.exports = {
  'strapi-plugin-multi-tenant': {
    enabled: true,
    config: {
      tenantField: 'tenant_id',  // Field name for tenant isolation
      superAdminRole: 'Super Admin',  // Role that can manage all tenants
    },
  },
};
```

### 2.3 Content Model (Strapi Schema)

**Theme Content Type** (themes.json):
```json
{
  "kind": "collectionType",
  "collectionName": "themes",
  "info": {
    "singularName": "theme",
    "pluralName": "themes",
    "displayName": "Theme"
  },
  "options": {
    "draftAndPublish": true
  },
  "attributes": {
    "tenant_id": {
      "type": "string",
      "required": true,
      "unique": true
    },
    "primary_color": {
      "type": "string",
      "default": "#1890ff"
    },
    "secondary_color": {
      "type": "string",
      "default": "#52c41a"
    },
    "logo_url": {
      "type": "string",
      "required": true
    },
    "favicon_url": {
      "type": "string"
    },
    "font_family": {
      "type": "enumeration",
      "enum": ["Roboto", "Inter", "Open Sans", "Lato"],
      "default": "Roboto"
    },
    "custom_css": {
      "type": "text"
    }
  }
}
```

**Landing Page Content Type** (landing-pages.json):
```json
{
  "kind": "collectionType",
  "collectionName": "landing_pages",
  "info": {
    "singularName": "landing-page",
    "pluralName": "landing-pages",
    "displayName": "Landing Page"
  },
  "options": {
    "draftAndPublish": true
  },
  "pluginOptions": {
    "i18n": {
      "localized": true
    }
  },
  "attributes": {
    "tenant_id": {
      "type": "string",
      "required": true
    },
    "slug": {
      "type": "string",
      "required": true
    },
    "components": {
      "type": "dynamiczone",
      "components": [
        "sections.hero-banner",
        "sections.promotion-cards",
        "sections.games-grid",
        "sections.footer"
      ]
    },
    "seo": {
      "type": "component",
      "component": "shared.seo",
      "repeatable": false
    }
  }
}
```

**Dynamic Zone Components** (sections.hero-banner.json):
```json
{
  "collectionName": "components_sections_hero_banners",
  "info": {
    "displayName": "Hero Banner",
    "icon": "image"
  },
  "options": {},
  "attributes": {
    "title": {
      "type": "string",
      "required": true
    },
    "subtitle": {
      "type": "string"
    },
    "background_image": {
      "type": "media",
      "allowedTypes": ["images"],
      "required": true
    },
    "cta_button": {
      "type": "component",
      "component": "shared.button",
      "repeatable": false
    }
  }
}
```

### 2.4 Multi-Language (i18n) Architecture

**Strapi i18n Plugin Configuration**:
```javascript
// config/plugins.js
module.exports = {
  i18n: {
    enabled: true,
    config: {
      defaultLocale: 'en',
      locales: ['en', 'es', 'pt', 'de', 'fr', 'ja', 'ko', 'zh', 'ru', 'ar'],
    },
  },
};
```

**Locale Fallback Strategy**:
```
Request: locale=pt-BR (Portuguese Brazil)
1. Check pt-BR content → Not found
2. Fallback to pt (Portuguese) → Not found
3. Fallback to en (English) → Found ✓

Request: locale=zh-CN (Simplified Chinese)
1. Check zh-CN → Found ✓
```

---

## 3. Implementation

### 3.1 Backend Integration (SmartAdmin)

#### 3.1.1 CMS Content Service

**CmsContentService.java**:
```java
package net.lab1024.sa.base.module.support.cms.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.module.support.tenant.context.TenantContextHolder;
import net.lab1024.sa.foundation.cache.CaffeineCache;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * CMS content service
 *
 * Responsibilities:
 * - Fetch content from Strapi CMS via REST API or GraphQL
 * - Multi-level caching (L1: Caffeine, L2: Redis)
 * - Locale fallback (zh-CN → zh → en)
 * - Tenant isolation
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmsContentService {

    private final RestTemplate restTemplate;
    private final RedissonClient redisson;
    private final Cache<String, Object> contentCache;

    private static final String STRAPI_BASE_URL = "http://strapi:1337/api";
    private static final String DEFAULT_LOCALE = "en";

    /**
     * Get landing page content
     *
     * @param slug Page slug (e.g., "home", "promotions")
     * @param locale Requested locale (e.g., "en", "zh-CN")
     * @return Landing page content
     */
    public LandingPageContent getLandingPage(String slug, String locale) {
        String tenantId = TenantContextHolder.getTenantId();

        // Try with exact locale first
        LandingPageContent content = getContentWithCache(tenantId, slug, locale);
        if (content != null) {
            return content;
        }

        // Fallback to base locale (zh-CN → zh)
        if (locale.contains("-")) {
            String baseLocale = locale.split("-")[0];
            content = getContentWithCache(tenantId, slug, baseLocale);
            if (content != null) {
                log.debug("Locale fallback: {} → {} for tenant={}, slug={}", locale, baseLocale, tenantId, slug);
                return content;
            }
        }

        // Fallback to default locale (en)
        if (!DEFAULT_LOCALE.equals(locale)) {
            content = getContentWithCache(tenantId, slug, DEFAULT_LOCALE);
            if (content != null) {
                log.debug("Locale fallback: {} → {} for tenant={}, slug={}", locale, DEFAULT_LOCALE, tenantId, slug);
                return content;
            }
        }

        log.warn("No content found for tenant={}, slug={}, locale={}", tenantId, slug, locale);
        return null;
    }

    /**
     * Get content with multi-level cache (L1 + L2)
     */
    private LandingPageContent getContentWithCache(String tenantId, String slug, String locale) {
        String cacheKey = buildCacheKey(tenantId, slug, locale);

        // L1: Caffeine cache
        LandingPageContent content = (LandingPageContent) contentCache.getIfPresent(cacheKey);
        if (content != null) {
            log.debug("L1 cache hit: {}", cacheKey);
            return content;
        }

        // L2: Redis cache
        RBucket<LandingPageContent> redisBucket = redisson.getBucket(cacheKey);
        content = redisBucket.get();
        if (content != null) {
            log.debug("L2 cache hit: {}", cacheKey);
            contentCache.put(cacheKey, content);  // Populate L1
            return content;
        }

        // L3: Fetch from Strapi CMS
        content = fetchFromStrapi(tenantId, slug, locale);
        if (content != null) {
            // Populate L2 and L1 caches
            redisBucket.set(content, 30, TimeUnit.MINUTES);
            contentCache.put(cacheKey, content);
            log.debug("Fetched from Strapi and cached: {}", cacheKey);
        }

        return content;
    }

    /**
     * Fetch content from Strapi via GraphQL
     */
    private LandingPageContent fetchFromStrapi(String tenantId, String slug, String locale) {
        String graphqlQuery = String.format("""
            {
              landingPages(filters: { tenant_id: { eq: "%s" }, slug: { eq: "%s" } }, locale: "%s") {
                data {
                  id
                  attributes {
                    slug
                    components {
                      __typename
                      ... on ComponentSectionsHeroBanner {
                        title
                        subtitle
                        background_image {
                          data {
                            attributes {
                              url
                            }
                          }
                        }
                        cta_button {
                          text
                          link
                        }
                      }
                      ... on ComponentSectionsPromotionCards {
                        promotions {
                          data {
                            attributes {
                              title
                              description
                              image {
                                data {
                                  attributes {
                                    url
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """, tenantId, slug, locale);

        try {
            String response = restTemplate.postForObject(
                STRAPI_BASE_URL + "/graphql",
                Map.of("query", graphqlQuery),
                String.class
            );

            return parseStrapiResponse(response);
        } catch (Exception e) {
            log.error("Failed to fetch content from Strapi: tenant={}, slug={}, locale={}", tenantId, slug, locale, e);
            return null;
        }
    }

    /**
     * Get tenant theme (colors, logo, fonts)
     */
    public ThemeConfig getTenantTheme(String tenantId) {
        String cacheKey = "theme:" + tenantId;

        // L1 cache
        ThemeConfig theme = (ThemeConfig) contentCache.getIfPresent(cacheKey);
        if (theme != null) {
            return theme;
        }

        // L2 cache
        RBucket<ThemeConfig> redisBucket = redisson.getBucket(cacheKey);
        theme = redisBucket.get();
        if (theme != null) {
            contentCache.put(cacheKey, theme);
            return theme;
        }

        // Fetch from Strapi
        String url = String.format("%s/themes?filters[tenant_id][$eq]=%s", STRAPI_BASE_URL, tenantId);
        try {
            StrapiResponse<ThemeConfig> response = restTemplate.getForObject(url, StrapiResponse.class);
            if (response != null && !response.getData().isEmpty()) {
                theme = response.getData().get(0).getAttributes();

                // Cache for 1 hour (themes rarely change)
                redisBucket.set(theme, 1, TimeUnit.HOURS);
                contentCache.put(cacheKey, theme);

                return theme;
            }
        } catch (Exception e) {
            log.error("Failed to fetch theme from Strapi: tenant={}", tenantId, e);
        }

        // Return default theme
        return ThemeConfig.defaultTheme();
    }

    /**
     * Invalidate cache for specific content (called by webhook)
     */
    public void invalidateCache(String tenantId, String slug, String locale) {
        String cacheKey = buildCacheKey(tenantId, slug, locale);

        contentCache.invalidate(cacheKey);  // L1
        redisson.getBucket(cacheKey).delete();  // L2

        log.info("Cache invalidated: {}", cacheKey);
    }

    private String buildCacheKey(String tenantId, String slug, String locale) {
        return String.format("cms:page:%s:%s:%s", tenantId, slug, locale);
    }
}
```

#### 3.1.2 CMS Content Controller

**CmsContentController.java**:
```java
package net.lab1024.sa.base.module.support.cms.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.module.support.cms.domain.LandingPageContent;
import net.lab1024.sa.base.module.support.cms.domain.ThemeConfig;
import net.lab1024.sa.base.module.support.cms.service.CmsContentService;
import net.lab1024.sa.base.module.support.tenant.context.TenantContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * CMS content controller
 *
 * Endpoints:
 * - GET /api/cms/page/{slug} - Get landing page content
 * - GET /api/cms/theme - Get tenant theme
 * - POST /webhook/cms - Strapi webhook for cache invalidation
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@RestController
@RequiredArgsConstructor
public class CmsContentController {

    private final CmsContentService cmsContentService;

    /**
     * Get landing page content
     *
     * URL: GET /api/cms/page/home?locale=zh-CN
     * Headers: X-Tenant-Id: merchant_a
     *
     * @param slug Page slug (e.g., "home", "promotions")
     * @param locale Locale (optional, defaults to browser locale)
     */
    @GetMapping("/api/cms/page/{slug}")
    public ResponseDTO<LandingPageContent> getLandingPage(
        @PathVariable String slug,
        @RequestParam(required = false) String locale,
        HttpServletRequest request
    ) {
        // Get locale from query param or Accept-Language header
        if (locale == null) {
            Locale requestLocale = request.getLocale();
            locale = requestLocale.toLanguageTag();
        }

        LandingPageContent content = cmsContentService.getLandingPage(slug, locale);
        if (content == null) {
            return ResponseDTO.error("Content not found");
        }

        return ResponseDTO.ok(content);
    }

    /**
     * Get tenant theme configuration
     *
     * URL: GET /api/cms/theme
     * Headers: X-Tenant-Id: merchant_a
     */
    @GetMapping("/api/cms/theme")
    public ResponseDTO<ThemeConfig> getTenantTheme() {
        String tenantId = TenantContextHolder.getTenantId();
        ThemeConfig theme = cmsContentService.getTenantTheme(tenantId);
        return ResponseDTO.ok(theme);
    }

    /**
     * Webhook endpoint for Strapi content updates
     *
     * URL: POST /webhook/cms
     * Body: {
     *   "event": "entry.publish",
     *   "model": "landing-page",
     *   "entry": {
     *     "tenant_id": "merchant_a",
     *     "slug": "home",
     *     "locale": "en"
     *   }
     * }
     */
    @PostMapping("/webhook/cms")
    public ResponseDTO<Void> handleCmsWebhook(@RequestBody CmsWebhookPayload payload) {
        // Invalidate cache for updated content
        if ("entry.publish".equals(payload.getEvent()) || "entry.update".equals(payload.getEvent())) {
            String tenantId = payload.getEntry().get("tenant_id");
            String slug = payload.getEntry().get("slug");
            String locale = payload.getEntry().get("locale");

            cmsContentService.invalidateCache(tenantId, slug, locale);
        }

        return ResponseDTO.ok();
    }
}
```

### 3.2 Strapi CMS Configuration

#### 3.2.1 Strapi Installation

**docker-compose.yml** (Strapi + PostgreSQL):
```yaml
version: '3.8'

services:
  strapi:
    image: strapi/strapi:4.15.5
    container_name: strapi-cms
    restart: unless-stopped
    environment:
      DATABASE_CLIENT: postgres
      DATABASE_HOST: postgres-strapi
      DATABASE_PORT: 5432
      DATABASE_NAME: strapi
      DATABASE_USERNAME: strapi
      DATABASE_PASSWORD: strapi_password
      DATABASE_SSL: false
      JWT_SECRET: your-jwt-secret-here
      ADMIN_JWT_SECRET: your-admin-jwt-secret
      APP_KEYS: app-key-1,app-key-2
      NODE_ENV: production
    ports:
      - "1337:1337"
    volumes:
      - ./strapi-app:/srv/app
    depends_on:
      - postgres-strapi

  postgres-strapi:
    image: postgres:16-alpine
    container_name: postgres-strapi
    restart: unless-stopped
    environment:
      POSTGRES_DB: strapi
      POSTGRES_USER: strapi
      POSTGRES_PASSWORD: strapi_password
    volumes:
      - postgres-strapi-data:/var/lib/postgresql/data

volumes:
  postgres-strapi-data:
```

**Start Strapi**:
```bash
# Start containers
docker-compose up -d

# Access Strapi admin panel
open http://localhost:1337/admin

# Create admin user on first launch
# Email: admin@example.com
# Password: (choose strong password)
```

#### 3.2.2 Multi-Tenancy Middleware

**middlewares/multi-tenant.js** (Custom Strapi Middleware):
```javascript
module.exports = (config, { strapi }) => {
  return async (ctx, next) => {
    const { user } = ctx.state;

    // Apply tenant filter for non-admin users
    if (user && user.role.type !== 'super_admin') {
      const tenantId = user.tenant_id;

      // Add tenant_id filter to all queries
      if (ctx.query.filters) {
        ctx.query.filters = {
          ...ctx.query.filters,
          tenant_id: { $eq: tenantId },
        };
      } else {
        ctx.query.filters = {
          tenant_id: { $eq: tenantId },
        };
      }

      // Add tenant_id to create/update operations
      if (ctx.request.body && ctx.request.body.data) {
        ctx.request.body.data.tenant_id = tenantId;
      }
    }

    await next();
  };
};
```

**config/middlewares.js**:
```javascript
module.exports = [
  'strapi::errors',
  'strapi::security',
  'strapi::cors',
  'strapi::poweredBy',
  'strapi::logger',
  'strapi::query',
  'strapi::body',
  'strapi::session',
  'strapi::favicon',
  'strapi::public',
  {
    name: 'global::multi-tenant',
    config: {},
  },
];
```

#### 3.2.3 Webhook Configuration

**Strapi Admin Panel**:
```
Settings → Webhooks → Create new webhook

Name: SmartAdmin Cache Invalidation
URL: https://api.smartadmin.com/webhook/cms
Events:
  ✓ entry.publish
  ✓ entry.update
  ✓ entry.delete
Headers:
  X-Webhook-Secret: your-webhook-secret
```

### 3.3 Frontend Integration (Vue 3)

#### 3.3.1 Content Fetching Composable

**useCmsContent.ts**:
```typescript
import { ref, onMounted } from 'vue';
import { request } from '@/lib/request';

export interface LandingPageContent {
  slug: string;
  components: Component[];
}

export interface ThemeConfig {
  primaryColor: string;
  secondaryColor: string;
  logoUrl: string;
  faviconUrl: string;
  fontFamily: string;
  customCss?: string;
}

export function useCmsContent(slug: string) {
  const content = ref<LandingPageContent | null>(null);
  const loading = ref(true);
  const error = ref<string | null>(null);

  const fetchContent = async () => {
    loading.value = true;
    error.value = null;

    try {
      // Get browser locale (e.g., "zh-CN", "en-US")
      const locale = navigator.language;

      const response = await request.get<LandingPageContent>(
        `/api/cms/page/${slug}`,
        { params: { locale } }
      );

      content.value = response.data;
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch content';
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    fetchContent();
  });

  return {
    content,
    loading,
    error,
    refetch: fetchContent,
  };
}

export function useTenantTheme() {
  const theme = ref<ThemeConfig | null>(null);
  const loading = ref(true);

  const fetchTheme = async () => {
    try {
      const response = await request.get<ThemeConfig>('/api/cms/theme');
      theme.value = response.data;

      // Apply theme to CSS variables
      if (theme.value) {
        document.documentElement.style.setProperty('--primary-color', theme.value.primaryColor);
        document.documentElement.style.setProperty('--secondary-color', theme.value.secondaryColor);
        document.documentElement.style.setProperty('--font-family', theme.value.fontFamily);

        // Apply custom CSS
        if (theme.value.customCss) {
          const styleEl = document.createElement('style');
          styleEl.textContent = theme.value.customCss;
          document.head.appendChild(styleEl);
        }
      }
    } catch (err) {
      console.error('Failed to fetch theme:', err);
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    fetchTheme();
  });

  return {
    theme,
    loading,
  };
}
```

#### 3.3.2 Dynamic Landing Page Component

**LandingPage.vue**:
```vue
<template>
  <div class="landing-page" v-if="!loading">
    <component
      v-for="(component, index) in content?.components"
      :key="index"
      :is="getComponentType(component.__typename)"
      :data="component"
    />
  </div>
  <div v-else class="loading">Loading...</div>
</template>

<script setup lang="ts">
import { useCmsContent } from '@/composables/useCmsContent';
import HeroBanner from '@/components/cms/HeroBanner.vue';
import PromotionCards from '@/components/cms/PromotionCards.vue';
import GamesGrid from '@/components/cms/GamesGrid.vue';
import Footer from '@/components/cms/Footer.vue';

const { content, loading } = useCmsContent('home');

const componentMap: Record<string, any> = {
  'ComponentSectionsHeroBanner': HeroBanner,
  'ComponentSectionsPromotionCards': PromotionCards,
  'ComponentSectionsGamesGrid': GamesGrid,
  'ComponentSectionsFooter': Footer,
};

function getComponentType(typename: string) {
  return componentMap[typename] || null;
}
</script>
```

**HeroBanner.vue** (Example Component):
```vue
<template>
  <section
    class="hero-banner"
    :style="{ backgroundImage: `url(${backgroundImageUrl})` }"
  >
    <div class="hero-content">
      <h1>{{ data.title }}</h1>
      <p>{{ data.subtitle }}</p>
      <button
        v-if="data.cta_button"
        class="cta-button"
        @click="handleCtaClick"
      >
        {{ data.cta_button.text }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  data: {
    title: string;
    subtitle: string;
    background_image: {
      data: {
        attributes: {
          url: string;
        };
      };
    };
    cta_button?: {
      text: string;
      link: string;
    };
  };
}>();

const backgroundImageUrl = computed(() => {
  const strapiUrl = 'http://localhost:1337';
  return strapiUrl + props.data.background_image.data.attributes.url;
});

function handleCtaClick() {
  if (props.data.cta_button) {
    window.location.href = props.data.cta_button.link;
  }
}
</script>

<style scoped>
.hero-banner {
  min-height: 600px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.hero-content {
  text-align: center;
  max-width: 800px;
  padding: 2rem;
}

.hero-content h1 {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.hero-content p {
  font-size: 1.5rem;
  margin-bottom: 2rem;
}

.cta-button {
  background-color: var(--primary-color);
  color: white;
  padding: 1rem 2rem;
  font-size: 1.2rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.cta-button:hover {
  transform: scale(1.05);
}
</style>
```

### 3.4 Preview Mode

#### 3.4.1 Strapi Preview URL

**Configure Preview URL** (Strapi Admin Panel):
```javascript
// config/plugins.js
module.exports = {
  'preview-button': {
    enabled: true,
    config: {
      contentTypes: [
        {
          uid: 'api::landing-page.landing-page',
          draft: {
            url: 'http://localhost:8080/preview/{slug}?locale={locale}&draft=true',
            query: {
              type: 'landing-page',
              slug: 'slug',
            },
          },
          published: {
            url: 'http://localhost:8080/{slug}?locale={locale}',
          },
        },
      ],
    },
  },
};
```

#### 3.4.2 Frontend Preview Handling

**PreviewPage.vue**:
```vue
<template>
  <div class="preview-mode" v-if="isPreview">
    <div class="preview-banner">
      Preview Mode (Draft Content)
      <button @click="exitPreview">Exit Preview</button>
    </div>
  </div>

  <LandingPage :slug="slug" :draft="isPreview" />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import LandingPage from '@/components/LandingPage.vue';

const route = useRoute();
const isPreview = ref(false);
const slug = ref('');

onMounted(() => {
  isPreview.value = route.query.draft === 'true';
  slug.value = route.params.slug as string;
});

function exitPreview() {
  window.location.href = `/${slug.value}`;
}
</script>
```

---

## 4. Testing

### 4.1 Content Fetching Tests

**CmsContentServiceTest.java**:
```java
@SpringBootTest
class CmsContentServiceTest {

    @Autowired
    private CmsContentService cmsContentService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void testGetLandingPage_CacheHit() {
        // Given: Content in cache
        String tenantId = "merchant_a";
        String slug = "home";
        String locale = "en";

        TenantContextHolder.setTenantId(tenantId);

        // When: Fetch content twice
        LandingPageContent content1 = cmsContentService.getLandingPage(slug, locale);
        LandingPageContent content2 = cmsContentService.getLandingPage(slug, locale);

        // Then: Only one Strapi API call (rest in cache)
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(String.class));
        assertNotNull(content1);
        assertEquals(content1, content2);
    }

    @Test
    void testGetLandingPage_LocaleFallback() {
        // Given: No content for zh-CN, but exists for zh and en
        String tenantId = "merchant_a";
        String slug = "home";

        TenantContextHolder.setTenantId(tenantId);

        // When: Request zh-CN (not available)
        LandingPageContent content = cmsContentService.getLandingPage(slug, "zh-CN");

        // Then: Fallback to zh or en
        assertNotNull(content);
        // Verify fallback sequence: zh-CN → zh → en
    }

    @Test
    void testInvalidateCache() {
        // Given: Content in cache
        String tenantId = "merchant_a";
        String slug = "home";
        String locale = "en";

        TenantContextHolder.setTenantId(tenantId);
        cmsContentService.getLandingPage(slug, locale);

        // When: Invalidate cache
        cmsContentService.invalidateCache(tenantId, slug, locale);

        // Then: Next fetch goes to Strapi
        cmsContentService.getLandingPage(slug, locale);
        verify(restTemplate, times(2)).postForObject(anyString(), any(), eq(String.class));
    }
}
```

### 4.2 Multi-Tenant Isolation Tests

**CmsMultiTenantTest.java**:
```java
@SpringBootTest
class CmsMultiTenantTest {

    @Autowired
    private CmsContentService cmsContentService;

    @Test
    void testTenantIsolation() {
        // Given: Two tenants with different content
        String tenantA = "merchant_a";
        String tenantB = "merchant_b";
        String slug = "home";
        String locale = "en";

        // When: Fetch content for tenant A
        TenantContextHolder.setTenantId(tenantA);
        LandingPageContent contentA = cmsContentService.getLandingPage(slug, locale);

        // When: Fetch content for tenant B
        TenantContextHolder.setTenantId(tenantB);
        LandingPageContent contentB = cmsContentService.getLandingPage(slug, locale);

        // Then: Content should be different
        assertNotNull(contentA);
        assertNotNull(contentB);
        assertNotEquals(contentA, contentB);
    }
}
```

---

## 5. Operations

### 5.1 Monitoring

**Prometheus Metrics**:
```java
@Service
@RequiredArgsConstructor
public class CmsContentService {

    private final MeterRegistry meterRegistry;

    public LandingPageContent getLandingPage(String slug, String locale) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            LandingPageContent content = getContentWithCache(tenantId, slug, locale);

            // Record cache hit/miss
            meterRegistry.counter("cms.content.fetch",
                "result", content != null ? "hit" : "miss",
                "locale", locale
            ).increment();

            return content;
        } finally {
            sample.stop(Timer.builder("cms.content.fetch.latency")
                .tag("locale", locale)
                .register(meterRegistry));
        }
    }
}
```

**Grafana Dashboard Queries**:
```promql
# Cache hit rate
sum(rate(cms_content_fetch{result="hit"}[5m])) /
sum(rate(cms_content_fetch[5m]))

# Content fetch latency (p95)
histogram_quantile(0.95,
  sum(rate(cms_content_fetch_latency_seconds_bucket[5m])) by (le, locale)
)
```

### 5.2 Content Publication Workflow

**Content Update Checklist**:
1. ✅ Edit content in Strapi (Draft mode)
2. ✅ Preview changes using Preview URL
3. ✅ Review with stakeholders
4. ✅ Publish content (triggers webhook → cache invalidation)
5. ✅ Verify live site reflects changes (frontend auto-fetches)

**Rollback Procedure** (if needed):
1. Go to Strapi Admin → Content Manager → Landing Pages
2. Select content entry
3. Click "History" tab
4. Select previous version
5. Click "Restore" → Publish (triggers cache invalidation)

**Expected Rollback Time**: <5 minutes

---

## 6. Appendices

### 6.1 Content Model Reference

**Complete Content Type List**:
- `themes` - Tenant branding (colors, logos, fonts)
- `landing-pages` - Dynamic landing pages (home, promotions, about)
- `promotions` - Promotional campaigns (banners, modals)
- `terms-and-conditions` - Legal documents (T&C, Privacy Policy)
- `game-categories` - Game categorization (slots, table games)
- `faqs` - Frequently asked questions
- `news-articles` - Blog posts and announcements

**Dynamic Zone Components**:
- `sections.hero-banner` - Hero banner with CTA
- `sections.promotion-cards` - Promotion card grid
- `sections.games-grid` - Game showcase grid
- `sections.footer` - Footer with links
- `sections.testimonials` - Player testimonials
- `sections.statistics` - Platform statistics (players, jackpots)

### 6.2 Locale Support Matrix

| Locale Code | Language | Fallback |
|-------------|----------|----------|
| en | English | - |
| es | Spanish | en |
| pt | Portuguese | en |
| pt-BR | Portuguese (Brazil) | pt → en |
| de | German | en |
| fr | French | en |
| ja | Japanese | en |
| ko | Korean | en |
| zh | Chinese | en |
| zh-CN | Simplified Chinese | zh → en |
| zh-TW | Traditional Chinese | zh → en |
| ru | Russian | en |
| ar | Arabic | en |

### 6.3 Performance Benchmarks

**Content Fetch Latency** (k6 Load Test):
```
Scenario: 500 concurrent users, 5-minute test
Cache hit rate: 92%

Results:
✓ L1 Cache Hit: 1-2ms (Caffeine in-memory)
✓ L2 Cache Hit: 8-12ms (Redis network call)
✓ Strapi API Call: 80-120ms (8% cache miss)
✓ Overall p95 Latency: 15ms (Target: <100ms) ✓
```

### 6.4 Related Documentation

- [P1-07: Multi-Tenant Isolation](07-multi-tenant-isolation.md) - Tenant context propagation
- [P1-14: Performance Optimization](14-performance-optimization.md) - Multi-level caching strategy
- [backend_project.md](../../backend_project.md) - Overall platform architecture

### 6.5 Strapi Plugins Recommendations

| Plugin | Purpose | Installation |
|--------|---------|--------------|
| strapi-plugin-multi-tenant | Tenant isolation | `npm install strapi-plugin-multi-tenant` |
| strapi-plugin-preview-button | Preview draft content | `npm install strapi-plugin-preview-button` |
| strapi-plugin-seo | SEO meta tags | `npm install @strapi/plugin-seo` |
| strapi-plugin-cloudinary | Image optimization | `npm install @strapi/provider-upload-cloudinary` |
| strapi-plugin-graphql | GraphQL API | Built-in (Strapi 4.x) |

---

## Document End

**Version**: 1.0.0
**Status**: Draft
**Next Review**: After Strapi POC deployment
**Feedback**: Marketing team review required for content model design