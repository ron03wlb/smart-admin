# Scheduled Task Manager - Quick Reference

**Sprint:** 3 (Weeks 9-11) | **Priority:** P1 - Roadmap #3 | **Status:** 🚧 Skeleton

## One-Line Summary
Generate XXL-Job/Snail-Job integration for distributed scheduled tasks and batch processing.

## When to Use
- Scheduled task automation
- Batch processing
- Periodic data cleanup
- Scheduled reports
- Job dependency chains

## What It Generates
✅ XXL-Job/Snail-Job executor config
✅ Job handler code (simple, sharding, broadcast)
✅ Cron expression builder
✅ Job parameter configuration
✅ Monitoring and alerting
✅ Retry and failure handling
✅ Job dependency management

## Quick Example
```
User: "Create daily user statistics job at 2 AM"
→ Generates scheduled job with retry logic in minutes
```

## Success Metric
**Job execution reliability > 99.9%**

## Integration
- Works with: `report-generator-skill`, `db-migration-manager`
- Uses: SmartAdmin logging
- Compatible: All modules needing automation

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
