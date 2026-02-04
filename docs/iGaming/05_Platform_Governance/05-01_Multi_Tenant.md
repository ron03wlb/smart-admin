# 05-01 多租戶架構 (Multi-Tenant Architecture)

## 1. 架構定義
本系統採用 **SaaS (Software as a Service)** 多租戶架構，層級定義如下：
**Super Admin (平台方) -> Brand (品牌/集團) -> Tenant (商戶/站點) -> Agent (代理)**

## 2. 層級詳解

### 2.1 Super Admin (平台總管)
- **權限**：上帝視角，可見所有 Brand 與 Tenant 的數據。
- **功能**：
  - 開設新 Brand。
  - 全局遊戲開關 (若某 GP 故障，可一鍵切斷所有商戶連接)。
  - 全局風控規則設定。

### 2.2 Brand (品牌/集團)
- **定義**：代表一個營運集團，底下可擁有多個不同域名的站點 (Tenant)。
- **共享資源**：
  - Brand 底下的 Tenant 可選擇是否共享 "玩家黑名單"。
  - 財務額度 (Quota) 通常在 Brand 層級控管。

### 2.3 Tenant (商戶/站點)
- **定義**：實際運營的網站，擁有獨立的域名 (Domain)、Logo、前台樣式。
- **數據隔離**：Tenant A 的玩家無法登入 Tenant B (除非設定了集團通帳)。
- **配置獨立性**：
  - 獨立的支付商戶號。
  - 獨立的遊戲選品。
  - 獨立的推廣活動。

## 3. 跨層級管理需求
- **切換視角**：Super Admin 登入後，可 "Impersonate" (模擬) 進入任意 Tenant 的後台進行操作。
- **報表匯總**：
  - Tenant 級報表：單站盈虧。
  - Brand 級報表：集團總盈虧 (匯總旗下所有 Tenant)。

## 4. 層級權限傳遞機制

### 4.1 權限繼承規則

**向下繼承（Top-Down Inheritance）**：
```text
Super Admin
  ├─ 可管理所有 Brand
  │   └─ 每個 Brand Admin 可管理其下所有 Tenant
  │       └─ 每個 Tenant Admin 可管理其下所有 Agent
```

**權限範圍表**：
| 層級 | 可見數據範圍 | 可操作範圍 | 示例 |
|------|------------|-----------|------|
| **Super Admin** | 全局所有數據 | 開設Brand、全局配置 | 平台CTO |
| **Brand Admin** | 所屬Brand下所有Tenant數據 | 開設Tenant、Brand級配置 | 集團CEO |
| **Tenant Admin** | 所屬Tenant數據 | 管理玩家、代理、活動 | 網站運營經理 |
| **Agent** | 自己及下級代理數據 | 查看下級玩家、佣金 | 代理商 |

### 4.2 權限檢查實現

**Middleware 攔截器**：

**資料庫層級強制過濾（PostgreSQL RLS）**：

---

## 5. 跨層級數據查詢

### 5.1 Brand 級彙總報表

**需求**：Brand Admin 需要查看旗下所有 Tenant 的財務彙總。

**SQL 實現**：

### 5.2 Super Admin 全局監控

**實時監控指標**：

---

## 6. 租戶切換實現

### 6.1 Super Admin 模擬登入（Impersonate）

**功能**：Super Admin 可以"化身"為任意 Tenant Admin，進入其後台進行操作。

**實現方案**：

**前端顯示提示**：
```html
<!-- 當處於模擬模式時，頂部顯示警告條 -->
{% if request.session.impersonated_tenant_id %}
<div class="impersonation-banner" style="background: #ff0000; color: #fff; padding: 10px;">
    🚨 您正在模擬 Tenant: {{ current_tenant.name }}
    <button onclick="stopImpersonation()">退出模擬</button>
</div>
{% endif %}
```

### 6.2 集團通帳（Brand-Wide SSO）

**需求**：同一 Brand 下的多個 Tenant，玩家可以使用同一帳號登入。

**實現方案**：

---

## 7. 多租戶計費模型

### 7.1 計費方式（引用 02-05 賬單與發票）

**三種計費模式**：
1. **固定月費（Subscription）**：
   - 每月固定收費 $1,000/Tenant
   - 適合小型站點

2. **按流水抽成（Revenue Share）**：
   - 平台抽取玩家有效流水的 2-5%
   - 適合大型站點

3. **混合模式（Hybrid）**：
   - 基礎月費 $500 + 流水抽成 1%
   - 平衡風險與收益

### 7.2 計費數據計算

**SQL 查詢範例**（計算某 Tenant 本月應付費用）：

### 7.3 自動扣費與欠費處理

**定時任務（每月 1 號執行）**：

---

## 8. 數據遷移與租戶轉移

### 8.1 Tenant 數據遷移（跨 Brand 轉移）

**場景**：某 Tenant 從 Brand A 轉移至 Brand B（如併購案）。

**遷移步驟**：
1. **數據完整性檢查**：

2. **執行遷移**：

3. **審計日誌記錄**（引用 09-02）：

### 8.2 Tenant 數據導出（Data Portability）

**符合 GDPR 數據可攜權**：

---

## 9. 權限隔離與安全

### 9.1 數據隔離層級

**三種數據隔離策略**（引用 02-01）：
1. **資料庫隔離（Database per Tenant）**：
   - 每個 Tenant 獨立資料庫
   - **優點**：最強隔離、易於遷移
   - **缺點**：成本高、維護複雜

2. **Schema 隔離（Schema per Tenant）**：
   - 同一資料庫，每個 Tenant 獨立 Schema
   - **優點**：中等隔離、降低成本
   - **缺點**：跨 Tenant 查詢複雜

3. **Row-Level 隔離（Shared Database）**：
   - 所有 Tenant 共享資料表，用 `tenant_id` 過濾
   - **優點**：成本最低、跨 Tenant 查詢簡單
   - **缺點**：需嚴格執行 RLS 避免洩漏

**推薦方案**：**Row-Level 隔離 + PostgreSQL RLS**

### 9.2 API 層級權限驗證

**JWT Token 包含租戶信息**：
```json
{
  "user_id": 12345,
  "role": "tenant_admin",
  "tenant_id": 789,
  "brand_id": 10,
  "exp": 1706356800
}
```

**API 請求驗證**：

---

## 📚 相關文檔

### 業務邏輯參考
- [02-05 賬單與發票](../02_Finance_Center/02-05_Billing_&_Invoicing.md) - 多租戶計費模型
- [06-01 代理系統設計](../03_Player_Journey/03-04_Agent_System.md) - 層級結構延伸

### 技術架構參考
- [09-01 管理後台RBAC](../05_Platform_Governance/05-02_RBAC_Permissions.md) - 層級權限實現
- [09-02 審計日誌系統](../05_Platform_Governance/05-03_Audit_Log.md) - 租戶操作審計
- [02-01 出金風控](../01_Player_Center/01-05_Withdrawal_Risk.md) - 多租戶數據隔離策略

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Platform Team & Backend Team
