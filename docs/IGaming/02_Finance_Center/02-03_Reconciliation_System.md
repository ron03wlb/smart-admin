# 02-03 對帳中心系統 (Reconciliation System)

## 1. 系統概述
對帳是確保平台資金安全與數據準確性的最後一道防線。
本系統負責比對「平台內部帳 (Internal Ledger)」與「外部渠道帳 (External Statement)」，並生成財務報表。

## 2. 核心功能需求

### 2.1 三方對帳 (Three-Way Matching)
需比對以下三方數據：
1. **平台訂單 (Platform Orders)**：玩家存款/提款記錄。
2. **支付商報表 (PSP Reports)**：實際資金進出記錄。
3. **銀行/錢包實際流水 (Bank Statement)**：最終資金落地記錄 (若有)。

### 2.2 差異處理 (Discrepancy Handling)
- **長款 (Over)**：外部有錢，平台無訂單 (可能是掉單或玩家誤轉)。-> 需人工補錄。
- **短款 (Short)**：平台有成功訂單，外部無錢 (極高風險，可能是虛假回調攻擊)。-> 需立即報警並凍結玩家帳號。
- **金額不符**：平台訂單 $100，實際入帳 $90 (可能是手續費扣除差異)。-> 需配置容差範圍。

### 2.3 日結流程 (Daily Closing)
- **自動化排程**：每日 02:00 自動下載前一日 PSP 報表 (CSV/API)。
- **自動比對**：系統自動進行 Key-Value 比對 (Order ID)。
- **生成日報**：包含 總存款、總提款、手續費、差異筆數、差異金額。

### 2.4 報表中心 (Report Center)
- **商戶報表**：各商戶每日盈虧 (PNL)、充提差。
- **渠道報表**：各支付渠道的成功率、手續費成本分析。

### 2.5 代理信用對帳 (Agent Settlement Reconciliation)
*   **針對信用網模式的核心對帳流程**。
*   **公式更新**:
    `Net Payable = (Total Win/Loss - Commission) + Adjustment (Late Settlement)`
*   **輸入數據**：
    1.  **System Report**: 系統生成的週結單 (Weekly Statement)。
    2.  **Manual Input / Bank Statement**: 財務收到的實際匯款憑證 (USDT TxID 或 銀行流水)。
*   **核銷邏輯 (Write-off Logic)**：
    *   財務人員在後台輸入 "實收金額"。
    *   系統自動比對 `Net Payable` vs `Actual Received`。
    *   若 **Match** (差異 < $10)：系統自動執行 `Credit Reset`，恢復代理額度。
    *   若 **Mismatch**：生成 `Outstanding Debt` 工單，代理額度維持凍結，直到補齊差額。


## 3. 對賬算法詳解

### 3.1 數據匹配邏輯

**匹配層級（按優先順序）**：
1. **精確匹配（Exact Match）**：
   - 條件：`Platform.order_id = PSP.merchant_order_id`
   - 狀態：✅ 自動標記為已對帳

2. **金額+時間匹配（Fuzzy Match）**：
   - 條件：`ABS(Platform.amount - PSP.amount) < $1 AND ABS(Platform.time - PSP.time) < 10 minutes`
   - 狀態：⚠️ 需人工審核確認

3. **未匹配（Unmatched）**：
   - 條件：在 PSP 報表中找不到對應記錄
   - 狀態：❌ 標記為差異，觸發警報

**SQL 實現範例**：
```sql
-- 三方對帳主查詢
WITH platform_txns AS (
    SELECT
        transaction_id,
        order_id,
        amount,
        currency,
        status,
        created_at
    FROM transactions
    WHERE DATE(created_at) = '2026-01-26'  -- 前一日
      AND type = 'deposit'
),
psp_txns AS (
    SELECT
        psp_transaction_id,
        merchant_order_id,
        amount AS psp_amount,
        settlement_currency,
        psp_status,
        transaction_time
    FROM psp_daily_report
    WHERE DATE(transaction_time) = '2026-01-26'
)
SELECT
    p.order_id,
    p.amount AS platform_amount,
    psp.psp_amount,
    p.status AS platform_status,
    psp.psp_status,
    CASE
        WHEN psp.merchant_order_id IS NULL THEN 'MISSING_IN_PSP'
        WHEN ABS(p.amount - psp.psp_amount) > 0.01 THEN 'AMOUNT_MISMATCH'
        WHEN p.status != psp.psp_status THEN 'STATUS_MISMATCH'
        ELSE 'MATCHED'
    END AS reconciliation_status
FROM platform_txns p
FULL OUTER JOIN psp_txns psp
    ON p.order_id = psp.merchant_order_id;
```

### 3.2 自動化對賬腳本

**每日對帳定時任務（Cron: 0 2 * * *）**：
```python
import requests
from datetime import datetime, timedelta

def daily_reconciliation():
    yesterday = datetime.now() - timedelta(days=1)
    date_str = yesterday.strftime('%Y-%m-%d')

    # Step 1: 下載 PSP 報表
    psp_report = download_psp_report(date=date_str)
    # 範例 API: GET https://api.nuvei.com/reports/transactions?date=2026-01-26

    # Step 2: 解析 PSP 報表（CSV 或 JSON）
    psp_data = parse_psp_report(psp_report)

    # Step 3: 從資料庫提取平台交易
    platform_data = db.query("""
        SELECT order_id, amount, status
        FROM transactions
        WHERE DATE(created_at) = %s AND type = 'deposit'
    """, (date_str,))

    # Step 4: 比對數據
    discrepancies = []
    for platform_txn in platform_data:
        psp_txn = find_matching_psp_txn(psp_data, platform_txn['order_id'])

        if not psp_txn:
            discrepancies.append({
                'type': 'MISSING_IN_PSP',
                'order_id': platform_txn['order_id'],
                'amount': platform_txn['amount']
            })
        elif platform_txn['amount'] != psp_txn['amount']:
            discrepancies.append({
                'type': 'AMOUNT_MISMATCH',
                'order_id': platform_txn['order_id'],
                'platform_amount': platform_txn['amount'],
                'psp_amount': psp_txn['amount']
            })

    # Step 5: 生成對帳報告
    report = generate_reconciliation_report(date_str, discrepancies)

    # Step 6: 發送通知
    if len(discrepancies) > 0:
        send_alert_to_finance_team(report)

    return report
```

---

## 4. 差異處理 SOP

### 4.1 長款處理流程（外部有錢，平台無訂單）

**Step 1: 驗證數據來源**
```
檢查 PSP 交易 ID → 查詢是否為測試交易
若是測試環境數據誤入生產 → 忽略
```

**Step 2: 查詢玩家帳戶**
```
根據 PSP 報表中的 player_email/phone 查找玩家
若找到玩家 → 檢查是否有相同金額的 Pending 訂單
```

**Step 3: 補單操作**
```python
# 若確認為掉單，執行補單
def manual_credit(order_id, player_id, amount, reason):
    transaction = Transaction.objects.create(
        order_id=f"manual_{order_id}",
        player_id=player_id,
        amount=amount,
        type='deposit',
        status='success',
        note=f"手動補單: {reason}"
    )

    # 增加玩家餘額（引用 02-06 統一錢包）
    wallet_service.credit(player_id, amount)

    # 記錄審計日誌（引用 09-02）
    audit_log.create(
        action='MANUAL_CREDIT',
        operator=current_user.id,
        details={'transaction_id': transaction.id, 'reason': reason}
    )
```

### 4.2 短款處理流程（平台有訂單，外部無錢）

**🚨 高風險警報**：可能是偽造回調攻擊！

**Step 1: 立即凍結**
```sql
-- 凍結可疑玩家帳號
UPDATE players
SET status = 'frozen', freeze_reason = '短款風險'
WHERE player_id = (SELECT player_id FROM transactions WHERE order_id = 'xxx');
```

**Step 2: 調查**
```
1. 檢查 PSP Callback 日誌（IP、時間戳、簽名）
2. 聯繫 PSP 客服確認是否收到款項
3. 若 PSP 確認未收款 → 回滾玩家餘額
```

**Step 3: 回滾操作**
```python
def rollback_fraudulent_transaction(transaction_id):
    txn = Transaction.objects.get(id=transaction_id)

    # 扣除玩家餘額
    wallet_service.debit(txn.player_id, txn.amount, reason='短款回滾')

    # 更新交易狀態
    txn.status = 'fraud_rollback'
    txn.save()

    # 通知風控團隊
    risk_alert.create(
        player_id=txn.player_id,
        alert_type='FRAUDULENT_DEPOSIT',
        severity='critical'
    )
```

### 4.3 金額不符處理（手續費差異）

**容差範圍配置**：
```python
# 配置表：reconciliation_tolerance
{
    "psp_code": "nuvei",
    "tolerance_type": "percentage",  # percentage 或 fixed_amount
    "tolerance_value": 0.02,         # 2% 容差
    "auto_approve": True             # 在容差範圍內自動通過
}
```

**處理邏輯**：
```python
def check_amount_tolerance(platform_amount, psp_amount, psp_code):
    tolerance = get_tolerance_config(psp_code)

    if tolerance['tolerance_type'] == 'percentage':
        diff_percentage = abs(platform_amount - psp_amount) / platform_amount
        if diff_percentage <= tolerance['tolerance_value']:
            return 'AUTO_APPROVED'

    return 'REQUIRES_MANUAL_REVIEW'
```

---

## 5. 銀行對賬文件解析

### 5.1 常見銀行報表格式

**中國銀行 CSV 範例**：
```csv
交易日期,交易時間,對方帳號,對方戶名,交易金額,幣種,交易類型,備註
2026-01-26,10:30:15,62170000012345,張三,1000.00,CNY,轉入,存款
2026-01-26,14:22:33,62170000067890,李四,5000.00,CNY,轉出,提款
```

**解析器實現**：
```python
import csv

def parse_bank_statement_cn(file_path):
    transactions = []
    with open(file_path, 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        for row in reader:
            transactions.append({
                'date': row['交易日期'],
                'time': row['交易時間'],
                'counterparty_name': row['對方戶名'],
                'amount': float(row['交易金額']),
                'type': 'credit' if row['交易類型'] == '轉入' else 'debit',
                'note': row['備註']
            })
    return transactions
```

### 5.2 SEPA 銀行報表解析（歐洲）

**MT940 格式範例**：
```
:20:REFERENCE123
:25:NL12ABNA0123456789
:28C:00001/001
:60F:C260125EUR1000,00
:61:2601260126C500,00NTRF//TRANSACTION_REF
:86:/TRTP/SEPA CREDIT TRANSFER/NAME/John Doe/REMI/Deposit for player_12345
:62F:C260126EUR1500,00
```

**解析器（使用 mt940 庫）**：
```python
from mt940 import MT940

def parse_sepa_statement(file_path):
    with open(file_path, 'r') as f:
        statements = MT940(f).statements

    transactions = []
    for stmt in statements:
        for txn in stmt.transactions:
            transactions.append({
                'date': txn.date,
                'amount': txn.amount,
                'type': 'credit' if txn.amount > 0 else 'debit',
                'reference': txn.id,
                'description': txn.data.get('transaction_details', '')
            })
    return transactions
```

---

## 6. 對賬報表模板

### 6.1 每日對賬報表

**Excel 輸出範例**：
```python
import pandas as pd

def generate_daily_reconciliation_report(date):
    # 查詢對帳數據
    data = db.query("""
        SELECT
            order_id,
            player_id,
            amount,
            platform_status,
            psp_status,
            reconciliation_status
        FROM reconciliation_results
        WHERE DATE(created_at) = %s
    """, (date,))

    df = pd.DataFrame(data)

    # 彙總統計
    summary = {
        '總交易數': len(df),
        '已對帳': len(df[df['reconciliation_status'] == 'MATCHED']),
        '差異筆數': len(df[df['reconciliation_status'] != 'MATCHED']),
        '差異金額': df[df['reconciliation_status'] != 'MATCHED']['amount'].sum()
    }

    # 輸出 Excel
    with pd.ExcelWriter(f'reconciliation_{date}.xlsx') as writer:
        df.to_excel(writer, sheet_name='明細', index=False)
        pd.DataFrame([summary]).to_excel(writer, sheet_name='彙總', index=False)
```

### 6.2 月度財務報表

**報表維度**：
1. **按商戶分組**：各商戶的總存款、總提款、淨充值
2. **按支付渠道分組**：各PSP的交易量、成功率、手續費成本
3. **按幣種分組**：USD、EUR、CNY等各幣種的資金流動

**SQL 查詢範例**：
```sql
-- 月度商戶財務報表
SELECT
    tenant_id,
    tenant_name,
    SUM(CASE WHEN type = 'deposit' THEN amount ELSE 0 END) AS total_deposits,
    SUM(CASE WHEN type = 'withdrawal' THEN amount ELSE 0 END) AS total_withdrawals,
    SUM(CASE WHEN type = 'deposit' THEN amount ELSE -amount END) AS net_cash_flow,
    COUNT(DISTINCT player_id) AS active_players
FROM transactions
WHERE DATE_TRUNC('month', created_at) = '2026-01-01'
  AND status = 'success'
GROUP BY tenant_id, tenant_name
ORDER BY net_cash_flow DESC;
```

---

## 7. 審批與權限

### 7.1 人工調帳（Manual Adjustment）

**操作流程**：
1. 財務人員發現差異，提交調帳申請
2. 填寫調帳原因（必填，至少 20 字）
3. 上傳佐證文件（如銀行截圖、PSP 郵件回覆）
4. **強制審批**：調帳金額 > $0，必須經由財務主管審核通過（引用 09-02 審批系統）

**資料表設計**：
```sql
CREATE TABLE manual_adjustments (
    adjustment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id BIGINT,
    adjustment_amount DECIMAL(15,2) NOT NULL,
    adjustment_reason TEXT NOT NULL,
    supporting_documents JSON,  -- [{"file_name": "proof.png", "url": "s3://..."}]

    -- 審批流程
    submitted_by BIGINT NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approval_status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',

    INDEX idx_status (approval_status),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);
```

### 7.2 權限控制（RBAC）

**角色權限矩陣**（引用 09-01 RBAC）：
| 操作 | 財務專員 | 財務主管 | CTO | 超級管理員 |
|------|---------|---------|-----|-----------|
| 查看對帳報表 | ✅ | ✅ | ✅ | ✅ |
| 提交調帳申請 | ✅ | ✅ | ❌ | ✅ |
| 審批調帳（< $1000）| ❌ | ✅ | ✅ | ✅ |
| 審批調帳（≥ $1000）| ❌ | ❌ | ✅ | ✅ |
| 修改對帳規則 | ❌ | ❌ | ✅ | ✅ |

---

## 8. 相關文檔

### 業務邏輯參考
- [02-02 支付網關集成](./02-02_Payment_Gateway_Integration.md) - PSP 交易數據來源
- [02-04 流水計算與對賬](./02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 遊戲對帳流程
- [02-06 統一錢包模型](./02-06_Unified_Wallet_Model.md) - 餘額調整邏輯

### 技術架構參考
- [09-02 審計日誌與審批](../09_System_Security/09-02_Audit_Log_&_Approval.md) - 調帳審批工作流
- [09-01 管理後台RBAC](../09_System_Security/09-01_Admin_RBAC.md) - 對帳系統權限控制
- [12-05 API 設計標準](../12_Technical_Operations/12-05_API_Design_Standard.md) - PSP API 規範

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Finance Team & Backend Team
