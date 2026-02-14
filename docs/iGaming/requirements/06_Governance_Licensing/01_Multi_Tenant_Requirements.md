# Multi-Tenant 需求（多租戶需求）

> **規範來源**: [06-01_Multi_Tenant.md](../../source-archive/06_Platform_Governance/06-01_Multi_Tenant.md)
> **目標讀者**: 高階主管、產品經理
> **相關架構**: [Multi_Tenant_Architecture.md](../../architecture/06_Platform_Core/01_Multi_Tenant_Architecture.md)
> **最後同步**: 2026-02-08

---

## 業務價值（Business Value）

此 Multi-Tenant 架構為業務提供價值：
- **可擴展性 (Scalability)**：在單一平台基礎設施上支援多個 Brand 和 Tenant，降低每個站點的運營開銷
- **靈活變現模式 (Flexible Monetization)**：啟用三種計費模式（固定月費、營收分成、混合模式）以適應不同客戶規模和商業模式
- **White-Label 能力**：允許每個 Tenant 自訂域名、品牌和 Email 模板，使新站點能快速進入市場
- **階層式控制 (Hierarchical Control)**：提供 Super Admin 全局監督、Brand 層級聚合和 Tenant 層級獨立性，平衡治理與運營自主性
- **監管合規 (Regulatory Compliance)**：強制執行嚴格的 Tenant 數據隔離，同時支援 Brand 範圍的 SSO 和跨 Tenant 報表（適用於集團運營商）

## 驗收標準（Acceptance Criteria）

- [ ] **Tenant 階層強制執行**：系統正確強制執行 4 層階層（Super Admin → Brand → Tenant → Agent），權限範圍定義於第 4.1 節
- [ ] **數據隔離驗證**：玩家數據、交易和遊戲會話嚴格按 tenant_id 隔離，無跨 Tenant 數據洩漏（第 5.1 節）
- [ ] **模擬模式 (Impersonation Mode)**：Super Admin 可模擬任何 Tenant，並在整個會話期間顯示清晰的視覺指示器（第 3.1 節）
- [ ] **White-Label 配置**：每個 Tenant 可獨立配置域名、Logo、色彩主題、Email 模板和貨幣設定（第 6.1 節）
- [ ] **計費週期執行**：每月計費週期在每月 1 日執行，支援所有三種計費模式（固定、營收分成、混合），並根據 31 天升級時程處理逾期帳號（第 7.2、7.3 節）
- [ ] **跨 Brand 轉移**：Tenant 從 Brand A 遷移至 Brand B 時保留所有玩家數據、交易歷史、錢包餘額和審計追蹤（依第 8.1 節檢查清單）
- [ ] **SSO 配置**：Brand 範圍的 SSO 支援所有三種模式（禁用、相同錢包、獨立錢包），錢包餘額和 VIP 狀態正確按 Tenant 範圍界定（第 9.2 節）
- [ ] **權限矩陣合規**：所有操作（建立 Brand、建立 Tenant、全局遊戲開關等）按第 4.2 節表格定義強制執行權限

---

## 1. Tenant 階層定義

系統採用 **SaaS (Software as a Service)** Multi-Tenant 架構，具有以下階層：

**Super Admin (Platform，平台) -> Brand (Group/Conglomerate，集團) -> Tenant (Merchant/Site，商戶/站點) -> Agent（代理）**

---

## 2. 階層需求

### 2.1 Super Admin (Platform Administrator，平台管理員)

- **權限**：上帝視角，可見所有 Brand 和 Tenant 數據
- **功能**：
  - 建立新 Brand
  - 全局遊戲開關（Game Provider 的緊急停機開關）
  - 全局風控規則設定

### 2.2 Brand (Group/Conglomerate，集團)

- **定義**：代表一個運營集團，可擁有多個不同域名的站點（Tenant）
- **共享資源**：
  - Brand 下的 Tenant 可選擇性共享「玩家黑名單」
  - 財務額度通常在 Brand 層級管理

### 2.3 Tenant (Merchant/Site，商戶/站點)

- **定義**：實際運營的網站，具有獨立域名、Logo 和前端樣式
- **數據隔離**：Tenant A 的玩家無法登入 Tenant B（除非配置了集團範圍的 SSO）
- **配置獨立性**：
  - 獨立的支付網關商戶 ID
  - 獨立的遊戲選擇
  - 獨立的促銷活動

---

## 3. 跨階層管理需求

### 3.1 視角切換

- Super Admin 可「模擬 (Impersonate)」（模擬）進入任何 Tenant 的後台進行操作
- 在模擬模式時需要視覺指示器

### 3.2 報表聚合

- **Tenant 層級報表**：單一站點損益
- **Brand 層級報表**：集團總損益（聚合所有 Tenant）

---

## 4. 權限繼承規則

### 4.1 自頂向下繼承

| 階層 | 可見數據範圍 | 可操作範圍 | 示例角色 |
|-----------|-------------------|----------------|--------------|
| **Super Admin** | 所有全局數據 | 建立 Brand、全局配置 | 平台 CTO |
| **Brand Admin** | 所屬 Brand 下的所有 Tenant | 建立 Tenant、Brand 層級配置 | 集團 CEO |
| **Tenant Admin** | 所屬 Tenant 數據 | 管理玩家、代理、活動 | 站點運營經理 |
| **Agent** | 自己及下級代理數據 | 查看下級玩家、佣金 | 代理合作夥伴 |

### 4.2 權限範圍矩陣

| 操作 | Super Admin | Brand Admin | Tenant Admin | Agent |
|-----------|:-----------:|:-----------:|:------------:|:-----:|
| 建立 Brand | 是 | 否 | 否 | 否 |
| 建立 Tenant | 是 | 是 | 否 | 否 |
| 全局遊戲開關 | 是 | 否 | 否 | 否 |
| Brand 額度調整 | 是 | 是 | 否 | 否 |
| 玩家管理 | 是 | 是 | 是 | 否 |
| 查看佣金報表 | 是 | 是 | 是 | 是 |

---

## 5. 數據隔離政策

### 5.1 隔離需求

1. **嚴格的 Tenant 隔離**：玩家、交易和遊戲會話必須按 tenant_id 隔離
2. **Brand 層級聚合**：Brand Admin 可查看跨 Tenant 的聚合數據，但未經模擬無法修改個別 Tenant 記錄
3. **無跨 Brand 訪問**：Brand A 無法訪問 Brand B 的任何數據

### 5.2 跨租戶查詢白名單限制（Cross-Tenant Query Whitelist）

當 Brand Admin 或 Super Admin 需要執行跨租戶聚合查詢時，系統必須遵循以下業務規則：

1. **白名單制度**：僅允許特定場景繞過租戶隔離（品牌聚合報表、全局管理操作、租戶遷移）
2. **禁止訪問敏感資料**：跨租戶查詢**禁止**返回玩家 PII 明細（email、電話、身份證號）、錢包餘額明細、KYC 文件或支付憑證
3. **審計追蹤**：每次跨租戶查詢必須記錄操作者、時間、查詢範圍和返回資料量
4. **最小權限原則**：跨租戶查詢僅限於 Super Admin 和 Brand Admin 角色

> **合規依據**: PCI-DSS v4 Req 7.2.1（最小權限）、GDPR Art 25（數據保護設計）

→ **[技術實作 — @TenantIgnore Safety Matrix](../../architecture/06_Platform_Core/01_Multi_Tenant_Architecture.md#63-tenantignore-安全使用策略tenant-bypass-safety-matrix)** — 白名單表清單、ArchUnit 強制檢查、審計日誌要求

### 5.3 共享數據考量

| 數據類型 | 共享層級 | 需求 |
|-----------|--------------|-------------|
| 玩家黑名單 | 可選（Brand） | 每個 Brand 可配置 |
| Game Provider 列表 | 全局 | 由 Super Admin 管理 |
| 支付提供商 | Tenant 特定 | 每個 Tenant 擁有自己的商戶 ID |
| 風控規則 | 階層式 | 全局預設值，允許 Tenant 覆蓋 |

---

## 6. 品牌化和自訂規則

### 6.1 Tenant 層級自訂

每個 Tenant 必須支援：
- **域名配置**：獨立 FQDN
- **視覺識別**：Logo、favicon、色彩方案
- **Email 模板**：品牌化交易 Email
- **SMS 發送者 ID**：本地化發送者識別
- **貨幣設定**：主要貨幣和匯率

### 6.2 White-Label 需求

| 組件 | 可自訂 | 備註 |
|-----------|:------------:|-------|
| 域名 | 是 | 需要 SSL 憑證 |
| Logo | 是 | 多種尺寸（favicon、header、footer） |
| 色彩主題 | 是 | 主色、次要色、強調色 |
| Footer 文字 | 是 | 版權、法律聲明 |
| Email From 地址 | 是 | 需要 SPF/DKIM 配置 |
| App 圖示（如果是 PWA） | 是 | iOS/Android 特定尺寸 |

---

## 7. 每個 Tenant 的授權

### 7.1 計費模式

支援三種計費模式：

1. **固定月費 (Fixed Monthly Fee，訂閱制)**：
   - 每個 Tenant 固定月費（例如 $1,000/月）
   - 適合小型站點

2. **營收分成 (Revenue Share)**：
   - 平台抽取玩家有效投注額 (Valid Turnover) 的百分比（2-5%）
   - 適合大型站點

3. **混合模式 (Hybrid Model)**：
   - 基礎月費 + 營收分成（例如 $500/月 + 1% 投注額）
   - 平衡風險與回報

### 7.2 計費週期需求

| 需求 | 規格 |
|------------|---------------|
| 計費週期 | 每月（每月 1 日） |
| 寬限期 | 服務暫停前 7 天 |
| 自動續訂 | 預設啟用 |
| 發票格式 | 帶有 VAT/稅務詳情的 PDF |
| 支付方式 | 電匯、信用卡、加密貨幣 |

### 7.3 逾期處理

| 逾期天數 | 動作 |
|--------------|--------|
| 0-7 | 每日發送警告 Email |
| 8-14 | 帳號標記為「有風險」 |
| 15-30 | 禁用新玩家註冊 |
| 31+ | 完全暫停服務 |

---

## 8. 數據遷移和 Tenant 轉移

### 8.1 跨 Brand 轉移需求

**場景**：Tenant 從 Brand A 轉移至 Brand B（例如收購）

**遷移檢查清單**：
- [ ] 所有玩家數據完整性已驗證
- [ ] 交易歷史完整
- [ ] 錢包餘額已對帳
- [ ] 待處理提現已處理
- [ ] 審計追蹤已保留
- [ ] 監管通知（如需要）

### 8.2 數據可攜性 (Data Portability，GDPR 合規)

Tenant 必須能夠匯出：
- 玩家註冊數據
- 交易歷史
- 遊戲會話記錄
- 通訊日誌
- 行銷同意記錄

**匯出格式**：JSON 或 CSV，附帶 Schema 文檔

---

## 9. 集團範圍 SSO 需求

### 9.1 Brand 範圍單一登入 (Single Sign-On)

**需求**：玩家可使用一個帳號登入同一 Brand 下的多個 Tenant。

**考量**：
- 錢包餘額保持 Tenant 特定（無自動轉移）
- VIP 狀態可能因 Tenant 而異
- 獎金資格按 Tenant 檢查

### 9.2 SSO 配置選項

| 選項 | 描述 |
|--------|-------------|
| 禁用 (Disabled) | 每個 Tenant 擁有完全獨立的玩家池 |
| 啟用（相同錢包，Same Wallet） | 跨 Tenant 共享錢包 |
| 啟用（獨立錢包，Separate Wallets） | 相同登入，每個 Tenant 獨立錢包 |

---

## 相關文檔

### 業務邏輯參考
- Billing & Invoicing *(planned)* - Multi-tenant 計費模式詳情
- [Agent System](../07_Agent_Operations/02_Agent_System_Requirements.md) - 階層結構擴展

### 技術架構參考
- [Multi-Tenant Architecture](../../architecture/06_Platform_Core/01_Multi_Tenant_Architecture.md) - 技術實作
- RBAC Permissions *(planned)* - 階層式權限實作
- Audit Logging *(planned)* - Tenant 操作審計

### 技術實作

→ **[Multi-Tenant Architecture](../../architecture/06_Platform_Core/01_Multi_Tenant_Architecture.md)** - Tenant 隔離機制（shared-nothing 模式）、動態 Tenant 路由、階層式數據分區（tenantId 傳播）、數據庫分片策略、SSO 配置選項

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Product Team
