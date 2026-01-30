---
name: apm-integration-skill
description: [P2 - Productivity] Generate APM (Application Performance Monitoring) integration with Skywalking, Micrometer, Grafana dashboards, and custom metrics for SmartAdmin applications. Use when setting up production monitoring, debugging performance issues, implementing distributed tracing, or creating custom business metrics. Triggers when user mentions "monitoring", "APM", "performance tracking", "Grafana dashboard", "metrics", "distributed tracing", "Skywalking", or "production observability".
---

# APM Integration Skill

**Priority:** P0 - Pain Point #1
**Sprint:** 1 (Weeks 1-4)
**Status:** 🚧 Skeleton Created - Detailed Content Pending

## Purpose

Eliminate production debugging pain by generating complete APM (Application Performance Monitoring) integration for SmartAdmin applications. Reduces debugging time by 70% through systematic observability.

## Problem Statement

**User Pain Point:** "监控/调试生产问题困难" (Monitoring/Debugging production issues is difficult)

**Current Issues:**
- No systematic performance monitoring
- Hard to trace issues across distributed calls
- Missing custom business metrics
- Manual dashboard creation is time-consuming
- Alert configuration is complex

## Solution Overview

This skill generates:
- ✅ Skywalking Java agent configuration
- ✅ Micrometer custom metrics (counters, gauges, timers, histograms)
- ✅ Grafana dashboard templates (JVM, HTTP, Database, Redis, Business Metrics)
- ✅ Alert rule generation (latency P95/P99, error rate, throughput)
- ✅ Distributed tracing context propagation (TraceId, SpanId)
- ✅ Log correlation integration (MDC with TraceId)

## Quick Start

**Most common usage:**
```
User: "Add APM monitoring to UserService with custom metrics for login events"
```

You will:
1. Configure Skywalking agent
2. Generate Micrometer custom metrics
3. Create Grafana dashboards
4. Set up alert rules
5. Enable distributed tracing
6. Integrate log correlation

## Scope

### Included
- Skywalking Java agent setup (auto-instrumentation)
- Micrometer integration (Spring Boot Actuator + Prometheus)
- Custom business metrics generation
- Grafana dashboard JSON templates (JVM, HTTP, DB, Redis, Custom)
- Alert rules (latency, error rate, throughput)
- Distributed tracing (TraceId propagation)
- MDC log correlation

### Not Included
- Infrastructure deployment (Skywalking OAP server, Grafana server)
- Alert notification channels (Slack, email, PagerDuty)
- Custom visualization plugins

## Integration Points

- Works with `java-performance-pro` for performance profiling
- Integrates with SmartAdmin logging infrastructure
- Uses Spring Boot Actuator for metrics endpoints
- Compatible with `smartadmin-crud-generator` (adds metrics to generated code)

## Success Criteria

- ✅ Production debugging time reduced by 70%
- ✅ P95/P99 latency tracking for all endpoints
- ✅ Error rate alerts within 1 minute
- ✅ Distributed tracing working across all layers
- ✅ Custom business metrics dashboards operational

## Next Steps

**Detailed documentation pending Sprint 1 completion.**

See `references/` directory for:
- Skywalking configuration patterns
- Micrometer custom metrics examples
- Grafana dashboard templates
- Alert rule configurations
- Distributed tracing patterns

---

**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
**Sprint:** 1 (Weeks 1-4)
**Status:** Awaiting detailed implementation
