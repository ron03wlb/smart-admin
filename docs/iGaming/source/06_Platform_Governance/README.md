# 06_Platform_Governance - 平台治理

**狀態**: ✅ 完成（v4.1.0）
**最後更新**: 2026-02-07

---

## 📋 模塊職責

租戶管理、權限安全、數據安全、審批工作流、**多牌照合規**。

**核心內容**：
- 多租戶架構（**SSOT**）
- RBAC 權限管理
- 審計日誌系統
- MFA 雙因素認證
- 審批工作流
- **多司法管轄區合規框架** 🆕
- **UKGC/MGA/PAGCOR 牌照合規** 🆕

**職責邊界**：
- ✅ 包含：租戶管理、權限、安全、審批、牌照合規
- ❌ 不包含：技術運維（在 09_Technical_Infrastructure）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 | 狀態 |
|------|---------|------|--------|------|
| 06-01 | [Multi_Tenant_Architecture.md](06-01_Multi_Tenant_Architecture.md) | 多租戶架構 | P0 | ✅ |
| 06-02 | [RBAC_Permissions.md](06-02_RBAC_Permissions.md) | RBAC 權限 | P0 | ✅ |
| 06-03 | [Audit_Log.md](06-03_Audit_Log.md) | 審計日誌 | P0 | ✅ |
| 06-04 | [Approval_Workflow.md](06-04_Approval_Workflow.md) | 審批工作流 | P1 | ✅ |
| 06-05 | [Data_Security.md](06-05_Data_Security.md) | 數據安全治理 | P0 | ✅ |
| 06-06 | [MFA_Implementation.md](06-06_MFA_Implementation.md) | MFA 實現 | P0 | ✅ |
| 06-07 | [Multi_Jurisdiction_Framework.md](06-07_Multi_Jurisdiction_Framework.md) | 多牌照合規框架 | P0 | 🆕 ✅ |
| 06-08 | [UKGC_Compliance.md](06-08_UKGC_Compliance.md) | UK 牌照合規 | P0 | 🆕 ✅ |
| 06-09 | [MGA_Compliance.md](06-09_MGA_Compliance.md) | Malta 牌照合規 | P1 | 🆕 ✅ |
| 06-10 | [Brazil_SPA_Compliance.md](06-10_Brazil_SPA_Compliance.md) | Brazil 牌照合規 | P1 | 🆕 ✅ |
| 06-11 | [PAGCOR_Curacao.md](06-11_PAGCOR_Curacao.md) | 亞洲/離岸牌照 | P2 | 🆕 ✅ |
| 06-12 | [Compliance_Timeline.md](06-12_Compliance_Timeline.md) | 合規時間線追蹤 | P0 | 🆕 ✅ |

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| **多租戶隔離策略** | 06-01 Multi_Tenant_Arch | §2.1 |
| **牌照配置中心** | 06-07 Multi_Jurisdiction | §2.1 |
| **UKGC LCCP 映射** | 06-08 UKGC_Compliance | §5.1 |
| **合規時間線追蹤** | 06-12 Compliance_Timeline | 全文 |

---

## 🔗 核心依賴

```text
06_Platform_Governance
    ↓
    ├─► 15_Responsible_Gambling - 負責任博彩（Gamstop 整合）
    ├─► 12_System_Security - ISO 27001、UK RTS 安全標準
    ├─► 05_Risk_Control - KYC/AML 合規
    └─► 03_Game_Center - GLI RNG 認證
```

---

**索引版本**: 2.0.0
**創建日期**: 2026-02-07
**維護團隊**: Compliance Team & Platform Team
