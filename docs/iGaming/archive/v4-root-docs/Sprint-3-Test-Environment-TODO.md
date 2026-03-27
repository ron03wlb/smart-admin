# Sprint 3 - Test Environment Configuration TODO

**Status**: 70% Complete (Deferred to Sprint 4)
**Created**: 2026-03-19
**Priority**: P2 (Non-blocking for production deployment)

---

## ✅ Completed Work (70%)

### 1. Test Configuration Files

#### TestSecurityConfig.java (116 lines)
**Location**: `smartadmin-igaming-integration/src/test/java/.../config/TestSecurityConfig.java`

**Provides**:
- ✅ Mock `BlindIndexService` (SHA-256 instead of HMAC-SHA256, no secret key required)
- ✅ Mock `SystemEnvironment` Bean (avoids `spring.profiles.active` conflicts with `@ActiveProfiles("test")`)

**Implementation**:
```java
@Bean
@Primary
public BlindIndexService mockBlindIndexService() {
  return new BlindIndexService(null) {
    @Override
    public String computeIndex(String plaintext) {
      // SHA-256 for deterministic test hashing
    }
  };
}

@Bean("systemEnvironment")
@Primary
public SystemEnvironment mockSystemEnvironment() {
  return new SystemEnvironment(false, "smartadmin-igaming-integration-test", SystemEnvironmentEnum.TEST);
}
```

---

#### TestKafkaConfig.java (168 lines)
**Location**: `smartadmin-igaming-integration/src/test/java/.../config/TestKafkaConfig.java`

**Provides**:
- ✅ Mock `KafkaProducerService` (8 methods: sendAsync, sendSync, sendBatchAsync, sendBatchSync, etc.)
- ✅ Real `DomainEventPublisher` (uses mock KafkaProducerService, preserves tenantId/traceId injection logic)

**Implementation**:
```java
@Bean
@Primary
public KafkaProducerService mockKafkaProducerService() {
  return new KafkaProducerService() {
    @Override
    public CompletableFuture<SendResult<String, String>> sendAsync(...) {
      // Return completed future with mock SendResult
    }
    // ... 7 other methods
  };
}

@Bean
@Primary
public DomainEventPublisher domainEventPublisher(KafkaProducerService kafkaProducerService) {
  return new DomainEventPublisher(kafkaProducerService);
}
```

**Benefits**:
- No real Kafka broker required (saves ~10s per test class startup)
- Tests focus on business logic, not message delivery
- All domain event publishing code paths are exercised

---

#### application-test.yml (146 lines)
**Location**: `smartadmin-igaming-integration/src/test/resources/application-test.yml`

**Configuration**:
- ✅ Spring (bean override enabled, application name)
- ✅ PostgreSQL Datasource (overridden by Testcontainers)
- ✅ Redis (overridden by Testcontainers)
- ✅ Flyway (enabled, baseline-on-migrate)
- ✅ Kafka (disabled for tests)
- ✅ MyBatis Plus (stdout logging, logic delete)
- ✅ LiteFlow (XML rule source, execution logging)
- ✅ Sa-Token (2592000s timeout, concurrent login)
- ✅ Redisson (standalone mode)
- ✅ Argon2 (test-optimized: m=65536, t=3, p=1)
- ✅ Project name (`smartadmin-igaming-integration-test`)
- ✅ Logging (DEBUG for igaming/common, WARN for Kafka)

**Key Settings**:
```yaml
spring:
  main:
    allow-bean-definition-overriding: true  # Required for @Primary mock beans

smart:
  security:
    argon2:
      enabled: true
      memory: 65536      # Test-optimized (production uses higher)
      iterations: 3      # Test-optimized
      parallelism: 1     # Test-optimized

project:
  name: smartadmin-igaming-integration-test  # Required by SystemEnvironmentConfig
```

---

#### IntegrationModuleTestConfig.java (76 lines)
**Location**: `smartadmin-igaming-integration/src/test/java/.../config/IntegrationModuleTestConfig.java`

**Component Scanning** (16 packages):
- ✅ iGaming modules: integration, player, wallet, activity, game, risk, common
- ✅ Common infrastructure: mybatis, mq, tenant, redislock, redis, cache, security, token
- ✅ Support: liteflow
- ❌ **Excluded**: `net.lab1024.sa.common.core` (to avoid SystemEnvironmentConfig @Value conflicts)

**MapperScan** (9 DAO packages):
- ✅ igaming: player, wallet, wallet.payment, activity, activity.turnover, game, risk
- ✅ support: liteflow
- ✅ common: tenant

**@Import**:
- ✅ `TestKafkaConfig.class`
- ✅ `TestSecurityConfig.class`

**@EnableConfigurationProperties**:
- ✅ `Argon2Properties.class`

---

#### BaseIntegrationTest.java (128 lines)
**Location**: `smartadmin-igaming-integration/src/test/java/.../journey/BaseIntegrationTest.java`

**Testcontainers Setup**:
- ✅ PostgreSQL 16 (`postgres:16-alpine`)
- ✅ Redis 7 (`redis:7-alpine` via GenericContainer)
- ✅ @DynamicPropertySource for datasource and Redis connection override
- ✅ Redisson configuration with dynamic Redis host/port

**Integration**:
```java
@SpringBootTest(classes = IntegrationModuleTestConfig.class)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.data.redis.host", redis::getHost);
    // ...
  }
}
```

---

### 2. Issues Resolved During Configuration

| Issue | Error Type | Resolution |
|-------|-----------|-----------|
| BlindIndexService missing | `NoSuchBeanDefinitionException` | Created `TestSecurityConfig.mockBlindIndexService()` |
| Argon2Properties missing | `NoSuchBeanDefinitionException` | Added `@EnableConfigurationProperties(Argon2Properties.class)` |
| LiteFlowChainDao missing | `NoSuchBeanDefinitionException` | Added `net.lab1024.sa.support.liteflow.dao` to MapperScan |
| TurnoverGameWeightRuleDao missing | `NoSuchBeanDefinitionException` | Added `net.lab1024.sa.igaming.activity.turnover.dao` to MapperScan |
| TenantDao missing | `NoSuchBeanDefinitionException` | Added `net.lab1024.sa.common.tenant.dao` to MapperScan |
| `spring.profiles.active` conflict | `InvalidConfigDataPropertyException` | Created mock SystemEnvironment, excluded common.core scan |
| Bean definition override | `BeanDefinitionOverrideException` | Enabled `allow-bean-definition-overriding: true` |
| Redis Testcontainers library N/A | Dependency not found | Switched to `GenericContainer<>(redis:7-alpine)` |

---

## ❌ Remaining Work (30%)

### 3. Missing Mock Beans (Current Blocker)

#### SecurityConfigProvider (P0 - Blocking)
**Error**:
```
Error creating bean with name 'tokenConfig':
Unsatisfied dependency expressed through constructor parameter 0:
No qualifying bean of type 'net.lab1024.sa.common.security.service.SecurityConfigProvider' available
```

**Required Action**:
1. Read `SecurityConfigProvider` interface to understand contract
2. Create mock implementation in `TestSecurityConfig.java`:
   ```java
   @Bean
   @Primary
   public SecurityConfigProvider mockSecurityConfigProvider() {
     return new SecurityConfigProvider() {
       // Implement required methods
     };
   }
   ```

**Estimated Effort**: 0.1 person-days

---

#### Potential Additional Missing Beans (P1 - TBD)
After resolving `SecurityConfigProvider`, additional beans may be required. Common patterns:
- Configuration providers (e.g., `TokenConfigProvider`, `CacheConfigProvider`)
- Service adapters (e.g., external API mocks)
- Utility beans referenced by SmartAdmin common infrastructure

**Strategy**:
1. Run tests after each fix
2. Identify missing bean from `NoSuchBeanDefinitionException`
3. Add mock to `TestSecurityConfig.java`
4. Repeat until Spring context starts successfully

**Estimated Effort**: 0.2-0.5 person-days (iterative test-fix cycle)

---

### 4. Integration Test Failures (P2 - After Context Starts)

Once Spring context starts successfully, expect test failures due to:
1. **Missing Flyway Migrations**: No database schema initialized
   - **Solution**: Create Flyway migration scripts in Sprint 3 P1
2. **Stub Method Implementations**: Many service methods return hardcoded values
   - **Solution**: Sprint 4 - implement real logic for critical paths
3. **Missing LiteFlow Rules**: Risk assessment rules not defined
   - **Solution**: Sprint 4 - create LiteFlow XML rule definitions

**Estimated Effort**: 1-2 person-days (after context starts)

---

## 📋 Recommended Completion Plan (Sprint 4)

### Phase 1: Fix Spring Context Startup (0.5-1 person-days)
1. Add `SecurityConfigProvider` mock
2. Iteratively fix remaining missing beans
3. Verify Spring context starts without errors

### Phase 2: Database Schema Initialization (Sprint 3 P1)
1. Create Flyway migration scripts for all modules:
   - `V001__create_player_tables.sql`
   - `V002__create_wallet_tables.sql`
   - `V003__create_activity_tables.sql`
   - `V004__create_game_tables.sql`
   - `V005__create_risk_tables.sql`
2. Verify tables created in Testcontainers PostgreSQL
3. Run integration tests

### Phase 3: Fix Integration Test Failures (1-2 person-days)
1. Analyze test failures (likely due to stub methods)
2. Implement critical service logic for test scenarios
3. Verify all 10 tests pass

---

## 🎯 Success Criteria (Sprint 4 Target)

- [ ] Spring context starts successfully (no `NoSuchBeanDefinitionException`)
- [ ] Flyway migrations execute successfully in Testcontainers
- [ ] All 10 integration tests pass:
  - [ ] Should register player with CASH and BONUS wallets
  - [ ] Should register player with referral code
  - [ ] Should fail registration with duplicate username
  - [ ] Should award first deposit bonus on first deposit
  - [ ] Should NOT award bonus on second deposit
  - [ ] Should handle first deposit without active promotion
  - [ ] Should isolate player data across tenants
  - [ ] Should isolate wallet data across tenants
  - [ ] Should handle invalid deposit callback gracefully
  - [ ] Should validate registration form constraints

---

## 📊 Current Test Environment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ PlayerRegistrationJourneyIntegrationTest (10 test cases)    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ BaseIntegrationTest (@SpringBootTest, @Testcontainers)      │
│  - PostgreSQL 16 Container                                  │
│  - Redis 7 Container                                        │
│  - @DynamicPropertySource (override datasource/redis)       │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ IntegrationModuleTestConfig (@SpringBootApplication)        │
│  - Component Scan: 16 packages                              │
│  - MapperScan: 9 DAO packages                               │
│  - @Import: TestKafkaConfig, TestSecurityConfig             │
│  - @EnableConfigurationProperties: Argon2Properties         │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Test Configuration Beans                                     │
│  - TestKafkaConfig: Mock KafkaProducerService (8 methods)   │
│  - TestSecurityConfig:                                       │
│    ✅ Mock BlindIndexService (SHA-256)                       │
│    ✅ Mock SystemEnvironment                                 │
│    ❌ SecurityConfigProvider (TODO)                          │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ Production Services (Auto-wired from component scan)        │
│  - PlayerRegistrationIntegrationService                     │
│  - PlayerAuthService                                        │
│  - WalletService                                            │
│  - FirstDepositBonusIntegrationService                      │
│  - ... (all integration orchestration services)             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 Notes for Future Implementation

1. **Test Data Builders**: Consider using factory pattern for complex entity creation (e.g., `PlayerTestDataBuilder`, `WalletTestDataBuilder`)

2. **Test Database Cleanup**: Add `@Transactional` + `@Rollback` to tests to auto-rollback after each test (currently missing)

3. **Parallel Test Execution**: Once stable, configure Gradle to run tests in parallel for faster feedback

4. **Test Coverage**: After all tests pass, verify integration test coverage with JaCoCo (target: 80%+ for integration orchestration services)

5. **Performance Baseline**: Record test execution times to detect performance regressions:
   - Target: < 30s for full suite (10 tests)
   - Current: N/A (context startup failing)

---

## 🔗 Related Documentation

- **Production Code**: [WithdrawalApprovalService.java](../../smart-admin-api-java21-springboot3/smartadmin-igaming/smartadmin-igaming-integration/src/main/java/net/lab1024/sa/igaming/integration/approval/WithdrawalApprovalService.java) (Sprint 2 Phase 3 completion)
- **Sprint 2 Summary**: Git commit `9f965696` - "feat(igaming-integration): Sprint 2 - End-to-end player journey integration"
- **Test Configuration**: [BaseIntegrationTest.java](../../smart-admin-api-java21-springboot3/smartadmin-igaming/smartadmin-igaming-integration/src/test/java/net/lab1024/sa/igaming/integration/journey/BaseIntegrationTest.java)

---

**Last Updated**: 2026-03-19
**Next Review**: Sprint 4 Planning
