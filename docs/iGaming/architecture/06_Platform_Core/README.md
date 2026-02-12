# 06 平台核心（Platform Core）

> **目標讀者**: Architects, Backend Developers, DevOps
> **狀態**: Phase 6 完成 - 8 份拆分文件 + 來源索引

---

## 拆分文件（Split Documents）

| 文件 | 說明 | 來源 |
|------|------|------|
| [Multi-Tenant Architecture](Multi_Tenant_Architecture.md) | 行級安全、MyBatis-Plus 租戶插件、資料隔離設計 | [source](../../source-archive/06_Platform_Governance/06-01_Multi_Tenant.md) |
| [MFA Technical](MFA_Technical.md) | TOTP/WebAuthn 實作、復原流程、合規驗證 | [source](../../source-archive/06_Platform_Governance/06-06_MFA_Implementation.md) |
| [MFA Compliance Validation](MFA_Compliance_Validation.md) | 備用碼演算法、審計日誌結構、異常偵測查詢 | [source](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md) |
| [MFA Recovery Implementation](MFA_Recovery_Implementation.md) | 復原流程狀態機、雙階段登入、信任裝置管理 | [source](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md) |
| [Jurisdiction Routing Architecture](Jurisdiction_Routing_Architecture.md) | 多司法管轄區路由、地理限制、牌照感知請求處理 | [source](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) |
| [MFA Technical Architecture](MFA_Technical_Architecture.md) | MFA 系統架構、STRIDE 威脅建模、CVSS 評分 | [source](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md) |
| [TOTP WebAuthn Implementation](TOTP_WebAuthn_Implementation.md) | TOTP RFC 6238 演算法、AES-256-GCM 加密、WebAuthn 證明 | [source](../../source-archive/06_Platform_Governance/06-06-02_TOTP_WebAuthn.md) |
| [Governance Implementation](Governance_Implementation.md) | SmartAdmin 層級映射、Sa-Token 整合、MyBatis 攔截器加密、非同步審計 | [source](../../source-archive/00_Foundation/guides/00-15_Governance_Implementation.md) |

## 來源索引（Source Index）

| 文件 | 說明 | 來源 |
|------|------|------|
| RBAC Implementation | 基於角色的存取控制系統設計 | [source](../../source-archive/06_Platform_Governance/06-02_RBAC_Permissions.md) |
| Audit Log | 系統審計日誌架構 | [source](../../source-archive/06_Platform_Governance/06-03_Audit_Log.md) |
| Approval Workflow | 多級審批系統 | [source](../../source-archive/06_Platform_Governance/06-04_Approval_Workflow.md) |
| Tenant Configuration | 租戶配置管理 | [source](../../source-archive/10_Platform_Management/10-02_Tenant_Configuration.md) |

---

**最後更新**: 2026-02-08
