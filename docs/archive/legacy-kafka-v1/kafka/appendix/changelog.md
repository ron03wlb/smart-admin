# Changelog

Version history and release notes for SmartAdmin Kafka integration.

All notable changes to the Kafka integration module are documented here. This project follows [Semantic Versioning](https://semver.org/).

---

## [1.2.0] - 2026-01-22

### Added
- **Comprehensive documentation** (42+ documents, 20,000+ lines)
  - Quick start guide and quick reference card
  - Architecture documentation (5 docs)
  - User guides (6 docs)
  - Operations guides (5 docs)
  - Advanced topics (5 docs)
  - Practical examples (5 docs)
  - Testing documentation (4 docs)
  - Reference documentation (4 docs)
  - Appendix documentation (4 docs)
- **6-dimension quality verification framework**
  - Configuration verification (L1)
  - Code quality verification (L2)
  - Functional correctness verification (L3)
  - Reliability verification (L4)
  - Performance verification (L5)
  - Operations verification (L6)
- **Message aggregation support**
  - Count-based aggregation (N messages)
  - Time-based aggregation (T seconds)
  - Hybrid aggregation strategy
- **Performance optimizations**
  - Batch processing with graceful degradation
  - Configurable compression (lz4, snappy, gzip)
  - Optimized consumer fetch settings

### Changed
- **Updated Spring Kafka to 3.1.0** (from 3.0.0)
  - Improved error handling
  - Better transaction support
- **Enhanced monitoring capabilities**
  - Additional Prometheus metrics
  - Grafana dashboard templates
  - Health check improvements

### Fixed
- **DLQ routing reliability** - Fixed edge case where some errors weren't routed to DLQ
- **Consumer rebalancing** - Improved handling during rebalancing events
- **Memory leak** - Fixed memory leak in batch processing with large payloads

### Documentation
- Complete API reference for all public classes
- Configuration reference with 50+ properties documented
- Error codes catalog with troubleshooting steps
- Migration guide for version upgrades
- 15+ Mermaid diagrams
- Integration test examples with EmbeddedKafka

---

## [1.1.0] - 2025-12-15

### Added
- **Batch processing support** via `AbstractBatchKafkaListener`
  - Process up to 500 messages per batch
  - Automatic fallback to single-message processing
  - 6x performance improvement for bulk operations
- **Idempotency framework**
  - Redis-based deduplication
  - Database unique constraints support
  - Message versioning strategy
- **Transaction support**
  - Transactional producer configuration
  - Read-process-write pattern
  - Exactly-once semantics across topics
- **Custom partitioning strategies**
  - VIP customer partitioner
  - Round-robin partitioner
  - Consistent hash partitioner

### Changed
- **Improved error handling**
  - Better exception categorization (retriable vs permanent)
  - Enhanced DLQ message metadata
  - Configurable retry strategies
- **Performance tuning**
  - Default `batch.size` increased to 32KB
  - Compression enabled by default (lz4)
  - Optimized consumer poll settings

### Fixed
- **Consumer lag accumulation** - Fixed issue where consumers couldn't keep up with high message rates
- **Serialization errors** - Better handling of malformed JSON messages
- **Connection timeout** - Increased default connection timeout to 30s

---

## [1.0.0] - 2025-10-01

### Added
- **Initial Kafka integration** for SmartAdmin 3.x
- **Core components**:
  - `KafkaProducerService` - Synchronous and asynchronous message sending
  - `AbstractKafkaListener` - Base class for consumers with DLQ routing
  - `KafkaAutoConfiguration` - Auto-configuration for Kafka beans
- **DLQ (Dead Letter Queue) support**
  - Automatic routing of failed messages
  - Configurable DLQ topic naming: `{topic}-dlq`
  - Error metadata capture (stacktrace, timestamp)
- **Configuration properties**:
  - `smart.kafka.enabled` - Enable/disable Kafka
  - `smart.kafka.bootstrap-servers` - Broker addresses
  - Environment-specific configurations (dev/test/prod)
- **Health checks**:
  - Kafka health indicator
  - Actuator endpoint: `/actuator/health/kafka`
- **Monitoring**:
  - Prometheus metrics exposure
  - Producer/consumer metrics
  - Consumer lag tracking
- **Documentation**:
  - Basic setup guide
  - Producer/consumer examples
  - Configuration reference

### Technical Details
- **Java**: 21
- **Spring Boot**: 3.5.4
- **Spring Kafka**: 3.0.0
- **Kafka**: 3.3.x - 3.5.x
- **Serialization**: String (JSON via Fastjson2)

---

## [0.9.0] - 2025-08-15 (Beta)

### Added
- **Beta release** for internal testing
- **Basic producer functionality**
  - `KafkaTemplate` wrapper
  - Synchronous send with blocking
  - Basic error handling
- **Basic consumer functionality**
  - `@KafkaListener` support
  - Manual offset commit
  - Simple error logging
- **Configuration**
  - YAML-based configuration
  - Development profile only

### Known Issues
- No DLQ support (messages lost on error)
- No batch processing
- Limited error handling
- No monitoring/metrics

---

## Version Compatibility Matrix

| SmartAdmin Kafka | SmartAdmin | Spring Boot | Spring Kafka | Kafka Broker | Java |
|------------------|------------|-------------|--------------|--------------|------|
| 1.2.0 | 3.5.x | 3.5.4 | 3.1.0 | 3.3.x - 3.5.x | 21 |
| 1.1.0 | 3.5.x | 3.5.0 | 3.0.0 | 3.2.x - 3.5.x | 21 |
| 1.0.0 | 3.4.x | 3.4.0 | 3.0.0 | 3.2.x - 3.4.x | 17+ |
| 0.9.0 (Beta) | 3.3.x | 3.3.0 | 2.9.x | 3.1.x - 3.3.x | 17+ |

---

## Migration Guides

### From 1.1.0 to 1.2.0

**No breaking changes**. Backward compatible upgrade.

**Steps**:
1. Update dependency versions in `build.gradle`
2. Run tests to verify compatibility
3. Optional: Enable new features (aggregation, enhanced monitoring)

### From 1.0.0 to 1.1.0

**Breaking changes**:
- `AbstractKafkaListener.handleError()` signature changed
  - Old: `handleError(String message, Exception e)`
  - New: `handleError(ConsumerRecord<String, String> record, Exception e)`

**Migration**:
```java
// Before (1.0.0)
@Override
protected void handleError(String message, Exception e) {
    log.error("Error: {}", message, e);
}

// After (1.1.0)
@Override
protected void handleError(ConsumerRecord<String, String> record, Exception e) {
    log.error("Error | Key: {}", record.key(), e);
}
```

### From 0.9.0 to 1.0.0

**Major changes**:
- DLQ support added (requires topic creation)
- Configuration namespace changed: `kafka.*` → `smart.kafka.*`
- `KafkaProducerService` replaces direct `KafkaTemplate` usage

**Migration**:
```yaml
# Before (0.9.0)
kafka:
  bootstrap-servers: localhost:9092

# After (1.0.0)
smart:
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
```

---

## Upcoming Features (Roadmap)

### Version 1.3.0 (Planned: Q2 2026)

- **Schema Registry integration**
  - Avro serialization support
  - Schema evolution
  - Compatibility checking
- **Kafka Streams support**
  - Stream processing DSL
  - Windowing and aggregation
  - Stateful processing
- **Advanced monitoring**
  - Custom Grafana dashboards
  - Alert rule templates
  - Consumer lag alerts
- **Performance improvements**
  - Adaptive batching
  - Compression optimization
  - Zero-copy transfer

### Version 2.0.0 (Planned: Q4 2026)

- **Multi-cluster support**
  - Active-active replication
  - Disaster recovery
  - Geographic distribution
- **Enhanced security**
  - OAuth2 integration
  - Fine-grained ACLs
  - Encryption at rest
- **Cloud-native features**
  - Kubernetes operators
  - Auto-scaling support
  - Cloud provider integrations

---

## Deprecation Notices

### Version 1.2.0
- None

### Version 1.1.0
- `AbstractKafkaListener.handleError(String, Exception)` - Deprecated in favor of `handleError(ConsumerRecord, Exception)`

### Version 1.0.0
- Direct `KafkaTemplate` injection - Use `KafkaProducerService` instead

---

## Release Notes Format

Each release follows this format:

### Added
New features and capabilities

### Changed
Changes to existing functionality

### Deprecated
Features marked for removal in future versions

### Removed
Features removed in this version

### Fixed
Bug fixes

### Security
Security patches and improvements

---

## Contributing

To report bugs or request features:
- GitHub Issues: [smart-admin/issues](https://github.com/smart-admin/smart-admin/issues)
- Documentation: See [Contributing Guide](/kafka/appendix/contributing)

---

## See Also

- [Migration Guide](/kafka/reference/migration-guide) - Detailed migration procedures
- [Breaking Changes](#migration-guides) - Version-specific breaking changes
- [Roadmap](#upcoming-features-roadmap) - Future features

---

**Last Updated**: 2026-01-22
