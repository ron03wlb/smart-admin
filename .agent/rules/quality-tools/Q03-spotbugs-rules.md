---
trigger: always_on
description: SpotBugs Bug Detection Standards
tags: [static-analysis, spotbugs, bug-detection, security]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - technology/patterns/P04-concurrency-rules.md
  - security/S01-owasp-top10-part1.md
spotbugs_rule: NP_NULL_ON_SOME_PATH,DM_STRING_CTOR,SQL_INJECTION,EI_EXPOSE_REP,EI_EXPOSE_REP2,ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD,CT_CONSTRUCTOR_THROW
last_updated: 2026-01-22
---

# SpotBugs Standards

**TL;DR**: SpotBugs detects potential bugs and security vulnerabilities. Configure MAX effort + LOW confidence, all detected issues must be fixed. Combined with FindSecBugs plugin for security scanning.

---

## 🤖 AI Directive Block

### When to Apply This Rule
- ✅ When generating any Java code
- ✅ During Code Review to check potential bugs
- ✅ When `./gradlew spotbugsMain` fails
- ✅ User asks about null safety or security vulnerabilities

### Mandatory Enforcement Checklist
- [ ] No potential NullPointerException
- [ ] Resources properly closed (try-with-resources)
- [ ] No SQL injection risks
- [ ] No hardcoded passwords/keys
- [ ] equals/hashCode correctly implemented

### AI Decision Tree
```
SpotBugs Violation → Identify Bug Type
  ├─ NP_* (Null Pointer)
  │   └─ Use Vavr Option or add null check
  ├─ RCN_* (Redundant Null Check)
  │   └─ Remove redundant check
  ├─ OBL_* (Obligatory Resource Close)
  │   └─ Use try-with-resources
  ├─ SQL_* (SQL Injection)
  │   └─ Use LambdaQueryWrapper or parameter binding
  └─ HARD_CODE_* (Hardcoded Secrets)
      └─ Move to configuration file or environment variable
```

### Error Pattern Detection and Auto-Fix

#### NP_NULL_ON_SOME_PATH - Possible Null Pointer
```java
// ❌ Violation
public String getUserEmail(Long id) {
    User user = userMapper.selectById(id);
    return user.getEmail();  // user might be null
}

// ✅ Fix - Use Vavr Option
public Option<String> getUserEmail(Long id) {
    return Option.of(userMapper.selectById(id))
        .map(User::getEmail);
}

// ✅ Fix - Traditional approach
public String getUserEmail(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        throw new NotFoundException("User not found");
    }
    return user.getEmail();
}
```

#### OBL_UNSATISFIED_OBLIGATION - Resource Not Closed
```java
// ❌ Violation
public String readFile(String path) throws IOException {
    InputStream is = new FileInputStream(path);
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    return reader.readLine();  // Resource not closed
}

// ✅ Fix - try-with-resources
public String readFile(String path) throws IOException {
    try (InputStream is = new FileInputStream(path);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
        return reader.readLine();
    }
}
```

#### SQL_INJECTION - SQL Injection
```java
// ❌ Violation
public List<User> findByName(String name) {
    String sql = "SELECT * FROM t_user WHERE name = '" + name + "'";
    return jdbcTemplate.query(sql, rowMapper);
}

// ✅ Fix - Use LambdaQueryWrapper
public List<User> findByName(String name) {
    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .eq(User::getName, name)
    );
}

// ✅ Fix - Use parameter binding
public List<User> findByName(String name) {
    String sql = "SELECT * FROM t_user WHERE name = ?";
    return jdbcTemplate.query(sql, rowMapper, name);
}
```

#### HARD_CODE_PASSWORD - Hardcoded Password
```java
// ❌ Violation
private static final String DB_PASSWORD = "mypassword123";

// ✅ Fix - Use configuration
@Value("${spring.datasource.password}")
private String dbPassword;
```

#### EI_EXPOSE_REP / EI_EXPOSE_REP2 - Lombok + DTO Pattern

**Scenario**: DTO/VO/Form/Config classes annotated with @Data/@Builder
**Violation Reason**: Getter returns mutable object reference, Setter/Builder directly stores parameter
**SmartAdmin Decision**: DTO/VO are data carriers, defensive copying is over-engineering
**Solution**: Exclude in `config/spotbugs/exclude.xml`
```xml
<Match>
    <Or>
        <Class name="~.*VO"/>
        <Class name="~.*DTO"/>
        <Class name="~.*Form"/>
        <Class name="~.*Properties"/>
        <Class name="~.*Config"/>
    </Or>
    <Or>
        <Bug pattern="EI_EXPOSE_REP"/>
        <Bug pattern="EI_EXPOSE_REP2"/>
    </Or>
</Match>
```

#### NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE - API Legitimate Usage

**Scenario**: CompletableFuture.getNow(null), Kafka send(topic, null, message)
**Violation Reason**: SpotBugs incorrectly flags these APIs as not accepting null
**SmartAdmin Decision**: CompletableFuture.getNow and Kafka allow null parameters
**Solution**: Exclude specific class NP violations
```xml
<Match>
    <Class name="net.lab1024.sa.common.mq.kafka.core.KafkaProducerServiceImpl"/>
    <Bug code="NP"/>
</Match>
```

#### ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD - Spring DI Bridge

**Scenario**: JsonUtil uses @PostConstruct to initialize static field
**Violation Reason**: Instance method writing to static field is usually a design issue
**SmartAdmin Decision**: Static utility class bridging Spring DI is standard pattern
**Solution**: Exclude specific method
```xml
<Match>
    <Class name="net.lab1024.sa.common.json.util.JsonUtil"/>
    <Method name="init"/>
    <Bug pattern="ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"/>
</Match>
```

#### CT_CONSTRUCTOR_THROW - Constructor Validation Pattern

**Scenario**: Constructor validates parameters and throws exception
**Violation Reason**: Theoretically possible finalizer attack
**SmartAdmin Decision**: Internal classes don't need protection, actual risk is zero
**Solution**: Globally exclude this rule
```xml
<Match>
    <Bug pattern="CT_CONSTRUCTOR_THROW"/>
</Match>
```

---

## [Mandatory] Enabled Rule Description

### Correctness

| Bug Pattern             | Description                      | Severity |
| ----------------------- | -------------------------------- | -------- |
| `NP_NULL_ON_SOME_PATH`  | Possible null pointer dereference | High     |
| `NP_NULL_PARAM_DEREF`   | Passing null to non-null parameter | High     |
| `EC_UNRELATED_TYPES`    | equals() parameter type mismatch | High     |
| `HE_EQUALS_NO_HASHCODE` | equals without corresponding hashCode | High     |

### Bad Practice

| Bug Pattern                  | Description                  | Severity |
| ---------------------------- | ---------------------------- | -------- |
| `OBL_UNSATISFIED_OBLIGATION` | Resource not properly closed | High     |
| `RCN_REDUNDANT_NULLCHECK`    | Redundant null check         | Medium   |
| `SE_NO_SERIALVERSIONID`      | Missing serialVersionUID     | Medium   |

### Security (FindSecBugs)

| Bug Pattern               | Description        | Severity |
| ------------------------- | ------------------ | -------- |
| `SQL_INJECTION`           | SQL injection vulnerability | Critical |
| `COMMAND_INJECTION`       | Command injection vulnerability | Critical |
| `PATH_TRAVERSAL_IN`       | Path traversal vulnerability | Critical |
| `XXE_DOCUMENT`            | XXE attack vulnerability | Critical |
| `XSS_REQUEST_WRAPPER`     | XSS vulnerability   | High     |
| `HARD_CODE_PASSWORD`      | Hardcoded password  | High     |
| `WEAK_MESSAGE_DIGEST_MD5` | Weak hash algorithm | High     |

---

## Configuration Description

### Gradle Configuration Location
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # SpotBugs plugin configuration
```

### build.gradle.kts Configuration
```kotlin
configure<com.github.spotbugs.snom.SpotBugsExtension> {
    toolVersion.set(libs.findVersion("spotbugs").get().toString())
    ignoreFailures.set(false)           // Fail on bug detection
    effort.set(com.github.spotbugs.snom.Effort.MAX)           // Maximum effort
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW) // Report low confidence
}
```

### Add FindSecBugs Plugin
```kotlin
dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")
}
```

### Exclude Specific Rules (If Needed)
Create `config/spotbugs/exclude.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <Match>
        <Package name="~.*\.generated\..*"/>
    </Match>
    <Match>
        <Bug pattern="SE_NO_SERIALVERSIONID"/>
        <Class name="~.*DTO"/>
    </Match>
</FindBugsFilter>
```

### SmartAdmin Actual Configuration
SmartAdmin project's complete exclusion configuration is located at [`config/spotbugs/exclude.xml`](../../smart-admin-api-java21-springboot3/config/spotbugs/exclude.xml), including the following patterns:

- **Lombok + Spring Pattern**: DTO/VO/Form/Properties/Config classes exclude EI_EXPOSE_REP
- **Spring DI Bridge**: JsonUtil's @PostConstruct pattern excludes ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD
- **API Legitimate Usage**: KafkaProducerServiceImpl excludes NP violations
- **Constructor Validation**: Globally exclude CT_CONSTRUCTOR_THROW
- **Generated Code**: MapStruct generated classes and target/generated-sources automatically excluded

Reference in `build.gradle.kts`:
```kotlin
configure<com.github.spotbugs.snom.SpotBugsExtension> {
    excludeFilter.set(rootProject.file("config/spotbugs/exclude.xml"))
}
```

---

## Common Violation Type Statistics

Based on actual project violation analysis:

| Violation Type                             | Fixed | Resolution Method | Configuration Location     |
| ------------------------------------------ | ----- | ----------------- | -------------------------- |
| `EI_EXPOSE_REP/EI_EXPOSE_REP2`             | 24    | exclude.xml exclusion | DTO/VO/Form/Config classes |
| `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`   | 2     | exclude.xml exclusion | KafkaProducerServiceImpl   |
| `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD`  | 1     | exclude.xml exclusion | JsonUtil.init()            |
| `CT_CONSTRUCTOR_THROW`                     | 1     | exclude.xml global exclusion | All constructors           |

---

## Verification Commands

```bash
# Gradle
./gradlew spotbugsMain spotbugsTest

# View report
open smartadmin-app/build/reports/spotbugs/main.html

# Check specific module only
./gradlew :smartadmin-app:spotbugsMain
```

---

## Related Specifications

- [07-owasp-top10-part1.md](./07-owasp-top10-part1.md) - Security Standards Part 1
- [07-owasp-top10-part2.md](./07-owasp-top10-part2.md) - Security Standards Part 2
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - Using Option to handle null
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - Quality gates workflow
