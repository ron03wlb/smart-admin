# 可負擔性評估業務需求（Affordability Assessment Requirements）

> **Canonical Source**: [15-08_Affordability_Assessment.md](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md)
> **Audience**: 高階主管、合規官、產品經理
> **Related Architecture**: [Affordability_Implementation.md](../../architecture/05_Risk_Engine/Affordability_Implementation.md)
> **Last Synced**: 2026-02-08

---

## 1. 業務背景（Business Context）

可負擔性評估（Affordability Assessment）是 UKGC 2025 規則下的核心監管要求。系統評估玩家的財務能力，確保博弈活動保持在可負擔範圍內。未能合規將使營運商面臨牌照制裁、罰款和聲譽損害。

---

## 2. 監管規定摘要（Regulatory Mandate Summary）

| 監管機構 | 法規 | 生效日期 | 主要規定 |
|-----------|-----------|----------------|-----------------|
| **UKGC** | 2025 New Rules | 2025-01 | 強制性財務可負擔性評估 |
| **Netherlands** | KOA Remote Gambling | 2024 | 年度可負擔性自我申報 |
| **Germany** | GlueStV 2021 | 2021 | 每月 EUR 1,000 硬上限 |

---

## 3. UK 2025 規則 -- 評估觸發條件（Assessment Triggers）

### 3.1 年度淨損失觸發（Annual Net Loss Triggers）

UKGC 顯著降低了強制財務評估門檻：

| 觸發條件 | 2025 前門檻 | 2025 門檻 |
|-------------------|-------------------|----------------|
| 年度淨損失 | GBP 2,000/年 | GBP 500/年 |
| 存款速度 | 無 | 24 小時內多次存款達 GBP 500 |
| 高風險標記 | 營運商自行決定 | 強制性規則 |

### 3.2 評估等級（Assessment Tiers）

每個等級對營運商有特定義務：

| 等級 | 觸發條件 | 必要行動 |
|------|-------------------|-----------------|
| **Basic** | 淨損失 GBP 125 -- GBP 500 | 向玩家顯示警告訊息 |
| **Enhanced** | 淨損失 GBP 500 -- GBP 2,000 | 玩家自我申報表單 |
| **Full** | 淨損失超過 GBP 2,000 | 第三方數據驗證 |

### 3.3 自我申報數據點（Self-Declaration Data Points，Enhanced Tier）

玩家必須申報以下資訊：

| 數據點 | 目的 |
|------------|---------|
| 年收入範圍 | 建立基準收入 |
| 住房狀況（自有 / 租賃 / 家庭） | 評估固定生活成本 |
| 家庭規模 | 將被撫養人納入可負擔性考量 |
| 月可支配收入 | 確定博弈預算空間 |

### 3.4 第三方驗證來源（Third-Party Verification Sources，Full Tier）

| 來源 | 提供數據 |
|--------|--------------|
| Open Banking | 即時銀行帳戶餘額和交易歷史 |
| Credit Reference Agency (Experian, Equifax) | 信用檔案、收入驗證、財務承諾 |
| 公共財務記錄 | 破產、郡法院判決 |

---

## 4. 月度淨存款觸發（Monthly Net Deposit Trigger，UKGC 2025-02-28）

自 2025 年 2 月 28 日起，基於月度淨存款的獨立平行觸發器生效。

### 4.1 規則定義（Rule Definition）

| 屬性 | 值 |
|-----------|-------|
| 計算週期 | 滾動 30 天 |
| 公式 | 總存款減去總提款 |
| 門檻 | GBP 150 或以上 |
| 生效日期 | 2025-02-28 |

此規則與年度淨損失規則**獨立運作**，兩者可能同時觸發。

### 4.2 觸發比較矩陣（Trigger Comparison Matrix）

| 規則類型 | 計算週期 | 公式 | 門檻 | 生效日期 |
|-----------|--------------------|---------|------------|-----------|
| 年度淨損失 | 滾動 12 個月 | 損失金額減去贏款 | GBP 125 / GBP 500 / GBP 2,000 | 2025-01 |
| 月度淨存款 | 滾動 30 天 | 存款減去提款 | GBP 150 | 2025-02-28 |

### 4.3 觸發器之間的關係（Relationship Between Triggers）

玩家活動根據兩種觸發類型進行評估：

- 年度淨損失達到或超過 GBP 125 觸發 Basic 評估（警告）
- 年度淨損失達到或超過 GBP 500 觸發 Enhanced 評估（自我申報）
- 年度淨損失達到或超過 GBP 2,000 觸發 Full 評估（第三方驗證）
- 月度淨存款達到或超過 GBP 150 觸發財務脆弱性基礎檢查（可能根據綜合風險指標升級）

---

## 5. 財務脆弱性檢測（Financial Vulnerability Detection）

系統必須即時檢測財務脆弱性的行為指標。

### 5.1 脆弱性指標（Vulnerability Indicators）

| 指標 | 定義 | 嚴重程度 |
|-----------|-----------|----------|
| **追逐損失（Chasing losses）** | 玩家在損失後立即將賭注增加 50% 以上，在 20 次投注中發生 3 次或以上 | HIGH |
| **存款速度（Deposit velocity）** | 24 小時內 5 次或以上存款，總額超過 GBP 500 | MEDIUM |
| **異常模式（Unusual pattern）** | 偏離既定投注模式（時段、賭注大小、遊戲類型） | MEDIUM |

### 5.2 基於嚴重程度的回應政策（Severity-Based Response Policy）

| 嚴重程度 | 平台必要行動 |
|----------|------------------------|
| **HIGH** | 觸發完整可負擔性評估；向玩家發送關懷訊息 |
| **MEDIUM** | 顯示遊戲中關於負責任博弈的警告訊息 |
| **LOW** | 記錄指標以供監控；無即時玩家面向行動 |

---

## 6. 建議限額計算政策（Recommended Limit Calculation Policy）

當玩家完成自我申報時，平台計算建議的月度存款限額。

### 6.1 計算規則（Calculation Rules）

| 規則 | 描述 |
|------|------------|
| 基礎計算 | 申報月可支配收入的 10% |
| 家庭調整 | 如家庭規模超過 2 人，減少 20% |
| 最低下限 | 每月 GBP 50 |
| 最高上限 | 每月 GBP 2,000 |

### 6.2 限額應用政策（Limit Application Policy）

| 評估結果 | 限額應用 | 玩家選擇 |
|-------------------|-------------------|---------------|
| PASSED (Enhanced) | 顯示建議限額 | 玩家可接受或設定較低的自訂限額 |
| PASSED (Full) | 自動應用限額 | 玩家可請求較低限額但不能提高 |
| FAILED (Full) | 立即強制限額 | 無玩家覆蓋權；帳戶可能被限制 |

---

## 7. 評估有效期與續期（Assessment Validity and Renewal）

| 評估類型 | 有效期 | 續期流程 |
|----------------|----------------|-----------------|
| Enhanced（自我申報） | 3 個月 | 玩家完成新申報 |
| Full（第三方驗證） | 6 個月 | 需要新的第三方檢查 |

如玩家現有評估涵蓋所需等級且未過期，則不會觸發新評估。

---

## 8. 合規報告要求（Compliance Reporting Requirements）

### 8.1 月度報告（Monthly Reports）

| 報告 | 頻率 | 內容 |
|--------|-----------|---------|
| 評估統計 | 月度 | 總觸發次數、通過率、按等級限額分佈 |
| 脆弱性檢測 | 月度 | 按指標類型的檢測數量、解決結果 |
| 限額執行 | 月度 | 強制限額數量、玩家反應（接受 / 上訴） |

### 8.2 關鍵績效指標（Key Performance Indicators）

| KPI | 描述 |
|-----|------------|
| 評估觸發率 | 每活躍玩家群組觸發的評估數量 |
| 評估通過率 | PASSED 與總評估的比率 |
| 平均建議限額 | 所有評估中建議的月度限額平均值 |
| 脆弱性檢測率 | 每活躍玩家檢測到的脆弱性指標數量 |

---

## 9. 驗收標準（Acceptance Criteria）

可負擔性評估系統必須滿足以下驗收標準：

- [ ] **觸發檢測**：年度淨損失（GBP 125/500/2,000）和月度淨存款（GBP 150）觸發在交易結算後 30 分鐘內檢測到
- [ ] **分層評估執行**：Basic（警告訊息）、Enhanced（自我申報表單）和 Full（第三方驗證）評估根據觸發等級正確執行
- [ ] **自我申報收集**：Enhanced 等級玩家提交收入、住房狀況、家庭規模和可支配收入，驗證確保數據完整性
- [ ] **第三方整合**：Full 等級評估查詢 Open Banking API、Credit Reference Agency（Experian/Equifax）和公共記錄，P99 延遲 <5 秒
- [ ] **限額計算**：系統使用 5% 可支配收入（basic）、10%（enhanced）或第三方驗證數據（full）計算月度消費限額，並具備覆蓋審批工作流程
- [ ] **限額執行**：當玩家超過可負擔性衍生限額時，即時阻止存款和下注操作
- [ ] **評估有效性**：Enhanced（3 個月）和 Full（6 個月）評估在有效期內不會重新觸發，除非玩家超過新等級
- [ ] **脆弱性檢測**：即時監控標記存款速度（2 小時內 3 次以上存款）、極端損失（存款損失 50%）和快速增長（月度存款增加 >200%）
- [ ] **合規報告**：月度報告包括評估觸發率、通過率、平均建議限額和脆弱性檢測數量，數據差異 ≤1%

---

## 10. 相關業務需求（Related Business Requirements）

| 文檔 | 關係 |
|----------|-------------|
| Deposit Limits | 可負擔性限額輸入至存款限額系統 |
| Loss Limits | 淨損失計算與損失限額執行共享數據 |
| KYC / AML | Full 評估可能與 KYC 驗證共享數據 |
| UKGC Compliance | 可負擔性是 UKGC 整體牌照合規的子集 |

### 技術實現（Technical Implementation）

→ **[Affordability Assessment Implementation](../../architecture/05_Risk_Engine/Affordability_Implementation.md)** - Light/enhanced/full 評估演算法、銀行 API 整合、收入驗證工作流程、限額執行機制和即時監控儀表板

---

**Navigation**: [Risk and Compliance Requirements](../05_Risk_Compliance/) | [iGaming Home](../../README.md)
