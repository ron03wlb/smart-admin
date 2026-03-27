---
title: "Split 05: 風控與合規域 — Risk & Compliance Domain Spec"
part: spec
module: 05-risk-compliance
version: v1.0
created: 2026-03-26
sources:
  - requirements/06_Risk_Compliance_風控與合規.md (Ch6)
  - requirements/13_Security_Compliance_安全合規.md (Ch13)
depends_on:
  - 02-funding
  - 03-player
  - 04-gaming
priority: 5
---

# Split 05 — Risk & Compliance Domain Spec

> **涵蓋章節**: Ch6 風控與合規 + Ch13 安全合規
> **核心職責**: 五層偵測管線、風險評分、詐欺偵測、人工審核工作流、GDPR、PCI-DSS、加密標準
> **上游依賴**: 02-funding (交易數據), 03-player (玩家數據/KYC), 04-gaming (投注數據)
> **複雜度**: 極高 — 五層管線 × 七維評估 × 多管轄區 AML × GDPR vs AML 衝突解決

---

## Table of Contents

1. [Five-Layer Detection Pipeline](#1-five-layer-detection-pipeline)
2. [Layer 1 — Synchronous Block](#2-layer-1--synchronous-block)
3. [Layer 3 — Async Risk Rules](#3-layer-3--async-risk-rules)
4. [Risk Score Boundaries (SSOT)](#4-risk-score-boundaries-ssot)
5. [Seven-Dimension Risk Assessment](#5-seven-dimension-risk-assessment)
6. [Layer 4 — Manual Review Workflow](#6-layer-4--manual-review-workflow)
7. [Layer 5 — Withdrawal Delay Check](#7-layer-5--withdrawal-delay-check)
8. [Fraud Detection](#8-fraud-detection)
9. [Collusion Detection](#9-collusion-detection)
10. [Bot Detection](#10-bot-detection)
11. [Risk Rule Governance](#11-risk-rule-governance)
12. [Rate Check Thresholds](#12-rate-check-thresholds)
13. [AML Trigger Thresholds (SSOT)](#13-aml-trigger-thresholds-ssot)
14. [Affordability Assessment](#14-affordability-assessment)
15. [Family Account Verification](#15-family-account-verification)
16. [GDPR Data Subject Rights (SSOT)](#16-gdpr-data-subject-rights-ssot)
17. [Data Retention Matrix](#17-data-retention-matrix)
18. [PCI-DSS Level 1](#18-pci-dss-level-1)
19. [PII Encryption Tiers (SSOT)](#19-pii-encryption-tiers-ssot)
20. [Encryption Key Rotation](#20-encryption-key-rotation)
21. [ISO 27001 & UK RTS](#21-iso-27001--uk-rts)
22. [MITM Protection & Session Binding](#22-mitm-protection--session-binding)
23. [Audit Requirements](#23-audit-requirements)
24. [SSOT Registry](#24-ssot-registry)
25. [Success Metrics](#25-success-metrics)

---

## 1. Five-Layer Detection Pipeline

五層偵測管線是風控系統的核心架構，涵蓋從即時阻斷到最終提款攔截的完整防線。

```
L1 同步阻斷 (<10ms) → L2 交易處理 → L3 異步風控 (~5s) → L4 人工審核 → L5 提款延遲
```

| 層級 | 名稱 | 時機 | 功能 | 業務影響 |
|------|------|------|------|---------|
| **L1** | 同步阻斷 | 投注時 (<10ms) | 僅攔截最高確信度場景 | 極少觸發，不影響體驗 |
| **L2** | 交易處理 | 投注後即時 | 正常交易流程 | 玩家立即看到結果 |
| **L3** | 異步風控 | 投注後 ~5s | 規則評估 → 生成風控提案 | 不影響投注體驗 |
| **L4** | 人工審核 | 待審核佇列 | 分析師審核提案 → 決策 | SLA 驅動 |
| **L5** | 提款延遲檢查 | 提款申請時 | 30 天回溯 → 凍結可疑金額 | 攔截最終出金 |

**設計理念**: 投注時不拒絕，提款時執行 — 確保最佳玩家體驗同時有效攔截風險。

**三層 vs 五層**: 業務視角 (同步阻斷 → 異步 BLOCK → 異步 FLAG) 為三層；加上人工審核 + 提款延遲即完整五層。兩種描述指同一系統的不同抽象層次。

---

## 2. Layer 1 — Synchronous Block

**處理時間**: < 10ms (僅快取查找)

| 規則 | 說明 | 處置 |
|------|------|------|
| 黑名單玩家 | 已確認的詐欺帳戶 | 立即拒絕投注 |
| IP 封鎖 | 已知攻擊來源 IP | 拒絕請求 |
| 帳戶凍結 | 審核中的帳戶 | 拒絕所有操作 |
| 自我排除名單 | UKGC 法規要求 | 拒絕登入與投注 |
| 未成年偵測 | 年齡驗證未通過 | 拒絕所有操作 |

**關鍵原則**: L1 **僅拒絕**，不做複雜判斷。零容忍、零延遲。

---

## 3. Layer 3 — Async Risk Rules

### 3.1 BLOCK Rules (高優先級)

| 規則 | 說明 | 偵測方式 |
|------|------|---------|
| 機器人偵測 | 自動化投注行為 | 行為模式分析 |
| 同場對沖 | 同一賽事下相反注 | 即時投注比對 |
| 同 IP 套利 | 同 IP 關聯帳戶對沖 | IP + 帳戶關聯 |
| 異常賠率 | 統計異常的賠率選擇 | 賠率偏離分析 |
| 流水操控 | 人為製造流水 | 投注模式分析 |

### 3.2 FLAG Rules (中優先級)

| 規則 | 說明 | 偵測方式 |
|------|------|---------|
| 跨場對沖 | 不同賽事間的對沖 | 歷史投注分析 |
| 低賠率洗流水 | 賠率 < 1.5 的投注 | 賠率過濾 |
| 異常投注模式 | 不符合玩家歷史的投注行為 | 行為基線比對 |
| 高頻投注 | > 10 次/分鐘 | 速率檢查 |

### 3.3 Independent Rule Triggering

**設計原則**: 每條規則獨立觸發，**不做分數累積**。取所有匹配規則中的**最高優先級**。

**為何不累積分數**:
1. **稀釋風險**: 黑名單 (嚴重) 被次要異常稀釋
2. **混合風險**: 詐欺、AML、套利無法在同一維度比較
3. **合規要求**: MGA/UKGC 要求黑名單、自我排除、未成年人**立即行動**

### 3.4 Composite Risk Escalation (v2.2)

獨立觸發為基礎原則，但新增**複合升級**機制以攔截「多維度中度異常」：

| 觸發條件 | 升級行為 | 原因 |
|---------|---------|------|
| >= 3 條 MEDIUM 規則同時觸發 | 自動升級為 **HIGH** | 多維度同時異常暗示帳戶盜用或團夥操作 |
| >= 2 條 HIGH 規則同時觸發 | 自動升級為 **URGENT** (1h SLA) | 高度可疑，需經理介入 |
| BLACKLIST + 任何其他規則 | 維持 **AUTO_REJECT**，不降級 | 黑名單永遠最高優先 |

### 3.5 ML Weighted Auxiliary Layer (Phase 3)

| 項目 | 說明 |
|------|------|
| 模型架構 | 規則分數 60% + ML 行為分數 40% = 綜合風險分數 |
| 適用範圍 | 僅 L3 異步風控，**不影響** L1 同步阻斷 |
| 部署策略 | 影子模式 (Shadow Mode) 運行 >= 30 天，與規則引擎結果對比驗證後才啟用 |
| 回退機制 | ML 服務不可用時自動回退至純規則引擎 |
| 合規要求 | ML 決策需可解釋 (SHAP/LIME)，審計日誌記錄模型版本與輸入特徵 |

---

## 4. Risk Score Boundaries (SSOT)

> **SSOT**: 本段為風險分數邊界的權威定義。所有引用方 (02-funding, 03-player, 04-gaming) 不得重複定義。

### 4.1 Primary Boundaries

| 分數範圍 | 決策 | 處置 |
|---------|------|------|
| **[0, 30)** | AUTO_APPROVE | 自動通過，即時處理 |
| **[30, 70)** | MANUAL_REVIEW | 人工審核佇列，SLA 驅動 |
| **[70, 100]** | AUTO_REJECT | 凍結資產，自動拒絕提款 |

### 4.2 Withdrawal Approval Sub-Tiers

| 優先級 | 可疑金額 | SLA | 超時處置 | 升級規則 |
|--------|---------|-----|---------|---------|
| **URGENT** | > $10,000 | 1 小時 | 自動拒絕提款 | 立即升級至經理 |
| **HIGH** | > $5,000 | 2 小時 | 自動拒絕提款 | 2 小時未處理則升級 |
| **MEDIUM** | > $1,000 | 24 小時 | 自動拒絕提款 | 24 小時未處理則升級 |
| **LOW** | $0-$1,000 | 48 小時 | 自動通過 (釋放資金) | 標準佇列 |

### 4.3 SLA Management

- 動態優先級升級: LOW -> MEDIUM -> HIGH -> URGENT (超時自動升級)
- SLA 75% 預警通知
- SLA 監控每 5 分鐘執行一次

---

## 5. Seven-Dimension Risk Assessment

| 維度 | 說明 | 關鍵指標 |
|------|------|---------|
| **交易金額** | 單筆交易規模 | 閾值梯度觸發 |
| **頻率** | 交易速度 (存款、投注、提款) | 每小時/每日速率 |
| **KYC 等級** | 客戶盡職調查完成度 | L0-L3 驗證層級 |
| **帳齡** | 帳戶註冊時間 | 天/月/年 |
| **行為模式** | 投注模式、勝率、遊戲一致性 | 異常分數 |
| **IP / 設備** | 設備指紋、位置、瀏覽器指紋 | 65,000+ 數據點 |
| **VIP 等級** | 玩家分級 | Bronze -> Diamond |

### VIP NO Exemptions

**關鍵合規規則**: VIP 玩家**不享有任何風控豁免**。VIP 狀態僅影響審核佇列優先級，**絕不影響**規則觸發或 AML 閾值。

> Entain 17M GBP 罰款前車之鑑 — 決策 N-04 (VIP 無豁免，更快 SLA)

| VIP 等級 | 人工審核 SLA | 提款審核 SLA | 可豁免項目 | 不可豁免項目 |
|---------|-------------|-------------|----------|------------|
| Diamond | 15 分鐘 | 30 分鐘 | 無 | 所有風控規則、AML 閾值、KYC 升級 |
| Platinum | 30 分鐘 | 1 小時 | 無 | 同上 |
| Gold | 1 小時 | 2 小時 | 無 | 同上 |
| Silver / Bronze | 標準 SLA | 標準 SLA | 無 | 同上 |

**明確禁止**: 任何配置或規則**不得**允許 VIP 跳過風控審核步驟。此規則不可被 L2 (商戶) 或 L3 (玩家分層) 層級覆蓋。

---

## 6. Layer 4 — Manual Review Workflow

### 6.1 Review Queue

- 排序: 優先級 (DESC) -> 建立時間 (ASC)
- 分配: 審核人員**主動認領** (Claim-based，非自動分配)

### 6.2 Three-Level Permissions

| 角色 | 權限 | 範圍 |
|------|------|------|
| **Reviewer** | 處理提案 (通過/拒絕/部分/升級) | 所有一般提案 |
| **Senior Analyst** | 處理升級案件 | 複雜案件 |
| **Compliance Manager** | 最終決策、政策配置 | 最終裁決權 |

### 6.3 Decision Types

| 決策 | 說明 | 資金處置 | 狀態變更 |
|------|------|---------|---------|
| **Approve** | 可疑金額為合法 | 釋放至錢包 | PENDING -> APPROVED |
| **Reject** | 確認違規 | 永久扣除 | PENDING -> REJECTED |
| **Partial** | 部分合法 | 釋放部分，凍結其餘 | PENDING -> PARTIAL |
| **Escalate** | 案件過於複雜 | 維持凍結 | PENDING -> ESCALATED |

**業務規則**:
- **Reject 必須附帶文字理由** (強制)
- 所有決策記入審計記錄 (**保留 7 年**)

---

## 7. Layer 5 — Withdrawal Delay Check

| 項目 | 規則 |
|------|------|
| 觸發時機 | 玩家提交提款申請時 |
| 回溯窗口 | 30 天 |
| 判斷邏輯 | 匯總所有未解決的風控提案可疑金額 |
| 觸發條件 | 可疑金額 > 0 -> 凍結提款，生成審核提案 |

---

## 8. Fraud Detection

### 8.1 Multi-Account Detection (6 Correlation Types)

| 關聯類型 | 風險等級 | 處置 |
|---------|---------|------|
| 同 IP + 同設備 | CRITICAL | 立即凍結 |
| 同 IP + 不同設備 | MEDIUM | 家庭帳戶驗證 |
| 不同 IP + 同設備 | HIGH | 帳戶合併調查 |
| 同電話號碼 | CRITICAL | 註冊時阻擋 |
| 同支付方式 | URGENT | 帳戶連結驗證 |
| 3+ 帳戶在關聯圖中 | URGENT | 網路凍結 |

### 8.2 Device Fingerprint (SSOT)

> **SSOT**: 本段為設備指紋偵測的權威定義 (Ch6 S6.9)。

- 每設備 **65,000+** 數據點 (Canvas, WebGL, Audio 指紋等)
- 反規避偵測: 模擬器 -> FLAG / VPN -> HIGH / GPS 偽造 -> HIGH
- 反指紋工具 (GoLogin 等) -> BLOCK

### 8.3 Self-Exclusion Evasion Detection (UKGC LCCP 17.1.1)

**註冊時檢查**:
- 姓名模糊匹配 (Levenshtein 距離 <= 2)
- 生日精確匹配
- 地址模糊匹配
- Gamstop API 查詢 (英國)

**匹配處理**:
- 潛在匹配 (相似度 70%+): 暫停帳戶 -> 24 小時內人工審核
- 確認匹配: 立即凍結 -> 取消投注 -> 退還現金 -> 沒收紅利

> **SSOT REF**: 自我排除機制完整定義 -> Ch15 (03-player split)。本段僅涵蓋逃避偵測。

---

## 9. Collusion Detection

### 9.1 Live Casino Collusion

| 模式 | 偵測方式 | 風險等級 |
|------|---------|---------|
| 百家樂對沖 (Hedging) | 同桌多帳戶分別押莊/閒，關聯分析 (IP/設備/支付) | CRITICAL |
| 結果共享 (Result Sharing) | Live Dealer 同桌玩家間可疑通訊時間模式 | HIGH |
| 籌碼轉移 (Chip Dumping) | 同桌固定勝負配對 (A 恆輸、B 恆贏) | URGENT |

### 9.2 Poker Collusion (Soft Play)

| 模式 | 偵測方式 | 風險等級 |
|------|---------|---------|
| 合謀加注 | 同桌 2+ 玩家協調加注/棄牌模式 | HIGH |
| 資訊共享 | 行動時間異常一致 (< 1 秒差異) | MEDIUM |
| 籌碼傾倒 | 一方持續性地向另一方轉移籌碼 (Win/Loss 比異常) | URGENT |

### 9.3 Cross-Platform Hedging

| 模式 | 偵測方式 | 風險等級 |
|------|---------|---------|
| 體育對沖 | 同一賽事在本平台與外部平台下反向注 | FLAG (透過行業共享數據偵測) |

### 9.4 Post-Detection Handling

| 風險等級 | 處置 |
|---------|------|
| MEDIUM | FLAG — 納入人工審核佇列 |
| HIGH | 暫停涉及帳戶的提款 |
| URGENT / CRITICAL | 凍結所有關聯帳戶，通知合規團隊 |

---

## 10. Bot Detection

### 10.1 Detection Dimensions (6 維)

| # | 特徵 | 偵測方式 | 閾值 |
|---|------|---------|------|
| 1 | 滑鼠軌跡 | 軌跡直線率 > 90% (非人類曲線) | FLAG |
| 2 | 點擊頻率 | 精確等間隔點擊 (標準差 < 50ms) | HIGH |
| 3 | API 呼叫模式 | 非瀏覽器 User-Agent / 缺少 Cookie 支援 | BLOCK |
| 4 | 投注決策時間 | 持續 < 200ms 決策 (無閱讀時間) | FLAG |
| 5 | Session 持續時間 | 連續 > 12 小時無中斷活動 | FLAG |
| 6 | 反自動化工具偵測 | WebDriver / Selenium / Puppeteer 特徵 | BLOCK |

### 10.2 Handling

- **FLAG**: 標記監控 7 天，累計 3+ FLAG 升級為 HIGH
- **HIGH**: 觸發 CAPTCHA 挑戰 + 人工審核
- **BLOCK**: 立即封鎖 Session，帳戶進入審核佇列

---

## 11. Risk Rule Governance

### 11.1 Configurable Parameters

| 類別 | 可配置項 | 預設值 |
|------|---------|--------|
| 閾值 | 速率限制數值、金額閾值、分數邊界 | 見各節定義 |
| 動作 | 規則觸發後的處置 (FLAG/BLOCK/URGENT) | 見規則表 |
| 權重 | 七維風險評估的各維度權重 | 均等 |
| 白名單 | IP/設備/帳戶豁免名單 | 空 |
| 時間窗口 | 速率計算的滾動窗口 | 依規則 |

### 11.2 Change Governance (Maker-Checker)

| 步驟 | 說明 | 負責人 |
|------|------|--------|
| 1 | 提出變更請求 + 影響評估 | 風控分析師 |
| 2 | 雙人審批 (Maker-Checker) | 資深分析師 + 合規經理 |
| 3 | 灰度發布 (Canary 10% 流量，觀察 24 小時) | 系統 |
| 4 | 全量生效 | 系統 |
| 5 | 回滾機制 (一鍵回滾至前一版本) | 任何審批者 |

### 11.3 Change Audit

- 所有參數變更記入審計日誌 (含變更前/後值、變更人、時間、核准人)
- 保留 **7 年** (合規要求)

---

## 12. Rate Check Thresholds

| 指標 | 閾值 | 風險等級 |
|------|------|---------|
| 存款頻率 | > 10 次/小時 | FLAG |
| 小額頻繁存款 | > 20 筆 < EUR50/天 | FLAG (結構化存疑) |
| 提款頻率 | > 5 次/小時 | HIGH |
| 投注頻率 | > 10 次/分鐘 | FLAG (機器人存疑) |
| 24 小時存款速率 | 5+ 筆總計 > GBP500 | MEDIUM |
| 快速提款 | 存款後 24 小時內提款 | FLAG (測試帳戶存疑) |

---

## 13. AML Trigger Thresholds (SSOT)

> **SSOT**: 本段為 AML/SAR 流程的權威定義 (Ch6 S6.14)。所有引用方不得重複定義。

### 13.1 CDD/EDD/PEP Thresholds

| 事件 | 閾值 | 動作 | 管轄區 |
|------|------|------|--------|
| CDD 觸發 | 單筆 >= EUR2,000 / GBP2,000 | 要求 L1 身份驗證 | 英國/馬爾他 |
| EDD 觸發 | 單筆 >= EUR10,000 或累計 >= EUR50,000 | 要求 L3 + 資金來源 | 全部 |
| PEP 篩查 | 姓名匹配 >= 95% | 自動觸發 EDD | 全部 |
| PEP 模糊匹配 | 姓名匹配 70-95% | 24 小時內人工審核 | 全部 |
| 結構化偵測 | >= 3 筆 EUR1,500-EUR1,999 / 24 小時 | FLAG + SAR 調查 | AML 法規 |

### 13.2 SAR Submission Timelines by Jurisdiction

| 管轄區 | 時限 | 報告機構 |
|--------|------|---------|
| 英國 (UKGC) | **7 個工作天** | NCA |
| 馬爾他 (MGA) | 15 天 | FIAU |
| 直布羅陀 | 7 天 | GFIU |
| 庫拉索 | 30 天 | Gaming Control Board |
| 菲律賓 | 5 個工作天 | AMLC |

---

## 14. Affordability Assessment

> **SSOT REF**: 可負擔性評估完整定義以 Ch15 (03-player split) 為權威來源。本段為風控視角的觸發閾值。

### UKGC 2025+ Triggers

| 累計存款 | 動作 | 要求 |
|---------|------|------|
| GBP125-GBP500 | 警告 | 顯示可負擔性提醒 |
| GBP500-GBP2,000 | 自我聲明 | 玩家填寫財務狀況 |
| > GBP2,000 | 第三方驗證 | 銀行對帳單、薪資證明 |
| 淨虧損 > GBP2,000 / 90 天 | 財務評估 | MLRO 審查 |

---

## 15. Family Account Verification

### 15.1 Family Determination Criteria

| 關聯信號 | 權重 | 說明 |
|---------|------|------|
| 同一住址 (KYC 地址匹配) | 高 | 主要判定依據 |
| 同一 IP 地址 (持續性，非偶發) | 中 | 需排除 VPN / 公共 WiFi |
| 同一支付方式 (銀行帳戶/信用卡) | 高 | 共用支付方式 |
| 同一設備指紋 | 極高 | 同一物理設備 |
| 姓名相似度 (Levenshtein <= 3 + 同地址) | 中 | 家庭成員姓名相近 |

**判定邏輯**: 任一「高」或「極高」信號 + 至少一個「中」信號 -> 觸發家庭帳戶驗證流程。

### 15.2 Verification Flow

1. 偵測關聯信號 -> 觸發驗證
2. 各帳戶獨立完成 KYC (確認為不同自然人)
3. 人工審核 (確認不同身份)
4. 標記為「已驗證家庭帳戶」
5. 限制: 不可共參同一活動、不可帳間轉帳、不可配對投注

### 15.3 UKGC Family-Level Affordability

- 已驗證家庭帳戶的總存款/損失**合併計算**可負擔性閾值
- 單一家庭成員觸發閾值 -> 所有家庭成員納入評估範圍

---

## 16. GDPR Data Subject Rights (SSOT)

> **SSOT**: 本段為 GDPR 刪除/保留的權威定義 (Ch13 S13.2)。

### 16.1 Rights SLA

| 權利 | SLA | 說明 |
|------|-----|------|
| 存取權 (SAR) | 30 天 (複雜案件可延至 90 天) | 提供所有個人數據副本 |
| 刪除權 | 37 天流程 (7 天確認 + 30 天冷靜期) | 加密銷毀 (Crypto-shredding) |
| 可攜權 | 30 天 | 機器可讀格式 (JSON/CSV/XML) |
| 更正權 | 30 天 | 更正不準確的個人數據 |
| 限制處理權 | 即時 | 暫停數據處理 |

### 16.2 Deletion Flow

| 步驟 | 時間 | 說明 |
|------|------|------|
| 1. 提交刪除請求 | Day 0 | 玩家提交 |
| 2. 確認郵件 | Day 0 | 7 天有效確認連結 |
| 3. 玩家確認 | Day 0-7 | 點擊確認 |
| 4. 冷靜期 | Day 7-37 | 期間可恢復 (Day 7/21/29 提醒) |
| 5. 加密銷毀 | Day 37 | 不可逆，銷毀加密金鑰 |
| 6. 刪除證明 | Day 37+ | 發送至玩家 |

### 16.3 Deletion Exceptions

| 條件 | 暫停期限 | 法律依據 |
|------|---------|---------|
| **AML 調查中** | <= 1 年 (可延長) | AML 法定義務 > GDPR 刪除權 |
| 訴訟中 | <= 10 年 | 法律訴訟特權 |
| 待結流水 | <= 90 天 | 合約義務 |
| 餘額 > $0 | 無限期 (通知玩家) | 合約義務 |
| 稅務審計 | <= 7 年 | 稅法要求 |

### 16.4 GDPR vs AML Conflict Resolution

衝突解決優先級:
1. **AML 調查中**: 法定義務 (AMLD/POCA) 優先於 GDPR 刪除權。暫停刪除，但須通知玩家「因法律要求暫時無法處理您的刪除請求」(**不可透露調查詳情 — 反 tipping-off**)。
2. **AML 調查結束**: 調查結束後 30 天內重啟刪除流程。
3. **AML 資料保留期**: 交易記錄匿名化後保留 5-7 年 (依管轄區)，期間 PII 已透過加密銷毀不可還原。
4. **審計追蹤**: 所有暫停/恢復動作記入合規審計日誌。

---

## 17. Data Retention Matrix

| 數據類型 | 保留期限 | 刪除後處理 |
|---------|---------|----------|
| 姓名/地址 (身份資料) | 5 年 | 刪除 |
| KYC 文件 | 5 年 | 刪除 |
| 交易記錄 | 7 年 | 匿名化 player_id |
| 遊戲歷史 | — | 匿名化 (UUID 替換) |
| 稅務記錄 | 7 年 | 匿名化 |
| 違規記錄 | 永久 | 永久保留 (詐欺防範) |
| 自我排除記錄 | 永久 | 永久保留 (合規要求) |

**墓碑記錄**: 刪除後保留最小記錄用於防止重複註冊、GDPR 合規審計、月度刪除統計、法律保護。

---

## 18. PCI-DSS Level 1

### Key Requirements

| 要求 | 說明 |
|------|------|
| 安全網路 | 防火牆、無預設密碼 |
| 持卡人數據保護 | 加密、**永不存儲完整 PAN** (主帳號) |
| Token 化 | 以 Token 替代卡片數據 |
| 密碼雜湊 | 單向雜湊 (不可逆) |
| TLS 1.2+ | 所有支付 API 強制加密傳輸 |
| 定期測試 | 年度滲透測試、漏洞掃描 |

### Card Data Restrictions

- 信用卡禁令: 英國 (2020), 德國 (2021), 澳洲 (2026)
- 加密貨幣需 AML 錢包風險評分

---

## 19. PII Encryption Tiers (SSOT)

> **SSOT**: 本段為加密標準的權威定義 (Ch13 S13.4)。

### 19.1 Encryption Classification

| 欄位 | 風險 | 保護方式 | 脫敏規則 |
|------|------|---------|---------|
| 姓名 | 高 | 靜態加密 (Static Encrypt) | 保留首尾字 |
| 電話 | 極高 | 加密 + 盲索引 (Blind Index) | 保留前 3 + 末 3 |
| Email | 極高 | 加密 + 盲索引 | 保留前 2 + 域名 |
| 銀行帳號 | 極高 | 加密 + 盲索引 | 僅末 4 碼 |
| 密碼 | 極高 | 單向雜湊 (One-way Hash) | 不可還原 |
| IP | 中 | Hash (用於索引) | — |

### 19.2 Role-Based Masking Matrix

| 角色 | 電話 | Email | 銀行帳號 |
|------|------|-------|---------|
| 玩家 (本人) | 明文 (2FA) | 明文 | 僅末 4 碼 |
| L1 客服 | 脫敏 | 脫敏 | 無權限 |
| 風控人員 | 明文 (需審批) | 明文 (需審批) | 明文 (需審批) |
| DBA | 僅密文 | 僅密文 | 僅密文 |

### 19.3 Core Principles

- **零信任**: 假設所有系統 (DB、備份、日誌) 皆可能被攻破
- **應用層加密**: 寫入數據庫前加密
- **可搜索加密**: 透過盲索引 (Blind Index) 查詢加密數據
- **加密銷毀**: 銷毀加密金鑰使數據永久不可恢復

---

## 20. Encryption Key Rotation

| 金鑰類型 | 輪替週期 | 策略 | 說明 |
|---------|---------|------|------|
| Master Key (KEK) | 365 天 | 手動 + 雙人控制 | HSM 保護，輪替需 CISO + CTO 審批 |
| Per-Player Data Key (DEK) | 90 天 | 自動漸進式 | 背景任務逐批重加密，不影響服務可用性 |
| Blind Index Salt | 180 天 | 批次重建 | 需重建所有盲索引 (排程於低峰窗口) |
| TLS 證書 | 90 天 | 自動 (Let's Encrypt / ACM) | 到期前 30 天自動更新 |
| API 金鑰 | 見 Ch14 S14.5 | 自動 | 由 06-frontend-integration 定義 |

**輪替期間服務可用性保證**:
- 新舊金鑰共存期: 輪替期間系統同時接受新舊金鑰解密 (雙金鑰模式)
- 漸進式重加密: 每批 1,000 筆記錄，批間間隔可配置 (預設 100ms)
- 監控: 重加密進度儀表板、失敗記錄告警
- 回滾: 若重加密失敗率 > 0.1%，自動暫停並告警

---

## 21. ISO 27001 & UK RTS

### 21.1 ISO 27001:2022 Key Controls (P0)

| 控制項 | 說明 |
|--------|------|
| 5.1 | 資訊安全政策 (文件化) |
| 5.3 | 職責分離 |
| 8.2 | 特權存取管理 (PAM) |
| 8.3 | 資訊存取限制 (行級安全) |
| 8.5 | 多因子認證 (TOTP/WebAuthn) |
| 8.12 | 數據防洩漏 (DLP) |
| 8.15 | 全面日誌記錄 |
| 8.20 | 網路安全 — 對外 API: TLS 1.2+, 內部服務間: TLS 1.3 |
| 8.25 | 安全 SDLC |
| 8.28 | 安全編碼 (OWASP) |

### 21.2 UK RTS Section 4

| 要求 | 說明 |
|------|------|
| 4.1 | ISO 27001 合規 + 定期風險評估 |
| 4.2 | 時間同步: NTP, < 1 秒偏差, UTC 日誌 |
| 4.3 | 環境隔離: DEV/UAT/PROD 完全分離 |
| 4.4 | 存取控制: MFA + RBAC + 審計 |
| 4.5 | 第三方開發控制: 代碼審查、漏洞掃描 |
| 4.6 | 特權工具: 獨立認證、操作審計 |

---

## 22. MITM Protection & Session Binding

### 22.1 MITM Attack Vectors

| 威脅 | 偵測要求 |
|------|---------|
| TLS 降級 | 偵測強制使用弱 TLS |
| SSL 剝離 | 驗證 HTTPS 強制 |
| 證書偽造 | 證書固定 (Certificate Pinning) |
| DNS 欺騙 | DNS 驗證 |
| Session 劫持 | 設備指紋 + IP 同時變更偵測 |

### 22.2 Session Binding

- 綁定設備指紋 + IP
- 設備變更觸發重新驗證
- 「不可能旅行」偵測 (IP 地理變化速度超過物理可能)

---

## 23. Audit Requirements

### 23.1 Audit Record per Risk Proposal

每筆風控提案須記錄:
- 提案 ID、建立時間、觸發規則、風險分數、優先級
- 審核人、決策時間、決策類型、核准/拒絕金額
- 文字說明、補償狀態
- MLRO 簽核 (如適用)

### 23.2 Retention & Protection

- 保留期限: 最少 **7 年** (不可篡改)
- 保護方式: **Hash 鏈保護** (確保記錄未被竄改)
- 監管存取: 24 小時內提供給監管機構

### 23.3 Reporting Frequency

| 頻率 | 報告內容 |
|------|---------|
| 每日 | 多帳戶偵測、帳戶凍結、自我排除逃避 |
| 每週 | 偵測率趨勢、假陽性分析、設備指紋穩定性 |
| 每月 | 合規報告、多帳戶損失估計、系統效能評估 |

---

## 24. SSOT Registry

### This Split HOLDS (權威定義)

| SSOT 項目 | 來源章節 | 說明 |
|----------|---------|------|
| AML/SAR 流程 | Ch6 S6.14 | AML 觸發閾值、SAR 提交時限 |
| 風險分數邊界 | Ch6 S6.6 | [0,30) / [30,70) / [70,100] 三段 |
| GDPR 刪除/保留 | Ch13 S13.2 | 資料主體權利、刪除流程、保留矩陣 |
| 設備指紋偵測 | Ch6 S6.9 | 65,000+ 數據點、反規避偵測 |
| 加密標準 | Ch13 S13.4 | PII 加密分級、角色脫敏、金鑰輪替 |

### This Split REFERENCES (引用，不重複定義)

| 引用項目 | SSOT 來源 | 所屬 Split |
|---------|----------|-----------|
| KYC 四級驗證規則 | Ch1 S1.4 | 03-player |
| 自我排除機制 | Ch15 S15.1 | 03-player |
| 遊戲權重表/流水貢獻率 | Ch5 S5.3 | 04-gaming |
| 平台配置三層覆蓋 | Ch0 S0.10 | Cross-cutting |
| 多管轄區合規矩陣 | Ch7 S7.9 | 01-governance-agent |

---

## 25. Success Metrics

### 25.1 Risk Detection KPIs

| KPI | 目標 | 說明 |
|-----|------|------|
| 詐欺率 | < 0.5% | 確認詐欺 / 總交易 |
| 價值偵測率 (VDR) | > 90% | 攔截詐欺金額 / 總詐欺金額 |
| 召回率 | > 94% | 拒絕詐欺 / 總詐欺 |
| 假陽性率 | < 3% | 拒絕合法 / 總合法 |
| 通過率 | > 97% | 通過交易 / 總交易 |
| 人工審核率 | < 5% | 人工審核 / 總交易 |
| 平均偵測時間 (MTTD) | < 30 分鐘 | 詐欺發生 -> 偵測 |
| 審核回應時間 (P95) | < 1 小時 | 提案建立 -> 決策 |
| SLA 合規率 | >= 95% | 在 SLA 內完成的提案比例 |
| 多帳戶偵測率 | >= 95% | — |
| 自我排除逃避率 | < 1% | — |

### 25.2 Compliance KPIs

| 指標 | 目標 |
|------|------|
| GDPR SAR 回應率 | 100% (30 天內) |
| PCI-DSS 合規 | Level 1 認證 |
| 滲透測試 | 每年至少 1 次 |
| 數據洩漏事件 | 0 |
| 加密覆蓋率 | 100% PII |
| SAR 提交及時率 | 100% (在法定期限內完成) |
| 投注通過率 | > 99.9% (L1 極少觸發) |
| 合規零罰款 | 100% (無監管罰款) |
