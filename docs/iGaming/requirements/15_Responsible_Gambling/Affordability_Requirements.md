# 可負擔性評估與玩家保護 API 需求（Affordability & Player Protection API Requirements）

> **規範來源**: [15-07_Player_Protection_API.md](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md), [15-08_Affordability_Assessment.md](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md)
> **目標讀者**: 產品經理、合規官
> **相關架構**: [Player_Protection_API.md](../../architecture/15_Responsible_Gambling/Player_Protection_API.md)
> **最後同步**: 2026-02-09

---

## 商業價值（Business Value）

此需求文件提供的策略價值：
- **監管合規（Regulatory Compliance）**：實施 UKGC 2025 強制可負擔性評估要求，降低門檻（GBP 500/年 vs. 先前 GBP 2,000/年）
- **玩家保護（Player Protection）**：建立三層評估框架（Basic、Enhanced、Full），依淨損失等級進行升級介入
- **永續收入（Sustainable Revenue）**：目標高風險玩家 GGR 比例 < 5%，避免依賴傷害性收入導致監管罰款與聲譽損害
- **統一 API（Unified API）**：提供單一玩家保護 API 整合所有責任博弈工具（存款限制、損失限制、自我排除、會話限制），降低整合複雜度

---

## 成功指標（Success Metrics）

| 指標 | 目標 | 衡量方式 |
|--------|--------|-------------|
| 評估觸發合規率（Assessment Trigger Compliance） | 100% | 達到門檻玩家已評估 / 總門檻玩家數 |
| 評估通過率（Assessment Pass Rate） | 監控 | PASSED 評估 / 總評估數 |
| 高風險玩家 GGR 比例（High-Risk Player GGR Ratio） | < 5% | 高風險玩家 GGR / 總 GGR |
| 月淨存款觸發（Monthly Net Deposit Trigger） | 100% | 在 GBP 150/30 天門檻觸發 |
| 評估有效期追蹤（Assessment Validity Tracking） | 100% | 到期時觸發重新評估（3/6 個月） |
| 脆弱性偵測率（Vulnerability Detection Rate） | 監控 | 偵測到的行為指標 / 總活躍玩家數 |

---

## 1. 概述（Overview）

可負擔性評估是 UK Gambling Commission（英國博弈委員會）2025 年法規核心要求，用於評估玩家財務能力，確保博弈活動不超過可負擔範圍。玩家保護 API 為所有責任博弈工具提供統一介面。

---

## 2. 商業價值（Business Value）

此功能提供的價值：
- **監管合規（Regulatory Compliance）**：確保符合 UKGC 2025 可負擔性評估法規，避免罰款並維持關鍵司法管轄區（英國、荷蘭、德國）的營運執照
- **玩家保護（Player Protection）**：透過偵測脆弱博弈行為（追逐損失、存款速度）防止財務傷害，在玩家超過可負擔範圍前執行適當限制
- **風險控管（Risk Mitigation）**：降低對高風險玩家的依賴（目標 < 5% GGR），保護業務免受聲譽損害與監管審查
- **營運效率（Operational Efficiency）**：為所有責任博弈工具（存款限制、損失限制、自我排除）提供統一玩家保護 API，降低整合複雜度並確保一致執行

---

## 3. 成功指標（Success Metrics）

| 指標 | 目標 | 衡量方式 | 商業影響 |
|--------|--------|-------------|-----------------|
| UKGC 評估覆蓋率 | ≥98% 觸發玩家在 7 天內完成評估 | 追蹤 GBP 125/500/2,000 淨損失門檻與 GBP 150 月淨存款觸發的完成率 | 監管合規 - 避免執法行動 |
| 高風險玩家 GGR 比例 | <5% 總 GGR 來自高風險玩家 | 每月計算標記行為脆弱性指標玩家的 GGR | 永續收入組合 - 降低聲譽風險 |
| 評估通過率 | 監控趨勢（建立基準） | (PASSED 評估 / 總評估數) × 100% | 整體玩家群過度博弈模式的早期預警 |
| 脆弱性偵測準確度 | ≥85% 偵測到的模式確認為真實風險（非誤報） | 手動審查行為偵測（追逐損失、存款速度）vs. 玩家後續結果 | 優化偵測規則以最小化玩家摩擦並捕捉真實風險 |
| API 回應時間（P95） | <300ms 玩家端點 | 獲取保護設定、設定限制、獲取限制使用的 P95 延遲 | 玩家體驗 - 確保即時限制執行無延遲 |
| 月淨存款觸發採用率 | ≥95% 淨存款 ≥GBP 150 玩家接受財務脆弱性檢查 | 月淨存款規則（2025-02-28 生效）的審計日誌覆蓋率 | UKGC 2025 合規 - 新觸發規則執行 |

---

## 4. 監管要求（Regulatory Requirements）

### 2.1 可負擔性評估（Affordability Assessment）

| 監管機構 | 條款 | 生效日期 | 關鍵要求 |
|-----------|--------|---------------|-----------------|
| **UKGC** | 2025 New Rules | 2025-01 | 強制財務評估 |
| **Netherlands** | KOA Remote Gambling | 2024 | 年度可負擔性聲明 |
| **Germany** | GlüStV 2021 | 2021 | EUR 1,000/月 上限 |

### 2.2 UK 2025 觸發門檻（UK 2025 Trigger Thresholds）

UKGC 已降低強制財務評估門檻：

| 觸發條件 | 舊規則（2024 前） | 新規則（2025） |
|-------------------|--------------------|--------------------|
| 淨損失門檻 | GBP 2,000/年 | GBP 500/年 |
| 存款速度 | 無 | GBP 500/24 小時多次存款 |
| 高風險標記 | 營運商裁量 | 強制規則 |

---

## 3. 評估等級（Assessment Levels）

### 3.1 三層評估框架（Three-Tier Assessment Framework）

| 等級 | 觸發條件 | 必要行動 |
|-------|------------------|-----------------|
| **Basic** | 淨損失 GBP 125-500 | 顯示警告訊息 |
| **Enhanced** | 淨損失 GBP 500-2,000 | 玩家自我聲明 |
| **Full** | 淨損失 > GBP 2,000 | 第三方資料驗證 |

### 3.2 評估內容（Assessment Content）

#### 玩家自我聲明（Player Self-Declaration - Enhanced Level）
- 年收入範圍
- 住房狀況（自有/租賃）
- 家庭人數
- 月可支配收入

#### 第三方資料驗證（Third-Party Data Verification - Full Level）
- Open Banking 資料
- 信用評估機構資料
- 公開財務記錄

---

## 4. 月淨存款觸發規則（Monthly Net Deposit Trigger Rule - UKGC 2025-02-28）

### 4.1 觸發條件（Trigger Conditions）

此規則與年度淨損失規則**並行運作**：

| 規則類型 | 計算期間 | 公式 | 門檻 | 生效日期 |
|-----------|-------------------|---------|-----------|---------------|
| 年度淨損失 | 12 個月滾動 | 損失 - 獲勝 | GBP 125/500/2,000 | 2025-01 |
| **月淨存款** | 30 天滾動 | 存款 - 提款 | **>= GBP 150** | **2025-02-28** |

### 4.2 月淨存款計算（Monthly Net Deposit Calculation）

```
月淨存款 = 30 天存款總和 - 30 天提款總和
```

### 4.3 組合評估流程（Combined Assessment Flow）

```
玩家活動
    |
    +-- 年度淨損失 >= GBP 125 --> Basic 評估（顯示警告）
    +-- 年度淨損失 >= GBP 500 --> Enhanced 評估（自我聲明）
    +-- 年度淨損失 >= GBP 2,000 --> Full 評估（第三方驗證）
    |
    +-- 月淨存款 >= GBP 150 --> 財務脆弱性基本檢查
                                            |
                                            +-- 結合風險指標決定升級
```

---

## 5. 評估結果處理（Assessment Result Handling）

### 5.1 建議限制計算（Recommended Limit Calculation）

- 建議博弈支出不應超過可支配收入的 10%
- 家庭人數 > 2 調整：減少 20%
- 最小建議限制：GBP 50
- 最大建議限制：GBP 2,000

### 5.2 評估結果（Assessment Results）

| 結果 | 描述 | 行動 |
|--------|------------|--------|
| **PASSED** | 玩家博弈在可負擔範圍內 | 建議限制，玩家可接受/拒絕 |
| **FAILED** | 玩家博弈超過可負擔範圍 | 強制套用存款限制 |
| **PENDING** | 等待玩家聲明或第三方驗證 | 在完成前封鎖限制增加 |
| **EXPIRED** | 評估有效期過期 | 觸發重新評估 |

### 5.3 評估有效期（Assessment Validity）

| 評估類型 | 有效期 |
|----------------|-----------------|
| Enhanced（自我聲明） | 3 個月 |
| Full（第三方驗證） | 6 個月 |

---

## 6. 財務脆弱性指標（Financial Vulnerability Indicators）

### 6.1 行為偵測（Behavioral Detection）

| 行為模式 | 風險等級 | 偵測規則 |
|-----------------|-----------|---------------|
| **追逐損失（Chasing losses）** | 高 | 損失後，投注金額在 20 次投注內連續 3+ 次增加 > 50% |
| **存款速度（Deposit velocity）** | 中等 | 24 小時內 5+ 次存款總計 > GBP 500 |
| **異常模式（Unusual pattern）** | 中等 | 與正常投注模式顯著偏離 |

### 6.2 指標處理（Indicator Handling）

| 嚴重性 | 行動 |
|----------|--------|
| **高（High）** | 觸發 Full 評估 + 發送關懷訊息 |
| **中等（Medium）** | 顯示警告訊息 |
| **低（Low）** | 僅記錄，無行動 |

---

## 7. 玩家保護 API 需求（Player Protection API Requirements）

### 7.1 玩家端點（Player-Facing Endpoints）

| 端點 | 描述 |
|----------|-------------|
| Get protection settings overview | 單一視圖顯示所有當前保護設定 |
| Set/update deposit limits | 每日/每週/每月限制 |
| Set/update loss limits | 每日/每週/每月損失限制 |
| Request self-exclusion | 包含期間與理由 |
| Request exclusion revocation | 排除期結束後 |
| Start cooling-off period | 包含期間 |
| Set session limits | 期間與閒置超時 |
| Get limit usage | 當前使用 vs. 限制 |
| Get activity history | 分頁保護活動日誌 |

### 7.2 管理端點（Admin-Facing Endpoints）

| 端點 | 描述 |
|----------|-------------|
| List excluded players | 依狀態、類型篩選 |
| View player exclusion details | 完整排除歷史 |
| Operator-exclude player | 包含理由與通知 |
| Generate compliance reports | 月度、季度、年度 |
| Export reports | 多種格式 |
| Gamstop sync | 手動同步觸發 |
| Gamstop check | 手動玩家檢查 |

### 7.3 錯誤代碼（Error Codes）

| 代碼 | 描述 |
|------|-------------|
| RG_001 | 玩家已被排除 |
| RG_002 | 已在冷靜期中 |
| RG_003 | 存款限制已超過 |
| RG_004 | 損失限制已超過 |
| RG_005 | 無法撤銷永久排除 |
| RG_006 | 排除期未結束 |
| RG_007 | 無待處理限制變更 |
| RG_008 | 強制休息進行中 |
| RG_009 | Gamstop 同步失敗 |
| RG_010 | 無效限制設定（層級錯誤） |

---

## 8. 合規報告（Compliance Reporting）

### 8.1 報告類型（Report Types）

| 報告 | 頻率 | 內容 |
|--------|-----------|---------|
| 評估統計（Assessment statistics） | 月度 | 觸發次數、通過率、限制分佈 |
| 脆弱性偵測（Vulnerability detection） | 月度 | 偵測次數、處理結果 |
| 限制套用（Limit application） | 月度 | 強制限制次數、玩家回應 |

### 8.2 月度報告摘要欄位（Monthly Report Summary Fields）

| 欄位 | 描述 |
|-------|-------------|
| 總自我排除數（Total self-exclusions） | 新增、活躍、完成、撤銷 |
| 存款限制採用率（Deposit limit adoption） | 有限制玩家、違反數、平均限制 |
| 損失限制採用率（Loss limit adoption） | 有限制玩家、違反數 |
| 現實檢查資料（Reality check data） | 總檢查數、繼續率、平均回應時間 |
| Gamstop 同步狀態 | 執行檢查數、匹配數、同步失敗數 |

---

## 9. 效能指標（Effectiveness Metrics）

| 指標 | 目標 | 描述 |
|--------|--------|-------------|
| 評估觸發率（Assessment trigger rate） | 監控 | 每司法管轄區觸發的評估數 |
| 評估通過率（Assessment pass rate） | 監控 | PASSED / 總評估數 |
| 平均建議限制（Average recommended limit） | 監控 | 平均建議月限制 |
| 脆弱性偵測次數（Vulnerability detection count） | 監控 | 偵測到的行為指標數 |
| 高風險玩家 GGR 比例（High-risk player GGR ratio） | < 5% | 避免依賴高風險玩家 |

---

## 10. 測試場景（Testing Scenarios）

| 場景 | 預期結果 |
|----------|----------------|
| 玩家淨損失達到 GBP 125 | 顯示 Basic 評估警告 |
| 玩家淨損失達到 GBP 500 | 要求 Enhanced 評估表單 |
| 玩家淨損失達到 GBP 2,000 | 第三方驗證的 Full 評估 |
| 月淨存款 >= GBP 150 | 觸發財務脆弱性檢查 |
| 玩家提交自我聲明 | 計算並顯示建議限制 |
| 評估過期（3/6 個月） | 觸發重新評估 |
| 偵測到追逐損失 | 高嚴重性指標 + Full 評估 |

---

## 相關文件（Related Documents）

- [Deposit_Limits_Requirements.md](Deposit_Limits_Requirements.md) - 存款限制
- [Self_Exclusion_Requirements.md](Self_Exclusion_Requirements.md) - 自我排除
- [Session_Protection_Requirements.md](Session_Protection_Requirements.md) - 會話保護
- [Player_Protection_API.md](../../architecture/15_Responsible_Gambling/Player_Protection_API.md) - 技術架構

---

**Return**: [Responsible Gambling Module](../../source-archive/15_Responsible_Gambling/README.md) | [iGaming Home](../../source-archive/README.md)
