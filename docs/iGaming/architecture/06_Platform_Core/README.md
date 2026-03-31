# 06 平台核心（Platform Core）

> **目標讀者**: Architects, Backend Developers, DevOps
> **重點**: 多租戶隔離、RBAC 治理、MFA 安全、司法管轄區路由
> **最後更新**: 2026-03-31（MFA 文件已從 8 份合併為 4 份）

---

## 文件列表

| 文件 | 描述 | 狀態 |
|------|------|------|
| [01_Multi_Tenant_Architecture.md](01_Multi_Tenant_Architecture.md) | 行級安全（RLS）、MyBatis-Plus 租戶插件、資料隔離設計 | ✅ 最新 |
| [02_Governance_Implementation.md](02_Governance_Implementation.md) | Sa-Token 整合、RBAC 層級映射、MyBatis 攔截器加密、非同步審計 | ✅ 最新 |
| [03_MFA_Architecture.md](03_MFA_Architecture.md) | MFA 系統架構、STRIDE 威脅建模、CVSS 評分、**方法選擇評估**（合併自 03+05） | ✅ 合併 |
| [04_MFA_Implementation.md](04_MFA_Implementation.md) | TOTP RFC 6238 演算法、AES-256-GCM 加密、API 規格、DB Schema（合併自 04+06） | ✅ 合併 |
| [05_MFA_Compliance.md](05_MFA_Compliance.md) | 稽核日誌、備份碼加密、異常偵測、設備遺失恢復（合併自 07+08） | ✅ 合併 |
| [06_MFA_Recovery.md](06_MFA_Recovery.md) | 恢復流程、雙階段登入、信任裝置 Token（合併自 09+10） | ✅ 合併 |
| [07_Jurisdiction_Routing_Architecture.md](07_Jurisdiction_Routing_Architecture.md) | 多司法管轄區路由、地理限制、牌照感知請求處理 | ✅ 最新 |

---

## MFA 文件結構說明（整理前 → 後）

```
整理前（8 個 MFA 文件）               整理後（4 個 MFA 文件）
03_MFA_Technical_Architecture.md  ┐
05_MFA_Technical_Evaluation.md    ┘ → 03_MFA_Architecture.md

04_MFA_Technical.md               ┐
06_TOTP_WebAuthn_Implementation.md┘ → 04_MFA_Implementation.md

07_MFA_Compliance_Technical.md    ┐
08_MFA_Compliance_Validation.md   ┘ → 05_MFA_Compliance.md

09_MFA_Recovery_Implementation.md ┐
10_MFA_Login_Recovery_Technical.md┘ → 06_MFA_Recovery.md
```

---

## 相關需求文件（requirements/06_Governance_Licensing/）

- [03_MFA_Architecture_Spec.md](../../requirements/06_Governance_Licensing/03_MFA_Architecture_Spec.md) — MFA 設計需求
- [04_MFA_Requirements.md](../../requirements/06_Governance_Licensing/04_MFA_Requirements.md) — MFA 業務規則
- [05_MFA_Compliance_Requirements.md](../../requirements/06_Governance_Licensing/05_MFA_Compliance_Requirements.md) — MFA 合規要求
- [06_MFA_Recovery_Requirements.md](../../requirements/06_Governance_Licensing/06_MFA_Recovery_Requirements.md) — MFA 恢復程序
