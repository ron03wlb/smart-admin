---
trigger: always_on
description: SonarQube Code Quality Rules
tags: [sonarqube, code-quality, static-analysis]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - security/07-owasp-top10-part1.md
  - quality-tools/11-checkstyle-rules.md
  - quality-tools/12-pmd-rules.md
  - quality-tools/13-spotbugs-rules.md
last_updated: 2025-01-21
---

# SonarQube Rules Configuration

## Must-Enable Blocker/Critical Rules

### Security Vulnerability
| Rule ID | Name              | Type          |
| ------- | ----------------- | ------------- |
| S3649   | SQL Injection     | Vulnerability |
| S5131   | XSS Prevention    | Vulnerability |
| S2076   | Command Injection | Vulnerability |
| S5135   | Deserialization   | Vulnerability |
| S2755   | XXE Vulnerability | Vulnerability |

### Bug
| Rule ID | Name             | Description           |
| ------- | ---------------- | --------------------- |
| S2259   | Null Pointer     | Potential NPE         |
| S2095   | Resources Closed | Resource leak         |
| S1143   | Jump in finally  | Prohibit return in finally |

### Code Smell
| Rule ID | Name                 | Threshold |
| ------- | -------------------- | --------- |
| S3776   | Cognitive Complexity | ≤ 15      |
| S1192   | String Duplication   | ≥ 3 times |
| S1481   | Unused Variables     | 0         |

## Spring-Specific Rules (2024-2025)

### Must Enable
```yaml
rules:
  S4684: ERROR  # Prohibit Entity as RequestMapping parameter
  S4288: ERROR  # Must use constructor injection
  S2229: ERROR  # @Transactional self-invocation issue
  S2230: ERROR  # @Transactional method must be public
  S4601: ERROR  # Security URL match order
  S4602: ERROR  # Prohibit default package
```

### Code Examples

#### S4684 - Entity Exposure
```java
// ❌ Incorrect - Entity directly exposed
@PostMapping("/user")
public void createUser(@RequestBody User user) { }

// ✅ Correct - Use DTO
@PostMapping("/user")
public void createUser(@RequestBody UserCreateDTO dto) { }
```

#### S4288 - Constructor Injection
```java
// ❌ Incorrect - Field injection
@Service
public class UserService {
    @Autowired
    private UserRepository repository;
}

// ✅ Correct - Constructor injection
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
}
```

#### S4601 - Security URL Order
```java
// ❌ Incorrect - Wildcard first
http.authorizeRequests()
    .antMatchers("/admin/**").authenticated()
    .antMatchers("/admin/user").hasRole("ADMIN");

// ✅ Correct - Specific rules first
http.authorizeRequests()
    .antMatchers("/admin/user").hasRole("ADMIN")
    .antMatchers("/admin/**").authenticated();
```

## Quality Gate Configuration
```yaml
# New Code Standard (Clean as You Code)
conditions:
  - metric: new_reliability_rating
    operator: GREATER_THAN
    value: "1"  # A grade - No new bugs
  - metric: new_security_rating
    operator: GREATER_THAN
    value: "1"  # A grade - No new vulnerabilities
  - metric: new_coverage
    operator: LESS_THAN
    value: "80" # New code coverage ≥ 80%
  - metric: new_duplicated_lines_density
    operator: GREATER_THAN
    value: "3"  # Duplicated code ≤ 3%
```
