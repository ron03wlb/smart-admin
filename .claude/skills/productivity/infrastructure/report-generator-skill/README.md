# Report Generator Skill - Quick Reference

**Sprint:** 2 (Weeks 5-8) | **Priority:** P0 - Pain Point #3 | **Status:** 🚧 Skeleton

## One-Line Summary
Generate Excel/PDF/CSV export functionality with templates, charts, and scheduled reports.

## When to Use
- Implementing export features
- Creating business reports
- Large dataset exports
- Scheduled report generation
- Document generation (invoices, certificates)

## What It Generates
✅ Excel export (POI, EasyExcel) with templates
✅ PDF generation (iText, Flying Saucer)
✅ CSV streaming for large datasets
✅ Scheduled report jobs (XXL-Job)
✅ Async export with download links
✅ Chart generation (bar, line, pie)

## Quick Example
```
User: "Add Excel export to UserQueryForm with charts"
→ Generates complete export feature in 15 minutes
```

## Success Metric
**Export development time: 4 hours → 15 minutes**

## Integration
- Works with: `smartadmin-crud-generator`, `scheduled-task-manager`
- Uses: SmartAdmin `FileService`
- Compatible: All list queries

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
