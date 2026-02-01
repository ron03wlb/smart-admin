# SmartAdmin Skill Usage Metrics

**Purpose**: Track skill usage, execution time, and success rates for continuous improvement.

**Last Updated**: 2026-02-01
**Total Size**: 256KB
**Retention Policy**: 90 days (raw), indefinite (aggregated)

---

## Directory Structure

```
metrics/
├── raw/                    # Daily execution logs (JSON)
│   └── YYYY-MM-DD.json    # Raw execution records
├── aggregated/             # Daily summaries
│   └── YYYY-MM-DD-summary.json
├── reports/                # Weekly Markdown reports
│   └── YYYY-WXX-report.md
└── README.md               # This file
```

---

## Data Schema

### raw/YYYY-MM-DD.json

```json
{
  "timestamp": "2026-01-30T10:15:00Z",
  "skill_name": "smartadmin-crud-generator",
  "skill_priority": "P0",
  "duration_seconds": 1200,
  "success": true,
  "error": null,
  "user_id_hash": "sha256_hash",
  "phase": "all",
  "generated_files": 12
}
```

### aggregated/YYYY-MM-DD-summary.json

```json
{
  "date": "2026-01-30",
  "total_executions": 15,
  "success_rate": 0.93,
  "avg_duration_seconds": 850,
  "top_skills": [
    {
      "name": "smartadmin-crud-generator",
      "count": 5,
      "avg_duration": 1200,
      "success_rate": 1.0
    },
    {
      "name": "smartadmin-integration-test",
      "count": 3,
      "avg_duration": 450,
      "success_rate": 0.67
    }
  ],
  "by_priority": {
    "P0": 8,
    "P1": 4,
    "P2": 3
  }
}
```

---

## Monitoring Scripts

Located in `.claude/scripts/monitoring/`:

### log-skill-usage.js
Log skill execution events (called automatically by skill hooks).

**Usage:**
```bash
node .claude/scripts/monitoring/log-skill-usage.js \
  --skill=smartadmin-crud-generator \
  --duration=1200 \
  --success=true \
  --phase=all \
  --files=12
```

### aggregate-metrics.js
Generate daily summary from raw logs.

**Usage:**
```bash
# Aggregate yesterday's data
node .claude/scripts/monitoring/aggregate-metrics.js --date=$(date -d yesterday +%Y-%m-%d)

# Aggregate specific date
node .claude/scripts/monitoring/aggregate-metrics.js --date=2026-01-30
```

### generate-weekly-report.js
Create human-readable Markdown report.

**Usage:**
```bash
# Generate report for current week
node .claude/scripts/monitoring/generate-weekly-report.js --week=$(date +%Y-W%V)

# Generate report for specific week
node .claude/scripts/monitoring/generate-weekly-report.js --week=2026-W05
```

---

## Weekly Report Example

```markdown
# SmartAdmin Skills Usage Report - Week 2026-W05

**Period**: 2026-01-27 to 2026-02-02
**Total Executions**: 87
**Success Rate**: 91.95%

## Top 5 Most Used Skills

1. **smartadmin-crud-generator** (23 executions, 100% success)
   - Avg duration: 1,150s
   - Most common phase: all-phases

2. **smartadmin-integration-test** (15 executions, 86.67% success)
   - Avg duration: 420s
   - Common issue: Testcontainers timeout

3. **archunit-test-generator** (12 executions, 100% success)
   - Avg duration: 180s

## Priority Distribution

- P0 (Foundation): 48 executions (55%)
- P1 (Extended): 26 executions (30%)
- P2 (Productivity): 13 executions (15%)

## Recommendations

- **smartadmin-integration-test**: Investigate timeout issues (13.33% failure rate)
- **Performance**: Consider caching for crud-generator (avg 19 min)
```

---

## Privacy & Compliance

### Data Anonymization
- **User IDs**: SHA-256 hashed (irreversible)
- **File Paths**: Only project-relative paths stored
- **No PII**: No personal information collected

### GDPR Compliance
- **Right to Erasure**: Raw logs auto-deleted after 90 days
- **Data Minimization**: Only essential metrics collected
- **Transparency**: Full schema documented above

### Access Control
- Metrics files stored locally (no external transmission)
- Only aggregated data used for reporting
- Individual execution logs retained for debugging only

---

## Automation

### Daily Aggregation (Cron Job)
```bash
# Add to crontab
0 1 * * * cd /path/to/smart-admin && node .claude/scripts/monitoring/aggregate-metrics.js --date=$(date -d yesterday +\%Y-\%m-\%d)
```

### Weekly Report (Cron Job)
```bash
# Every Monday at 9 AM
0 9 * * 1 cd /path/to/smart-admin && node .claude/scripts/monitoring/generate-weekly-report.js --week=$(date +\%Y-W\%V)
```

---

## Troubleshooting

### Issue: Metrics not being logged

**Check:**
1. Verify hook is enabled in `.claude/settings.local.json`
2. Check log-skill-usage.js has execute permission
3. Ensure `metrics/raw/` directory exists

**Fix:**
```bash
mkdir -p .claude/metrics/raw
chmod +x .claude/scripts/monitoring/log-skill-usage.js
```

### Issue: Aggregation script fails

**Common causes:**
- Invalid JSON in raw logs
- Missing date parameter
- Node.js version < 18

**Fix:**
```bash
# Validate JSON
jq empty .claude/metrics/raw/2026-01-30.json

# Check Node.js version
node --version  # Should be >= 18
```

---

## Contributing

To add new metrics fields:

1. Update data schema in this README
2. Modify `log-skill-usage.js` to capture new fields
3. Update `aggregate-metrics.js` to process new fields
4. Regenerate weekly report template

---

**Maintained by**: SmartAdmin AI Documentation System
**Version**: 1.0.0
**Last Review**: 2026-02-01
