# 12-05 UK RTS Security (UK RTS 安全標準)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

UK Gambling Commission Remote Technical Standards (RTS) Section 4 定義了遠程博彩系統的安全要求。

---

## RTS 4 安全要求映射

### RTS 4.1 - 資訊安全管理

| 要求 | 說明 | SmartAdmin 實現 |
|------|------|----------------|
| 4.1.1 | ISO 27001 合規 | [12-04 ISO映射](12-04_ISO27001_2022_Mapping.md) |
| 4.1.2 | 風險評估 | 定期進行 |
| 4.1.3 | 安全政策 | CLAUDE.md 安全指南 |

### RTS 4.2 - 時鐘同步

所有系統必須使用同步的時間源：

```java
@Configuration
public class ClockSyncConfig {

    /**
     * 配置 NTP 時間同步
     * RTS 要求：所有遊戲記錄必須使用同步的時間戳
     */
    @Bean
    public Clock synchronizedClock() {
        // 使用 UTC 時區
        return Clock.systemUTC();
    }
}
```

**配置要求**:
- NTP 服務器同步
- 最大時間偏差 < 1 秒
- 所有日誌使用 UTC

### RTS 4.3 - 環境分離

| 環境 | 用途 | 隔離要求 |
|------|------|---------|
| 開發 (DEV) | 開發測試 | 完全隔離 |
| 測試 (UAT) | 驗收測試 | 與生產隔離 |
| 生產 (PROD) | 實際運營 | 最嚴格控制 |

**實現**:
```yaml
# 環境配置
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
    # 生產環境使用獨立的資料庫
```

### RTS 4.4 - 訪問控制

| 要求 | 實現 |
|------|------|
| 身份驗證 | Sa-Token |
| 多因素認證 | TOTP/WebAuthn |
| 角色權限 | RBAC |
| 審計日誌 | 完整操作記錄 |

### RTS 4.5 - 外包開發控制

當使用第三方開發時：
- 安全要求合約條款
- 程式碼審查
- 漏洞掃描
- 安全測試驗收

### RTS 4.6 - 特權工具控制

管理員工具需要：
- 獨立認證
- 操作審計
- 使用限制

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

        // 記錄到審計日誌
        auditLogService.logAdminAction(adminId, action, params);
    }
}
```

---

## 通訊安全

### TLS 要求

| 要求 | 配置 |
|------|------|
| 最低版本 | TLS 1.2 (建議 1.3) |
| 加密套件 | 強加密 |
| 證書 | 有效的 CA 簽發 |

```yaml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3,TLSv1.2
    ciphers: TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256
```

---

## 相關文檔

- [12-04_ISO27001_2022_Mapping.md](12-04_ISO27001_2022_Mapping.md) - ISO 映射
- [06-06_MFA_Implementation.md](../06_Platform_Governance/06-06_MFA_Implementation.md) - MFA
- [06-03_Audit_Log.md](../06_Platform_Governance/06-03_Audit_Log.md) - 審計日誌

---

**返回**: [系統安全](README.md) | [iGaming 首頁](../README.md)
