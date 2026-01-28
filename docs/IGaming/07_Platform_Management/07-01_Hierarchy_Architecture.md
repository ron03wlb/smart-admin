# 07-01 平台層級架構 (Hierarchy Architecture)

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
```
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
```python
class TenantIsolationMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        user = request.user

        # 提取用戶的 tenant_id
        if user.role == 'super_admin':
            request.accessible_tenants = Tenant.objects.all()
        elif user.role == 'brand_admin':
            request.accessible_tenants = Tenant.objects.filter(brand_id=user.brand_id)
        elif user.role == 'tenant_admin':
            request.accessible_tenants = Tenant.objects.filter(id=user.tenant_id)
        else:
            request.accessible_tenants = Tenant.objects.none()

        response = self.get_response(request)
        return response
```

**資料庫層級強制過濾（PostgreSQL RLS）**：
```sql
-- 啟用 Row-Level Security
ALTER TABLE players ENABLE ROW LEVEL SECURITY;

-- 創建策略：只能查詢所屬租戶的玩家
CREATE POLICY tenant_isolation_policy ON players
    FOR ALL
    TO authenticated_users
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

-- 應用層設定當前租戶
SET app.current_tenant_id = 123;
SELECT * FROM players;  -- 自動過濾 tenant_id = 123
```

---

## 5. 跨層級數據查詢

### 5.1 Brand 級彙總報表

**需求**：Brand Admin 需要查看旗下所有 Tenant 的財務彙總。

**SQL 實現**：
```sql
-- Brand 級財務彙總（過去 30 天）
SELECT
    b.brand_id,
    b.brand_name,
    COUNT(DISTINCT t.tenant_id) AS tenant_count,
    COUNT(DISTINCT p.player_id) AS total_players,
    SUM(CASE WHEN txn.type = 'deposit' THEN txn.amount ELSE 0 END) AS total_deposits,
    SUM(CASE WHEN txn.type = 'withdrawal' THEN txn.amount ELSE 0 END) AS total_withdrawals,
    SUM(CASE WHEN txn.type = 'deposit' THEN txn.amount ELSE -txn.amount END) AS net_revenue
FROM brands b
JOIN tenants t ON t.brand_id = b.brand_id
JOIN players p ON p.tenant_id = t.tenant_id
LEFT JOIN transactions txn ON txn.player_id = p.player_id
    AND txn.created_at > NOW() - INTERVAL '30 days'
    AND txn.status = 'success'
WHERE b.brand_id = ? -- Brand Admin 的 brand_id
GROUP BY b.brand_id, b.brand_name;
```

### 5.2 Super Admin 全局監控

**實時監控指標**：
```python
def get_platform_overview():
    return {
        'total_brands': Brand.objects.count(),
        'total_tenants': Tenant.objects.count(),
        'total_active_players': Player.objects.filter(
            last_login__gte=timezone.now() - timedelta(days=7)
        ).count(),
        'total_deposits_today': Transaction.objects.filter(
            type='deposit',
            status='success',
            created_at__date=timezone.now().date()
        ).aggregate(Sum('amount'))['amount__sum'] or 0,
        'total_withdrawals_today': Transaction.objects.filter(
            type='withdrawal',
            status='success',
            created_at__date=timezone.now().date()
        ).aggregate(Sum('amount'))['amount__sum'] or 0,
    }
```

---

## 6. 租戶切換實現

### 6.1 Super Admin 模擬登入（Impersonate）

**功能**：Super Admin 可以"化身"為任意 Tenant Admin，進入其後台進行操作。

**實現方案**：
```python
# 1. Super Admin 選擇要模擬的 Tenant
def impersonate_tenant(request, tenant_id):
    if not request.user.is_super_admin:
        raise PermissionDenied("Only Super Admin can impersonate")

    target_tenant = Tenant.objects.get(id=tenant_id)

    # 2. 在 Session 中記錄原始身份與模擬身份
    request.session['original_user_id'] = request.user.id
    request.session['impersonated_tenant_id'] = tenant_id

    # 3. 重定向至 Tenant 後台
    return redirect(f'/tenant/{tenant_id}/dashboard')

# 4. 退出模擬
def stop_impersonation(request):
    original_user_id = request.session.pop('original_user_id', None)
    request.session.pop('impersonated_tenant_id', None)

    if original_user_id:
        request.user = User.objects.get(id=original_user_id)

    return redirect('/super-admin/dashboard')
```

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
```sql
-- 玩家表增加 brand_id 欄位
ALTER TABLE players ADD COLUMN brand_id BIGINT;

-- 唯一索引：同一 Brand 下 username 唯一
CREATE UNIQUE INDEX idx_brand_username ON players(brand_id, username);

-- 玩家登入邏輯
-- 允許玩家在同 Brand 的不同 Tenant 之間切換
SELECT * FROM players
WHERE brand_id = ? AND username = ?;
```

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
```sql
-- 計算本月有效流水
WITH monthly_turnover AS (
    SELECT
        SUM(valid_turnover) AS total_turnover
    FROM game_bets
    WHERE tenant_id = ?
      AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW())
)
SELECT
    tc.tenant_id,
    tc.billing_model,  -- 'subscription', 'revenue_share', 'hybrid'
    tc.subscription_fee,
    tc.revenue_share_percentage,
    mt.total_turnover,
    CASE
        WHEN tc.billing_model = 'subscription' THEN tc.subscription_fee
        WHEN tc.billing_model = 'revenue_share' THEN mt.total_turnover * tc.revenue_share_percentage / 100
        WHEN tc.billing_model = 'hybrid' THEN tc.subscription_fee + (mt.total_turnover * tc.revenue_share_percentage / 100)
    END AS amount_due
FROM tenant_configs tc
CROSS JOIN monthly_turnover mt
WHERE tc.tenant_id = ?;
```

### 7.3 自動扣費與欠費處理

**定時任務（每月 1 號執行）**：
```python
def charge_monthly_fees():
    for tenant in Tenant.objects.filter(status='active'):
        billing_config = tenant.billing_config
        amount_due = calculate_billing_amount(tenant, billing_config)

        # 從 Tenant 錢包扣款
        if tenant.wallet_balance >= amount_due:
            tenant.wallet_balance -= amount_due
            tenant.save()

            Invoice.objects.create(
                tenant=tenant,
                amount=amount_due,
                status='paid',
                billing_period=get_last_month()
            )
        else:
            # 欠費處理
            Invoice.objects.create(
                tenant=tenant,
                amount=amount_due,
                status='overdue',
                billing_period=get_last_month()
            )

            # 通知 Tenant Admin
            send_overdue_notice(tenant)

            # 若連續 3 個月欠費，自動暫停服務
            if tenant.overdue_months >= 3:
                tenant.status = 'suspended'
                tenant.save()
```

---

## 8. 數據遷移與租戶轉移

### 8.1 Tenant 數據遷移（跨 Brand 轉移）

**場景**：某 Tenant 從 Brand A 轉移至 Brand B（如併購案）。

**遷移步驟**：
1. **數據完整性檢查**：
   ```sql
   -- 檢查該 Tenant 是否有未結算的財務數據
   SELECT COUNT(*) FROM transactions
   WHERE tenant_id = ? AND status = 'pending';
   ```

2. **執行遷移**：
   ```sql
   BEGIN;

   -- 更新 Tenant 的 brand_id
   UPDATE tenants SET brand_id = ? WHERE tenant_id = ?;

   -- 更新所有關聯數據的 brand_id
   UPDATE players SET brand_id = ? WHERE tenant_id = ?;
   UPDATE transactions SET brand_id = ? WHERE tenant_id = ?;
   UPDATE agents SET brand_id = ? WHERE tenant_id = ?;

   COMMIT;
   ```

3. **審計日誌記錄**（引用 09-02）：
   ```python
   AuditLog.create(
       action='TENANT_MIGRATION',
       operator_id=current_user.id,
       details={
           'tenant_id': tenant_id,
           'from_brand_id': old_brand_id,
           'to_brand_id': new_brand_id,
           'reason': '併購案：Brand A 併入 Brand B'
       }
   )
   ```

### 8.2 Tenant 數據導出（Data Portability）

**符合 GDPR 數據可攜權**：
```python
def export_tenant_data(tenant_id):
    tenant = Tenant.objects.get(id=tenant_id)

    data_export = {
        'tenant_info': model_to_dict(tenant),
        'players': list(Player.objects.filter(tenant_id=tenant_id).values()),
        'transactions': list(Transaction.objects.filter(tenant_id=tenant_id).values()),
        'agents': list(Agent.objects.filter(tenant_id=tenant_id).values()),
        'bonuses': list(Bonus.objects.filter(tenant_id=tenant_id).values()),
    }

    # 輸出為 JSON 檔案
    output_path = f'/exports/tenant_{tenant_id}_export.json'
    with open(output_path, 'w') as f:
        json.dump(data_export, f, indent=2, default=str)

    return output_path
```

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
```python
@require_tenant_access
def get_player_list(request, tenant_id):
    # 自動驗證：request.user.tenant_id == tenant_id
    if request.user.tenant_id != tenant_id and not request.user.is_super_admin:
        raise PermissionDenied("Cannot access other tenant's data")

    players = Player.objects.filter(tenant_id=tenant_id)
    return JsonResponse({'players': list(players.values())})
```

---

## 10. 相關文檔

### 業務邏輯參考
- [02-05 賬單與發票](../02_Finance_Center/02-05_Billing_&_Invoicing.md) - 多租戶計費模型
- [06-01 代理系統設計](../06_Agent_Center/06-01_Affiliate_System_Design.md) - 層級結構延伸

### 技術架構參考
- [09-01 管理後台RBAC](../09_System_Security/09-01_Admin_RBAC.md) - 層級權限實現
- [09-02 審計日誌系統](../09_System_Security/09-02_Audit_Log_System.md) - 租戶操作審計
- [02-01 出金風控](../02_Finance_Center/02-01_Withdrawal_Risk_Control.md) - 多租戶數據隔離策略

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Platform Team & Backend Team
