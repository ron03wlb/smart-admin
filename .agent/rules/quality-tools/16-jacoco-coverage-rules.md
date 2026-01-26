---
trigger: on_test_failure
description: JaCoCo Test Coverage Standards
tags: [testing, coverage, jacoco, quality-gate]
positioning: current-standard
ai_role: test_generator
auto_apply: false
ask_before_fix: true
related_rules:
  - foundation/10-architecture-rules.md
prerequisites:
  - workflows/tdd-workflow.md
last_updated: 2025-01-21
---

# JaCoCo Test Coverage Standards

**TL;DR**: JaCoCo measures test coverage, mandating line coverage ≥80%, branch coverage ≥70%. New code must have corresponding tests.

---

## 🤖 AI Directive Block

### When to Apply This Rule
- ✅ When creating new Service/Manager classes
- ✅ When `./gradlew jacocoTestCoverageVerification` fails
- ✅ During Code Review to check test coverage
- ✅ User asks about test coverage issues

### Mandatory Enforcement Checklist
- [ ] Line coverage ≥ 80%
- [ ] Branch coverage ≥ 70%
- [ ] New code has corresponding unit tests
- [ ] Exception branches have test coverage
- [ ] Service layer methods fully covered

### AI Decision Tree
```
Coverage Insufficient → Identify Uncovered Area
  ├─ Service method not tested
  │   └─ Create corresponding unit test
  ├─ Exception branch not covered
  │   └─ Add exception scenario test
  ├─ Conditional branch not covered
  │   └─ Add boundary condition test
  └─ Code not requiring tests
      └─ Add to exclusion configuration
```

### Test Template

#### Service Unit Test Template
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        // Given
        User user = User.builder().id(1L).name("Test").build();
        when(userMapper.selectById(1L)).thenReturn(user);

        // When
        Option<User> result = userService.findById(1L);

        // Then
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().getName()).isEqualTo("Test");
    }

    @Test
    void findById_shouldReturnNone_whenUserNotExists() {
        // Given
        when(userMapper.selectById(1L)).thenReturn(null);

        // When
        Option<User> result = userService.findById(1L);

        // Then
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void findById_shouldThrow_whenIdIsNull() {
        // Then
        assertThatThrownBy(() -> userService.findById(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

## [Mandatory] Coverage Standards

| Metric               | Minimum | Target |
| -------------------- | ------- | ------ |
| Line Coverage        | ≥ 80%   | ≥ 85%  |
| Branch Coverage      | ≥ 70%   | ≥ 75%  |
| Method Coverage      | ≥ 75%   | ≥ 80%  |
| Class Coverage       | ≥ 90%   | ≥ 95%  |

### Layered Coverage Requirements

| Layer          | Line Coverage | Description            |
| -------------- | ------------- | ---------------------- |
| Service Layer  | ≥ 85%         | Core business logic    |
| Manager Layer  | ≥ 80%         | Transaction/cache logic |
| Controller Layer | ≥ 60%       | HTTP handling (can be lower) |
| DTO/VO/Entity  | Excluded      | Pure data classes      |
| Config         | Excluded      | Configuration classes  |

---

## Configuration Description

### Gradle Configuration Location
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # JaCoCo plugin configuration
```

### build.gradle.kts Configuration
```kotlin
subprojects {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = libs.findVersion("jacoco").get().toString()
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)   // Required for SonarQube
            html.required.set(true)  // For manual review
        }
    }

    tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn("test")

        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn("jacocoTestCoverageVerification")
    }
}
```

### Exclusion Configuration
```kotlin
tasks.withType<JacocoReport> {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/domain/**",      // Entity
                    "**/dto/**",         // DTO
                    "**/vo/**",          // VO
                    "**/config/**",      // Config classes
                    "**/constant/**",    // Constant classes
                    "**/*Application*",  // Startup class
                    "**/*Config*"        // Config classes
                )
            }
        }))
    }
}
```

---

## Report Interpretation

### Report Location
```
sa-admin/build/reports/jacoco/test/html/index.html
```

### Color Meaning
- 🟢 **Green**: Covered
- 🔴 **Red**: Not covered
- 🟡 **Yellow**: Partially covered (branches)

### Common Issues

#### Insufficient Branch Coverage
```java
// Original code - only tested true branch
public String getStatus(boolean active) {
    if (active) {        // 🟡 Partially covered
        return "ACTIVE";
    }
    return "INACTIVE";   // 🔴 Not covered
}

// Add test
@Test
void getStatus_shouldReturnInactive_whenNotActive() {
    assertThat(service.getStatus(false)).isEqualTo("INACTIVE");
}
```

#### Exception Branch Not Covered
```java
// Original code
public User findById(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {       // 🔴 Exception branch not covered
        throw new NotFoundException("User not found");
    }
    return user;
}

// Add test
@Test
void findById_shouldThrow_whenUserNotFound() {
    when(userMapper.selectById(1L)).thenReturn(null);

    assertThatThrownBy(() -> service.findById(1L))
        .isInstanceOf(NotFoundException.class);
}
```

---

## Verification Commands

```bash
# Run tests and generate coverage report
./gradlew test jacocoTestReport

# View coverage report
open sa-admin/build/reports/jacoco/test/html/index.html

# Verify coverage threshold
./gradlew jacocoTestCoverageVerification

# Full verification
./gradlew check

# Maven (if used)
mvn clean test jacoco:report
mvn jacoco:check -Djacoco.minimum=0.80
```

---

## SonarQube Integration

JaCoCo reports can be uploaded to SonarQube:

```kotlin
sonarqube {
    properties {
        property("sonar.coverage.jacoco.xmlReportPaths",
            "${buildDir}/reports/jacoco/test/jacocoTestReport.xml")
    }
}
```

---

## Related Specifications

- [06-sonarqube-rules.md](./06-sonarqube-rules.md) - SonarQube quality gates
- [workflows/tdd-workflow.md](../workflows/tdd-workflow.md) - TDD workflow
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - Quality gates workflow
