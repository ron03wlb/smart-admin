# 需求第一性原理審查報告

> **版本**: v2.0.0
> **審查日期**: 2026-03-24
> **修正完成日期**: 2026-03-24
> **審查範圍**: requirements/ 全部 16 章（Ch0–Ch15）
> **方法論**: 從第一性原理出發，逐章交叉比對，識別缺口、衝突與歧義
> **狀態**: ✅ 全部 42 項 findings 已修正完成

---

## 摘要

本次審查共發現 **42 項 findings**，全部已於 Sprint v2.1 中修正完成：

| 嚴重度 | 數量 | 狀態 |
|--------|------|------|
| **CRITICAL** | 6 | ✅ 全部 RESOLVED |
| **HIGH** | 18 | ✅ 全部 RESOLVED |
| **MEDIUM** | 12 | ✅ 全部 RESOLVED |
| **LOW** | 6 | ✅ 全部 RESOLVED |

### 新增附錄
| 附錄 | 文件 | 對應 Finding |
|------|------|-------------|
| 附錄 A | Appendix_A_Glossary_術語表.md | L-01 |
| 附錄 B | Appendix_B_Traceability_Matrix_需求追溯矩陣.md | L-02 |
| 附錄 C | Appendix_C_Performance_Requirements_效能需求彙總.md | L-03 |

---

## CRITICAL — ✅ 全部已修復

### C-01：Chargeback 工作流完全缺失 — ✅ RESOLVED

- **位置**: Ch3 支付系統
- **修正**: 新增 §3.8 Chargeback 爭議處理 — 完整生命週期狀態圖 (Mermaid)、SLA 表、證據收集檢查表、結果處理矩陣、玩家懲罰升級 (1st=FLAG / 2nd=限制支付 / 3rd=凍結)、監控閾值 (0.5%/0.8%/1.0% CB rate)。可配置參數依用戶決定（預設 30 天爭議窗口、1% 閾值）。

### C-02：遊戲權重衝突 — Baccarat 15% vs 10% — ✅ RESOLVED

- **位置**: Ch0, Ch2, Ch5
- **修正**: 統一為「可配置（預設 10%）」。Ch5 §5.3 標記為 **SSOT**（此為 SSOT），Ch0 §0.8 和 Ch2 §2.9 改為引用 Ch5。三處一致。

### C-03：過期 Token 處理矛盾 — ✅ RESOLVED

- **位置**: Ch2, Ch4
- **修正**: 依用戶決策採用**嚴格拒絕**策略 — 所有金額相關操作 (Debit/Credit/Rollback/Adjust) 一律拒絕過期 Token。僅 GetBalance 允許寬鬆驗證。Ch2 §2.8 和 Ch4 §4.3 同步更新，並新增 Resettlement / 客服工單補發路徑。

### C-04：自我排除時 Token 撤銷未定義 — ✅ RESOLVED

- **位置**: Ch15
- **修正**: 新增 6 步驟 Token/Session 撤銷流程 — 撤銷 Token → 通知 GP → 等待 5 分鐘 → 強制 Rollback → 凍結錢包 → 阻止代理信用。關鍵規則: 排除檢查須覆蓋所有投注管道。

### C-05：多幣種外匯核算未定義 — ✅ RESOLVED

- **位置**: Ch2, Ch3
- **修正**: 依用戶決策採用「存款時轉換」策略 — 存款進來按匯率轉換為商戶定義法幣，投注統一計算，出金按匯率轉回。Ch2 §2.13 完整定義 FX Rate Provider（5 分鐘更新、加密貨幣 30 秒）、15 分鐘匯率凍結、加密貨幣 2% 滑點保護、報表幣種統一規則。Ch3 新增 §3.5 跨幣種規則。

### C-06：代理信用可繞過自我排除 — ✅ RESOLVED

- **位置**: Ch8, Ch15
- **修正**: Ch8 §8.3 新增自我排除檢查規則 — 授信或調整額度前須檢查玩家排除狀態，已排除玩家禁止任何信用操作。Ch15 新增排除廣播機制，自動凍結 CREDIT 錢包並通知代理。

---

## HIGH — ✅ 全部已修復

### H-01：負餘額與可投注餘額矛盾 — ✅ RESOLVED
- **修正**: Ch2 §2.11 新增 5 秒容忍窗口、自動補償順序 (BONUS→CASH→手動)、告警閾值。

### H-02：紅利疊加與返水衝突解決缺失 — ✅ RESOLVED
- **修正**: Ch5 §5.6 後新增返水/紅利互動規則 — 返水進入 CASH 錢包、獨立於流水要求、衝突策略僅適用紅利間。

### H-03：紅利到期邊界 — 未結算投注 — ✅ RESOLVED
- **修正**: Ch5 §5.5 新增 — 依用戶決策採用 Option B（立即沒收未使用 BONUS 餘額），未結算投注的 BONUS 部分即時沒收。

### H-04：真人/撲克反串謀偵測缺失 — ✅ RESOLVED
- **修正**: Ch6 新增 §6.10 串謀偵測 — 百家樂對沖、撲克 Soft Play、Chip Dumping、跨平台對沖，含偵測指標與處置。

### H-05：UKGC 可負擔性決策框架缺失 — ✅ RESOLVED
- **修正**: Ch15 以詳細決策框架取代簡單 pass/fail — 6 種評估結果 + 對應行動 + 人工介入觸發條件。

### H-06：代理負結轉多幣種未定義 — ✅ RESOLVED
- **修正**: Ch8 §8.5 新增 — 負結轉統一以代理結算幣種計算，月結時按結算日即時匯率折算。

### H-07：代理信用額度競態條件 — ✅ RESOLVED
- **修正**: Ch8 §8.5 新增併發控制 — 樂觀鎖 (version 欄位)、高併發序列化、超額拒絕不回滾。

### H-08：信用額度執行與負結轉矛盾 — ✅ RESOLVED
- **修正**: Ch8 §8.5 明確 — 信用額度為硬上限，負結轉不影響信用額度計算。公式: `可用信用 = 信用額度 - 已使用額度`。

### H-09：App Store 博彩合規缺失 — ✅ RESOLVED
- **修正**: Ch11 新增 §11.8 App Store 合規 — Apple Guideline 5.3.4 (17+, no IAP, license docs)、Google Play 博彩政策、合規檢查表。

### H-10：GDPR 刪除與 AML 調查保留衝突 — ✅ RESOLVED
- **修正**: Ch13 §13.2 新增 GDPR vs AML 衝突解決 — AML 法定義務 > GDPR、通知規則、30 天重啟、審計追蹤。

### H-11：加密金鑰輪替策略缺失 — ✅ RESOLVED
- **修正**: Ch13 §13.4 新增 — Master Key 365 天 / DEK 90 天自動 / Blind Index Salt 180 天 / TLS 90 天。雙金鑰共存、漸進式重加密、監控。

### H-12：Webhook 冪等性缺失 — ✅ RESOLVED
- **修正**: Ch14 §14.4 新增接收端冪等性 — idempotency_key 要求、24h Redis 去重、簽名驗證 HMAC-SHA256。

### H-13：PSP SLA 與支付 RTO 不兼容 — ✅ RESOLVED
- **修正**: Ch14 §14.3 新增 PSP 降級策略 — Circuit Breaker、自動路由備用 PSP < 30 秒、漸進恢復 (25%→50%→100%)。RTO 排除外部不可控停機。

### H-14：Bot 偵測簽名未定義 — ✅ RESOLVED
- **修正**: Ch6 新增 §6.11 Bot Detection — 6 維偵測 (滑鼠軌跡、點擊頻率、API 模式、決策時間、Session 時長、自動化工具) + 閾值。

### H-15：Config-Driven 規則引擎邊界模糊 — ✅ RESOLVED
- **修正**: Ch6 新增 §6.12 Config-Driven 規則治理 — 可配置參數清單、Maker-Checker 變更流程、Canary 部署、審計日誌。

### H-16：家庭驗證（Household）標準模糊 — ✅ RESOLVED
- **修正**: Ch6 §6.16 增強 — 關聯信號權重 (地址=高, IP=中, 支付=高, 設備=極高, 姓名=中)、UKGC 家庭層級可負擔性聚合。

### H-17：SLA 違反處罰未定義 — ✅ RESOLVED
- **修正**: Ch12 §12.5 新增 — 內部處罰 (KPI 記錄、PIP) + 客戶補償 (可配置，預設 $5 credit，$50/月/玩家上限)。

### H-18：TLS 版本衝突 — ✅ RESOLVED
- **修正**: Ch11 新增 TLS 1.2+ 備註引用 Ch13。Ch13 §13.5 統一策略: 外部 API TLS 1.2+，內部服務 TLS 1.3。

---

## MEDIUM — ✅ 全部已修復

### M-01：多租戶時區規範缺失 — ✅ RESOLVED
- **修正**: Ch10 新增 §10.10 — UTC 存儲、本地顯示、ISO 8601 API 格式。

### M-02：CAP 定理取捨未闡述 — ✅ RESOLVED
- **修正**: Ch10 新增 §10.11 — 錢包/支付/風控=CP，遊戲大廳/CMS/報表/客服=AP。

### M-03：LCP < 2.5s 與大型 Banner 衝突 — ✅ RESOLVED
- **修正**: Ch11 §11.3 新增 — Banner ≤200KB + lazy load + WebP/AVIF + 4G 低解析度。

### M-04：即時報表 vs 最終一致性矛盾 — ✅ RESOLVED
- **修正**: Ch9 §9.2 新增報表延遲 SLA 分級 — 即時 ≤5s / 準即時 ≤1min / 批次 ≤1h / 日結 T+1 (06:00)。

### M-05：VIP 降級保護期長度未定 — ✅ RESOLVED
- **修正**: Ch1 §1.5 新增具體值 — Diamond 3 個月 / Platinum 2 個月 / 其他 1 個月（可配置）。

### M-06：遊戲供應商違約處理缺失 — ✅ RESOLVED
- **修正**: Ch4 新增 §4.8 GP 違約處理 — 4 級升級 (警告→限流→暫停→下架)。

### M-07：代理層級變更時資料遷移 — ✅ RESOLVED
- **修正**: Ch8 新增 §8.6 — 6 場景遷移規則 (上移/下移/轉移/結算中/佣金歷史/信用額度)。

### M-08：客服 AI 機器人降級策略不完整 — ✅ RESOLVED
- **修正**: Ch12 §12.4 新增 AI 服務降級策略 — 3 階段 (正常→降級→不可用) + 漸進恢復。

### M-09：密碼策略與 MFA 恢復流程缺失 — ✅ RESOLVED
- **修正**: Ch1 新增 §1.12 MFA 恢復流程 — 5 場景 (設備遺失/備用碼遺失/Email/TOTP/WebAuthn) + 24h 提款凍結。

### M-10：部分結算（Partial Settlement）場景 — ✅ RESOLVED
- **修正**: Ch4 §4.8 新增 Partial Cashout 規則 — 計算公式、流水計算、紅利資金限制。

### M-11：報表數據修正（Reconciliation Adjustment） — ✅ RESOLVED
- **修正**: Ch9 新增 §9.8 報表數據修正流程 — 6 步驟 + CFO 審批合規報表 + 不可覆蓋原始值。

### M-12：Webhook 簽名演算法升級路徑 — ✅ RESOLVED
- **修正**: Ch14 §14.5 新增 — 版本協商 Header、90 天過渡期、自動回退。

---

## LOW — ✅ 全部已修復

### L-01：術語表缺失 — ✅ RESOLVED
- **修正**: 新增 Appendix_A_Glossary_術語表.md — 涵蓋 9 大類 150+ 術語。

### L-02：需求追溯矩陣缺失 — ✅ RESOLVED
- **修正**: 新增 Appendix_B_Traceability_Matrix_需求追溯矩陣.md — 雙向追溯 (監管→需求 + 需求→監管)。

### L-03：效能需求散落各章 — ✅ RESOLVED
- **修正**: 新增 Appendix_C_Performance_Requirements_效能需求彙總.md — 統一回應時間、吞吐量、SLA、DR、準確性。

### L-04：文件版本控制策略 — ✅ RESOLVED
- **修正**: README.md 新增版本控制策略 — 版本命名規則、變更管理流程、SSOT 變更特殊規則。

### L-05：災難復原演練排程 — ✅ RESOLVED
- **修正**: Ch10 §10.4 新增 DR 演練排程 — Q1-Q4 演練範圍、驗證檢查清單、失敗處置。

### L-06：合規報告自動化程度 — ✅ RESOLVED
- **修正**: Ch9 新增 §9.9 合規報表自動化等級 — 全自動/半自動/手動三級定義 + 8 類報表分級。

---

## 跨章衝突交叉矩陣 — ✅ 全部已解決

| 衝突 | 章節 A | 章節 B | 嚴重度 | 狀態 |
|------|--------|--------|--------|------|
| Baccarat 權重 15% vs 10% | Ch0 | Ch5 | CRITICAL | ✅ 統一為「可配置（預設 10%）」，SSOT=Ch5 |
| 過期 Token 接受 vs 拒絕 | Ch2 | Ch4 | CRITICAL | ✅ 統一為嚴格拒絕（金額操作） |
| 負餘額容忍 vs 不可為負 | Ch2 §2.3 | Ch2 §2.11 | HIGH | ✅ 定義 5 秒容忍窗口 |
| PSP SLA 99.5% vs RTO <15min | Ch10 | Ch14 | HIGH | ✅ PSP 降級 + RTO 排除外部不可控 |
| GDPR 刪除 vs AML 保留 | Ch13 | Ch6 | HIGH | ✅ AML 法定義務 > GDPR |
| TLS 1.3 vs TLS 1.2+ | Ch13 | Ch11 | MEDIUM | ✅ 外部 1.2+，內部 1.3 |
| 信用額度硬上限 vs 負結轉 | Ch8 §8.3 | Ch8 §8.5 | HIGH | ✅ 獨立 — 負結轉不影響信用額度 |
| 代理信用 vs 自我排除 | Ch8 | Ch15 | CRITICAL | ✅ 排除檢查覆蓋代理信用管道 |

---

## 驗證摘要 (V-01 ~ V-04)

### V-01 SSOT 一致性掃描
全部 9 項 SSOT 值（風險分數邊界、Token TTL、遊戲權重、SLA 層級、DR 層級、TLS 策略、KYC 等級、Webhook 重試、VIP 層級）跨章節一致。

### V-02 章節完整性檢查
全部 16 章均包含: 模組概述、核心職責、功能需求、非功能需求、成功指標/KPI、技術文檔引用。100% 完整。

### V-03 審查報告更新
本文件已更新，全部 42 項標記為 RESOLVED 並附修正摘要。

### V-04 技術同步評估
15/16 技術文檔需同步更新（僅 Ch7 無直接影響）。高優先: Ch0/Ch1/Ch2/Ch3/Ch4/Ch5/Ch6/Ch8/Ch13/Ch15。中優先: Ch9/Ch10/Ch11/Ch12/Ch14。記錄於此，技術文檔同步列為後續工作。

---

*本報告由 AI 輔助審查並修正，v2.0 版本反映全部 42 項 findings 的修正結果。建議由領域專家逐項驗收後正式簽核。*
