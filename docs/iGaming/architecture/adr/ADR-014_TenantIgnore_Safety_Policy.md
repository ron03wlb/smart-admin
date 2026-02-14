# ADR 014: @TenantIgnore 安全使用策略

**狀態**: ✅ 已採納（2026-02-13）

**決策者**: iGaming 技術團隊

**相關文檔**:
- [Multi_Tenant_Architecture.md](../06_Platform_Core/01_Multi_Tenant_Architecture.md) — Section 6.3 Tenant Bypass Safety Matrix
- [Multi_Tenant_Requirements.md](../../requirements/06_Governance_Licensing/01_Multi_Tenant_Requirements.md) — Section 5.2 跨租戶查詢白名單

---

## 背景

iGaming 平台的多租戶架構使用 `@TenantIgnore` 註解繞過租戶過濾器，用於品牌級別聚合查詢和跨 Brand 遷移。原始文件未明確記錄哪些場景允許使用此註解，存在安全風險：

- 錯誤使用可能暴露玩家 PII（違反 GDPR）
- 支付憑證可能被跨租戶訪問（違反 PCI-DSS）

---

## 決策

**@TenantIgnore 必須遵循白名單制度，僅在明確授權的場景中使用。**

### 允許場景

1. 品牌級別財務聚合報表（`t_transaction` 唯讀聚合）
2. 品牌級別玩家統計（`t_player` 唯讀計數，禁止返回 PII）
3. 全局遊戲供應商管理（`t_game_provider`）
4. 全局風控規則管理（`t_risk_rule_global`）
5. 跨 Brand 租戶遷移（`TenantMigrationManager` 內）

### 禁止場景

- 玩家 PII 明細查詢（`t_player` 含 email/phone）
- 錢包餘額直接訪問（`t_wallet`）
- KYC 文件訪問（`t_kyc_document`）
- 支付憑證訪問（`t_payment_credential`）

### 強制措施

1. **ArchUnit 測試**: `@TenantIgnore` 僅限 `*ReportService` 和 `*MigrationManager` 使用
2. **審計日誌**: 所有繞過操作必須記錄操作者、時間、範圍和資料量
3. **最小權限**: 僅 Super Admin 和 Brand Admin 角色可觸發

### 合規標準

- PCI-DSS v4 Req 7.2.1: 基於最小權限原則的訪問控制
- GDPR Art 25: 數據保護設計
- ISO 27001 A.9.2: 用戶訪問管理

---

## 後果

**正面**:
- 安全性：明確的白名單防止未授權的跨租戶數據洩漏
- 可審計性：ArchUnit 自動強制 + 審計日誌追蹤
- 合規性：滿足 PCI-DSS、GDPR、ISO 27001 要求

**負面**:
- 開發限制：新增跨租戶功能需要先更新白名單（但這是期望的行為）
