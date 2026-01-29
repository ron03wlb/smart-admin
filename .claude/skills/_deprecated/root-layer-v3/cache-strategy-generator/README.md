# Cache Strategy Generator - Quick Reference

**Sprint:** 1 (Weeks 1-4) | **Priority:** P0 - Pain Point #2 | **Status:** 🚧 Skeleton

## One-Line Summary
Generate multi-level caching strategies (Caffeine + Redis) with intelligent invalidation and monitoring.

## When to Use
- Slow query performance issues
- Need systematic caching strategy
- Implementing cache invalidation
- Preventing cache stampede
- Optimizing hot data access

## What It Generates
✅ L1 (Caffeine) + L2 (Redis) cache config
✅ Cache patterns (aside, write-through, write-behind)
✅ Invalidation strategies (TTL, event-driven, tag groups)
✅ Cache warming on startup
✅ Redisson distributed locks
✅ Cache hit rate monitoring

## Quick Example
```
User: "Add two-level caching to ProductService.getById"
→ Generates L1+L2 cache with 5-min TTL in minutes
```

## Success Metric
**80% reduction in slow query incidents**

## Integration
- Works with: `smartadmin-crud-generator`, `java-performance-pro`
- Uses: `foundation.cache`, `foundation.redis-lock`
- Compatible: All CRUD modules

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
