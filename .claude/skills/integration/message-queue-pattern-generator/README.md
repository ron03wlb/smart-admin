# Message Queue Pattern Generator - Quick Reference

**Sprint:** 2 (Weeks 5-8) | **Priority:** P0 - Pain Point #4 | **Status:** 🚧 Skeleton

## One-Line Summary
Generate Kafka/RocketMQ event-driven patterns (producer/consumer, CQRS, Saga, idempotency).

## When to Use
- Implementing event-driven architecture
- Distributed transaction handling
- Asynchronous processing
- Event sourcing patterns
- Decoupling microservices

## What It Generates
✅ Kafka producer/consumer code
✅ Event sourcing patterns
✅ CQRS implementation
✅ Saga patterns (choreography, orchestration)
✅ Dead letter queue handling
✅ Idempotency with Redis
✅ Schema registry integration

## Quick Example
```
User: "Generate Kafka producer for OrderCreated events"
→ Generates event-driven code with idempotency in minutes
```

## Success Metric
**3+ SmartAdmin modules using event-driven patterns**

## Integration
- Works with: `liteflow-rule-builder`, `smartadmin-crud-generator`
- Uses: `foundation.mq.kafka`
- Compatible: All modules needing async processing

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
