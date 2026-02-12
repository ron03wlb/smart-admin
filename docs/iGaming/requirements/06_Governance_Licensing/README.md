# 06 治理與授權（Governance Licensing）

> **受眾（Audience）**: 高階主管、產品經理、合規官
> **狀態（Status）**: Phase 6 完成 - 6 份拆分文件 + 來源索引

---

## 拆分文件（Split Documents）

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| [多租戶需求（Multi-Tenant Requirements）](Multi_Tenant_Requirements.md) | 租戶層級、數據隔離、計費模型 | [source](../../source-archive/06_Platform_Governance/06-01_Multi_Tenant.md) |
| [MFA 需求](MFA_Requirements.md) | 身份驗證政策、合規要求 | [source](../../source-archive/06_Platform_Governance/06-06_MFA_Implementation.md) |
| [MFA 合規需求（MFA Compliance Requirements）](MFA_Compliance_Requirements.md) | MFA 審計政策、監管要求、合規檢查清單 | [source](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md) |
| [MFA 恢復需求（MFA Recovery Requirements）](MFA_Recovery_Requirements.md) | 恢復選項、身份驗證、帳戶恢復合規 | [source](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md) |
| [MFA 架構規範（MFA Architecture Spec）](MFA_Architecture_Spec.md) | MFA 方法選擇標準、驗證政策、用戶體驗 | [source](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md) |
| [治理需求（Governance Requirements）](Governance_Requirements.md) | 多租戶政策、RBAC 要求、審計日誌、數據加密標準 | [source](../../source-archive/00_Foundation/guides/00-15_Governance_Implementation.md) |

## 來源索引（Source Index）

### 存取控制與權限

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| RBAC 權限 (RBAC Permissions) | 基於角色的存取控制、權限管理 | [source](../../source-archive/06_Platform_Governance/06-02_RBAC_Permissions.md) |
| 審計日誌 (Audit Log) | 系統審計日誌、合規軌跡 | [source](../../source-archive/06_Platform_Governance/06-03_Audit_Log.md) |
| 審批工作流 (Approval Workflow) | 多級審批流程、授權鏈 | [source](../../source-archive/06_Platform_Governance/06-04_Approval_Workflow.md) |
| 數據安全 (Data Security) | 數據保護、加密、存取控制 | [source](../../source-archive/06_Platform_Governance/06-05_Data_Security.md) |

### 多因素身份驗證

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| MFA 架構 (MFA Architecture) | 多因素身份驗證設計 | [source](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md) |
| TOTP/WebAuthn | 基於時間的 OTP 和 WebAuthn 實施 | [source](../../source-archive/06_Platform_Governance/06-06-02_TOTP_WebAuthn.md) |
| MFA 恢復流程 (MFA Recovery Flow) | 帳戶恢復程序 | [source](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md) |
| MFA 合規審計 (MFA Compliance Audit) | MFA 合規和審計要求 | [source](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md) |

### 授權與司法管轄區

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| 合規時間表 (Compliance Timeline) | 監管合規截止日期和里程碑 | [source](../../source-archive/06_Platform_Governance/06-12_Compliance_Timeline.md) |
| 多司法管轄區申報 (Multi-Jurisdiction Filing) | 跨司法管轄區監管申報對齊 | [source](../../source-archive/06_Platform_Governance/06-13_Multi_Jurisdiction_Filing_Alignment.md) |
| 巴西 SPA 合規 (Brazil SPA Compliance) | 巴西 SPA 監管要求 | [source](../../source-archive/06_Platform_Governance/06-10_Brazil_SPA_Compliance.md) |
| PAGCOR/Curacao | PAGCOR 和 Curacao 授權要求 | [source](../../source-archive/06_Platform_Governance/06-11_PAGCOR_Curacao.md) |

### 代理中心

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| 信用網絡邏輯 (Credit Network Logic) | 代理信用網絡、分層信用管理 | [source](../../source-archive/07_Agent_Center/07-02_Credit_Network_Logic.md) |
| 代理系統 (Agent System) | 代理管理、佣金結構 | [source](../../source-archive/07_Agent_Center/07-03_Agent_System.md) |

---

**最後更新（Last Updated）**: 2026-02-08
