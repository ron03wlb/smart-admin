# SmartAdmin Performance Suite

> One-stop performance optimization: Diagnose --> Optimize --> Monitor unified workflow

For complete reference, see [SKILL.md](SKILL.md).

---

## Quick Start

```bash
# Scenario: Employee listing endpoint is slow (5+ seconds)
User: "The employee listing endpoint is slow, taking 5+ seconds"

# AI auto-detects and runs the full workflow
/performance /api/employees/list --workflow

# Auto-executes:
# 1. Diagnose (10 min): N+1 query detection, JVM analysis, slow SQL
# 2. Optimize (12 min): Multi-level caching (Caffeine L1 + Redis L2)
# 3. Monitor  (8 min):  Skywalking APM, Micrometer metrics, Grafana dashboards
# Total: ~30 min (vs 60 min with 3 separate skills)
```

### Expected Output

- **Diagnosis report** (Markdown): N+1 queries, JVM heap usage, slow SQL with EXPLAIN ANALYZE
- **Optimization code** (Java): Cache annotations in Manager layer, configuration YAML
- **Monitoring config**: Skywalking agent config, Grafana dashboard JSON, Prometheus alert rules

---

## Execution Modes

| Mode | Command | Time | Use When |
|------|---------|------|----------|
| **Workflow** (recommended) | `--workflow` | ~30 min | Full optimization project (diagnose + optimize + monitor) |
| **Diagnose** | `--mode=diagnose` | ~10 min | Find root cause of known performance issue |
| **Optimize** | `--mode=optimize` | ~12 min | Implement caching after diagnosis |
| **Monitor** | `--mode=monitor` | ~8 min | Set up observability after optimization |

### Mode 1: `--workflow` (Recommended)

Full pipeline: Diagnose --> Optimize --> Monitor with shared context and automated handoffs.

```bash
/performance /api/orders/list --workflow
```

### Mode 2: `--mode=diagnose`

Identify performance bottlenecks: N+1 queries (P6Spy), JVM analysis (JMX), slow SQL (EXPLAIN ANALYZE), CPU hotspots (Async-profiler).

```bash
/performance /api/employees/list --mode=diagnose
```

### Mode 3: `--mode=optimize`

Generate multi-level cache code (Caffeine L1 + Redis L2), cache consistency strategies, and N+1 query fixes.

```bash
/performance ProductService.getById --mode=optimize
```

### Mode 4: `--mode=monitor`

Configure Skywalking APM (distributed tracing), Micrometer metrics, Grafana dashboards, and alert rules.

```bash
/performance OrderService --mode=monitor
```

**Generated files**:
```
monitoring/
├── skywalking/agent.config
├── grafana/
│   ├── *-dashboard.json
│   └── alert-rules.yml
└── prometheus/prometheus.yml
```

---

## When to Use / When NOT to Use

**Use when**:
- Production endpoint response time > 500ms
- New project needs performance baseline and monitoring
- Performance issue triage (unknown root cause)
- Cache architecture design for hot data
- Observability setup (APM + metrics + dashboards)

**Do NOT use when**:
- Problem is functional (bugs, business logic errors)
- Bottleneck is in external services (third-party APIs, DB hardware)
- Mature monitoring stack already deployed
- Ultra-low-latency requirements (sub-ms SLA needs deeper tuning)

---

## Composed Skills

This suite integrates three previously standalone skills:

| Original Skill | Integrated As | Deprecated Command |
|---------------|---------------|-------------------|
| `java-performance-pro` | `--mode=diagnose` | `/java-performance-pro` |
| `cache-strategy-generator` | `--mode=optimize` | `/cache-strategy` |
| `apm-integration` | `--mode=monitor` | `/apm-integration` |

Deprecated commands still work with migration warnings during the soft-deprecation period.

**Efficiency gain**: 50% time reduction (60 min separately --> 30 min unified) through shared context, automated handoffs, and parallel execution.

---

## Combining Modes Manually

If you need only a subset of the workflow:

```bash
/performance /api/orders --mode=diagnose   # Step 1
/performance /api/orders --mode=optimize   # Step 2 (skip monitor)
```

---

## Related Resources

- **[SKILL.md](SKILL.md)** - Full technical specification and implementation details
- **Mode docs**: [Diagnose](modes/mode-1-diagnose.md) | [Optimize](modes/mode-2-optimize.md) | [Monitor](modes/mode-3-monitor.md) | [Workflow](modes/mode-workflow.md)
- **Composed skills**: [java-performance-pro](../../analysis/java-performance-pro/SKILL.md) | [cache-strategy-generator](../../integration/cache-strategy-generator/SKILL.md) | [apm-integration](../../devops/apm-integration/SKILL.md)
- **External**: [Skywalking](https://skywalking.apache.org/) | [Micrometer](https://micrometer.io/) | [Grafana](https://grafana.com/docs/) | [Caffeine](https://github.com/ben-manes/caffeine)

---

**Skill Version**: 2.0.0 | **Status**: Stable | **Compatible with**: SmartAdmin v4.0.0+

**Last Updated**: 2026-01-30
