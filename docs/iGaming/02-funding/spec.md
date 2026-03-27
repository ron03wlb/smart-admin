---
title: "02-funding Spec — 資金域"
split: 02-funding
covers:
  - requirements/02_Wallet_System_錢包系統.md  # Ch2 錢包系統
  - requirements/03_Payment_System_支付系統.md  # Ch3 支付系統
depends_on:
  - 01-governance-agent  # tenant_id, RBAC, 代理信用管理
priority: 2
complexity: 極高
version: v1.0
created: 2026-03-26
status: ready-for-deep-plan
ssot_holds:
  - FX 風險分攤矩陣 (Ch2 §2.13)
  - 扣款優先序 (Ch2 §2.4)
  - Seamless Wallet 協議 (Ch2 §2.8)
ssot_references:
  - 遊戲權重表 → Ch5 §5.3 (gaming 域)
  - 風險分數邊界 → Ch6 §6.6 (risk 域)
  - KYC 提款限額 → Ch1 §1.4 (player 域)
  - Valid Bet 計算 (Standard Principal Method) → Ch0 §0.8
---

# 02-funding Spec — 資金域

> **定位**: 資金域是平台的**核心引擎**。錢包是資金心臟，支付是資金通道。所有遊戲投注、促銷發放、代理結算最終都經過此域處理。
>
> **本文檔目的**: 為 `/deep-plan` 提供自足的規劃輸入。引用而不重複原始需求文檔。

---

## 1. Scope & Boundaries

### 1.1 本域負責

| 子系統 | 來源章節 | 核心職責 |
|--------|---------|---------|
| **錢包系統** | Ch2 | 三類錢包管理、可下注餘額計算、回合生命週期、九大交易情境、Seamless Wallet 協議、對帳 |
| **支付系統** | Ch3 | 存提款流程、PSP 智慧路由、多幣種處理、Chargeback 管理、3D Secure / SCA |

### 1.2 本域不負責 (明確排除)

| 職責 | 歸屬域 | 交互方式 |
|------|--------|---------|
| 流水計算邏輯 (三層驗證) | 04-gaming (Ch5 §5.3) | 出金時同步查詢 API |
| 遊戲權重表維護 | 04-gaming (Ch5 §5.3) | 讀取配置 |
| 風險評分與規則引擎 | 05-risk-compliance (Ch6) | 交易前/後風控檢查 |
| KYC 等級與限額管理 | 03-player (Ch1 §1.4) | 提款時查詢 KYC 限額 |
| 紅利活動定義與發放邏輯 | 04-gaming (Ch5) | BONUS 入帳指令 |
| 多租戶隔離 / RBAC | 01-governance-agent (Ch7) | 所有 API 攜帶 tenant_id |

### 1.3 Dependencies

```
01-governance-agent ──→ 02-funding
  提供: tenant_id, RBAC permission, 代理信用配額, 審批工作流
  阻塞: 無 tenant_id 則無法建立錢包
```

---

## 2. Wallet System — 錢包系統 (Ch2)

> **完整定義**: `requirements/02_Wallet_System_錢包系統.md`

### 2.1 Three Wallet Types — 三類錢包

| 錢包 | 英文 | 資金性質 | 提款規則 | 初始值 |
|------|------|---------|---------|--------|
| 現金錢包 | CASH | 玩家自有資金 | 自由提款 (依 KYC 限額, 引用 Ch1 §1.4) | 0 |
| 紅利錢包 | BONUS | 促銷獎金 | 須完成流水要求後轉為現金 | 0 |
| 信用錢包 | CREDIT | 代理授信額度 (負債) | 不可提款，僅可投注 | 0 (由代理設定) |

**初始化**: 玩家帳戶建立時自動初始化三類錢包，所有餘額為 0，鎖定金額為 0。
(詳見 Ch2 §2.2)

### 2.2 Playable Balance — 可下注餘額

**公式 (SSOT — Ch2 §2.3)**:

```
Playable Balance = (CASH + BONUS) - Locked - InProgress
```

**信用模式變體**: `Playable Balance = 可用信用 (信用額度 - 已使用額度)`

**精度要求**: 計算準確率 **99.99%**。多扣 = 玩家投訴；少扣 = 平台虧損。

鎖定/解鎖規則見 Ch2 §2.3 表格 (Bet 鎖定、Win 解鎖、Rollback 解鎖、Timeout 維持、提款申請鎖定)。

### 2.3 Deduction Priority — 扣款優先序

> **SSOT — 本域持有 (Ch2 §2.4)**

**預設扣款順序**:

```
BONUS → CASH → CREDIT
```

**四層優先序覆蓋 (由高到低)**:

| Layer | 層級 | 範例 |
|-------|------|------|
| 1 | 玩家級別 | Diamond VIP 優先用 CASH |
| 2 | GP 合約 | 某 GP 不接受 BONUS 資金 |
| 3 | 遊戲級別 | Live Casino 僅限 CASH |
| 4 | 系統預設 | BONUS → CASH → CREDIT |

**執行規則**: 按層級由高到低查找第一個適用規則；主錢包不足時自動跳到下一優先序錢包；每筆交易記錄實際使用的扣款層級 (審計)。

> **KNOWN CONFLICT / 已知衝突**:
> Ch2 §2.7 情境 H 描述為「CASH 先扣，不足部分扣 BONUS」，與 §2.4 預設順序 (BONUS → CASH → CREDIT) 矛盾。
> **裁決 (Ron, 2026-03-26)**: **§2.4 為準**。情境 H 的扣款順序應為 BONUS → CASH (遵循預設)，除非該遊戲有 Layer 3 覆蓋規則。§2.7 情境 H 描述待修正。

### 2.4 Transaction Models — 交易模型

四種模型 (Ch2 §2.5):

| 模型 | 推薦度 | 說明 |
|------|--------|------|
| **Transaction-Based** | 推薦 | 每次請求獨立，TransactionId 全局唯一 |
| **Round-Based** | 推薦 (最常見) | 以 RoundId 關聯，Bet 開啟回合、Win 關閉 |
| Transfer-Based | 已棄用 | 舊架構模擬 TransferIn/TransferOut |
| Result-Only | 不建議 | 僅發送結果，無 Bet/Win 區分，風險極高 |

### 2.5 Round Lifecycle — 回合生命週期

**狀態機 (Ch2 §2.6)**:

```
[*] → OPEN (Bet 成功)
OPEN → OPEN (追加 Bet)
OPEN → CLOSED (Win/Loss 結算)
OPEN → CANCELLED (Rollback)
OPEN → TIMEOUT (2 小時無結算)
TIMEOUT → CLOSED (GP 查詢確認)
TIMEOUT → PENDING_REVIEW (GP 查詢失敗)
PENDING_REVIEW → CLOSED (人工確認結算)
PENDING_REVIEW → CANCELLED (人工確認取消)
CLOSED → ADJUSTED (Resettlement 重新結算)
```

**孤兒回合偵測 (Ch2 §2.6)**:
- 偵測頻率: 每 **15 分鐘**掃描
- 判定條件: 狀態 = OPEN 且已超過 **2 小時**
- 處理: 查詢 GP API → 有結果自動關閉 → 無結果升級人工
- SLA: 高優先 ($1,000+) 24h 內解決

### 2.6 Nine Transaction Scenarios — 九大交易情境

完整定義見 Ch2 §2.7。以下為 deep-plan 需關注的摘要:

| 情境 | 代號 | 核心挑戰 | 關鍵決策 |
|------|------|---------|---------|
| 餘額不足 | A | 多錢包扣款判斷 | 遊戲不支援 BONUS → 忽略 BONUS 餘額 |
| 併發投注 | B | 同一玩家同時多筆 | 序列化處理，防止超額扣款 |
| 超時/重試 | C | 冪等性保證 | 相同 transactionId → 回傳原始結果，快取 1h |
| 亂序請求 | D | Win 先於 Bet 到達 | **策略 3 (推薦)**: 暫存 Win 等 Bet，30min 過期 |
| 回滾/退款 | E | 反向交易 | 原始投注不存在 → 回傳 Success (冪等) |
| 重新結算 | F | 差額調整 | 支援負向調整，允許受控負餘額 |
| 免費旋轉 | G | Bet=0 的合法性 | 驗證免費旋轉配額，Win>0 正常入帳 CASH |
| 紅利錢包投注 | H | 雙錢包扣款 | **遵循 §2.4 扣款優先序** (見 2.3 衝突修正) |
| 大獎/巨額派彩 | I | 風控審查 | Win>$10K → 人工審批或自動入帳+凍結 (依 GP 合約) |

### 2.7 Seamless Wallet Protocol — Seamless Wallet 協議

> **SSOT — 本域持有 (Ch2 §2.8)**

**五個核心端點**:

| # | 端點 | 功能 | 修改餘額 |
|---|------|------|---------|
| 1 | **GetBalance** | 查詢餘額 | 否 (唯讀) |
| 2 | **Debit (Bet)** | 扣除投注 | 是 (減少) |
| 3 | **Credit (Win)** | 派彩入帳 | 是 (增加) |
| 4 | **Rollback** | 回滾交易 | 是 (退還) |
| 5 | **Adjust** | 重新結算 | 是 (增/減) |

**原子性**: 所有錢包更新為 ACID 交易，不允許中間狀態。

**冪等性**: 所有端點需 `transactionId` (全局唯一)，缺少則拒絕；重複 ID 回傳原始結果；快取保留 1h。

**Token 驗證規則 (Ch0 §0.8 + Ch2 §2.8)**:
- 簽名算法: **HMAC-SHA256**
- 有效期: **5 分鐘**
- 使用次數: **一次性** (防重放)
- **所有金額操作 (Debit / Credit / Rollback / Adjust)**: 嚴格驗證，過期即拒絕 — **包括 Win/Credit**
- **GetBalance**: 唯讀，允許寬鬆驗證
- **Token 過期 × Win 被拒**: GP 應透過 Resettlement 端點或客服工單補發派彩 (確保最終一致性)
- **Token 過期 × 流水**: 流水照計入，Credit 走 Resettlement 路徑 (Appendix E BS-01 決策)

### 2.8 Bonus Wallet Rules — 紅利錢包規則

完整定義見 Ch2 §2.9。

**流水要求**: `流水要求 = 紅利金額 x 倍率` (通常 20x-40x，由促銷活動定義)

**有效投注額 (Valid Bet)**: 引用 Ch0 §0.8 Standard Principal Method
- `ValidBet = BetAmount x RiskFactor(0/1) x GameWeight`
- WIN/LOSS → Bet Amount 全額計入; DRAW/VOID → 0

**遊戲權重 (引用 Ch5 §5.3 — gaming 域持有)**:

| 遊戲類型 | 流水貢獻率 |
|---------|----------|
| Slots | 100% |
| Sports | 50% |
| Baccarat | 可配置 (預設 10%) |
| Blackjack | 10% |
| 其他桌遊 | 5%-20% |

**轉現金規則**: 流水達標 → 自動轉 CASH；轉入上限 = 原始紅利金額 (超額利潤不轉)；過期 → 全額沒收 (到期前 24h 通知)。

### 2.9 Credit Wallet Rules — 信用錢包規則

完整定義見 Ch2 §2.10。

- **額度設定者**: 代理 (Agent)，依賴 01-governance-agent 域
- **結算週期**: 代理定義 (通常每週一 12:00 UTC)
- **結算計算**: 週期內淨盈虧；玩家淨盈利 → 代理付；玩家淨虧損 → 玩家付
- **限制**: 不可提款 (負債非資產)；僅信用資金盈利部分可提取；不適用紅利活動

### 2.10 Negative Balance Policy — 負餘額政策

**原則**: 玩家主動投注**絕不允許**負餘額。僅系統強制扣款場景 (GP Rollback 超額、Resettlement 追回) 允許受控負餘額。(Ch2 §2.11)

- 容忍窗口: 負餘額應在 **<=5 秒**內完成帳戶鎖定
- 補償順序: BONUS → CASH → 人工介入
- 告警閾值: 單帳戶 > -$100 或全平台 > -$10,000
- 處理流程: 扣款 → SUSPENDED → 封鎖登入/投注/提款 → 風控警報 → 僅 RISK_MANAGER 可解鎖

### 2.11 Reconciliation — 三層對帳模型

(Ch2 §2.12)

| 層級 | 頻率 | 數據源 | 目的 |
|------|------|--------|------|
| **即時層** | 即時 | GP Debit/Credit 請求 | 即時餘額與流水追蹤 |
| **對帳層** | 每小時 | 平台記錄 vs GP 交易報告 | 差異偵測與修正 |
| **分析層** | 每日 02:00 UTC | 數據倉庫彙總 | 每日玩家彙總報表 |

**差異處理**: <$1 自動修正; <5min 時間差自動匹配; >$100 人工警報; >10 筆缺失/hr 檢查 GP API; 連續 3+hr 失敗暫停遊戲。

**權威數據源**: 金額爭議以 **GP 為權威**。

### 2.12 Currency & Precision — 貨幣精度

(Ch2 §2.13)

- **精度**: 所有金額 **4 位小數** (0.0001)
- **累積誤差**: 不允許四捨五入誤差累積超過 0.0001
- **錢包幣種**: 一錢包一幣種 = 商戶定義的結算法幣
- **策略**: 存款即兌換 — 玩家存款 (任意幣種) → 即時匯率兌換 → 入帳結算法幣錢包
- **匯率更新**: 法幣每 5min; 加密貨幣每 30sec
- **匯率凍結**: 存款/提款確認後 15min 內凍結

### 2.13 FX Risk Sharing Matrix — FX 風險分攤矩陣

> **SSOT — 本域持有 (Ch2 §2.13)**。Ch3/Ch8 引用此定義。
> 已決策 (Ron, 2026-03-25)。所有數值透過 DB 配置，不硬編碼。

| 幣種類型 | 平台吸收範圍 | 超出部分處理 |
|---------|------------|------------|
| **法幣** | <= 0.5% 匯率波動 | 超出部分平台與玩家各 50% |
| **加密貨幣** | <= 2% 匯率波動 | 超出部分由玩家承擔 (需 UI 確認) |
| **代理結算** | 0% (代理全額承擔) | 結算日匯率為準 |

配置 param_key: `ch2.fx.fiat_absorption_pct`, `ch2.fx.crypto_absorption_pct`

**加密貨幣特殊**: 兌換時機 = 區塊鏈確認完成時; 滑價 > 2% 需玩家重新確認; 最小精度 BTC 8 位 / ETH 18 位 / USDT 6 位。

---

## 3. Payment System — 支付系統 (Ch3)

> **完整定義**: `requirements/03_Payment_System_支付系統.md`

### 3.1 Payment Methods — 支付方式

(Ch3 §3.2)

**法幣**: 銀行轉帳 (Wire/ACH/SEPA)、信用卡/借記卡 (VISA/MC/AMEX)、電子錢包 (LinePay/GCash/PayPal/Skrill/Momo)、QR Code (Alipay/WeChat Pay/PIX)

**加密貨幣**: USDT (TRC20/ERC20)、BTC (確認>=3)、ETH (確認>=12)

### 3.2 Jurisdiction Payment Restrictions — 管轄區支付限制

(Ch3 §3.2)

| 管轄區 | 限制 | 生效日期 |
|--------|------|---------|
| **英國 (UKGC)** | 禁止信用卡博彩 | 2020-04 |
| **澳洲** | 禁止線上博彩信用卡 | **2026-04** (需提前準備) |
| **德國** | 完全禁止信用卡 | 2021 |
| **瑞典** | 擴大禁止範圍 | 2025+ |

**規則**: 支付方式白名單根據玩家管轄區**動態載入**，禁止硬編碼。

### 3.3 Smart Routing — PSP 智慧路由

(Ch3 §3.4)

**路由權重因子**:

| 因子 | 權重 | 商業考量 |
|------|------|---------|
| **成功率** | 50% | 直接影響玩家體驗 |
| **手續費** | 30% | 成本控制 |
| **結算速度** | 15% | 體驗提升 |
| **VIP 優先** | 5% | 高價值差異化 |

**PSP 健康狀態判定**:

| 狀態 | 成功率 | 路由決策 |
|------|--------|---------|
| **健康 (Healthy)** | >= 80% | 正常路由 |
| **降級 (Degraded)** | 50%-80% | 降低權重，分流至備用 |
| **不可用 (Unavailable)** | < 50% | 完全切換備用；恢復需連續 3 次健康檢查通過 |

### 3.4 Deposit Flow — 存款流程

(Ch3 §3.5)

**標準流程**: 玩家發起 → 建立訂單 (Pending) → PSP 支付頁 → 支付完成 → PSP 回調 → 驗證 → 入帳錢包 → Success → 通知

**存款限額**: 單筆最低/最高 (商戶配置)、每日限額 (玩家等級)、每月限額 (VIP 等級) — 均為即時/累計檢查。

**收銀台摩擦分層 (v2.2 新增)**:

| 風險等級 | 判定依據 | 最大步驟 |
|---------|---------|---------|
| 低風險 | KYC L2+, 歷史存款>=3, 風險分數<30 | <= 3 步 |
| 中風險 | KYC L1 或風險 30-70 或首次新支付方式 | <= 5 步 |
| 高風險 | KYC L0 或風險>=70 或管轄區要求 | <= 7 步 |

### 3.5 Withdrawal Flow — 提款流程

(Ch3 §3.6)

**提款審批層級**:

| 金額範圍 | 審批層級 | SLA |
|---------|---------|-----|
| < $100 | 客服專員 (Agent) | 即時 |
| $100 - $1,000 | 客服主管 (Supervisor) | 1 小時 |
| $1,000 - $10,000 | CFO | 4 小時 |
| > $10,000 | CFO + CEO | 24 小時 |

**出金流水驗證時序 (v2.2 GAP-1, Ch2 §2.16)**:
1. 玩家提交出金 → 即時鎖定金額 (<50ms)
2. **同步查詢**流水進度 (引用 Ch5 §5.3 三層驗證 Layer 3), SLA < **200ms**
3. 流水達標 → BONUS 自動轉 CASH → 進入出金流程
4. 流水未達標 → 拒絕 + 顯示剩餘流水金額

**降級策略 (流水服務不可用)**: 查詢超時 >500ms → 排入待審核佇列 (`WAGERING_CHECK_PENDING`); **禁止自動放行**; 佇列超時 2h → 升級客服人工。

**假日 SLA 調整 (v2.2 GAP-9, Ch3 §3.6)**:
- 銀行轉帳: SLA 排除銀行非營業日
- 電子錢包/加密貨幣: 24/7，無 SLA 調整
- 假日期間提示玩家使用即時出金方式

### 3.6 Chargeback Lifecycle — 退款爭議管理

(Ch3 §3.8)

**生命週期**:
```
NOTIFICATION → EVIDENCE_COLLECTION (7d) → REPRESENTMENT
  → WON | LOST | PRE_ARBITRATION → ARBITRATION → WON | LOST
LOST → PLAYER_ACTION
```

**玩家處置規則**:
| CB 次數 | 處置 |
|---------|------|
| 1 次 | FLAG 標記監控 |
| 2 次 | 限制支付方式 (僅電子錢包/加密) |
| 3 次 | 帳戶凍結 + 人工審核 |
| 累計 > $1,000 | 永久封禁信用卡存款 |

**監控閾值**: CB Rate >= 0.5% 黃色; >= 0.8% 橙色; >= 1.0% 紅色 (通知 CFO); 單日 > 10 筆即時警報。

### 3.7 3D Secure / SCA + Crypto AML

(Ch3 §3.9)

- **歐盟 (PSD2)**: 3D Secure 2.0 強制，所有卡片交易
- **英國**: SCA, > GBP 30 或累計 > GBP 100
- **加密 AML**: 所有加密存款須錢包地址風險評分; 高風險地址 (混幣器/暗網) 拒絕; 單筆 > $10,000 等值觸發 EDD

---

## 4. Cross-Domain Interfaces — 跨域接口

### 4.1 本域消費的接口 (Inbound Dependencies)

| 來源域 | 接口 | 用途 | SLA |
|--------|------|------|-----|
| 01-governance-agent | `tenant_id` + RBAC | 所有 API 的租戶識別與權限 | 每次請求 |
| 01-governance-agent | 代理信用配額 API | 信用錢包額度設定/查詢 | 即時 |
| 03-player (Ch1) | KYC 等級 + 提款限額 | 提款前驗證 | < 50ms |
| 04-gaming (Ch5) | 流水進度查詢 | 出金流水驗證 | < 200ms |
| 04-gaming (Ch5) | 遊戲權重表 | 流水貢獻率計算 | 配置讀取 |
| 05-risk-compliance (Ch6) | 風險分數 | 收銀台摩擦分層、交易風控 | < 100ms |

### 4.2 本域提供的接口 (Outbound — 被其他域消費)

| 消費域 | 接口 | 用途 |
|--------|------|------|
| 04-gaming | Seamless Wallet 5 端點 | GP 投注/結算/回滾 |
| 04-gaming | GetBalance | 遊戲內餘額顯示 |
| 03-player | 餘額查詢 | 玩家帳戶總覽 |
| 05-risk-compliance | 交易事件流 | 即時風控監控 |
| 07-data-infrastructure | 交易日誌 | 報表與分析 |

### 4.3 Cross-Module Boundary Scenarios — 跨模組邊界情境

以下邊界情境已在 Appendix E 決策，實作時須遵循:

| 編號 | 情境 | 決策摘要 |
|------|------|---------|
| BS-01 | Token 過期 x 流水進度 | 流水照計，Credit 走 Resettlement 補發 (Option C) |
| BS-02 | 紅利過期 x 未結算投注 | 立即沒收未使用 BONUS，未結算投注 Win 歸 CASH |
| BS-03 | GP 維護 x 回合超時 | 正常超時流程，GP 維護結束後重新查詢 |
| BS-04 | 自我排除 x 代理信用結算中 | 未結算投注正常結算，新投注封鎖 (Option B) |
| BS-05 | 出金請求 x 流水驗證時序 | 同步阻塞查詢，不可用時排隊 (絕不自動放行) |
| BS-07 | KYC 升級觸發 x 進行中交易 | 進行中交易正常完成，新交易按新 KYC 限額 |
| BS-08 | 帳戶凍結 x 未結算投注 | 未結算投注正常結算入帳，新投注封鎖 |

---

## 5. Known Conflicts & Corrections — 已知衝突與修正

| # | 位置 | 衝突描述 | 裁決 |
|---|------|---------|------|
| 1 | Ch2 §2.7 情境 H | 描述「CASH 先扣，不足部分扣 BONUS」| **以 §2.4 為準**: BONUS → CASH → CREDIT。情境 H 描述待修正。(Ron 決策) |

---

## 6. Monitoring KPIs — 監控指標

### 6.1 Wallet KPIs (Ch2 §2.14)

| KPI | 目標 | 警報閾值 |
|-----|------|---------|
| 可下注餘額準確率 | 99.99% | — |
| 交易處理成功率 | >= 99.95% | — |
| 冪等性覆蓋率 | 100% | — |
| 對帳匹配率 (日) | >= 99.9% | < 99.5% |
| 孤兒回合解決率 (24h) | >= 95% | — |
| 併發處理零超額扣款 | 100% | 任何超額 = P0 |
| 亂序請求頻率 | < 0.1% | > 0.5% |
| 負餘額累計 | $0 | < -$10,000 |

### 6.2 Wallet Performance Targets (Ch2 §2.14)

| 場景 | 回應時間 |
|------|---------|
| GetBalance | < 50ms |
| 餘額不足檢查 | < 100ms |
| 併發鎖定處理 | < 200ms |
| 超時重試偵測 | 2h |
| 負餘額偵測 | 即時 |

### 6.3 Payment KPIs (Ch3 §3.10)

| KPI | 目標 | 警報閾值 |
|-----|------|---------|
| 存款成功率 | >= 95% | — |
| 掉單率 | < 1% | > 3% |
| 入帳成功率 | > 95% | < 90% |
| 平均入帳延遲 | < 30min | > 2h |
| 對帳準確率 | >= 99.9% | — |
| 支付路由最優率 | >= 85% | — |
| 零資金損失 | 100% | 任何損失 = P0 |
| PSP API 成功率 | > 99% | < 95% |

---

## 7. Implementation Notes for /deep-plan — 規劃指引

### 7.1 Suggested Sub-modules

| 子模組 | 範圍 | 複雜度 |
|--------|------|--------|
| wallet-core | 三類錢包 CRUD、可下注餘額計算、扣款優先序引擎 | 高 |
| wallet-transaction | 九大交易情境處理、回合生命週期狀態機 | 極高 |
| seamless-wallet-api | 5 端點實作、Token 驗證、冪等性 | 高 |
| wallet-reconciliation | 三層對帳模型、孤兒回合偵測 | 中 |
| payment-gateway | PSP 整合、智慧路由、健康檢查 | 高 |
| payment-deposit | 存款流程、收銀台摩擦分層、限額檢查 | 中 |
| payment-withdrawal | 提款審批、流水驗證調用、假日 SLA | 中 |
| payment-dispute | Chargeback 生命週期、證據收集、玩家處置 | 中 |
| currency-engine | 多幣種兌換、FX 風險分攤、精度控制 | 高 |

### 7.2 Critical Path & Risks

| 風險 | 影響 | 緩解 |
|------|------|------|
| 併發扣款導致超額 | 玩家投訴 + 平台虧損 | 序列化鎖 + 樂觀鎖 + 嚴格測試 |
| GP 回調延遲造成孤兒回合 | 資金鎖定 | 15min 掃描 + 2h 超時 + GP API 查詢 |
| PSP 全部降級 | 存提款中斷 | 多 PSP 冗餘 + 加密貨幣備援通道 |
| FX 滑價超過吸收範圍 | 平台或玩家損失 | 凍結窗口 + UI 確認 + 配置化閾值 |
| 流水服務不可用 | 出金阻塞 | 降級佇列 + 絕不自動放行 + 2h 升級 |

### 7.3 Key Invariants (Must-Hold Properties)

以下不變式在任何實作中必須成立:

1. **Playable Balance >= 0** (玩家主動操作場景)
2. **每筆交易有唯一 transactionId** (冪等性基礎)
3. **扣款順序可追溯** (每筆交易記錄實際使用的扣款層級)
4. **金額精度 4 位小數，誤差不累積超過 0.0001**
5. **信用餘額不可提款**
6. **負餘額僅允許系統強制扣款場景，且 <=5s 內鎖定**
7. **流水服務不可用時，絕不自動放行出金**
8. **GP 為金額爭議的權威數據源**

---

## 8. Source References — 原始需求引用

| 文檔 | 路徑 | 引用章節 |
|------|------|---------|
| Ch2 錢包系統 | `requirements/02_Wallet_System_錢包系統.md` | §2.1-§2.16 (全部) |
| Ch3 支付系統 | `requirements/03_Payment_System_支付系統.md` | §3.1-§3.11 (全部) |
| Ch0 總覽 | `requirements/00_Overview_總覽.md` | §0.8 Token 驗證, Valid Bet |
| v2.2 缺漏補充 | `requirements/PRD_Sprint_v2.2_Gap_Supplements_缺漏補充規格書.md` | GAP-1, GAP-9 |
| 跨模組邊界 | `requirements/Appendix_E_Cross_Module_Boundary_Scenarios_跨模組邊界情境矩陣.md` | BS-01~BS-08 |
| 可配置參數 | `requirements/PRD_Configurable_Parameters_Registry_可配置參數註冊表.md` | FX 參數 |
| 項目 Manifest | `project-manifest.md` | 02-funding 定義 |
