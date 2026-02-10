# Infrastructure Implementation

> **Business Requirements**: N/A — Pure technical infrastructure document
> **Canonical Source**: [00-16_Infrastructure_Implementation.md](../../source-archive/00_Foundation/guides/00-16_Infrastructure_Implementation.md)
> **Audience**: Architects, DevOps Engineers, Backend Engineers
> **Last Synced**: 2026-02-09

---

## 1. Overview

This document covers the technical implementation of the iGaming platform infrastructure, including API gateway design, Blue-Green deployment strategy, and API rate limiting. All implementations follow SmartAdmin architectural patterns and target production-grade reliability.

**Note**: This document is purely technical with no separate requirements counterpart. All infrastructure decisions are driven by non-functional requirements (availability, scalability, performance).

---

## 2. API Gateway Design

**Status**: PLANNED (Phase 5+)
**Modules**: 12_Technical_Operations, 10_Platform_Management

### Implementation Goal

Configure Spring Cloud Gateway with dynamic routing, filter chains (authentication, rate limiting), and circuit breaker integration via Resilience4j.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S2 Route Config | Dynamic routing |
| 2 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S3 Filter Chain | Auth, rate limiting |
| 3 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S4 Circuit Breaker | Resilience4j |

### Gateway Architecture

```
Client Request
    ↓
[Spring Cloud Gateway]
    ↓
Route Predicate Matching → Route Not Found → 404
    ↓ (matched)
Filter Chain Execution
    ├── 1. Authentication Filter (Sa-Token validation)
    ├── 2. Rate Limiting Filter (Redis-based token bucket)
    ├── 3. Request Logging Filter (audit trail)
    └── 4. Circuit Breaker Filter (Resilience4j)
    ↓
Backend Service (load balanced)
    ↓
Response → Client
```

### SmartAdmin Layer Mapping

| Component | Technology | Configuration |
|-----------|-----------|---------------|
| Gateway Framework | Spring Cloud Gateway | Reactive routing engine |
| Auth Filter | Sa-Token | Token validation on every request |
| Rate Limiter | Redisson | Token bucket algorithm with Redis backend |
| Circuit Breaker | Resilience4j | Configurable failure thresholds |
| Load Balancer | Spring Cloud LoadBalancer | Round-robin with health checks |

### Route Configuration Pattern

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: player-service
          uri: lb://player-service
          predicates:
            - Path=/api/player/**
          filters:
            - name: CircuitBreaker
              args:
                name: player-cb
                fallbackUri: forward:/fallback/player
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
```

### Filter Chain Order

```java
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    @Override
    public int getOrder() {
        return -100; // Authentication FIRST
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Sa-Token validation
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StpUtil.isLogin(token)) {
            return unauthorized(exchange);
        }
        return chain.filter(exchange);
    }
}
```

### Verification Checklist

- [ ] Route rules are correct
- [ ] Filter chain operates normally
- [ ] Circuit breaker is effective
- [ ] Load balancing is accurate

### Common Pitfalls

1. **Route Conflicts**: Multiple routes match the same request -- use specific path predicates with ordering
2. **Filter Ordering Error**: Auth executed after rate limiting -- ensure auth filter has lowest order value
3. **Circuit Breaker Thresholds**: Too sensitive or too lenient -- tune based on actual error rates in staging

---

## 3. Blue-Green Deployment

**Status**: PLANNED (Phase 5+)
**Modules**: 12_Technical_Operations

### Implementation Goal

Configure Kubernetes-based Blue-Green deployment with Istio/Nginx traffic switching, rollback strategy, and smoke test validation.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) | S3 Deployment Strategy | Blue-Green |
| 2 | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) | S4 Traffic Management | Istio/Nginx |
| 3 | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) | S5 Monitoring Validation | Smoke Test |

### Deployment Architecture

```
                    ┌──────────────┐
                    │   Ingress    │
                    │  Controller  │
                    └──────┬───────┘
                           │
                    ┌──────┴───────┐
                    │ Traffic Split │ (Istio VirtualService)
                    └──┬────────┬──┘
                       │        │
              ┌────────┴──┐  ┌──┴────────┐
              │   Blue    │  │   Green   │
              │ (current) │  │  (new)    │
              │ v1.2.0    │  │ v1.3.0    │
              └───────────┘  └───────────┘
```

### Deployment Pipeline

```yaml
# Kubernetes Blue-Green Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smartadmin-green
  labels:
    app: smartadmin
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: smartadmin
      version: green
  template:
    spec:
      containers:
        - name: smartadmin
          image: smartadmin:v1.3.0
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 1024
            initialDelaySeconds: 30
            periodSeconds: 10
```

### Traffic Switching with Istio

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: smartadmin-vs
spec:
  hosts:
    - smartadmin.example.com
  http:
    - route:
        - destination:
            host: smartadmin
            subset: blue
          weight: 0      # Old version
        - destination:
            host: smartadmin
            subset: green
          weight: 100    # New version (after validation)
```

### Smoke Test Strategy

```bash
#!/bin/bash
# Post-deployment smoke test
ENDPOINTS=(
  "/actuator/health"
  "/api/system/health"
  "/api/system/version"
)

for endpoint in "${ENDPOINTS[@]}"; do
  status=$(curl -s -o /dev/null -w "%{http_code}" "http://green-service:1024${endpoint}")
  if [ "$status" != "200" ]; then
    echo "FAIL: ${endpoint} returned ${status}"
    exit 1  # Trigger rollback
  fi
done
echo "All smoke tests passed"
```

### Rollback Strategy

| Trigger | Action | Duration |
|---------|--------|----------|
| Smoke test failure | Automatic rollback to Blue | < 30 seconds |
| Error rate spike (>5%) | Automatic traffic shift to Blue | < 1 minute |
| Manual rollback request | Istio weight shift to Blue | < 1 minute |

### Verification Checklist

- [ ] Blue/Green environments are independent
- [ ] Traffic switching is smooth
- [ ] Rollback mechanism is effective
- [ ] Monitoring alerts are operational

### Common Pitfalls

1. **Database Migration**: Schema version mismatch between Blue/Green -- use backward-compatible migrations only
2. **State Leakage**: Session/cache not synchronized -- use external Redis for shared state
3. **Rollback Failure**: DNS cache prevents complete traffic switch -- set low TTL values, use Istio for instant switching

---

## 4. API Rate Limiting

**Status**: PLANNED (Phase 5+)
**Modules**: 12_Technical_Operations, Foundation modules

### Implementation Goal

Implement token bucket rate limiting with Redis backend, supporting multi-layer limiting (global, per-tenant, per-user) and proper 429 error responses.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [09-03-01 Design Principles](../../source-archive/09_Technical_Infrastructure/09-03-01_Design_Principles.md) | S6 Rate Limiting | Token bucket / leaky bucket |
| 2 | Foundation Redis Limiter | - | Redisson rate limiter |
| 3 | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) | S3.4 Rate Limiting Filter | Gateway rate limiting |

### Rate Limiting Architecture

```
Request → Gateway Rate Limiter (global)
              ↓
         Tenant Rate Limiter (per-tenant)
              ↓
         User Rate Limiter (per-user)
              ↓
         Endpoint Rate Limiter (per-API)
              ↓
         Backend Service
```

### Multi-Layer Rate Limiting

| Layer | Scope | Implementation | Default Limits |
|-------|-------|---------------|----------------|
| Global | All requests | Gateway filter | 10,000 req/s |
| Tenant | Per operator | Redisson RRateLimiter | 1,000 req/s |
| User | Per player | Redisson RRateLimiter | 100 req/s |
| Endpoint | Per API path | Spring annotation | Varies by endpoint |

### Token Bucket Implementation

```java
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {
    private final RedissonClient redissonClient;

    public boolean tryAcquire(String key, long rate, long rateInterval,
                               RateIntervalUnit unit) {
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        limiter.trySetRate(RateType.OVERALL, rate, rateInterval, unit);
        return limiter.tryAcquire(1);
    }
}
```

### Endpoint-Level Rate Limiting

```java
// Custom annotation for endpoint-specific rate limiting
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int permits() default 100;
    int period() default 60;  // seconds
    String key() default "";  // defaults to endpoint path
}

// Controller usage
@GetMapping("/api/player/profile")
@RateLimit(permits = 50, period = 60)
public ResponseDTO<PlayerVO> getProfile() {
    // ...
}
```

### 429 Error Response

```java
// Standard rate limit exceeded response
public class RateLimitFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientKey = extractClientKey(exchange);
        if (!rateLimiter.tryAcquire(clientKey, 100, 1, RateIntervalUnit.MINUTES)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
```

### Verification Checklist

- [ ] Rate limiting thresholds are correct
- [ ] Redis rate limiter operates normally
- [ ] Multi-layer rate limiting takes effect
- [ ] 429 error responses are returned correctly

### Common Pitfalls

1. **Granularity Too Coarse**: Global-only limiting is too aggressive -- implement multi-layer limiting
2. **Redis Single Point**: Rate limiter not using Redis Cluster -- configure Redisson with cluster mode
3. **Sliding Window**: Window not implemented correctly -- use Redisson's built-in RRateLimiter which handles this

---

## 5. Reference Documents

| Area | Document |
|------|----------|
| Gateway Core | [09-02-01 Gateway Core](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) |
| Deployment | [09-01 Deployment](../../source-archive/09_Technical_Infrastructure/09-01_Deployment.md) |
| API Design Principles | [09-03-01 Design Principles](../../source-archive/09_Technical_Infrastructure/09-03-01_Design_Principles.md) |
| Rate Limiting | [09-02-02 Rate Limiting](../../source-archive/09_Technical_Infrastructure/09-02-02_Rate_Limiting.md) |

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Source Version**: 4.0.0
