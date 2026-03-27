# iGaming 平台文檔中心

> **版本**: v5.1.0
> **最後更新**: 2026-03-24

---

## 文檔結構

| 目錄                                 | 受眾            | 說明                         | 檔案數  |
| ---------------------------------- | ------------- | -------------------------- | ---- |
| **[requirements/](requirements/)** | 老闆、PM、合規      | 業務需求 — WHAT & WHY（0% 程式碼）  | 17   |
| **[technical/](technical/)**       | 架構師、開發、DevOps | 技術實作 — HOW（架構 + 程式碼 + ADR） | 22   |
| [archive/](archive/)               | 唯讀存檔          | 所有舊版文檔（v4 及更早）             | 400+ |

---

## requirements/ — 業務需求（16 章 + README）

| # | 章節 | 關鍵主題 |
|---|------|---------|
| 0 | [總覽](requirements/00_Overview_總覽.md) | 商業模式、5 大領域、收入模型、開發路線圖 |
| 1 | [玩家管理](requirements/01_Player_Management_玩家管理.md) | 生命週期、KYC 4 級、VIP 5 級、RFM 分群 |
| 2 | [錢包系統](requirements/02_Wallet_System_錢包系統.md) | 3 種錢包、9 大交易場景、Seamless Wallet |
| 3 | [支付系統](requirements/03_Payment_System_支付系統.md) | PSP 智能路由、審批階梯、3D Secure |
| 4 | [遊戲整合](requirements/04_Game_Integration_遊戲整合.md) | GP 上架流程、RTP 監控、UKGC 限額 |
| 5 | [促銷與 VIP](requirements/05_Promotions_VIP_促銷與VIP.md) | 7 種紅利、流水計算、衝突策略 |
| 6 | [風控與合規](requirements/06_Risk_Compliance_風控與合規.md) | 五層偵測管線、風險分數邊界、AML |
| 7 | [治理與牌照](requirements/07_Governance_Licensing_治理與牌照.md) | 4 級層級、資料隔離、計費模式 |
| 8 | [代理營運](requirements/08_Agent_Operations_代理營運.md) | 10 級代理、佣金模型、信用管理 |
| 9 | [分析與報表](requirements/09_Analytics_Reporting_分析與報表.md) | 三路資料、角色權限、PII 遮罩 |
| 10 | [基礎設施](requirements/10_Infrastructure_基礎設施需求.md) | SLA 3 級、DR 3 級、成本模型 |
| 11 | [前端體驗](requirements/11_Frontend_Experience_前端體驗.md) | i18n、行動 App、SEO、無障礙 |
| 12 | [客戶服務](requirements/12_Customer_Service_客戶服務.md) | 360° 視圖、AI 聊天機器人、SLA |
| 13 | [安全合規](requirements/13_Security_Compliance_安全合規.md) | GDPR、PCI-DSS、PII 加密 |
| 14 | [第三方整合](requirements/14_Third_Party_Integration_第三方整合.md) | 統一 Adapter、熔斷降級、Webhook |
| 15 | [負責任博彩](requirements/15_Responsible_Gambling_負責任博彩.md) | 自我排除、存款限額、可負擔性評估 |

## technical/ — 技術實作（16 章 + 5 ADR + README）

| # | 章節 | 關鍵主題 |
|---|------|---------|
| 0 | [架構總覽](technical/00_Architecture_Overview_架構總覽.md) | Modular Monolith、SmartAdmin 四層、多租戶、事件驅動 |
| 1–15 | 各模組技術實作 | 資料模型、API、程式碼範例、部署 |
| ADR | [架構決策記錄](technical/ADR/) | ADR-001 命名、012 Flag Mode、013 Manager 邊界、014 TenantIgnore、015 三層冪等 |

---

## 按角色快速存取

| 角色 | 建議路徑 |
|------|---------|
| **老闆 / PM** | [requirements/](requirements/) 全部（純業務語言，無程式碼） |
| **架構師** | [technical/00](technical/00_Architecture_Overview_架構總覽.md) → 全部 ADR → 各模組 |
| **後端工程師** | [technical/00](technical/00_Architecture_Overview_架構總覽.md) → ADR-001/013 → 負責模組 |
| **前端工程師** | [technical/11](technical/11_Frontend_Experience_前端體驗.md) → [technical/04](technical/04_Game_Integration_遊戲整合.md) |
| **DevOps** | [technical/10](technical/10_Infrastructure_基礎設施.md) → [technical/09](technical/09_Analytics_Reporting_分析與報表.md) |
| **風控** | [requirements/06](requirements/06_Risk_Compliance_風控與合規.md) + [technical/06](technical/06_Risk_Compliance_風控與合規.md) |
| **合規** | [requirements/13](requirements/13_Security_Compliance_安全合規.md) + [requirements/15](requirements/15_Responsible_Gambling_負責任博彩.md) |

---

## 核心參考值 (SSOT)

| 項目 | 值 |
|------|---|
| 風險分數邊界 | `[0,30)` AUTO_APPROVE / `[30,70)` MANUAL_REVIEW / `[70,100]` AUTO_REJECT |
| 流水計算 | Standard Principal Method: WIN/LOSS = Bet Amount; DRAW/VOID = 0 |
| 遊戲權重 | Slots 100%, Sports 100%, Baccarat 可配置（預設 10%）, Roulette 20%, Blackjack 10%, Poker 5% |
| SLA 3 級 | External 99.9% / Security 99.95% / K8s 99.99% |
| DR 3 級 | Tier 1 RPO<5min RTO<15min / Tier 2 RPO<1h RTO<4h / Tier 3 RPO<24h |

---

## 版本歷史

| 版本 | 日期 | 變更 |
|------|------|------|
| v5.2.0 | 2026-03-25 | 綜合審查修正：SSOT 引用標記、新增附錄 D/E、Sprint v2.2 計畫 |
| v5.1.0 | 2026-03-24 | 舊文檔全部歸檔至 archive/，移除 -v2 後綴，根目錄乾淨化 |
| v5.0.0 | 2026-03-24 | 完整重整：requirements (17 files) + technical (22 files + 5 ADR) |
| v4.1.0 | 2026-02-09 | Phase 9: 16 模組全覆蓋 (requirements + architecture) |

---

**維護團隊**: iGaming Platform Team
