# CI/CD Pipeline Builder - Quick Reference

**Priority:** P2 - Productivity | **Status:** ✅ Production Ready

## One-Line Summary
Automate CI/CD pipeline setup for SmartAdmin with GitHub Actions or GitLab CI.

## When to Use
- Setting up automated testing pipeline
- Implementing continuous deployment
- Configuring quality gates for PRs
- Automating Docker image builds
- Need to standardize deployment process

## What It Generates
✅ GitHub Actions / GitLab CI configuration files
✅ Multi-stage pipeline (build → test → quality → deploy)
✅ Docker image build and push workflow
✅ Automated quality checks (Checkstyle, PMD, SpotBugs, ArchUnit)
✅ Environment-specific deployment configs (dev/staging/production)
✅ Rollback mechanisms and health checks

## Quick Example
```
User: "Set up GitHub Actions pipeline with quality gates"
→ Generates complete .github/workflows/ configuration in minutes
```

## Success Metric
**100% automated deployment with zero manual steps**

## Integration
- Works with: `quality-gate-orchestrator`, `db-migration-manager`
- Uses: GitHub Actions, GitLab CI, Docker, Gradle
- Compatible: All SmartAdmin modules

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0
**Created:** 2026-01-30
