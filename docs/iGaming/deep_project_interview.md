# Deep Project Interview Transcript

**Date**: 2026-03-26
**Interviewer**: Claude (Claudian)
**Interviewee**: Ron
**Subject**: iGaming 平台需求重新確認 — 從第一性原理出發

---

## 1. Project Context

Ron 要求從第一性原理重新審視 iGaming 平台的完整需求文檔集（v5.x, 16 requirements chapters + 22 technical files + 5 ADR），目標為：
- 補充缺漏觀點
- 彙整衝突觀點
- 刪除重複內容

文檔集結構：
- `requirements/` — 17 files (Ch0–Ch15 + 參數註冊表)
- `technical/` — 22 files (Ch0–Ch15 + 5 ADR)
- `archive/` — v4 及更早版本 (400+ files)

---

## 2. Goal Confirmation

**Q**: 最終目標是產生拆分 spec 還是差異報告？
**A**: **拆分 spec** — 產出可直接進入 /deep-plan 的拆分目錄與 spec.md。

---

## 3. Boundary Division

**Q**: 16 章的邊界劃分方式？
**A**: **按業務域群組** — 將 16 章聚合成 5~7 個業務域。

### 確認的 7 個業務域分群

| # | 業務域 | 涵蓋章節 |
|---|--------|---------|
| 1 | **資金域** | Ch2 錢包系統 + Ch3 支付系統 |
| 2 | **玩家域** | Ch1 玩家管理 + Ch15 負責任博彩 + Ch12 客戶服務 |
| 3 | **遊戲域** | Ch4 遊戲整合 + Ch5 促銷與VIP |
| 4 | **風控與合規域** | Ch6 風控與合規 + Ch13 安全合規 |
| 5 | **治理與代理域** | Ch7 治理與牌照 + Ch8 代理營運 |
| 6 | **數據與基礎設施域** | Ch9 分析與報表 + Ch10 基礎設施 |
| 7 | **前端與整合域** | Ch11 前端體驗 + Ch14 第三方整合 |

Ch0 總覽作為跨域 SSOT 參考，不獨立成 split。

---

## 4. Gaps Identified — All 4 Marked as TBD

Ron 確認以下**全部**需要在 split spec 中標註為待補充：

### 4.1 通知中心 (Notification Hub)
- 目前多個模組提到「通知玩家」但缺少統一的通知管道策略
- 需要定義: push / SMS / email / in-app 的優先級、合併策略、頻率限制
- 歸屬建議: 前端與整合域 (Ch11/Ch14) 或獨立成 cross-cutting concern

### 4.2 體育博彩引擎 (Sports Betting Engine)
- Phase 4 才啟動，但 Ch4/Ch5/Ch6 多處引用體育博彩規則
- 缺少獨立的體育博彩需求章節（盤口、賠率、即時結算、Partial Cashout）
- 歸屬建議: 遊戲域 待補充項

### 4.3 後台管理 UX (Admin UX)
- CMS、活動建立、客服工具、報表操作的統一後台體驗需求
- 目前散落在各章描述
- 歸屬建議: 前端與整合域 待補充項

### 4.4 Feature Flag / Dark Launch 策略
- README 有 Gantt 圖但各章的 Phase 依賴是隱含的
- 缺少漸進式上線策略（feature flag、canary、dark launch）
- 歸屬建議: 數據與基礎設施域 或跨域 concern

---

## 5. Conflicts Resolved

### 5.1 Sports 遊戲權重 vs 流水貢獻率 — **待確認**
- Ch0 §0.8: Sports 遊戲權重 = 100%
- Ch5 §5.3: Sports 流水貢獻率 = 50%
- **Ron 的決策**: 不確定是否為同一概念，標記為 TBD
- spec 中應明確標註此矛盾，待 /deep-plan 階段釐清

### 5.2 扣款順序 — **已解決**
- Ch2 §2.4 預設: `BONUS → CASH → CREDIT` ✅ **這是正確的**
- Ch2 §2.7 情境 H: 描述為「CASH 先扣，不足部分扣 BONUS」❌ 這段描述有誤
- **Ron 的決策**: BONUS → CASH → CREDIT 為正確預設
- spec 中應標註情境 H 描述需修正

### 5.3 VIP 返水率差異
- Ch1 §1.5: Silver 0.3%, Gold 0.5%, Platinum 0.8%, Diamond 1.2%
- Ch5 §5.6: Bronze 新增 0.1%, Silver 0.3%, Gold 0.5%, Platinum 0.8%, Diamond 1.2%
- Ch5 §5.6 為 SSOT（促銷模組持有返水規則），Ch1 §1.5 應引用

---

## 6. Duplication Handling

**Q**: 如何處理 KYC/AML/自我排除的跨章重複？
**A**: **SSOT 標註 + 去重** — 在 spec 中明確標註每個概念的 SSOT 所在章節，其他章節僅引用不重複定義。

### 已識別的重複清單

| 概念 | 涉及章節 | 建議 SSOT |
|------|---------|----------|
| KYC 四級驗證規則 | Ch1 §1.4, Ch6 §6.14 | **Ch1** (玩家域) |
| AML/SAR 流程 | Ch1 §1.9, Ch6 §6.14 | **Ch6** (風控域) |
| GDPR 刪除/保留 | Ch1 §1.11, Ch13 §13.2 | **Ch13** (安全合規) |
| 自我排除機制 | Ch1 §1.8, Ch6 §6.9, Ch15 §15.2 | **Ch15** (負責任博彩) |
| 多管轄區合規矩陣 | Ch1 §1.10, Ch7 §7.9, Ch15 §15.8 | **Ch7** (治理域) 為主矩陣，各模組引用 |
| 問題博彩預警 | Ch1 §1.8, Ch15 §15.5 | **Ch15** (負責任博彩) |
| VIP 返水率 | Ch1 §1.5, Ch5 §5.6 | **Ch5** (促銷模組) |
| 設備指紋偵測 | Ch1 §1.3, Ch6 §6.9 | **Ch6** (風控域) |
| 遊戲權重表 | Ch0 §0.8, Ch2 §2.9, Ch5 §5.3 | **Ch5** (促銷模組) |

---

## 7. Execution Priority

**Q**: 最基礎的域是哪個？
**A**: **治理與代理域** — 多租戶架構是全平台的地基，所有其他域都依賴 tenant 隔離和 RBAC。

### 建議執行順序

| 優先級 | 業務域 | 原因 |
|--------|--------|------|
| 1 | 治理與代理域 | 多租戶架構是地基 |
| 2 | 資金域 | 錢包與支付是核心業務 |
| 3 | 玩家域 | 玩家管理是業務入口 |
| 4 | 遊戲域 | 遊戲整合需要錢包和玩家 |
| 5 | 風控與合規域 | 需要所有業務數據作為輸入 |
| 6 | 前端與整合域 | 整合所有後端功能 |
| 7 | 數據與基礎設施域 | 報表依賴所有業務數據 |

---

## 8. Interview Summary

### 決策清單
- ✅ 目標: 拆分 spec (7 個業務域)
- ✅ 分群方式: 按業務域
- ✅ 扣款順序: BONUS → CASH → CREDIT
- ✅ 重複處理: SSOT 標註 + 去重
- ✅ 執行基礎: 治理與代理域優先
- ⏳ Sports 權重 vs 流水貢獻率: 待確認
- ⏳ 4 個缺漏項: 標註為待補充

### Claude 的觀察
1. 文檔集整體品質很高，v5.x 重整後結構清晰
2. 主要問題是跨章重複造成的 SSOT 模糊，而非需求缺失
3. 7 個業務域的劃分與開發 Phase 高度對齊
4. 最大的架構風險在於「資金域」的複雜度（Seamless Wallet + 多幣種 + 對帳）
