---
title: "ADR-014: @TenantIgnore 白名單"
status: accepted
date: 2026-02-20
deciders: Security Team, Architecture Team
---

# ADR-014: @TenantIgnore 白名單

## 狀態

已接受 (Accepted)

## 背景

多租戶架構中，`TenantLineInnerInterceptor` 自動為所有查詢注入 `WHERE tenant_id = ?` 條件。但某些跨租戶操作（如平台級報表、全域遊戲管理）需要繞過租戶隔離。

直接使用 `@TenantIgnore` 繞過隔離存在安全風險——若開發者濫用可能導致跨租戶資料洩漏。

## 決策

### 白名單制度

`@TenantIgnore` 僅允許用於以下場景，違反者由 ArchUnit 自動攔截：

### 允許的場景

| 場景 | 類名模式 | 條件 |
|------|---------|------|
| 品牌級報表 | `*ReportService` | 只讀 (SELECT) |
| 全域遊戲供應商管理 | `*GameProviderManager` | 平台管理員角色 |
| 全域風控規則 | `*GlobalRiskRuleManager` | 平台管理員角色 |
| 跨品牌遷移 | `*MigrationManager` | 需審批工單 |

### 禁止的場景

| 場景 | 原因 |
|------|------|
| PII 查詢 | GDPR 合規風險 |
| 錢包存取 | 財務安全風險 |
| KYC 文件存取 | 資料保護風險 |
| 支付憑證存取 | PCI-DSS 合規風險 |

### 實作

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantIgnore {
    String reason();  // 必須填寫理由
}

// 使用範例
@Component
public class GlobalGameProviderManager {

    @TenantIgnore(reason = "Platform-level GP management, no tenant-specific data")
    public List<GameProviderEntity> listAllProviders() {
        return gameProviderDao.selectList(null);
    }
}
```

### ArchUnit 驗證

```java
@ArchTest
static ArchRule tenantIgnoreWhitelist =
    methods().that().areAnnotatedWith(TenantIgnore.class)
        .should().beDeclaredInClassesThat()
        .haveSimpleNameEndingWith("ReportService")
        .orShould().beDeclaredInClassesThat()
        .haveSimpleNameEndingWith("MigrationManager")
        .orShould().beDeclaredInClassesThat()
        .haveSimpleNameContaining("GlobalRiskRule")
        .orShould().beDeclaredInClassesThat()
        .haveSimpleNameContaining("GameProviderManager")
        .because("ADR-014: @TenantIgnore is only allowed in whitelisted classes");
```

### 審計

所有 `@TenantIgnore` 方法呼叫自動記錄審計日誌：

```java
@Aspect
@Component
public class TenantIgnoreAuditAspect {
    @Around("@annotation(tenantIgnore)")
    public Object audit(ProceedingJoinPoint pjp, TenantIgnore tenantIgnore) throws Throwable {
        auditLog.info("TenantIgnore invoked: method={}, reason={}, caller={}",
            pjp.getSignature(), tenantIgnore.reason(),
            SecurityContextHolder.getContext().getAuthentication().getName());
        return pjp.proceed();
    }
}
```

## 後果

- 正向：跨租戶操作受控且可審計
- 正向：ArchUnit 自動阻止未經授權的使用
- 正向：每次使用都有理由記錄
- 負向：新增跨租戶需求需更新白名單（流程成本）
