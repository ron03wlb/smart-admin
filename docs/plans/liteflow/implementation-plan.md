# LiteFlow Module Implementation Plan

## Overview

**Goal**: Implement a comprehensive LiteFlow integration module for SmartAdmin to replace Evrete as the primary rule engine and provide complex business process orchestration.

**Module Location**: `sa-base/support/liteflow/` (Support module with full UI management)

**Key Requirements**:
- Complex business process orchestration with conditional branching and parallel execution
- EL script support (QLExpress primary, Groovy/JS optional)
- Declarative flow configuration (XML/YAML/JSON)
- Database-backed flow storage with hot-reload
- Monitoring and execution metrics
- Web UI for flow/script management

## Architecture Decisions

### 1. Technology Stack

| Component | Choice | Version | Rationale |
|-----------|--------|---------|-----------|
| **LiteFlow Core** | liteflow-spring-boot-starter | 2.15.3 | Latest stable, Spring Boot 3.5.4 compatible |
| **Script Engine** | liteflow-script-qlexpress | 2.15.3 | Alibaba's battle-tested expression engine |
| **Database** | PostgreSQL | 16 | Existing SmartAdmin standard |
| **Cache** | JetCache (two-level) | 2.7.7 | Existing SmartAdmin foundation module |
| **Framework** | Spring Boot | 3.5.4 | SmartAdmin standard |

### 2. Module Structure

Following SmartAdmin's strict layered architecture:

```
sa-base/support/liteflow/
├── build.gradle.kts
├── src/main/
│   ├── java/net/lab1024/sa/base/module/support/liteflow/
│   │   ├── config/                    # Auto-configuration
│   │   │   ├── LiteFlowAutoConfiguration.java
│   │   │   ├── LiteFlowProperties.java
│   │   │   └── LiteFlowDataSourceConfiguration.java
│   │   ├── controller/                # REST APIs
│   │   │   ├── LiteFlowChainController.java
│   │   │   ├── LiteFlowScriptController.java
│   │   │   ├── LiteFlowExecutionController.java
│   │   │   └── LiteFlowMonitorController.java
│   │   ├── service/                   # Business logic
│   │   │   ├── LiteFlowChainService.java
│   │   │   ├── LiteFlowScriptService.java
│   │   │   ├── LiteFlowExecutionService.java
│   │   │   └── LiteFlowMonitorService.java
│   │   ├── manager/                   # Transactions & cache
│   │   │   ├── LiteFlowChainManager.java
│   │   │   ├── LiteFlowCacheManager.java
│   │   │   └── LiteFlowMetricsManager.java
│   │   ├── dao/                       # Data access
│   │   │   ├── LiteFlowChainDao.java
│   │   │   ├── LiteFlowScriptDao.java
│   │   │   ├── LiteFlowExecutionLogDao.java
│   │   │   └── LiteFlowExecutionMetricsDao.java
│   │   ├── domain/                    # Domain objects
│   │   │   ├── entity/
│   │   │   ├── form/
│   │   │   └── vo/
│   │   ├── constant/                  # Constants & enums
│   │   ├── core/                      # LiteFlow integration
│   │   │   ├── datasource/SmartLiteFlowDataSource.java
│   │   │   ├── executor/SmartFlowExecutor.java
│   │   │   ├── listener/LiteFlowExecutionListener.java
│   │   │   └── reload/LiteFlowReloadCommand.java
│   │   └── exception/
│   └── resources/
│       ├── META-INF/spring/
│       │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│       └── mapper/
```

See [architecture.md](architecture.md) for detailed architecture documentation.

## Implementation Phases

### Phase 1: Foundation Setup (Week 1)
**Goal**: Establish module structure and database schema

- [ ] Create module directory structure
- [ ] Configure build.gradle.kts with LiteFlow dependencies
- [ ] Add to sa-base aggregator
- [ ] Update libs.versions.toml
- [ ] Create database migration SQL
- [ ] Execute migration (create 4 tables)
- [ ] Create domain entities (4 entity classes)
- [ ] Create DAO interfaces (extend BaseMapper)
- [ ] Create constant classes and enums

### Phase 2: Core Integration (Week 2)
**Goal**: Integrate LiteFlow engine with database-backed storage

- [ ] Implement LiteFlowProperties.java
- [ ] Implement SmartLiteFlowDataSource.java (RuleSource SPI)
- [ ] Implement LiteFlowDataSourceConfiguration.java
- [ ] Implement LiteFlowAutoConfiguration.java
- [ ] Register auto-configuration in META-INF/spring
- [ ] Implement SmartFlowExecutor.java (wrapper)
- [ ] Implement LiteFlowExecutionListener.java
- [ ] Test basic flow execution from database

### Phase 3: CRUD Operations (Week 2-3)
**Goal**: Implement chain/script management with caching

- [ ] Create form classes (Add/Update/Query forms)
- [ ] Create VO classes
- [ ] Implement LiteFlowCacheManager.java (JetCache)
- [ ] Implement LiteFlowChainManager.java (transactional CRUD)
- [ ] Implement LiteFlowScriptManager.java (transactional CRUD)
- [ ] Implement LiteFlowChainService.java
- [ ] Implement LiteFlowScriptService.java
- [ ] Create MyBatis XML mappers
- [ ] Unit test: CRUD operations, cache invalidation

### Phase 4: API Layer (Week 3)
**Goal**: Expose REST APIs with permission control

- [ ] Implement LiteFlowChainController.java
- [ ] Implement LiteFlowScriptController.java
- [ ] Add permission codes to system
- [ ] Configure Sa-Token permissions
- [ ] Add Swagger/Knife4j documentation
- [ ] Test API endpoints with Postman/Swagger

### Phase 5: Execution & Monitoring (Week 4)
**Goal**: Flow execution with logging and metrics

- [ ] Implement LiteFlowExecutionService.java
- [ ] Implement execution logging (insert to t_liteflow_execution_log)
- [ ] Implement LiteFlowMetricsManager.java (aggregation)
- [ ] Implement LiteFlowMonitorService.java
- [ ] Implement LiteFlowExecutionController.java
- [ ] Implement LiteFlowMonitorController.java
- [ ] Test: Execute flows, verify logs, check metrics

### Phase 6: Hot Reload & Integration (Week 5)
**Goal**: Hot-reload and SmartAdmin integration

- [ ] Implement LiteFlowReloadCommand.java (Reload module integration)
- [ ] Configure hot-reload interval (optional auto-reload)
- [ ] Test: Update chain in DB → reload → execute → verify new behavior
- [ ] Integration test: Full CRUD → execute → monitor workflow
- [ ] Performance test: Concurrent executions, cache hit rate

### Phase 7: Testing & Documentation (Week 6)
**Goal**: Quality assurance and documentation

- [ ] Unit tests: 80%+ coverage for Service/Manager layers
- [ ] Integration tests: End-to-end flow execution scenarios
- [ ] ArchUnit tests: Validate layer dependencies, @Transactional placement
- [ ] Performance tests: Load testing with JMeter
- [ ] Create README.md (module documentation)
- [ ] Create migration guide (Evrete → LiteFlow)
- [ ] Create usage examples (order processing, rule evaluation)
- [ ] Update CLAUDE.md with LiteFlow patterns

## SmartAdmin Pattern Compliance Checklist

### Architecture Constraints (STRICT)
- [ ] Layer dependencies: Controller → Service → Manager → Dao ✓
- [ ] Manager layer: ONLY calls Dao (never Service or other Manager) ✓
- [ ] Controller layer: ONLY calls Service (never Manager or Dao) ✓
- [ ] `@Transactional(rollbackFor = Throwable.class)` ONLY in Manager ✓

### Dependency Injection (STRICT)
- [ ] Use `@RequiredArgsConstructor` + `private final` for all dependencies ✓
- [ ] NEVER use `@Autowired` field injection ✓

### Naming Conventions
- [ ] Controllers: `{Entity}Controller` ✓
- [ ] Services: `{Entity}Service` ✓
- [ ] Managers: `{Entity}Manager`, `{Entity}CacheManager` ✓
- [ ] DAOs: `{Entity}Dao` ✓
- [ ] Entities: `{Entity}Entity` ✓
- [ ] Forms: `{Entity}{Action}Form` (AddForm, UpdateForm, QueryForm) ✓
- [ ] VOs: `{Entity}VO` ✓
- [ ] Boolean fields: `deleted`, `enabled` (NOT `isDeleted`, `isEnabled`) ✓

### Response & Error Handling
- [ ] All APIs return `ResponseDTO.ok(data)` or throw `BusinessException` ✓
- [ ] Use `ResponseDTO.userErrorParam()` for validation errors ✓
- [ ] Use `ResponseDTO.error(ErrorCode)` for business errors ✓

### Data Access
- [ ] Use MyBatis-Plus `LambdaQueryWrapper` (type-safe) ✓
- [ ] Use `SmartBeanUtil.copy()` for bean conversion ✓
- [ ] Use `SmartPageUtil.convert2PageQuery()` and `convert2PageResult()` ✓

### Auto-Configuration
- [ ] `@AutoConfiguration` with `@AutoConfigureAfter` ✓
- [ ] `@ConditionalOnProperty` for optional features ✓
- [ ] Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` ✓

### Cache Management
- [ ] JetCache `@CreateCache` for cache beans (in Manager) ✓
- [ ] Two-level cache: local (Caffeine) + remote (Redis) ✓
- [ ] Explicit cache eviction on CRUD operations ✓

### Security
- [ ] `@SaCheckPermission` on all controller methods ✓
- [ ] Input validation with `@Valid` and JSR-303 annotations ✓
- [ ] Script injection prevention (QLExpress sandboxing) ✓

## Verification Plan

### 1. Unit Tests
```bash
./gradlew :sa-base:support:liteflow:test
```
**Expected**: 80%+ line coverage, all tests pass

### 2. Architecture Tests
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```
**Expected**:
- LiteFlow module follows layer dependency rules
- `@Transactional` only in Manager layer
- No field injection violations

### 3. Integration Test Scenario
**End-to-End Flow**:
1. Create a chain via API: `POST /liteflow/chain/add`
2. Create script nodes (validateOrder, checkInventory, etc.)
3. Reload chain: `POST /liteflow/chain/reload?chainCode=order-process-chain`
4. Execute chain: `POST /liteflow/execution/execute`
5. Verify execution log: `GET /liteflow/execution/logDetail/{logId}`
6. Check metrics: `GET /liteflow/monitor/overview`

See [api-specification.md](api-specification.md) for complete API documentation.

### 4. Hot-Reload Test
1. Execute chain (initial version)
2. Update chain in database: `POST /liteflow/chain/update` (modify chainData)
3. Reload: `POST /liteflow/chain/reloadAll`
4. Execute chain again
5. Verify new behavior (different execution path)

### 5. Performance Test
- **Tool**: JMeter or Gatling
- **Scenario**: 100 concurrent users executing flows
- **Metrics**:
  - Throughput: > 500 executions/sec
  - Response time: P95 < 100ms
  - Cache hit rate: > 90%
  - Error rate: < 0.1%

### 6. Quality Gate
```bash
./gradlew :sa-base:support:liteflow:build
```
**Expected**:
- Checkstyle: 0 errors
- PMD: 0 violations
- SpotBugs: 0 bugs
- Tests: 100% pass
- Coverage: > 80%

## Dependencies Update Summary

### gradle/libs.versions.toml
Add to `[versions]`:
```toml
liteflow = "2.15.3"
```

Add to `[libraries]`:
```toml
liteflow-spring-boot-starter = { module = "com.yomahub:liteflow-spring-boot-starter", version.ref = "liteflow" }
liteflow-script-qlexpress = { module = "com.yomahub:liteflow-script-qlexpress", version.ref = "liteflow" }
```

### sa-base/build.gradle.kts
Add to dependencies section:
```kotlin
api(project(":sa-base:support:liteflow"))
```

### settings.gradle.kts
Add to includes:
```kotlin
include(":sa-base:support:liteflow")
```

## Related Documentation

- [Architecture Documentation](architecture.md) - Detailed architecture and design decisions
- [Database Schema](database-schema.md) - Complete database DDL and schema design
- [API Specification](api-specification.md) - REST API endpoints and request/response formats
- [Migration Guide](migration-guide.md) - Migration from Evrete to LiteFlow
- [README](README.md) - Quick start and overview

## Success Criteria

### Functional Requirements
✓ Flow/script CRUD operations via REST API
✓ Database-backed storage with hot-reload
✓ Flow execution with input/output
✓ Execution logging and metrics
✓ Monitoring dashboard data APIs
✓ Permission-controlled access

### Non-Functional Requirements
✓ Architecture: 100% compliance with SmartAdmin patterns
✓ Performance: P95 < 100ms, throughput > 500/sec
✓ Quality: 0 Checkstyle/PMD/SpotBugs violations
✓ Testing: 80%+ coverage, 100% pass rate
✓ Documentation: README, migration guide, usage examples

### Integration Requirements
✓ Auto-configuration with Spring Boot
✓ JetCache two-level caching
✓ Sa-Token permission control
✓ Reload module integration
✓ MyBatis-Plus data access

---

## Summary

This plan provides a complete, production-ready design for integrating LiteFlow into SmartAdmin as a support module. The implementation strictly follows SmartAdmin's architectural patterns (layered architecture, dependency injection, transaction management, caching, etc.) while providing comprehensive flow orchestration capabilities.

**Key Highlights**:
- Database-backed flow storage with hot-reload
- Two-level caching (Caffeine + Redis)
- Complete CRUD APIs with permission control
- Execution logging and metrics for monitoring
- QLExpress script engine for dynamic business logic
- Phased migration from Evrete
- Comprehensive testing and quality gates

**Estimated Timeline**: 6 weeks (foundation to production-ready)

**Critical Path**: Database schema → Core integration → CRUD operations → Execution & monitoring
