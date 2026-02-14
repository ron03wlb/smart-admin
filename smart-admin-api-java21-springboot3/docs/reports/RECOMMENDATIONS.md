# SmartAdmin Backend - Quality Improvement Recommendations

**Priority-Ordered Action Plan**
**Generated:** 2026-01-22

---

## Quick Decision Matrix

| Priority | Issue | Count | Effort | Impact | Action |
|----------|-------|-------|--------|--------|--------|
| **P1** | SpotBugs Critical Bugs | 4 | Low | High | Fix Now |
| **P1** | PMD Configuration | 1,039 | Medium | High | Decide Strategy |
| **P2** | Lombok EI/EI2 | 27 | Low | Low | Suppress |
| **P2** | Run Tests | N/A | Low | High | Execute |
| **P3** | Fix PMD foundation:core | 530 | High | Medium | Refactor |
| **P3** | Fix PMD other modules | 509 | High | Medium | Refactor |
| **P4** | Test Coverage | N/A | High | Medium | Expand |

---

## Priority 1: Critical - Fix Immediately (Before Next Build)

### 1.1 Fix Critical SpotBugs Issues (4 bugs)

**Estimated Time:** 30-60 minutes
**Impact:** Prevents runtime bugs and security vulnerabilities

#### Issue #1: Static Field Write from Instance Method (HIGH PRIORITY)

**Location:** [JsonUtil.java:31](smart-admin-api-java21-springboot3/sa-base/foundation/core/src/main/java/net/lab1024/sa/base/common/json/JsonUtil.java#L31)

**Problem:**
```
H D ST: Write to static field net.lab1024.sa.base.core.json.JsonUtil.staticMapper
        from instance method net.lab1024.sa.base.core.json.JsonUtil.init()
```

**Current Code (Problematic):**
```java
public class JsonUtil {
    private static ObjectMapper staticMapper;

    public void init() {  // Instance method
        staticMapper = new ObjectMapper();  // Writing to static field - THREAD UNSAFE
    }
}
```

**Fix Option A: Use Static Initializer (Recommended)**
```java
public class JsonUtil {
    private static final ObjectMapper staticMapper = new ObjectMapper();

    // Remove init() method or make it private static
}
```

**Fix Option B: Use Static Initialization Block**
```java
public class JsonUtil {
    private static final ObjectMapper staticMapper;

    static {
        staticMapper = new ObjectMapper();
        // Configure mapper here
    }
}
```

**Fix Option C: Make Field Non-Static**
```java
@Component
public class JsonUtil {
    private final ObjectMapper objectMapper;

    public JsonUtil() {
        this.objectMapper = new ObjectMapper();
    }
}
```

**Recommended:** Option A (static final field with direct initialization)

#### Issue #2: Null Passed for Non-Null Parameter (MEDIUM PRIORITY)

**Location:** [KafkaProducerServiceImpl.java:34](smart-admin-api-java21-springboot3/sa-base/foundation/mq/src/main/java/net/lab1024/sa/common/mq/kafka/core/KafkaProducerServiceImpl.java#L34)

**Problem:**
```
M C NP: Null passed for non-null parameter of sendAsync(String, String, String)
        in KafkaProducerServiceImpl.sendAsync(String, String)
```

**Current Code (Problematic):**
```java
public CompletableFuture<SendResult> sendAsync(String topic, String message) {
    return sendAsync(topic, null, message);  // Passing null for key parameter
    //                     ^^^^ NULL PASSED TO NON-NULL PARAMETER
}
```

**Fix Option A: Add @Nullable Annotation (If null is valid)**
```java
public CompletableFuture<SendResult> sendAsync(
    String topic,
    @Nullable String key,  // Mark as nullable
    String message) {
    // ...
}
```

**Fix Option B: Use Empty String Instead of Null**
```java
public CompletableFuture<SendResult> sendAsync(String topic, String message) {
    return sendAsync(topic, "", message);  // Use empty string instead of null
}
```

**Fix Option C: Add Null Check**
```java
public CompletableFuture<SendResult> sendAsync(
    String topic,
    String key,
    String message) {
    if (key == null) {
        key = "";  // Or throw exception, or use default value
    }
    // ...
}
```

**Recommended:** Review business logic to determine if null keys are valid, then apply appropriate fix.

#### Issue #3: Null Passed to CompletableFuture.getNow() (LOW PRIORITY)

**Location:** [KafkaProducerServiceImpl.java:147](smart-admin-api-java21-springboot3/sa-base/foundation/mq/src/main/java/net/lab1024/sa/common/mq/kafka/core/KafkaProducerServiceImpl.java#L147)

**Problem:**
```
L C NP: Null passed for non-null parameter of CompletableFuture.getNow(Object)
```

**Current Code (Problematic):**
```java
future.whenComplete((result, ex) -> {
    SendResult sendResult = future.getNow(null);  // Passing null
    //                                     ^^^^ LOW PRIORITY - API allows null
});
```

**Fix:**
```java
future.whenComplete((result, ex) -> {
    if (future.isDone() && !future.isCompletedExceptionally()) {
        SendResult sendResult = future.join();  // Use join() instead of getNow(null)
    } else {
        // Handle exception case
    }
});
```

**Note:** This is low priority - CompletableFuture.getNow() actually accepts null as a valid default value.

#### Issue #4: Constructor Throws Exception (MEDIUM PRIORITY)

**Location:** [MessageAggregator.java:78](smart-admin-api-java21-springboot3/sa-base/foundation/mq/src/main/java/net/lab1024/sa/common/mq/kafka/batch/MessageAggregator.java#L78)

**Problem:**
```
M B CT: Exception thrown in constructor will leave object partially initialized
        and may be vulnerable to Finalizer attacks.
```

**Current Code (Problematic):**
```java
public class MessageAggregator {
    public MessageAggregator(int maxSize, long maxWait, Consumer<List<T>> consumer) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
            // ^^^^ EXCEPTION IN CONSTRUCTOR - SECURITY RISK
        }
        this.maxSize = maxSize;
        this.maxWait = maxWait;
        this.consumer = consumer;
    }
}
```

**Fix Option A: Use Factory Method (Recommended)**
```java
public class MessageAggregator {
    private MessageAggregator(int maxSize, long maxWait, Consumer<List<T>> consumer) {
        this.maxSize = maxSize;
        this.maxWait = maxWait;
        this.consumer = consumer;
    }

    public static MessageAggregator create(int maxSize, long maxWait, Consumer<List<T>> consumer) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        return new MessageAggregator(maxSize, maxWait, consumer);
    }
}
```

**Fix Option B: Move Validation to Setter/Init Method**
```java
public class MessageAggregator {
    public MessageAggregator(int maxSize, long maxWait, Consumer<List<T>> consumer) {
        this.maxSize = maxSize;  // Don't validate in constructor
        this.maxWait = maxWait;
        this.consumer = consumer;
    }

    public void init() {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
    }
}
```

**Recommended:** Option A (factory method pattern)

---

### 1.2 Decide PMD Configuration Strategy (CRITICAL DECISION)

**Estimated Time:** 1-2 hours for decision + implementation
**Impact:** Unblocks builds and determines technical debt approach

You have **three strategic options**:

#### Option A: Relax PMD Rules (Pragmatic Approach) ⭐ RECOMMENDED

**Pros:**
- ✅ Unblocks builds immediately
- ✅ Focuses on real bugs (SpotBugs) vs. style preferences
- ✅ Aligns with SmartAdmin coding style
- ✅ Common in Spring Boot projects

**Cons:**
- ⚠️ Code won't be as strictly immutable
- ⚠️ Variable names may be longer

**Implementation:**

Edit `config/pmd/ruleset.xml` to exclude problematic rules:

```xml
<?xml version="1.0"?>
<ruleset name="SmartAdmin Custom PMD Rules">
    <description>PMD rules aligned with SmartAdmin coding standards</description>

    <!-- Keep all existing rules -->
    <rule ref="category/java/bestpractices.xml"/>
    <rule ref="category/java/performance.xml"/>
    <rule ref="category/java/security.xml"/>
    <rule ref="category/java/errorprone.xml"/>

    <!-- Code Style: Exclude overly strict rules -->
    <rule ref="category/java/codestyle.xml">
        <exclude name="MethodArgumentCouldBeFinal"/>
        <exclude name="LocalVariableCouldBeFinal"/>
        <exclude name="LongVariable"/>
        <exclude name="UseUnderscoresInNumericLiterals"/>
    </rule>

    <!-- Design: Keep complexity rules -->
    <rule ref="category/java/design.xml/CyclomaticComplexity">
        <properties>
            <property name="methodReportLevel" value="15"/>
            <property name="classReportLevel" value="80"/>
        </properties>
    </rule>
</ruleset>
```

**Verify:**
```bash
./gradlew clean build
# Expected: PMD passes, build continues to tests
```

#### Option B: Fix All PMD Violations (Strict Approach)

**Pros:**
- ✅ Maximum code quality and immutability
- ✅ Enforces best practices
- ✅ Educational for team

**Cons:**
- ❌ Requires ~10-20 hours of refactoring work
- ❌ 1,039 violations to fix across 12 modules
- ❌ May conflict with Lombok patterns
- ❌ Changes entire coding style

**Implementation:**

For each module, apply PMD fixes:

```bash
# Example fixes for foundation:core (530 violations)

# 1. Add 'final' to all parameters and local variables
# Before:
public void process(String value) {
    String result = getValue();
}

# After:
public void process(final String value) {
    final String result = getValue();
}

# 2. Shorten variable names
# Before:
public void deserialize(JsonParser jsonParser,
                       DeserializationContext deserializationContext) { }

# After:
public void deserialize(final JsonParser parser,
                       final DeserializationContext context) { }

# 3. Add underscores to numeric literals
# Before:
private static final long MAX = 9007199254740991L;

# After:
private static final long MAX = 9_007_199_254_740_991L;
```

**Effort:** 530 violations in core × 2 minutes each = ~18 hours just for core module

#### Option C: Hybrid Approach (Gradual Improvement)

**Pros:**
- ✅ Fix critical rules only
- ✅ Suppress style rules temporarily
- ✅ Plan gradual improvement

**Cons:**
- ⚠️ Requires discipline to actually fix later
- ⚠️ Creates technical debt

**Implementation:**

1. Exclude style rules (Option A)
2. Fix only critical PMD rules (performance, security)
3. Add to backlog: "Improve code immutability"

**Recommendation Matrix:**

| Factor | Option A | Option B | Option C |
|--------|----------|----------|----------|
| **Unblock Build** | ✅ Immediate | ❌ Weeks | ✅ Immediate |
| **Code Quality** | 🟡 Good | ✅ Excellent | 🟡 Good |
| **Team Effort** | Low (2h) | High (20h) | Medium (5h) |
| **Technical Debt** | None | None | Some |
| **Industry Standard** | ✅ Yes | ⚠️ Rare | 🟡 Common |

**RECOMMENDATION: Choose Option A (Relax PMD Rules)**

**Rationale:**
1. PMD violations are 97% style preferences, not functional bugs
2. SmartAdmin has excellent Checkstyle compliance already
3. SpotBugs catches actual bugs (which we'll fix in P1)
4. Spring Boot projects commonly exclude these PMD rules
5. Unblocks build immediately for production deployment

---

## Priority 2: High - Fix Before Production

### 2.1 Suppress Lombok EI/EI2 Violations (27 issues)

**Estimated Time:** 15-30 minutes
**Impact:** Cleans up SpotBugs reports without changing functionality

These violations are **by design** in Lombok-generated code. Suppress them:

#### Fix: Add @SuppressFBWarnings Annotations

**Example: KafkaProperties.java**

```java
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Lombok-generated code, internal state exposure is acceptable for configuration DTOs")
public class KafkaProperties {
    private Consumer consumer;
    private Producer producer;
    private DeadLetterQueue deadLetterQueue;
    private Listener listener;
    private Batch batch;

    @Data
    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static class Consumer { }

    @Data
    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static class Producer { }
}
```

**Apply to All Affected Classes:**

1. `KafkaProperties` and nested classes (10 suppressions)
2. `BatchSendResult` and builders (8 suppressions)
3. `KafkaProducerServiceImpl` (1 suppression)
4. `DeadLetterServiceImpl` (1 suppression)
5. `DataSourceConfig` (2 suppressions)

**Alternative: Global Suppression**

Add to `config/spotbugs/exclude.xml` (create if doesn't exist):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- Suppress EI/EI2 for all Lombok @Data classes -->
    <Match>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
        <Class name="~.*\$.*Builder"/>  <!-- Suppress for all builders -->
    </Match>

    <Match>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
        <Or>
            <Class name="net.lab1024.sa.common.mq.kafka.config.KafkaProperties"/>
            <Class name="net.lab1024.sa.common.mq.kafka.batch.BatchSendResult"/>
        </Or>
    </Match>
</FindBugsFilter>
```

Then update `build.gradle.kts`:

```kotlin
spotbugs {
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}
```

### 2.2 Run Tests to Validate Functionality

**Estimated Time:** 5-10 minutes
**Impact:** Ensures code works correctly despite quality violations

After fixing P1 issues, run tests:

```bash
# Run all tests
./gradlew test

# Run specific test suites
./gradlew :sa-admin:test --tests ArchitectureTest
./gradlew :sa-admin:test --tests LoginServiceTest

# Run with coverage
./gradlew test jacocoTestReport

# View coverage report
open sa-admin/build/reports/jacoco/test/html/index.html
```

**Expected Results:**
- ✅ All 7 test files pass
- ✅ ArchitectureTest validates layered architecture
- ✅ LoginServiceTest passes 40+ test cases
- ⚠️ AdminApplicationTest still disabled (requires database)

### 2.3 Validate Architecture with ArchUnit

**Estimated Time:** 2 minutes
**Impact:** Ensures layered architecture integrity

```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Expected Validations:**
- ✅ Controller → Service (not direct to Manager/Dao)
- ✅ Service → Manager OR Dao
- ✅ Manager → Dao ONLY (NOT to Service)
- ✅ @Transactional only in Manager layer
- ✅ Naming conventions (Controller suffix, @RestController)

**If Violations Found:**

Review the architecture test output and fix package dependencies:

```java
// WRONG - Controller calls Manager directly
@RestController
public class EmployeeController {
    @Autowired private EmployeeManager employeeManager;  // ❌ VIOLATION
}

// CORRECT - Controller calls Service only
@RestController
public class EmployeeController {
    @Autowired private EmployeeService employeeService;  // ✅ CORRECT
}
```

---

## Priority 3: Medium - Technical Debt Reduction

### 3.1 Fix PMD Violations in foundation:core (530 violations)

**Estimated Time:** 10-15 hours
**Impact:** Improves code immutability and readability

**Only pursue if choosing Option B (Fix All) from P1.2**

**Automated Approach:**

Use IntelliJ IDEA or Spotless to auto-fix many violations:

1. **Add 'final' keyword automatically:**

IntelliJ: Settings → Editor → Inspections → Java → Code style issues
- Enable: "Local variable or parameter can be final"
- Apply fix: Ctrl+Alt+Shift+I → "Make final"

2. **Shorten variable names:**

Requires manual review - Jackson framework parameters may need to stay long for clarity.

3. **Add numeric separators:**

Find-Replace with regex:
```regex
Find: (\d{4,})L
Replace: ${1}_separated_manually
```

### 3.2 Fix PMD Violations in Other Modules (509 violations)

**Module-by-Module Plan:**

| Module | Violations | Estimated Time | Priority |
|--------|-----------|----------------|----------|
| foundation:mq | 155 | 3-4h | High |
| foundation:cache | 100 | 2-3h | Medium |
| foundation:api-encrypt | 57 | 1-2h | Medium |
| foundation:repeat-submit | 47 | 1h | Medium |
| foundation:data-masking | 44 | 1h | Medium |
| foundation:security-protect | 39 | 1h | Medium |
| foundation:redis-lock | 37 | 1h | Medium |
| infrastructure:datasource | 14 | 30min | Low |
| foundation:captcha | 13 | 30min | Low |
| infrastructure:mybatis | 3 | 15min | Low |

**Total Estimated Effort:** 10-15 hours

### 3.3 Improve Test Coverage

**Estimated Time:** Ongoing effort (weeks/months)
**Impact:** Increases confidence in code quality

**Current State:**
- Only 7 test files across 36 modules
- Most modules have zero tests
- LoginServiceTest is exemplary (40+ test cases)

**Recommended Approach:**

**Phase 1: Foundation Modules (Weeks 1-2)**

Focus on modules with violations:
- ✅ Write tests for foundation:core (highest violations)
- ✅ Write tests for foundation:mq (Kafka logic)
- ✅ Write tests for foundation:cache (caching logic)

**Phase 2: Business Logic (Weeks 3-4)**

Expand sa-admin test coverage:
- ✅ Controller tests (using BaseControllerTest)
- ✅ Service tests (using BaseUnitTest with mocks)
- ✅ Manager tests (using BaseIntegrationTest with real DB)

**Phase 3: Coverage Targets (Ongoing)**

Add JaCoCo thresholds to enforce coverage:

```kotlin
// build.gradle.kts
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal()  // Start with 50%
            }
        }
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()
            }
            excludes = listOf(
                "**.config.**",  // Exclude config classes
                "**.domain.entity.**",  // Exclude entities
                "**.domain.form.**",  // Exclude DTOs
                "**.domain.vo.**"
            )
        }
    }
}
```

**Testing Best Practices:**

Follow LoginServiceTest patterns:
- Use `@RequiredArgsConstructor` for dependency injection
- Use Mockito for unit tests
- Use Testcontainers for integration tests
- Use test fixtures for common test data

---

## Priority 4: Low - Future Enhancements

### 4.1 Enable AdminApplicationTest with Docker

**Estimated Time:** 1 hour
**Impact:** Validates full application context loading

**Current State:**
```java
@Disabled("Integration test - requires database connection")
class AdminApplicationTest {
    @Test
    void contextLoads() {
        // Test disabled
    }
}
```

**Fix:**

Use Testcontainers PostgreSQL:

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AdminApplicationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("smart_admin_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
        // Application context should load successfully
    }
}
```

Or use docker-compose from project root:

```bash
cd /c/Workspace/open_source/smart-admin
docker-compose up -d postgres
./gradlew :sa-admin:test --tests AdminApplicationTest
```

### 4.2 Add JaCoCo Coverage Thresholds

**Estimated Time:** 30 minutes
**Impact:** Enforces minimum test coverage

See Phase 3 of section 3.3 above for implementation.

### 4.3 Integrate SonarQube for Ongoing Quality

**Estimated Time:** 2-4 hours
**Impact:** Centralized quality dashboard and historical tracking

**Implementation:**

1. Start SonarQube server (Docker):
```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
```

2. Configure in `build.gradle.kts` (already present):
```kotlin
sonar {
    properties {
        property("sonar.projectKey", "smart-admin")
        property("sonar.projectName", "SmartAdmin")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.login", "your-token-here")
    }
}
```

3. Run SonarQube analysis:
```bash
./gradlew sonar -Dsonar.login=your-token
```

4. View results at http://localhost:9000

**Benefits:**
- Historical trend tracking
- Code smell detection
- Security vulnerability scanning
- Technical debt quantification
- Coverage visualization

### 4.4 Refactor Kafka Sample Consumers

**Estimated Time:** 1 hour
**Impact:** Low (sample code only)

**Current State:**
```java
// KafkaBatchConsumerSample.java
// TODO: 在此实现批量业务逻辑
// TODO: 在此实现单条业务逻辑

// KafkaConsumerSample.java
// TODO: 在此处实现业务逻辑
// TODO: 可以在此实现自定义异常处理
```

**Recommendation:** Leave as-is - these are intentional examples for developers.

---

## Implementation Roadmap

### Week 1: Critical Fixes

**Day 1-2: P1 Critical Fixes**
- ✅ Fix 4 SpotBugs critical issues
- ✅ Decide PMD strategy (recommend Option A)
- ✅ Update `config/pmd/ruleset.xml`
- ✅ Run build to verify fixes

**Day 3: P2 High Priority**
- ✅ Suppress Lombok EI/EI2 violations
- ✅ Run all tests
- ✅ Validate architecture with ArchUnit

**Day 4-5: Verification**
- ✅ Full build validation
- ✅ Generate coverage report
- ✅ Document decisions

**Deliverables:**
- ✅ Successful build
- ✅ All tests passing
- ✅ Architecture validated
- ✅ Updated PMD configuration

### Week 2-4: Technical Debt (Optional)

Only if choosing Option B (Fix All PMD):
- Week 2: Fix foundation:core violations (530)
- Week 3: Fix foundation:mq and cache (255)
- Week 4: Fix remaining modules (254)

**OR**

Focus on test coverage expansion:
- Week 2: Foundation module tests
- Week 3: Service layer tests
- Week 4: Controller and integration tests

### Month 2+: Continuous Improvement

- Monthly: Review SonarQube quality trends
- Quarterly: Increase JaCoCo thresholds
- Ongoing: Add tests for new features

---

## Success Metrics

### Short-term (Week 1)

- [ ] Build completes successfully (❌ → ✅)
- [ ] Zero critical SpotBugs issues (4 → 0)
- [ ] Tests execute and pass (N/A → ✅)
- [ ] Architecture validated (N/A → ✅)
- [ ] Deployable JAR created (❌ → ✅)

### Medium-term (Month 1)

If Option A (Relax PMD):
- [ ] PMD violations reduced to 0 (by excluding rules)
- [ ] SpotBugs violations = 0
- [ ] Test coverage baseline established

If Option B (Fix All):
- [ ] PMD violations reduced to <100 (1039 → <100)
- [ ] SpotBugs violations = 0
- [ ] Test coverage >50%

### Long-term (Month 3+)

- [ ] Test coverage >70%
- [ ] All modules have tests
- [ ] SonarQube quality gate: GREEN
- [ ] Technical debt ratio <5%
- [ ] AdminApplicationTest enabled

---

## Estimated Total Effort

| Approach | Effort | Timeline |
|----------|--------|----------|
| **Option A (Relax PMD)** | 10-15 hours | 1 week |
| **Option B (Fix All)** | 30-40 hours | 3-4 weeks |
| **Option C (Hybrid)** | 15-20 hours | 1-2 weeks |

**Recommendation:** Option A for immediate production readiness, then invest in test coverage expansion.

---

## Conclusion

The SmartAdmin backend has a **solid foundation** with excellent compilation, Checkstyle compliance, and comprehensive quality infrastructure. The build failure is due to **overly strict PMD configuration** (97% style violations) and a **small number of actual bugs** (3% critical SpotBugs issues).

**Recommended Path Forward:**

1. ✅ **Fix 4 critical SpotBugs bugs** (1 hour)
2. ✅ **Relax PMD rules** to match SmartAdmin coding style (1 hour)
3. ✅ **Suppress Lombok EI/EI2 warnings** (30 minutes)
4. ✅ **Run tests and validate architecture** (30 minutes)
5. ✅ **Deploy to production** with confidence

This approach unblocks builds immediately while maintaining high code quality standards. Focus future efforts on **expanding test coverage** rather than fixing cosmetic PMD style violations.

---

**Questions or Need Clarification?**

Review the detailed explanations above or ask for specific code examples for any recommendation.
