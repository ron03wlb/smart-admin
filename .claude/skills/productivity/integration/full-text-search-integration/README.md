# Full-Text Search Integration - Quick Reference

**Sprint:** 4 (Weeks 12-15) | **Priority:** P1 - Roadmap #2 | **Status:** 🚧 Skeleton

## One-Line Summary
Generate Elasticsearch integration for advanced search, analytics, and log aggregation.

## When to Use
- Advanced search needed (fuzzy, autocomplete)
- Business analytics and aggregations
- Log analysis (ELK stack)
- Geo-spatial search
- Full-text search requirements

## What It Generates
✅ ES document mapping from entities
✅ Search API (full-text, fuzzy, phrase, autocomplete)
✅ Aggregation queries (bucket, metrics, pipeline)
✅ Index lifecycle management
✅ Sync strategies (dual-write, CDC, Logstash)
✅ Search highlighting
✅ Geo-spatial search

## Quick Example
```
User: "Add Elasticsearch search to ProductEntity with autocomplete"
→ Generates ES integration with CDC sync in one command
```

## Success Metric
**Search 100x faster than MyBatis LIKE queries**

## Integration
- Works with: `smartadmin-crud-generator`, `apm-integration`
- Uses: MyBatis Plus entities
- Compatible: All searchable modules

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
