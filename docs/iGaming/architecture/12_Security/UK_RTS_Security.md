# UK RTS Security Architecture

> **Business Requirements**: [Compliance Standards Requirements](../../requirements/12_Security_Compliance/Compliance_Standards_Requirements.md)
> **Canonical Source**: [source-archive/12_System_Security/12-05](../../source-archive/12_System_Security/12-05_UK_RTS_Security.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Security Engineers, Compliance Officers

---

## 1. RTS 4 Security Requirements Mapping

### RTS 4.1 - Information Security Management

| Requirement | Description | Implementation |
|-------------|-------------|---------------|
| 4.1.1 | ISO 27001 Compliance | ISO 27001:2022 Mapping |
| 4.1.2 | Risk Assessment | Regular risk assessments |
| 4.1.3 | Security Policy | CLAUDE.md security guidelines |

### RTS 4.2 - Clock Synchronization

```java
@Configuration
public class ClockSyncConfig {

    /**
     * Configure NTP time synchronization
     * RTS requirement: All game records must use synchronized timestamps
     */
    @Bean
    public Clock synchronizedClock() {
        return Clock.systemUTC();
    }
}
```

**Configuration Requirements**:
- NTP server synchronization
- Maximum time drift < 1 second
- All logs use UTC

### RTS 4.3 - Environment Separation

| Environment | Purpose | Isolation |
|-------------|---------|-----------|
| Development (DEV) | Development testing | Fully isolated |
| Testing (UAT) | Acceptance testing | Isolated from production |
| Production (PROD) | Live operations | Strictest controls |

```yaml
spring:
  profiles:
    active: ${ENVIRONMENT:dev}

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${PROD_DB_URL}
```

### RTS 4.4 - Access Control

| Requirement | Implementation |
|-------------|---------------|
| Authentication | Sa-Token |
| Multi-Factor Authentication | TOTP/WebAuthn |
| Role-Based Permissions | RBAC |
| Audit Logging | Complete operation records |

### RTS 4.5 - Outsourced Development Controls

- Security requirements in contracts
- Code review
- Vulnerability scanning
- Security testing acceptance

### RTS 4.6 - Privileged Tool Controls

```java
@Aspect
@Component
@Slf4j
public class PrivilegedToolAudit {

    @Before("@annotation(AdminOnly)")
    public void auditAdminAction(JoinPoint joinPoint) {
        Long adminId = StpUtil.getLoginIdAsLong();
        String action = joinPoint.getSignature().toShortString();
        String params = Arrays.toString(joinPoint.getArgs());

        log.info("ADMIN_ACTION: adminId={}, action={}, params={}",
            adminId, action, params);

        auditLogService.logAdminAction(adminId, action, params);
    }
}
```

## 2. Communication Security

### TLS Requirements

| Requirement | Configuration |
|-------------|--------------|
| Minimum Version | TLS 1.2 (recommended 1.3) |
| Cipher Suites | Strong encryption only |
| Certificate | Valid CA-issued |

```yaml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3,TLSv1.2
    ciphers: TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256
```
