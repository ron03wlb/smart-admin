# APM Integration Skill - Quick Reference

**Sprint:** 1 (Weeks 1-4) | **Priority:** P0 - Pain Point #1 | **Status:** 🚧 Skeleton

## One-Line Summary
Generate complete APM (Skywalking, Micrometer, Grafana) integration for production monitoring and debugging.

## When to Use
- Setting up production monitoring
- Debugging performance issues
- Implementing distributed tracing
- Creating custom business metrics
- Need to reduce debugging time

## What It Generates
✅ Skywalking agent configuration
✅ Micrometer custom metrics
✅ Grafana dashboards (JVM, HTTP, DB, Redis)
✅ Alert rules (latency, error rate, throughput)
✅ Distributed tracing setup
✅ Log correlation (MDC + TraceId)

## Quick Example
```
User: "Add APM monitoring to OrderService"
→ Generates complete monitoring stack in minutes
```

## Success Metric
**70% reduction in production debugging time**

## Integration
- Works with: `java-performance-pro`, SmartAdmin logging
- Uses: Spring Boot Actuator, Prometheus
- Compatible: All SmartAdmin modules

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
