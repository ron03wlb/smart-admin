# Kafka Documentation

Enterprise-grade Apache Kafka integration for SmartAdmin framework.

## Overview

SmartAdmin's Kafka module provides production-ready integration with Apache Kafka, featuring:

- ✅ **Batch Processing** - Efficient batch message production and consumption
- ✅ **Dead Letter Queue (DLQ)** - Automatic error handling with retry mechanism
- ✅ **Graceful Degradation** - Automatic fallback when batch processing fails
- ✅ **Comprehensive Monitoring** - Built-in metrics, health checks, and alerts
- ✅ **Transaction Support** - ACID guarantees for critical operations
- ✅ **Idempotency** - Prevent duplicate message processing
- ✅ **Zero-Downtime Deployment** - Graceful shutdown and consumer rebalancing

---

## Quick Links

### 🚀 Getting Started (5 minutes)

- **[Quick Start](/kafka/getting-started/quick-start)** - Send your first message in 5 minutes
- **[Quick Reference](/kafka/getting-started/quick-reference)** - API cheat sheet and common patterns
- **[Hello World Example](/kafka/getting-started/hello-world)** - Complete runnable example

### 📐 Architecture & Design

- **[System Architecture](/kafka/architecture/overview)** - Component overview and design principles
- **[Module Structure](/kafka/architecture/module-structure)** - Code organization and package design
- **[Message Flow](/kafka/architecture/message-flow)** - How messages flow through the system
- **[Batch Processing](/kafka/architecture/batch-processing)** - Batch processing architecture
- **[Dead Letter Queue](/kafka/architecture/dead-letter-queue)** - DLQ design and retry mechanism

### 📖 User Guides

- **[Producer Guide](/kafka/guides/producer-guide)** - How to send messages
- **[Consumer Guide](/kafka/guides/consumer-guide)** - How to consume messages
- **[Batch Operations](/kafka/guides/batch-operations)** - Batch send/consume patterns
- **[Error Handling](/kafka/guides/error-handling)** - Error handling strategies
- **[Configuration](/kafka/guides/configuration)** - Configuration reference
- **[Best Practices](/kafka/guides/best-practices)** - Production best practices

### 🔧 Operations

- **[Deployment](/kafka/operations/deployment)** - Deploy Kafka cluster and application
- **[Monitoring](/kafka/operations/monitoring)** - Metrics, dashboards, and alerts
- **[Health Checks](/kafka/operations/health-checks)** - Configure health check endpoints
- **[Performance Tuning](/kafka/operations/performance-tuning)** - Optimize for throughput and latency
- **[Backup & Recovery](/kafka/operations/backup-recovery)** - Disaster recovery strategies

### 🔍 Troubleshooting

- **[Common Issues](/kafka/troubleshooting/common-issues)** - Top 7 issues and solutions
- **[Diagnostic Guide](/kafka/troubleshooting/diagnostic-guide)** - Step-by-step debugging
- **[FAQ](/kafka/troubleshooting/faq)** - Frequently asked questions
- **[Debugging Tips](/kafka/troubleshooting/debugging-tips)** - Advanced debugging techniques

### 🎯 Advanced Topics

- **[Idempotency](/kafka/advanced/idempotency)** - Implement exactly-once semantics
- **[Transactions](/kafka/advanced/transactions)** - Transactional message support
- **[Schema Registry](/kafka/advanced/schema-registry)** - Schema management
- **[Custom Listeners](/kafka/advanced/custom-listeners)** - Develop custom message listeners
- **[Extending Framework](/kafka/advanced/extending-framework)** - Framework extension points

### 💡 Examples

- **[Basic Example](/kafka/examples/basic-example)** - Simple producer/consumer
- **[Batch Processing](/kafka/examples/batch-example)** - Batch operations example
- **[DLQ Handling](/kafka/examples/dlq-example)** - Dead letter queue example
- **[Message Aggregation](/kafka/examples/aggregator-example)** - Aggregate messages
- **[Docker Compose](/kafka/examples/docker-compose-example)** - Complete local setup

### 🧪 Testing

- **[Testing Strategy](/kafka/testing/testing-strategy)** - Test approach and 6-dimension scoring
- **[Unit Testing](/kafka/testing/unit-testing)** - Unit test patterns
- **[Integration Testing](/kafka/testing/integration-testing)** - Integration test setup
- **[Verification Framework](/kafka/testing/verification-framework)** - Comprehensive verification guide

### 📚 Reference

- **[API Reference](/kafka/reference/api-reference)** - Complete API documentation
- **[Configuration Reference](/kafka/reference/configuration-reference)** - All configuration properties
- **[Error Codes](/kafka/reference/error-codes)** - Error code reference
- **[Migration Guide](/kafka/reference/migration-guide)** - Version migration guide

### 📎 Appendix

- **[Glossary](/kafka/appendix/glossary)** - Terms and definitions
- **[Changelog](/kafka/appendix/changelog)** - Version history
- **[Contributing](/kafka/appendix/contributing)** - Contribution guidelines
- **[Resources](/kafka/appendix/resources)** - External resources and links

---

## At a Glance

### Sending Messages

```java
// Simple send
kafkaProducerService.send("topic-name", message);

// Send with key
kafkaProducerService.send("topic-name", key, message);

// Batch send
kafkaProducerService.sendBatch("topic-name", messageList);
```

### Consuming Messages

```java
@KafkaListener(topics = "topic-name", groupId = "my-group")
public void handleMessage(String message) {
    // Process message
}

// Batch consume
@KafkaListener(topics = "topic-name", groupId = "my-group")
public void handleBatch(List<String> messages) {
    // Process batch
}
```

### Error Handling

```java
@KafkaListener(topics = "topic-name", groupId = "my-group")
public void handleMessage(String message) {
    try {
        // Process message
    } catch (BusinessException e) {
        // Send to DLQ automatically via @DLQListener
        throw e;
    }
}
```

---

## Next Steps

1. **New to Kafka?** → Start with [Quick Start](/kafka/getting-started/quick-start)
2. **Need API reference?** → Check [Quick Reference](/kafka/getting-started/quick-reference)
3. **Deploying to production?** → Read [Deployment Guide](/kafka/operations/deployment)
4. **Having issues?** → See [Common Issues](/kafka/troubleshooting/common-issues)

---

## Version Information

| Component | Version | Compatibility |
|-----------|---------|---------------|
| SmartAdmin Kafka Module | 1.0.0 | Spring Boot 3.5.4+ |
| Spring Kafka | 3.x | Kafka 2.8+ |
| Apache Kafka | 3.x | - |

---

<div style="text-align: center; margin-top: 48px;">
  <p><strong>Questions or feedback?</strong></p>
  <p>
    <a href="https://github.com/smart-admin/issues">Report Issue</a> ·
    <a href="/kafka/appendix/contributing">Contribute</a> ·
    <a href="/kafka/troubleshooting/faq">FAQ</a>
  </p>
</div>
