# Database Migration Manager - Quick Reference

**Priority:** P2 - Productivity | **Status:** ✅ Production Ready

## One-Line Summary
Generate and manage Flyway/Liquibase database migrations for SmartAdmin projects.

## When to Use
- Adding new tables or columns
- Modifying database schema
- Managing multi-environment database changes
- Need versioned database evolution
- Rolling back failed migrations

## What It Generates
✅ Flyway migration scripts (V{version}__{description}.sql)
✅ Liquibase changesets (XML/YAML format)
✅ Rollback scripts for each migration
✅ Migration validation and checksum verification
✅ Environment-specific migration configs
✅ Integration with Spring Boot application startup

## Quick Example
```
User: "Create migration to add vip_level column to t_player table"
→ Generates versioned migration script with rollback support
```

## Success Metric
**Zero manual SQL execution in production environments**

## Integration
- Works with: `smartadmin-crud-generator`, `cicd-pipeline-builder`
- Uses: Flyway, Liquibase, Spring Boot
- Compatible: MySQL, PostgreSQL, H2

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0
**Created:** 2026-01-30
