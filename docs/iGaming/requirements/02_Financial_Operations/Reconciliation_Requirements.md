# 對帳需求（Reconciliation Requirements）

> **規範來源**: [02-03_Reconciliation_System.md](../../source-archive/02_Finance_Center/02-03_Reconciliation_System.md)
> **目標讀者**: 高層管理人員、合規官、財務團隊、營運經理
> **相關架構**: [Reconciliation_Technical.md](../../architecture/02_Finance_Service/Reconciliation_Technical.md)
> **最後同步**: 2026-02-09
>
> **Refinement Note**: 技術細節（PostgreSQL/S3 Glacier 存儲、UTC 時區轉換算法、3DS 驗證實現、區塊鏈確認次數）已移至架構層。本文檔僅專注於業務需求。

---

## Business Value

此對帳系統提供關鍵商業價值：

- **保護平台資金**：三層對帳（實時、批次、每日）作為防禦假回調攻擊、支付欺詐和系統錯誤的最後一道防線，防止財務損失
- **確保合規遵從**：10 年統一數據保留期滿足 UKGC（5 年）、MGA（10 年）、PAGCOR（5 年）和稅務合規（7 年）要求，避免高達全球年營業額 4% 或 2,000 萬歐元（GDPR 最高罰款）的監管處罰
- **建立信任與透明**：自動化差異處理，具備明確的升級路徑（L1 → L4）和強制雙重批准，確保審計追蹤完整性，建立監管機構和玩家信任
- **降低營運成本**：80-100% 自動化水平，通過容差自動批准（金額 < $10）和首次匹配率（≥95% 目標）最小化人工介入，加速解決週期

---

## 1. 業務概述（Business Overview）

對帳是平台資金安全和數據準確性的最後一道防線。系統比對「內部帳本」與「外部對帳單」並生成財務報表。

---

## 2. 核心功能需求（Core Functional Requirements）

### 2.1 三方匹配（Three-Way Matching）

系統必須比對三個數據來源：

1. **平台訂單**：玩家存款/提款記錄
2. **PSP 報表**：支付提供商的實際資金流動記錄
3. **銀行對帳單**：最終資金落地記錄（如有）

### 2.2 三層對帳架構（Three-Tier Reconciliation Architecture）

| 層級 | 執行頻率 | 數據來源 | 匹配方法 | 延遲 | 主要目的 | 自動化水平 |
|------|---------|---------|---------|------|---------|-----------|
| **Tier 1: 實時** | 交易後 30 秒內 | Platform <-> PSP API | 主動查詢驗證 | < 1 分鐘 | 防止假回調攻擊 | 100% 自動化 |
| **Tier 2: 批次** | 每小時 | Platform <-> PSP API | 批次查詢比對 | 1 小時 | 檢測延遲/丟失訂單 | 95% 自動化（例外人工） |
| **Tier 3: T+1 每日** | 每日 02:00 AM | Platform <-> PSP <-> Bank | 完整三方匹配 | 1 天 | 財務報表 + 審計 | 80% 自動化（差異人工） |

### 2.3 容差配置（Tolerance Configuration）

| 容差類型 | 數值 | 說明 |
|---------|------|------|
| 金額容差 | 2% 或 $1（取較小者） | 可接受的差異範圍 |
| 時間容差 | 10 分鐘 | 模糊匹配時間窗口 |
| 自動批准閾值 | < $10 差異在容差內 | 自動通過無需審核 |

---

## 3. 差異處理需求（Discrepancy Handling Requirements）

### 3.1 差異類型（Discrepancy Types）

| 類型 | 定義 | 風險等級 | 處理方式 |
|------|------|---------|---------|
| **長款（Over/Long）** | 外部有錢，平台無訂單 | MEDIUM | 人工補單 |
| **短款（Short）** | 平台有成功訂單，外部無錢 | CRITICAL | 立即告警 + 凍結玩家帳戶 |
| **金額不符（Amount Mismatch）** | 平台訂單 $100，實際存入 $90 | VARIES | 配置容差範圍 |

### 3.2 差異處理矩陣（Discrepancy Handling Matrix）

| 差異類型 | 金額範圍 | 風險等級 | 響應時間 | 批准權限 | 自動化 | 典型原因 |
|---------|---------|---------|---------|---------|--------|---------|
| **長款** | < $10 | LOW | 24 小時 | 財務專員 | 80% 自動 | 測試交易、丟單 |
| | $10-$1000 | MEDIUM | 4 小時 | 財務經理 | 50% 自動 | 玩家誤轉、丟單 |
| | >= $1000 | HIGH | 1 小時 | CFO | 0%（全人工） | 大額丟單、異常轉帳 |
| **短款** | 任意金額 | CRITICAL | 立即 | CTO + CFO | 0%（全人工） | 假回調、欺詐攻擊 |
| **金額不符** | < $1（容差內） | LOW | 自動通過 | 系統自動 | 100% 自動 | 匯率波動、精度誤差 |
| | $1-$10 | LOW | 24 小時 | 系統自動 | 100% 自動 | 手續費扣除、小額偏差 |
| | $10-$100 | MEDIUM | 4 小時 | 財務經理 | 0%（全人工） | 匯率錯誤、費用配置錯誤 |
| | >= $100 | HIGH | 1 小時 | CFO | 0%（全人工） | 系統錯誤、PSP 計算錯誤 |

### 3.3 升級路徑（Escalation Path）

| 級別 | 處理人 | 決策權限 | SLA | 升級條件 |
|------|--------|---------|-----|---------|
| **L1** | 系統自動 | 風險標記 + 對帳異常 -> 自動凍結 | 立即 | 自動觸發 |
| **L2** | 財務經理 | 衝突 < $10,000 | 4h | L1 未解決 |
| **L3** | CTO + CFO | >= $10,000 或涉及 SAR | 1h | L2 未解決 |
| **L4** | 董事會 | 監管報告或重大資金 | 24h | L3 需董事會通知 |

---

## 4. 爭議解決程序（Dispute Resolution Procedures）

### 4.1 長款處理（Over Payment Handling）

**步驟 1：驗證數據來源**
- 檢查 PSP 交易是否為測試環境交易
- 如果測試數據進入生產 -> 忽略

**步驟 2：查詢玩家帳戶**
- 通過 PSP 報表的郵箱/電話搜尋玩家
- 檢查是否有匹配金額的待處理訂單

**步驟 3：補單**
- 自動補單條件：金額 < $100 且玩家為可信玩家
- 否則：提交人工審核，需附上憑證

### 4.2 短款處理（Short Payment Handling）

**關鍵告警**：可能的假回調攻擊！

**步驟 1：立即凍結**
- 立即凍結玩家帳戶
- 更新玩家狀態為「凍結」

**步驟 2：調查**
1. 檢查 PSP 回調日誌（IP、時間戳、簽名）
2. 聯繫 PSP 客服確認是否收到款項
3. 如果 PSP 確認無款項 -> 回滾玩家餘額

**步驟 3：解決方案**
- 確認欺詐：回滾餘額 + 風險告警 + 考慮報警（如果 > $10,000）
- PSP 數據問題：等待 PSP 數據同步（24-48 小時）後重新檢查

### 4.3 金額不符處理（Amount Mismatch Handling）

**容差檢查流程**：
1. 計算差額：`diff = |platform - psp|`
2. 計算百分比：`diff_pct = diff / platform × 100%`
3. 根據上述矩陣應用容差規則

---

## 5. 每日收盤流程（Daily Closing Process）

### 5.1 自動化排程（Automated Schedule）

- **執行時間**：每日 02:00 AM
- **目標數據**：前一天的 PSP 報表（CSV/API）
- **匹配方法**：鍵值比對（訂單 ID）

### 5.2 每日報表內容（Daily Report Contents）

| 報表項目 | 說明 |
|---------|------|
| 總存款 | 當日所有存款總和 |
| 總提款 | 當日所有提款總和 |
| 手續費 | 總交易手續費 |
| 差異數量 | 未匹配記錄數 |
| 差異金額 | 未匹配總金額 |

---

## 6. 代理結算對帳（Agent Settlement Reconciliation）

適用於信用網路模式營運：

### 6.1 核心公式（Core Formula）

```
應付淨額 = (總輸贏 - 佣金) + 調整項（延遲結算）
```

### 6.2 輸入數據（Input Data）

1. **系統報表**：系統生成的週報表
2. **人工輸入/銀行對帳單**：實際匯款憑證（USDT TxID 或銀行對帳單）

### 6.3 銷帳邏輯（Write-off Logic）

| 條件 | 操作 |
|------|------|
| 匹配（差異 < $10） | 自動執行信用重置，恢復代理信用額度 |
| 不匹配 | 生成未結債務工單，凍結代理信用直到差額結清 |

---

## 7. 告警通知規則（Alert Notification Rules）

| 差異類型 | 告警等級 | 通知渠道 | 接收人 | 頻率 | 持續告警條件 |
|---------|---------|---------|--------|------|-------------|
| 短款 | P0 | SMS + Email + Slack | CTO、CFO、風控團隊 | 立即 + 每 30 分鐘 | 未處理 |
| 大額長款（>=$10k） | P1 | Email + Slack | CFO、財務經理 | 立即 + 每 2 小時 | 未批准 |
| 中額長款（$1k-$10k） | P2 | Email | 財務經理 | 立即 + 每日匯總 | 24h 未處理 |
| 小額長款（<$1k） | P3 | Email | 財務專員 | 每日匯總 | 無持續 |
| 大額金額不符（>=$100） | P2 | Email + Slack | 財務經理 | 立即 + 每 4 小時 | 未批准 |
| 小額金額不符（<$100） | P3 | Email | 財務專員 | 每日匯總 | 無持續 |
| PSP API 故障 | P1 | SMS + Slack | DevOps、後端團隊 | 立即 + 每 15 分鐘 | 服務未恢復 |

---

## 8. 批准與權限（Approval and Permissions）

### 8.1 人工調整流程（Manual Adjustment Process）

1. 財務人員識別差異，提交調整請求
2. 填寫調整原因（必填，最少 20 字元）
3. 上傳憑證（銀行截圖、PSP 郵件回覆等）
4. **強制批准**：調整金額 > $0 需財務經理批准

### 8.2 角色權限矩陣（Role Permission Matrix - RBAC）

| 操作 | 財務專員 | 財務經理 | CTO | Super Admin |
|------|---------|---------|-----|-------------|
| 查看對帳報表 | 是 | 是 | 是 | 是 |
| 提交調整請求 | 是 | 是 | 否 | 是 |
| 批准調整（< $1000） | 否 | 是 | 是 | 是 |
| 批准調整（>= $1000） | 否 | 否 | 是 | 是 |
| 修改對帳規則 | 否 | 否 | 是 | 是 |

---

## 9. 合規需求（Compliance Requirements）

### 9.1 審計追蹤需求（Audit Trail Requirements）

- 所有人工調整必須通過雙重批准（提交人 != 批准人）
- 調整金額 >= $1000 必須附上憑證（銀行截圖、PSP 郵件等）
- 短款事件必須在 24 小時內調查並報告
- 審計日誌必須保留 7 年（稅務合規要求）
- 每月對帳報表必須提交給外部審計師（如適用）
- 差異處理 SOP 必須每年審查和更新（合規部門責任）

### 9.2 多監管數據保留需求（Multi-Regulatory Data Retention Requirements）

| 監管機構 | 對帳數據保留期 | 遊戲審計日誌 | 關鍵要求 |
|---------|--------------|------------|---------|
| **UKGC** | 5 年 | 5 年 | 完整交易歷史，重播驗證 |
| **MGA** | 10 年 | 10 年 | 不可變時間戳，審計可追溯性 |
| **PAGCOR** | 5 年 | 5 年 | 玩家自助查詢 |
| **Curacao** | 3 年 | 3 年 | 最低要求 |
| **稅務合規** | 7 年 | - | 財務報表和交易憑證 |

**SmartAdmin 標準**：採用 **10 年**統一保留期，滿足所有監管要求。

### 9.3 數據歸檔策略（Data Archival Strategy）

系統必須根據數據年齡實施分層存儲：

| 數據層級 | 範圍 | 訪問要求 |
|---------|------|---------|
| 熱數據 | 最近 90 天 | 立即訪問（< 100ms） |
| 溫數據 | 90 天 - 2 年 | 近實時訪問（< 1s） |
| 冷數據 | 2 年 - 10 年 | 可接受恢復延遲（分鐘級） |

→ **[技術存儲實現](../../architecture/02_Finance_Service/Reconciliation_Technical.md#data-archival)** - PostgreSQL/MySQL 配置、S3 Glacier/Azure Archive 設置、分區策略

### 9.4 每月合規報告（Monthly Compliance Report）

| 報表項目 | 說明 | 監管要求 |
|---------|------|---------|
| 對帳完成率 | 每月交易 vs 已對帳交易 | UKGC/MGA 要求 99%+ |
| 差異解決時間 | 平均處理時間 | P0 < 1h, P1 < 4h |
| 人工調整記錄 | 所有調整的審計日誌 | 雙重批准 + 憑證 |
| PSP 對帳率 | 按 PSP 的對帳成功率 | 識別問題渠道 |

---

## 10. 多幣種匯率對帳（Multi-Currency Exchange Rate Reconciliation）

### 10.1 核心原則（Core Principle）

記錄交易時的匯率快照，與 PSP 實際結算匯率比對。

### 10.2 匯率對帳規則（Exchange Rate Reconciliation Rules）

| 差異範圍 | 處理方式 | 說明 |
|---------|---------|------|
| < 0.5% | 自動通過 | 正常匯率波動 |
| 0.5% - 1% | 記錄告警 | 大幅波動，記錄但不阻擋 |
| 1% - 3% | 人工審核 | 異常波動，驗證 PSP 報表 |
| > 3% | 暫停交易 | 極端波動，可能錯誤 |

---

## 11. PSP 結算週期需求（PSP Settlement Cycle Requirements）

### 11.1 結算週期配置（Settlement Cycle Configuration）

| PSP 類型 | 結算週期 | 對帳策略 |
|---------|---------|---------|
| **即時結算**（PayPal、Crypto） | T+0 | Layer 1 實時對帳 |
| **次日結算**（Alipay、部分銀行卡） | T+1 | Layer 3 每日對帳 |
| **多日結算**（信用卡、Stripe） | T+2 ~ T+7 | 延遲對帳任務 |
| **週結算**（部分銀行轉帳） | T+7 | 週對帳任務 |

### 11.2 延遲對帳需求（Delayed Reconciliation Requirements）

- PSP 結算週期 > T+1 時標記為待對帳
- 記錄預期對帳日期
- 每日 03:00 執行延遲對帳任務
- 最長等待期：T+10 後標記為異常進行人工處理

---

## 12. 退款和拒付對帳（Refund and Chargeback Reconciliation）

### 12.1 退款類型（Refund Types）

| 類型 | 發起方 | 時間框架 | 流程 |
|------|-------|---------|------|
| **主動退款**（Active Refund） | 營運商 | 交易後 1-30 天 | 營運商發起 -> PSP 確認 -> 玩家收款 |
| **拒付**（Chargeback） | 玩家銀行 | 交易後 1-120 天 | 銀行通知 -> PSP 扣款 -> 營運商申訴 |
| **預授權取消**（Pre-authorization Cancel） | 營運商 | 授權後 7 天內 | 營運商發起 -> PSP 取消 |

### 12.2 拒付告警規則（Chargeback Alert Rules）

| 告警條件 | 優先級 | 響應時間 |
|---------|--------|---------|
| 單筆拒付 > $1,000 | P0 | 立即 |
| 玩家 >= 3 次拒付（30 天內） | P1 | 4h |
| 每日拒付率 > 0.5% | P1 | 4h |
| 每月拒付率 > 1% | P0 | 立即 |

---

## 13. 負餘額對帳（Negative Balance Reconciliation）

### 13.1 負餘額場景（Negative Balance Scenarios）

| 場景 | 原因 | 典型金額 | 風險等級 |
|------|------|---------|---------|
| **拒付**（Chargeback） | 餘額不足時銀行退款 | $10 - $10,000 | HIGH |
| **遊戲回滾**（Game Rollback） | 餘額不足時作廢/取消 | $1 - $500 | MEDIUM |
| **系統錯誤**（System Error） | 重複加款後回滾 | 可變 | HIGH |
| **獎金收回**（Bonus Clawback） | 違規撤銷獎金 | $10 - $5,000 | MEDIUM |
| **匯率調整**（Exchange Rate Adjustment） | 結算 vs 交易匯率差異 | $1 - $100 | LOW |

### 13.2 自動處理規則（Auto-Processing Rules）

| 條件 | 處理方式 | 說明 |
|------|---------|------|
| 負餘額 < $10 | 自動追回 | 從下次存款扣除 |
| 負餘額 $10 - $100 | 通知 + 等待 | 7 天自動追回窗口 |
| 負餘額 > $100 | 凍結帳戶 | 必須追回後才能解凍 |
| 負餘額 > $1,000 | 凍結 + 風險審查 | 可能欺詐 |

### 13.3 壞帳認列規則（Bad Debt Recognition Rules）

| 時間條件 | 金額條件 | 操作 |
|---------|---------|------|
| > 30 天持續 | 單筆 < $50 | 30 天後自動認列 |
| > 60 天持續 | 單筆 $50-$500 | 人工審核認列 |
| > 60 天持續 | 單筆 > $500 | 法務評估追討可能性 |
| > 90 天 + VIP 玩家 | 任意 | 延長至 90 天 |
| 爭議中 | 任意 | 不認列 |
| 法律追討中 | 任意 | 不認列 |

---

## 14. AML/KYC 整合需求（AML/KYC Integration Requirements）

### 14.1 整合點（Integration Points）

| 整合點 | 對帳責任 | AML/KYC 責任 | 數據流向 |
|-------|---------|-------------|---------|
| **大額交易標記**（Large Transaction Flagging） | 標記 > $10,000 交易 | 生成 CTR 報告 | 對帳 -> AML |
| **可疑交易報告**（Suspicious Transaction Report） | 識別異常模式 | 生成 STR 報告 | 對帳 -> AML |
| **KYC 狀態關聯**（KYC Status Association） | 驗證 KYC 狀態 | 提供 KYC 等級 | KYC -> 對帳 |
| **制裁名單篩查**（Sanctions Screening） | 交易方信息 | 清單匹配 | 對帳 -> AML |

### 14.2 KYC 等級與交易限額（KYC Level and Transaction Limits）

| KYC 等級 | 單筆限額 | 每日限額 | 每月限額 | 對帳處理方式 |
|---------|---------|---------|---------|------------|
| **NONE** | $100 | $500 | $2,000 | 超出自動拒絕 |
| **BASIC** | $1,000 | $5,000 | $20,000 | 超出人工審核 |
| **ENHANCED** | $10,000 | $50,000 | $200,000 | 自動標記大額交易 |
| **FULL** | $50,000 | $200,000 | 無限制 | 僅 CTR 記錄 |

---

## 15. 跨時區對帳需求（Cross-Timezone Reconciliation Requirements）

### 15.1 時區統一需求（Timezone Unification Requirements）

**核心原則**：所有對帳必須使用統一的基準時區，確保不同數據來源的準確比對。

**數據來源考量**：
- 平台訂單（基準時區）
- PSP 報表（可能使用不同時區）
- 銀行對帳單（本地時區）
- 遊戲提供商報表（按提供商配置）

### 15.2 時間容差需求（Time Tolerance Requirements）

| 設置 | 數值 | 目的 |
|------|------|------|
| 標準窗口 | 每日對帳期間 | 定義對帳日邊界 |
| 容差範圍 | +/- 30 分鐘 | 處理跨日邊界交易 |
| 最大延遲 | 48 小時 | 涵蓋週末和假期 |

→ **[時區轉換實現](../../architecture/02_Finance_Service/Reconciliation_Technical.md#timezone-handling)** - UTC 轉換算法、時區偏移配置、跨日識別邏輯

---

## 16. 資金隔離驗證（Fund Segregation Verification）

> **行業依據**：UKGC LCCP 4.2.1, MGA Rule 44
> **處罰案例**：William Hill 620 萬英鎊（2018）- 資金隔離不當

### 16.1 驗證公式（Verification Formula）

```
信託帳戶餘額 >= 玩家錢包餘額總和 + 在途存款 - 在途提款 + 安全緩衝
```

### 16.2 驗證排程（Verification Schedule）

**執行時間**：每日 03:00 UTC（T+1 對帳完成後）

### 16.3 差異處理矩陣（Variance Handling Matrix）

| 差異類型 | 範圍 | 風險等級 | 處理方式 | SLA |
|---------|------|---------|---------|-----|
| **正差**（Bank > Expected） | < 5% | LOW | 記錄為安全緩衝 | 24h |
| | 5-10% | MEDIUM | 調查 GGR 結算延遲 | 4h |
| | > 10% | HIGH | 可能錯誤加款，立即調查 | 1h |
| **負差**（Bank < Expected） | < 1% | MEDIUM | 檢查在途資金延遲 | 4h |
| | 1-5% | HIGH | 暫停大額提款（> $10,000） | 1h |
| | > 5% | **CRITICAL** | 凍結所有提款，通知監管機構 | 立即 |

---

## 17. 風控與對帳協同（Risk Control and Reconciliation Coordination）

> **行業依據**：UKGC AML Guidance 2023, ISO 27001 5.3（職責分離）
> **處罰案例**：Entain 1,700 萬英鎊（2023）- VIP 豁免 AML 檢查

### 17.1 優先級定義（Priority Definition）

**核心原則**：風控檢查優先於財務對帳；SAR 調查優先於所有操作。

| 場景 | 風控決策 | 對帳決策 | 最終處理方式 | 優先級規則 |
|------|---------|---------|-------------|-----------|
| SAR 調查中 + 任何對帳操作 | SAR 優先 | 暫停 | 等待 SAR 結果 | **SAR > All** |
| 玩家凍結 + 對帳差異 | 凍結資金 | 需對帳 | 先完成風險調查 | **風控 > 對帳** |
| 短款 + 正常玩家 | 無風險 | 回滾餘額 | 執行對帳回滾 | **對帳優先** |
| 大額提款 + 對帳未完成 | 需審核 | 未確認 | 延遲提款 | **風控 = 對帳** |
| AML 告警 + 長款補單 | AML 審核 | 待補單 | 先完成 AML 審核 | **風控 > 對帳** |

### 17.2 VIP 無豁免原則（VIP No-Exemption Principle）

> **關鍵**：VIP 玩家必須經過與普通玩家**完全相同**的風控/對帳規則。

**VIP 身份影響範圍**：
- 可影響：審核隊列優先級（VIP 優先處理）
- 可影響：客服響應 SLA（VIP 更快響應）
- 不可影響：風控規則觸發閾值
- 不可影響：AML 檢查流程
- 不可影響：差異處理邏輯

---

## 18. 多帳戶對帳（Multi-Account Reconciliation）

> **行業依據**：UKGC AML Guidance, MGA Rule 5.3.3（關聯帳戶追蹤）

### 18.1 關聯帳戶識別來源（Related Account Identification Sources）

| 數據來源 | 識別信號 | 關聯強度 | 對帳影響 |
|---------|---------|---------|---------|
| **Neo4j Graph** | 相同設備指紋 | HIGH | 合併統計 |
| **Neo4j Graph** | 相同 IP + 併發活動 | MEDIUM | 標記審查 |
| **Neo4j Graph** | 支付卡關聯 | HIGH | 合併統計 |
| **Risk System** | 資金聚集檢測 | HIGH | 反向追蹤 |
| **AML Alert** | 多帳戶同向投注 | HIGH | 標記雙方 |

### 18.2 資金追蹤規則（Fund Tracing Rules）

| 場景 | 追蹤範圍 | 處理方式 | 對帳標記 |
|------|---------|---------|---------|
| **確認多帳戶** | 所有關聯帳戶的所有交易 | 合併 GGR 統計 | `MULTI_ACCOUNT_LINKED` |
| **資金聚集** | 輸家帳戶 -> 贏家帳戶 | 反向追蹤資金流 | `FUND_AGGREGATION` |
| **套利投注** | 雙方對沖帳戶 | 標記雙方 | `ARBITRAGE_PAIR` |
| **洗錢嫌疑** | 整個關聯網路 | 全部凍結 + SAR | `AML_SUSPECTED` |

---

## 19. 監管報告整合（Regulatory Reporting Integration）

> **行業依據**：UKGC Reporting Requirements, MGA Reporting Requirements, Gambling Levy Act 2025

### 19.1 報告類型與排程（Report Types and Schedules）

| 報告類型 | 監管機構 | 頻率 | 來源 | 格式 | SLA |
|---------|---------|------|------|------|-----|
| **月度 GGR** | UKGC/MGA | 每月 | T+1 對帳結果 | JSON/CSV | 次月 15 日 |
| **博彩稅**（Gambling Levy） | UKGC | 每月 | GGR 計算表 | UKGC Portal | 次月 28 日 |
| **玩家保護** | UKGC | 每月 | 自我排除 + 對帳凍結 | PDF | 次月 15 日 |
| **SAR 匯總** | NCA/FIAU | 每月 | AML 告警 + 對帳異常 | XML | 次月 10 日 |
| **Jackpot 報告** | All | 立即 | Jackpot 對帳表 | Webhook | 24h 內 |
| **年度報告** | UKGC/MGA | 年度 | 全年對帳匯總 | PDF | 年終後 90 天 |

### 19.2 數據一致性要求（Data Consistency Requirement）

**驗證公式**：
```
|監管報告 GGR - 對帳報告 GGR| / 對帳報告 GGR < 0.01%
```

### 19.3 博彩稅計算（Gambling Levy Calculation - UKGC 2025）

| 年度 GGR 範圍 | 稅率 | 計算基礎 |
|-------------|------|---------|
| 0 - 100 萬英鎊 | 0.1% | 對帳 GGR |
| 100 萬 - 5,000 萬英鎊 | 0.25% | 對帳 GGR |
| 5,000 萬 - 2.5 億英鎊 | 0.5% | 對帳 GGR |
| > 2.5 億英鎊 | 1.1% | 對帳 GGR |

---

## 20. 性能 SLA 需求（Performance SLA Requirements）

### 20.1 分層 SLA（Tier-Level SLA）

| 層級 | 操作 | 目標延遲 | 警告閾值 | 嚴重閾值 | 告警優先級 |
|------|------|---------|---------|---------|-----------|
| **L1 實時** | 單筆交易驗證 | < 500ms | > 800ms | > 1s | P0 |
| **L1 實時** | PSP 回調處理 | < 30s | > 45s | > 60s | P0 |
| **L2 批次** | 每小時對帳批次 | < 5min | > 10min | > 15min | P1 |
| **L2 批次** | PSP 批次查詢（1000 筆） | < 30s | > 45s | > 60s | P1 |
| **L3 每日** | T+1 全量對帳 | < 30min | > 45min | > 1h | P2 |
| **L3 每日** | 財務報表生成 | < 5min | > 10min | > 15min | P2 |

### 20.2 匹配率 SLA（Match Rate SLA）

| 指標 | 目標 | 警告 | 嚴重 |
|------|------|------|------|
| **每日匹配率** | >= 99.5% | < 99% | < 95% |
| **首次通過匹配率** | >= 95% | < 90% | < 80% |
| **差異解決時間** | < 4h | > 8h | > 24h |
| **P0 差異響應** | < 15min | > 30min | > 1h |

### 20.3 吞吐量 SLA（Throughput SLA）

| 場景 | 設計容量 | 警告閾值 | 嚴重閾值 |
|------|---------|---------|---------|
| **每秒交易數**（TPS） | 1,000 TPS | > 800 TPS | > 950 TPS |
| **每小時批次量** | 50,000 筆 | > 40,000 筆 | > 48,000 筆 |
| **每日總量** | 500,000 筆 | > 400,000 筆 | > 480,000 筆 |

---

## 21. 支付方式特定需求（Payment Method Specific Requirements）

### 21.1 支付方式分類（Payment Method Classification）

不同支付方式有不同的結算週期和對帳需求：

| 類別 | 支付方式 | 結算週期 | 對帳粒度 | 特殊考量 |
|------|---------|---------|---------|---------|
| **卡支付** | Visa/MC | T+1~T+3 | 交易級別 | 需處理拒付 |
| **電子錢包** | PayPal/Skrill | T+0~T+1 | 交易級別 | 即時通知、貨幣轉換 |
| **銀行轉帳** | SEPA/Faster Payments | T+1~T+2 | 批次級別 | 銀行參考匹配 |
| **加密貨幣** | BTC/ETH/USDT | 確認後立即 | 區塊級別 | 確認要求不同 |
| **預付卡** | Paysafecard | T+1 | 交易級別 | PIN 驗證 |
| **電信計費** | Boku/Payforit | T+7~T+30 | 每月 | 高退款率（15-25%） |

### 21.2 卡支付業務規則（Card Payment Business Rules）

**拒付處理**：
- 流程階段：初始通知 → 證據收集（7-14 天）→ 申辯 → 最終裁決（45-120 天）
- 所有拒付必須追蹤並對帳
- 對帳時必須匹配授權碼

### 21.3 加密貨幣業務規則（Cryptocurrency Business Rules）

**確認要求**：
- Bitcoin 交易需要最少確認次數才能終結
- Ethereum/USDT 交易需要最少確認次數才能終結
- 所有加密貨幣交易必須對照區塊鏈記錄驗證

### 21.4 電信計費業務規則（Carrier Billing Business Rules）

**退款準備金**：
- 結算週期：T+30（每月）
- 預期退款率：15-25%（行業平均）
- 準備金公式：`provision = revenue × historical_refund_rate × 1.2`

→ **[支付方式技術規格](../../architecture/02_Finance_Service/Reconciliation_Technical.md#payment-methods)** - 3DS 驗證實現、區塊鏈確認次數、MNO 報表解析、鏈上驗證邏輯

---

## 相關文檔（Related Documents）

### 業務邏輯參考（Business Logic Reference）
- [02-02 Payment Gateway Integration](../../source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md) - PSP 交易數據來源
- [02-04 Turnover and Game Reconciliation Analysis](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 遊戲對帳流程
- [02-06 Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) - 餘額調整邏輯

### 技術架構參考（Technical Architecture Reference）
- [Reconciliation_Technical.md](../../architecture/02_Finance_Service/Reconciliation_Technical.md) - 技術實現細節

---

**文檔版本（Document Version）**: 1.0.0
**源版本（Source Version）**: 6.0.0
**最後更新（Last Updated）**: 2026-02-08
**維護者（Maintainer）**: 財務團隊

**變更日誌（Change Log）**:
- v1.0.0 (2026-02-08): 從規範來源初始拆分，提取業務需求視圖
