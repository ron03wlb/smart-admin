# 按模組導航索引

> **⚠️ v5.0 更新 (2026-03-24)**：文檔已重整為 [requirements-v2/](../../requirements-v2/) 和 [technical-v2/](../../technical-v2/)，請優先使用新版文檔。以下為舊版導航，保留供參考。

> 按業務模組分類，每個模組同時列出其需求文檔和技術架構文檔。

---

## 1. 玩家管理 (Player)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 玩家生命週期 | `requirements/01_Player_Experience/` |
| 架構 | 玩家服務實作 | `architecture/01_Player_Service/` |
| 實作 | 玩家設計文檔 | `implementation/02-player-design.md` |

**關鍵主題**: 註冊流程、KYC 四級驗證 (L0-L3)、VIP 等級、RFM 分群、帳戶狀態機

---

## 2. 錢包與財務 (Wallet & Finance)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 無縫錢包規則、支付、對帳、有效投注額 | `requirements/02_Financial_Operations/` |
| 架構 | 無縫錢包、支付閘道、對帳、投注額計算 (11 份) | `architecture/02_Finance_Service/` |
| 實作 | 錢包設計 + 支付設計 | `implementation/01-wallet-design.md`, `01-payment-design.md` |

**關鍵主題**: Seamless Wallet API、三層冪等、三層併發控制、PSP 適配器、SAGA 提款、三層對帳

---

## 3. 遊戲整合 (Game Integration)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 有效投注額規則、遊戲整合、大廳 | `requirements/03_Gaming_Operations/` |
| 架構 | GP 適配器、安全、投注額邏輯、大廳系統 | `architecture/03_Game_Integration/` |
| 實作 | 遊戲整合設計 | `implementation/02-game-integration.md` |
| 研究 | GP API 研究 | `research/game-provider-api-research.md` |

**關鍵主題**: Token 驗證 (HMAC-SHA256)、GP Adapter Pattern、RTP 監控、遊戲權重表

---

## 4. 促銷與活動 (Promotions & Activity)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 紅利規則、活動風控、促銷 | `requirements/04_Promotions_VIP/` |
| 架構 | 紅利計算引擎、促銷實作、活動風控 | `architecture/04_Activity_Engine/` |

**關鍵主題**: 紅利類型、衝突策略 (6 種)、流水要求計算、返水、區域市場策略

---

## 5. 風控引擎 (Risk Engine)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | KYC/AML、詐欺偵測、ML、司法管轄區、玩家保護 (11 份) | `requirements/05_Risk_Compliance/` |
| 架構 | 風控架構、偵測模型、ML 整合、KYC API、玩家保護 (10 份) | `architecture/05_Risk_Engine/` |
| 實作 | 風控引擎設計 | `implementation/03-risk-engine-design.md` |

**關鍵主題**: 五層偵測、LiteFlow 規則、Flink CEP、Flag 模式 (ADR-012)、Neo4j 圖分析、Isolation Forest

---

## 6. 平台治理 (Platform Governance)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 多租戶、MFA、治理 (6 份) | `requirements/06_Governance_Licensing/` |
| 架構 | 多租戶架構、MFA 技術、合規驗證 (9 份) | `architecture/06_Platform_Core/` |
| 實作 | 多租戶設計 | `implementation/00-multi-tenant-design.md` |

**關鍵主題**: 四層租戶隔離、RLS + MyBatis、TOTP MFA、Maker-Checker、RBAC

---

## 7. 代理營運 (Agent Operations)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 信用網路、代理系統 | `requirements/07_Agent_Operations/` |
| 架構 | 信用網路、代理系統 | `architecture/07_Agent_Service/` |

**關鍵主題**: 雙錢包 (Cash + Credit)、信用額度傳播、佣金模型 (RevShare/CPA/Hybrid)

---

## 8. 分析報表 (Analytics & BI)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 報表需求、BI 儀表板 | `requirements/08_Analytics_Operations/` |
| 架構 | 報表架構、BI 實作 | `architecture/08_Analytics_Service/` |

**關鍵主題**: Hot/Warm/Cold 數據路徑、角色化儀表板、ClickHouse OLAP、四層數據模型

---

## 9. 基礎設施 (Infrastructure)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | QA 標準、成本優化 | `requirements/09_Infrastructure_Requirements/` |
| 架構 | Gateway/快取/串流/部署/監控/Token (23 份) | `architecture/09_Infrastructure/` |
| 實作 | 基礎設施缺口分析 | `implementation/00-infrastructure-gap-analysis.md` |

**關鍵主題**: Kubernetes、ArgoCD GitOps、Redisson 限流、JetCache 二級快取、延遲預算

---

## 10. 平台管理 (Platform Management)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 租戶配置、通知、數據管線 | `requirements/10_Platform_Operations/` |
| 架構 | 租戶配置、通知、數據管線 | `architecture/10_Platform_Management/` |

---

## 11. 前端 (Frontend)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | UX、SEO、行動 App、國際化 | `requirements/11_Frontend_Experience/` |
| 架構 | 佈局引擎、i18n、行動端、A/B 測試 (11 份) | `architecture/11_Frontend/` |
| 實作 | 前端適配規格 | `implementation/g2.3-frontend-adaptation-spec.md` |

---

## 12. 安全 (Security)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 資料保護、合規、支付安全 | `requirements/12_Security_Compliance/` |
| 架構 | 加密、GDPR、盲索引、ISO 27001 (10 份) | `architecture/12_Security/` |

---

## 13. 客戶服務 (Customer Service)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 客服平台、營運 | `requirements/13_Customer_Service/` |
| 架構 | 客服平台、營運 | `architecture/13_Customer_Service/` |

---

## 14. 第三方整合 (Third-Party Integration)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 整合標準 | `requirements/14_Integration_Standards/` |
| 架構 | 整合架構 | `architecture/14_Third_Party/` |

---

## 15. 負責任博彩 (Responsible Gambling)

| 類型 | 文檔 | 路徑 |
|------|------|------|
| 需求 | 自我排除、存款限額、會話保護、可負擔性 (4 份) | `requirements/15_Responsible_Gambling/` |
| 架構 | 自我排除、限額、保護 API (4 份) | `architecture/15_Responsible_Gambling/` |

---

## 跨模組文檔

| 文檔 | 路徑 | 說明 |
|------|------|------|
| 業務需求總覽 | `IGaming 業務需求總覽.md` | 面向老闆/PM 的業務摘要 |
| 技術架構總覽 | `IGaming 技術架構總覽.md` | 面向開發團隊的技術摘要 |
| 缺口分析 | `IGaming 文檔缺口與矛盾分析.md` | 文檔品質審查報告 |
| 決策與優先級 | `IGaming 核心決策摘要與開發優先級.md` | ADR + 路線圖 |
| 翻譯詞彙表 | `TRANSLATION_GLOSSARY.md` | 中英術語對照 |
| ADR 模板 | `TEMPLATE_ADR.md` | 架構決策記錄模板 |
