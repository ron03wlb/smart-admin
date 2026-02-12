# 風險控制策略概述（Risk Control Strategy Overview）

> **Canonical Source**: [source-archive/05_Risk_Control/05-01_Risk_Framework.md](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md)
> **Audience**: Executives, Compliance Officers, Business Analysts
> **Related Architecture**: [Risk_System_Architecture.md](../../architecture/05_Risk_Engine/Risk_System_Architecture.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 4.0.0

---

## 執行摘要（Executive Summary）

線上博弈平台每年因欺詐損失超過 **12 億美元**，其中**優惠濫用**占所有欺詐案件的 **63.8%**。從 2022 年至 2024 年，iGaming 欺詐增長了 **64%**，使風險控制系統成為平台生存的關鍵。

本文件概述 iGaming 風險控制框架的業務策略、政策和 KPI。

---

## 1. 三層風險架構（業務視角）

iGaming 平台採用三層風險控制架構：

| 層級 | 模組 | 職責 | 關鍵指標 |
|-------|--------|----------------|-------------|
| **第 1 層** | 風險控制 | 基礎驗證（對沖偵測、賠率閾值、異常模式） | RiskFactor: 0 或 1 |
| **第 2 層** | 財務中心 | 狀態因子應用（WIN/LOSS/DRAW） | StatusFactor: 0%, 50%, 100% |
| **第 3 層** | 活動系統 | 遊戲權重應用（老虎機 100%、百家樂 15% 等） | GameWeight: 5%-100% |

**統一有效投注額計算（Unified Valid Turnover Calculation）**：
```
ValidTurnover = BetAmount × RiskFactor × StatusFactor × GameWeight
```

---

## 2. 風險類別與業務規則（Risk Categories & Business Rules）

### 2.1 依盛行率分類的欺詐類型（Fraud Types by Prevalence）

| 欺詐類型 | 盛行率 | 年度損失 | 主要目標 |
|------------|------------|-------------|----------------|
| **優惠濫用（Bonus Abuse）** | 69.9%（2024 Q1） | 最高 | 新玩家優惠 |
| **多帳號（Multi-Accounting）** | 15-20% | 高 | 歡迎優惠 |
| **支付欺詐（Payment Fraud）** | 10-15% | 每 $100 退單損失 $207 | 存款/提款 |
| **帳號接管（Account Takeover）** | 5-10% | 平均每次入侵損失 $481 萬 | 玩家帳號 |

### 2.2 偵測優先級（Detection Priorities）

| 優先級 | 代碼 | 場景 | SLA | 自動操作 |
|----------|------|-----------|-----|-------------|
| **URGENT** | 黑名單命中、資金聚集、模擬器偵測 | 1 小時 | 帳號凍結 |
| **HIGH** | 機器人行為、快速提款、高 ML 欺詐機率 | 2 小時 | 自動封鎖 |
| **MEDIUM** | 可疑 IP、異常投注模式 | 24 小時 | 人工審核 |
| **LOW** | 正常但有輕微異常 | 48 小時 | 允許並監控 |

### 2.3 優先級映射規則（v2.1.0）

| 維度 | 偵測項目 | LOW | MEDIUM | HIGH | URGENT |
|-----------|----------------|-----|--------|------|--------|
| **設備（Device）** | 每設備帳號數 | 1-2 | 3-4 | 5+ | 黑名單 |
| **設備（Device）** | 模擬器/VM | 否 | - | - | 是 |
| **設備（Device）** | VPN/Proxy | 否 | - | 是 | - |
| **支付（Payment）** | 每支付方式帳號數 | 1 | - | 2 | 3+ |
| **支付（Payment）** | 每提款帳號玩家數 | 1 | - | - | 2+ |
| **支付（Payment）** | 卡片 BIN vs IP 位置 | 匹配 | 不匹配 | - | - |
| **行為（Behavior）** | ML 欺詐機率 | < 0.5 | 0.5-0.8 | > 0.8 | - |
| **行為（Behavior）** | 優惠濫用模式 | 正常 | - | 最小投注 + 高流水 | 對沖投注 |
| **行為（Behavior）** | 提款率（每小時） | < 3 | 3-5 | > 5 | - |
| **圖譜（Graph）** | 關聯帳號（BFS-3） | < 3 | - | 3-4 | 5+ |
| **圖譜（Graph）** | 同步行為 | 否 | - | 是 | - |
| **圖譜（Graph）** | 資金流聚集 | 否 | - | - | 是 |

**關鍵設計決策（v2.1.0）**：
- 無評分系統（避免 59 vs 60 邊界模糊性）
- 每個規則直接映射至優先級層級
- 多個規則觸發：最高優先級獲勝

---

## 3. KYC/AML 合規需求（KYC/AML Compliance Requirements）

### 3.1 司法管轄區需求（Jurisdiction Requirements）

| 司法管轄區 | KYC 觸發 | CDD 閾值 | 關鍵需求 |
|--------------|-------------|---------------|------------------|
| **英國（UKGC）** | 註冊 | £2,000 | 強制 SAR、禁止信用卡博弈 |
| **馬爾他（MGA）** | 註冊 | €2,000 | 遵守歐盟 AML 指令、10 年執照 |
| **直布羅陀（Gibraltar）** | 註冊 | £1,000 | 專屬 AML 實務守則 |
| **庫拉索（Curacao）** | 註冊 | 不定 | 允許加密貨幣、國際認可度較低 |

### 3.2 KYC 流程（KYC Process Flow）

1. **客戶識別（Customer Identification）**：收集基本資訊（姓名、出生日期、地址）
2. **身份驗證（Identity Verification）**：政府核發的 ID + 地址證明
3. **持續監控（Ongoing Monitoring）**：交易模式、行為變化

### 3.3 加強盡職調查（Enhanced Due Diligence, EDD）

適用對象：
- 政治公眾人物（Politically Exposed Persons, PEPs）
- 高風險司法管轄區客戶
- 大額交易客戶

EDD 包含：
- 財富來源（Source of Wealth, SOW）驗證
- 資金來源（Source of Funds, SOF）驗證
- 加強交易監控

---

## 4. 監管罰款案例研究（Regulatory Penalty Case Studies）

### 4.1 2023 年歐洲罰款總結

**總罰款**：£3.48 億 / $4.43 億

| 營運商 | 罰款 | 關鍵缺失 |
|----------|------|--------------|
| **William Hill** | £1,920 萬 | AML 監督失敗，允許 20 分鐘內損失 £23,000 |
| **Entain** | £1,700 萬 | 客戶 18 個月內存款 £230,000 未進行負擔能力檢查 |
| **Betway** | £1,160 萬 | 客戶轉移 £800 萬、損失 £400 萬，4 年未進行 SOF 檢查 |

### 4.2 關鍵教訓（Key Lessons）

1. 健全的客戶盡職調查程序至關重要
2. 即時交易監控不可妥協
3. 多品牌帳號需要統一管理
4. VIP 客戶需要適當的 SOF 檢查和問題博弈干預
5. 員工安全意識培訓不可忽視

---

## 5. 風險控制 KPI（Risk Control KPIs）

### 5.1 核心欺詐指標（Core Fraud Metrics）

| 指標 | 公式 | 產業基準 | 目標 |
|--------|---------|-------------------|--------|
| **欺詐率（Fraud Rate）** | 確認欺詐 / 總交易 | < 1% | < 0.5% |
| **價值偵測率（Value Detection Rate, VDR）** | 封鎖欺詐金額 / 總欺詐金額 | > 80% | > 90% |
| **召回率（Recall Rate）** | 拒絕欺詐 / 總欺詐 | > 85% | > 94% |

**註**：JPMorgan AI 研究發現，選擇正確的欺詐 KPI 可將保護提升至少 **20%**。

### 5.2 營運效率指標（Operational Efficiency Metrics）

| 指標 | 最佳實務 | 我們的目標 |
|--------|---------------|-----------|
| **誤報率（False Positive Rate）** | < 5% | < 3% |
| **批准率（Approval Rate）** | > 95% | > 97% |
| **人工審核率（Manual Review Rate）** | < 10% | < 5% |
| **平均偵測時間（Mean Time to Detect, MTTD）** | < 1 小時 | < 30 分鐘 |

**重要**：誤報成本可能是實際欺詐成本的 **75 倍** - 控制誤報對收入至關重要。

### 5.3 博弈特定 KPI（Gaming-Specific KPIs）

| 類別 | KPI |
|----------|------|
| **收入（Revenue）** | GGR（總博弈收入）、NGR（淨博弈收入） |
| **玩家健康（Player Health）** | 留存率、促銷濫用率 |
| **合規（Compliance）** | SOF 審核完成率、STR 提交及時性（100%） |

---

## 6. 名單管理（Name List Management）

### 6.1 黑名單來源（Blacklist Sources）

- 內部確認欺詐案件
- 監管機構名單
- 產業共享資料庫
- 第三方反欺詐供應商

**英國 Cifas（國家欺詐資料庫）**：
- 1,100+ 家會員企業（包括 Bet Victor）
- 24/7 即時線上存取
- 每年數十萬筆新欺詐記錄

### 6.2 灰名單管理（Greylist Management）

針對可疑但未確認的用戶：
- 加強監控期：30/60/90 天
- 追蹤投注模式變化
- 累積風險評估

### 6.3 VIP 管理考量（VIP Management Considerations）

**888 案例警示**：因允許 NHS 員工（已知月薪 £1,400）設定 £1,300 月存款限額而被罰款。

**VIP 最佳實務**：
- 定期資金來源（SOF）審核
- 專屬客戶經理
- 更高交易限額並維持監控
- 問題博弈干預機制

---

## 7. 供應商解決方案比較（Vendor Solution Comparison）

| 供應商 | 核心能力 | 最適用於 | 關鍵指標 |
|--------|-----------------|----------|------------|
| **GeoComply** | 地理位置驗證 | 美國體育博弈 | 每月 12 億次驗證、每筆交易 350+ 檢查 |
| **Iovation/TransUnion** | 設備指紋識別 | 跨產業情報 | 每日 2,500 萬筆交易、300K 欺詐封鎖 |
| **Sift** | ML 欺詐偵測 | 全球覆蓋 | 每年 1T+ 事件、保護 90% 美國 iGaming 收入 |
| **Kambi** | 體育博弈風險 | 歐洲營運商 | €170 億+ 全球流動性、30% GGR 來自 AI 定價 |
| **Sportradar UFDS** | 假球偵測 | 體育誠信 | 600+ 營運商、分析 3,000 億賠率變化 |

### 7.1 自建 vs 購買決策矩陣（Build vs Buy Decision Matrix）

| 因素 | 自建優勢 | 購買優勢 |
|--------|-----------------|---------------|
| **成本（Cost）** | 長期較低（初期高） | 快速上市 |
| **客製化（Customization）** | 完全可客製 | 取決於供應商支援 |
| **數據安全（Data Security）** | 完全控制 | 可能與第三方共享 |
| **技術門檻（Technical Barrier）** | 需要 ML/DevOps 團隊 | 低門檻 |
| **更新速度（Update Speed）** | 取決於內部資源 | 供應商持續更新 |

**建議**：
- **新創公司**：購買成熟解決方案（Sift + Iovation）以快速啟動
- **成熟平台**：自建核心規則引擎，整合第三方 ML 和設備指紋識別

---

## 8. 配置驅動的風險控制（v2.1.0）

### 8.1 業務理由（Business Rationale）

SmartAdmin v2.1.0 引入配置驅動的風險控制，允許營運商在無需更改程式碼的情況下配置每個規則的處理方法（即時封鎖 vs 延遲審核）。

**核心原則**：
- **配置驅動（Configuration-Driven）**：每個規則的操作類型在資料庫中配置（BLOCK/FLAG/IGNORE）
- **以人為本（Human-Centric）**：異常生成風險提案供人工審核
- **平等對待（Equal Treatment）**：所有玩家經過風險控制（無 VIP 豁免）
- **營運商選擇（Operator Choice）**：營運商在無需更改程式碼的情況下決定風險策略

### 8.2 區域策略配置（Regional Strategy Configuration）

| 地區 | 監管程度 | 建議策略 |
|--------|------------------|---------------------|
| **英國/馬爾他** | 嚴格 | 更多 BLOCK 規則、更少 FLAG 規則 |
| **菲律賓/巴西** | 寬鬆 | 更多 FLAG 規則、更好的 UX |

### 8.3 業務價值（Business Value）

| 優勢 | 描述 | 業務價值 |
|-----------|-------------|----------------|
| **靈活性（Flexibility）** | 無需更改程式碼 | 快速市場回應 |
| **營運商自主權（Operator Autonomy）** | 自決風險策略 | 更高客戶滿意度 |
| **A/B 測試（A/B Testing）** | 每個市場測試不同策略 | 數據驅動決策 |
| **稽核軌跡（Audit Trail）** | 所有配置變更記錄 | 合規需求 |
| **風險降低（Risk Reduction）** | 避免硬編碼錯誤 | 系統穩定性 |

---

## 9. 認證與標準（Certification & Standards）

### 9.1 GLI-19 標準

涵蓋互動博弈系統需求：
- 玩家軟體安全
- 加密協定
- 位置偵測
- 防篡改需求

### 9.2 eCOGRA 認證

- 涵蓋 45+ 司法管轄區
- **安全與公平印章（Safe and Fair Seal）**：營運商層級
- **認證軟體印章（Certified Software Seal）**：軟體開發商層級

---

## 10. 成功因素（Success Factors）

1. **跨模組協作**：風險控制必須深度整合財務、活動、客服系統
2. **數據驅動決策**：定期審核欺詐偵測率、誤報率 KPI
3. **合規優先**：所有決策必須遵守 GDPR、AML 法規
4. **效能與安全平衡**：在不犧牲安全性的情況下維持低延遲

---

## 相關文件（Related Documents）

### 核心依賴（Core Dependencies）
- [02-03 Reconciliation Requirements](../02_Financial_Operations/Reconciliation_Requirements.md) - 餘額驗證
- [05_Risk_Compliance/KYC_AML_Requirements](KYC_AML_Requirements.md) - KYC 程序

### 合規（Compliance）
- UKGC Requirements *(planned)* - 英國監管需求
- MGA Requirements *(planned)* - 馬爾他監管需求

### 技術實施（Technical Implementation）

→ **[Risk System Architecture](../../architecture/05_Risk_Engine/Risk_System_Architecture.md)** - 五層風險控制架構、即時偵測引擎、規則配置系統、ML 模型整合和跨模組編排

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Risk & Compliance Team
