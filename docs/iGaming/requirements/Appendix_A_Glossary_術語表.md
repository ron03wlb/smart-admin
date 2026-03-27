---
title: "附錄 A: 統一術語表"
part: requirements
module: appendix
version: v2.0
created: 2026-03-24
---

# 附錄 A：統一術語表

> 本文件為全平台 16 章業務需求文檔的統一術語表，涵蓋 iGaming 業務、合規監管、技術架構三大領域。
> 如有衝突，以各章節的 SSOT 定義為準。

---

## A.1 iGaming 業務術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| 投注額 | Bet Amount | 單次下注的原始金額，確認後不可變。API 欄位: `betAmount` | Ch0, Ch2 |
| 有效投注額 | Valid Bet | 經風控與遊戲權重調整後的投注額。公式: `BetAmount × RiskFactor(0/1) × GameWeight` | Ch0, Ch2, Ch5 |
| 流水 | Turnover | 一段時間內有效投注額的累計總和 | Ch0, Ch2 |
| 流水要求 | Wagering Requirement | 紅利解鎖需達到的有效投注總額門檻。公式: `紅利金額 × 倍數 (20×–40×)` | Ch0, Ch2, Ch5 |
| 遊戲權重 | Game Weight | 不同遊戲對流水貢獻的折算比例 (5%–100%)。**SSOT 由 Ch5 持有，各活動可獨立配置** | Ch0, Ch2, Ch5 |
| 毛博彩收入 | GGR (Gross Gaming Revenue) | `總投注額 - 總派彩` | Ch0, Ch8, Ch9 |
| 淨博彩收入 | NGR (Net Gaming Revenue) | `GGR - 紅利成本 - 稅金` | Ch0, Ch9 |
| 玩家返還率 | RTP (Return to Player) | `總派彩 / 總投注額 × 100%`，典型範圍 94%–98% | Ch0, Ch4 |
| 莊家優勢 | House Edge | `1 - RTP` | Ch0 |
| 可下注餘額 | Playable Balance | `(CASH + BONUS) - 鎖定金額 - 進行中投注`，準確度要求 99.99% | Ch2 |
| 現金錢包 | CASH Wallet | 玩家自有現金，可自由提款（依 KYC 限額） | Ch2 |
| 紅利錢包 | BONUS Wallet | 促銷獲得的資金，需完成流水要求後轉入 CASH | Ch2, Ch5 |
| 信用錢包 | CREDIT Wallet | 代理授信額度，僅用於投注不可提款 | Ch2, Ch8 |
| 鎖定錢包 | LOCKED Wallet | 鎖定資金（如負結轉、暫扣等），無法投注或提款 | Ch2 |
| 部分結算 | Partial Cashout | 體育投注中提前結算部分金額，剩餘繼續。公式: `結算金額 = 投注金額 × 比例 × 當前賠率 / 原始賠率` | Ch4 |
| 免費旋轉 | Free Spins | 免費的老虎機旋轉次數，價值 $0.10–$1.00/次 | Ch5 |
| 返水/返現 | Cashback / Rebate | 虧損或流水退還，進入 CASH 錢包（非 BONUS），獨立於流水要求 | Ch5 |
| 遊戲供應商 | GP (Game Provider) | 外部遊戲內容供應商 | Ch4 |
| 無縫錢包 | Seamless Wallet | GP 與平台的錢包整合協議，5 端點: GetBalance / Debit / Credit / Rollback / Adjust | Ch2, Ch4 |
| 免費試玩 | Demo / Free Play | 非真錢模式，不計入流水，註冊前可用 | Ch4 |
| 佔成模式 | Position Holding | 代理宣告承擔風險百分比，盈虧按比例分配 | Ch8 |
| 負結轉 | Negative Carryover | 代理本期虧損結轉至下期佣金計算，**不影響**信用額度 | Ch8 |
| 白標 | White Label | 品牌定制的平台授權方案，$3K–$8K/月 | Ch0, Ch7 |
| 交鑰匙 | Turnkey | 全套營運支持方案，$50K–$150K | Ch0 |

---

## A.2 玩家與分群術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| VIP 等級 | VIP Tier | Bronze → Silver → Gold → Platinum → Diamond，按月度流水評估 | Ch1, Ch5 |
| RFM 分群 | RFM Segmentation | 依近期性 (Recency)、頻率 (Frequency)、金額 (Monetary) 三維分群，每日 02:00 UTC 更新 | Ch1 |
| 玩家生命週期價值 | LTV (Lifetime Value) | 玩家全生命週期貢獻的淨收入，$500–$5,000 | Ch0 |
| 獲客成本 | PAC (Player Acquisition Cost) | 獲得單個玩家的成本，$10–$50 | Ch0 |
| 首存轉化率 | FDC (First Deposit Conversion) | 註冊用戶 → 首次存款的轉化率，目標 30%–40% | Ch0 |
| 大R 玩家 | Whale | 月流水 > $100,000 的高價值玩家 | Ch1 |
| 問題博彩者 | Problem Gambler | 呈現過度博彩行為（高頻存款、追損、長時間遊戲）的玩家 | Ch1, Ch15 |
| 紅利濫用者 | Bonus Abuser | 利用紅利機制獲取不當利益的玩家 | Ch1, Ch5 |
| 360° 玩家視圖 | 360° Player View | 整合身份、財務、投注、風控、客服標籤的完整玩家資訊面板 | Ch12 |

---

## A.3 合規與監管術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| KYC | Know Your Customer | 身份驗證，四級: L0 (Email) → L1 (證件) → L2 (地址) → L3 (銀行+資金來源) | Ch1, Ch6 |
| AML | Anti-Money Laundering | 反洗錢合規，涵蓋 CDD/EDD/SAR/交易監控 | Ch1, Ch6 |
| CDD | Customer Due Diligence | 客戶盡職調查（標準等級） | Ch1, Ch6 |
| EDD | Enhanced Due Diligence | 加強盡職調查，觸發條件: 單筆 ≥ $10,000 / 30 天累計 ≥ $25,000 / 高風險國家 / PEP | Ch1, Ch6 |
| SAR | Suspicious Activity Report | 可疑活動報告，法定提交時限依管轄區 (UK 7 天 / Malta 15 天 / PH 5 天) | Ch1, Ch6 |
| PEP | Politically Exposed Person | 政治公眾人物，自動觸發 EDD | Ch1, Ch6 |
| MLRO | Money Laundering Reporting Officer | 洗錢防制報告官，獨立權限提交 SAR | Ch1, Ch6 |
| GDPR | General Data Protection Regulation | 歐盟個人資料保護規範 | Ch13 |
| PCI-DSS | Payment Card Industry Data Security Standard | 支付卡產業安全標準，平台須達 Level 1 | Ch13 |
| GLI-19 | Gaming Laboratories International Standard 19 | RTP 測試標準，長期偏差不得超過 ±0.5% | Ch0, Ch4 |
| UKGC | UK Gambling Commission | 英國博彩委員會，最嚴格監管標準 | Ch0, Ch1, Ch4, Ch7, Ch15 |
| MGA | Malta Gaming Authority | 馬爾他博彩管理局，License 10 年有效，需 EU AML 5 合規 | Ch0, Ch7 |
| PAGCOR | Philippine Amusement and Gaming Corporation | 菲律賓博彩監管機構 | Ch0, Ch1, Ch7 |
| GAMSTOP | UK National Self-Exclusion Registry | 英國全國自我排除登記系統，UKGC 強制要求整合 | Ch1, Ch6, Ch15 |
| ROFUS | Danish Self-Exclusion Registry | 丹麥自我排除登記系統 | Ch15 |
| Spelpaus | Swedish Self-Exclusion Registry | 瑞典自我排除登記系統 | Ch15 |
| CRUKS | Dutch Self-Exclusion Registry | 荷蘭自我排除登記系統 | Ch15 |
| FATF | Financial Action Task Force | 金融行動特別工作組，維護灰名單 (高風險國家清單) | Ch6 |
| LCCP | Licence Conditions and Codes of Practice | UKGC 牌照條件與操作規範 | Ch6 |
| POCA | Proceeds of Crime Act | 英國犯罪收益法，AML 法定義務 > GDPR 刪除權 | Ch13 |
| AMLD | Anti-Money Laundering Directive | 歐盟反洗錢指令 (第 5 版) | Ch7 |
| PSD2 / SCA | Revised Payment Services Directive / Strong Customer Authentication | 歐盟支付指令，要求 3D Secure 2.0 | Ch3 |
| ISO 27001 | Information Security Management Standard | 資訊安全管理標準 | Ch13 |
| UK RTS | UK Regulatory Technical Standard | 英國監管技術標準 (Section 4: 資訊安全) | Ch13 |
| 可負擔性評估 | Affordability Assessment | UKGC 2025+ 強制要求，按淨虧損分級: Basic (£125–£500) / Enhanced (£500–£2,000) / Full (>£2,000) | Ch1, Ch6, Ch15 |
| 自我排除 | Self-Exclusion | 玩家主動或被動禁止博彩的機制，冷靜期 7–30 天 | Ch1, Ch15 |

---

## A.4 風控與安全術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| 風險評分 | Risk Score | 0–100 分，三區間: [0,30) AUTO_APPROVE / [30,70) MANUAL_REVIEW / [70,100] AUTO_REJECT | Ch6 |
| 斷路器 | Circuit Breaker | 失敗快速切斷模式: CLOSED → OPEN → HALF_OPEN，用於 PSP 備援與 API 健康管理 | Ch3, Ch4, Ch14 |
| 冪等性 | Idempotency | 相同請求重複執行產生相同結果，透過 `idempotency_key` 實現，24h Redis 去重 | Ch2, Ch14 |
| 防重放 | Anti-Replay | Token 使用後標記已消費，防止重複使用 | Ch2, Ch4 |
| 串謀 | Collusion | 多帳戶協同套利: 百家樂對沖、撲克 Soft Play、Chip Dumping、跨平台對沖 | Ch6 |
| 機器人偵測 | Bot Detection | 6 維偵測: 滑鼠軌跡、點擊頻率、API 模式、決策時間、Session 時長、自動化工具 | Ch6 |
| 結構化交易 | Structuring | 拆分大額交易規避報告門檻 (如 3+ 筆 < €50/天但合計 > €1,500) | Ch6 |
| 多帳戶 | Multi-Account | 同一人持有多個帳戶的偵測，基於 IP / 設備 / 電話 / 支付方式關聯分析 | Ch1, Ch6 |
| 設備指紋 | Device Fingerprint | 65,000+ 數據點 (Canvas, WebGL, Audio, timezone 等) 識別唯一設備 | Ch1, Ch6 |
| 不可能旅行 | Impossible Travel | IP 地理位置變化速度超過物理可能，觸發 Session 失效 + 重新驗證 | Ch13 |
| 家庭帳戶驗證 | Household Verification | 同地址 + 共享支付方式 + 相似姓名的帳戶關聯偵測，UKGC 要求家庭層級可負擔性聚合 | Ch6 |
| 追損 | Loss Chasing | 輸錢後連續加大投注試圖回本的行為，觸發負責任博彩介入 | Ch1, Ch15 |
| 爭議退款 | Chargeback (CB) | 支付爭議/退款，生命週期: 通知 → 證據收集 → 申訴 → 仲裁。閾值: VISA 0.9%, MC 1.0% | Ch3 |
| 申訴 | Representment | 商戶對 Chargeback 的正式爭議回應，成功率目標 > 40% | Ch3 |
| 友善詐欺 | Friendly Fraud | 玩家對合法交易發起爭議退款 | Ch3 |
| 值偵測率 | VDR (Value Detection Rate) | `偵測到的詐欺金額 / 總詐欺金額`，目標 > 90% | Ch6 |
| 假陽性率 | False Positive Rate | 合法交易被誤判為詐欺的比例，目標 < 3% | Ch6 |
| 加密銷毀 | Crypto-shredding | 透過銷毀加密金鑰使數據永久不可恢復 | Ch13 |
| 盲索引 | Blind Index | 確定性雜湊索引，允許查詢加密欄位而不暴露明文 | Ch13 |
| 墓碑記錄 | Tombstone Record | GDPR 刪除後保留的最小記錄，用於防重複註冊與審計 | Ch13 |

---

## A.5 支付術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| PSP | Payment Service Provider | 支付服務提供商 | Ch3, Ch14 |
| 智慧路由 | Smart Routing | 基於成功率、費率、速度的自動 PSP 選擇 | Ch3 |
| 3D Secure (3DS) | 3D Secure 2.0 | 卡片交易的雙重驗證 (SCA)，PSD2 強制 | Ch3 |
| ARN | Acquirer Reference Number | 收單行參考號，用於追蹤交易 | Ch3 |
| SEPA | Single Euro Payments Area | 歐盟統一支付區域銀行轉帳，T+1 至 T+3 結算 | Ch3 |
| 匯率凍結 | FX Rate Freeze | 存提款交易匯率鎖定 15 分鐘 (加密貨幣 30 秒) | Ch2, Ch3 |
| 滑點保護 | Slippage Protection | 加密貨幣匯率波動保護，閾值 2% | Ch2 |
| MID | Merchant ID | 商戶識別碼 | Ch3 |

---

## A.6 技術架構術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| CQRS | Command Query Responsibility Segregation | 命令查詢責任分離，讀寫模型分離 | Ch0 (架構) |
| CAP 定理 | CAP Theorem | 一致性 (C) / 可用性 (A) / 分區容錯 (P) 三選二。錢包/支付/風控=CP，遊戲大廳/CMS/報表/客服=AP | Ch10 |
| SSOT | Single Source of Truth | 單一可信來源，避免同一數值在多處定義衝突 | Ch0, Ch5 |
| DLQ | Dead Letter Queue | 死信佇列，Webhook 6 次重試全失敗後進入，保留 7 天 | Ch14 |
| DEK | Data Encryption Key | 資料加密金鑰，Per-Player，90 天自動輪替 | Ch13 |
| KEK | Key Encryption Key | 金鑰加密金鑰 (Master Key)，HSM 保護，365 天手動輪替 | Ch13 |
| HMAC-SHA256 | Hash-based Message Authentication Code (SHA-256) | Token 簽名與 Webhook 驗證演算法 | Ch2, Ch4, Ch14 |
| TLS | Transport Layer Security | 傳輸層安全協議。外部 API: TLS 1.2+，內部服務: TLS 1.3 | Ch11, Ch13 |
| RBAC | Role-Based Access Control | 基於角色的存取控制 | Ch7, Ch13 |
| MFA | Multi-Factor Authentication | 多因子驗證 (TOTP / WebAuthn) | Ch1, Ch7, Ch13 |
| TOTP | Time-based One-Time Password | 基於時間的一次性密碼 | Ch1, Ch13 |
| WebAuthn | Web Authentication API | 基於 FIDO2 的無密碼認證 | Ch1, Ch13 |
| Maker-Checker | Maker-Checker (Dual Control) | 雙人控制審批，敏感操作需兩人確認 | Ch5, Ch6, Ch7 |
| RTO | Recovery Time Objective | 恢復時間目標，最大可接受停機時間 | Ch10 |
| RPO | Recovery Point Objective | 恢復點目標，最大可接受數據遺失量 | Ch10 |
| SLA | Service Level Agreement | 服務等級協議。Tier 1: 99.9% / Tier 2: 99.95% / Tier 3: 99.99% | Ch10 |
| TPS | Transactions Per Second | 每秒交易數，峰值目標 120 TPS，設計容量 300 TPS | Ch10 |
| P95 / P99 | 95th / 99th Percentile | 回應時間的百分位數指標 | Ch10 |
| Hot / Warm / Cold Path | Data Path Tiers | 數據時效: Hot ≤5s (95%準確) / Warm 5–60s (98%) / Cold T+1 (100%) | Ch9 |
| ETL | Extract, Transform, Load | 數據提取、轉換、載入管線 | Ch9 |
| PII | Personally Identifiable Information | 個人可識別資訊 | Ch9, Ch13 |

---

## A.7 客戶服務術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| FCR | First Contact Resolution | 首次接觸解決率，目標 > 80% | Ch12 |
| AHT | Average Handling Time | 平均處理時間，目標 < 15 分鐘 | Ch12 |
| CSAT | Customer Satisfaction Score | 客戶滿意度，目標 > 90% | Ch12 |
| AI Chatbot | AI Chatbot | 自動客服應答，意圖準確率目標 ≥ 90%，自動解決率 > 40% | Ch12 |

---

## A.8 代理營運術語

| 術語 | 英文 | 定義 | 參考章節 |
|------|------|------|---------|
| 總代理 | Master Agent | 租戶直屬代理，管理下線、分配額度、查看全線報表 | Ch8 |
| 中間代理 | Sub-Agent | 多層中間代理，管理下線並向下分配額度 | Ch8 |
| 末端代理 | End Agent | 直接面對玩家的代理，不可再分 | Ch8 |
| 收益分成 | Revenue Share | 淨盈虧 × 階梯分成比例 (30%–45%) | Ch8 |
| 流水返佣 | Turnover Commission | 有效投注額 × 返佣率 (依遊戲類型 0.2%–0.5%) | Ch8 |
| CPA | Cost Per Acquisition | 每位新註冊/首存玩家的固定佣金 | Ch8 |
| CAC | Customer Acquisition Cost | 代理獲客成本，目標 < $200 | Ch8 |

---

## A.9 禁用術語對照

| ❌ 禁用 | ✅ 正確用法 | 說明 |
|---------|----------|------|
| 有效流水 (Effective Turnover) | 有效投注額 (Valid Bet) | 避免與流水 (Turnover) 混淆 |
| 剩餘流水 (Remaining Turnover) | 剩餘流水要求 (Remaining Wagering Requirement) | 完整表述 |
| `effectiveTurnover` | `validBet` | API 欄位標準化 |
| `turnoverRequirement` | `totalRequirement` | API 欄位標準化 |
