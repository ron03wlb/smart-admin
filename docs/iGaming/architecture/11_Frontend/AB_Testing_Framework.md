# A/B Testing Framework Architecture

> **Business Requirements**: [Frontend UX Requirements](../../requirements/11_Frontend_Experience/Frontend_UX_Requirements.md)
> **Canonical Source**: [source-archive/11_Frontend_CMS/11-06](../../source-archive/11_Frontend_CMS/11-06_AB_Testing_Framework.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Data Engineers, Frontend Developers

---

## 1. System Architecture

```text
+-----------------------------------------------------+
|  Frontend (React/Vue)                                |
|  - Experiment Config UI                              |
|  - Real-time Data Dashboard                          |
+----------------+------------------------------------+
                 |
+----------------v------------------------------------+
|  AB Testing Service (Node.js / Go)                   |
|  - Traffic Allocation Algorithm                      |
|  - Experiment Config Management                      |
|  - Statistical Computation Engine                    |
+----------------+------------------------------------+
                 |
+----------------v------------------------------------+
|  Data Layer                                          |
|  - Redis (Experiment Config Cache)                   |
|  - PostgreSQL (Experiment Definitions, Results)      |
|  - ClickHouse (Event Stream, Metric Aggregation)     |
+-----------------------------------------------------+
```

**Platform Recommendation**: GrowthBook (open-source, self-hosted, full-featured)

## 2. Hash-Based Traffic Allocation

```javascript
function assignVariant(playerId, experimentId, variants) {
    const hash = murmur3(`${experimentId}:${playerId}`);
    const bucket = hash % 100;

    let cumulative = 0;
    for (const variant of variants) {
        cumulative += variant.trafficPercentage;
        if (bucket < cumulative) {
            return variant.name;
        }
    }
    return 'control';
}

// Example
const variant = assignVariant('12345', 'homepage-redesign', [
    { name: 'control', trafficPercentage: 50 },
    { name: 'variant_a', trafficPercentage: 25 },
    { name: 'variant_b', trafficPercentage: 25 }
]);
```

## 3. Experiment Configuration

```yaml
experiment:
  id: homepage-redesign
  name: "Homepage Redesign A/B Test"
  status: running
  start_date: "2026-01-28T00:00:00Z"
  end_date: "2026-02-28T00:00:00Z"

  variants:
    - name: control
      description: "Original homepage"
      traffic: 50%
    - name: variant_a
      description: "New design (large banner)"
      traffic: 50%

  primary_metric:
    name: first_deposit_conversion_rate
    goal: maximize
    min_sample_size: 10000
    min_detectable_effect: 5%

  secondary_metrics:
    - click_through_rate
    - bounce_rate

  guardrail_metrics:
    - error_rate
    - page_load_time
```

## 4. Event Tracking

```javascript
// Experiment exposure event
analytics.track('Experiment Viewed', {
    experiment_id: 'homepage-redesign',
    variant: 'variant_a',
    player_id: '12345'
});

// Conversion event
analytics.track('First Deposit Completed', {
    experiment_id: 'homepage-redesign',
    variant: 'variant_a',
    player_id: '12345',
    deposit_amount: 100
});
```

## 5. Statistical Significance

### Sample Size Formula

```
n = 2 * (Z_alpha/2 + Z_beta)^2 * sigma^2 / delta^2

Where:
- Z_alpha/2: Significance level (alpha=0.05 -> Z=1.96)
- Z_beta: Statistical power (beta=0.8 -> Z=0.84)
- sigma: Standard deviation
- delta: Minimum Detectable Effect (MDE)
```

### Auto-Stop Rules

```
Rule 1: Statistical significance reached
  IF p_value < 0.05 AND sample_size >= min_sample_size
  THEN stop_experiment()

Rule 2: Sample size cap
  IF sample_size >= max_sample_size
  THEN stop_experiment()

Rule 3: Guardrail metric triggered
  IF error_rate > baseline_error_rate * 1.5
  THEN stop_experiment() AND rollback()
```

## 6. Experiment Lifecycle

```
1. Hypothesis -> 2. Design (variants, metrics, sample size)
-> 3. Development (frontend variants, event tracking)
-> 4. Launch (traffic allocation, monitoring)
-> 5. Data Collection (await significance, guardrails)
-> 6. Analysis (statistical tests, business decision)
-> 7. Winner Rollout (100% traffic, decommission control)
```
