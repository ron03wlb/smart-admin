---
title: "technical/ 技術文檔索引"
part: technical
version: v2.2
created: 2026-03-24
---

# iGaming 平台技術文檔 v2.2

> **定位**：技術實作文檔（How）— 架構、程式碼模式、資料庫、API、部署
>
> **對應**：每章對應 [requirements/](../requirements/) 同編號業務需求文檔

---

## 章節索引

| # | 章節 | 檔案 | 行數 |
|---|------|------|------|
| 0 | [架構總覽](./00_Architecture_Overview_架構總覽.md) | 00_Architecture_Overview_架構總覽.md | ~1,800 |
| 1 | [玩家管理](./01_Player_Management_玩家管理.md) | 01_Player_Management_玩家管理.md | ~1,400 |
| 2 | [錢包系統](./02_Wallet_System_錢包系統.md) | 02_Wallet_System_錢包系統.md | ~2,000 |
| 3 | [支付系統](./03_Payment_System_支付系統.md) | 03_Payment_System_支付系統.md | ~1,200 |
| 4 | [遊戲整合](./04_Game_Integration_遊戲整合.md) | 04_Game_Integration_遊戲整合.md | ~840 |
| 5 | [促銷與 VIP](./05_Promotions_VIP_促銷與VIP.md) | 05_Promotions_VIP_促銷與VIP.md | ~870 |
| 6 | [風控與合規](./06_Risk_Compliance_風控與合規.md) | 06_Risk_Compliance_風控與合規.md | ~1,100 |
| 7 | [治理與牌照](./07_Governance_Licensing_治理與牌照.md) | 07_Governance_Licensing_治理與牌照.md | ~2,200 |
| 8 | [代理營運](./08_Agent_Operations_代理營運.md) | 08_Agent_Operations_代理營運.md | ~1,200 |
| 9 | [分析與報表](./09_Analytics_Reporting_分析與報表.md) | 09_Analytics_Reporting_分析與報表.md | ~1,500 |
| 10 | [基礎設施](./10_Infrastructure_基礎設施.md) | 10_Infrastructure_基礎設施.md | ~500 |
| 11 | [前端體驗](./11_Frontend_Experience_前端體驗.md) | 11_Frontend_Experience_前端體驗.md | ~400 |
| 12 | [客戶服務](./12_Customer_Service_客戶服務.md) | 12_Customer_Service_客戶服務.md | ~370 |
| 13 | [安全合規](./13_Security_Compliance_安全合規.md) | 13_Security_Compliance_安全合規.md | ~400 |
| 14 | [第三方整合](./14_Third_Party_Integration_第三方整合.md) | 14_Third_Party_Integration_第三方整合.md | ~340 |
| 15 | [負責任博彩](./15_Responsible_Gambling_負責任博彩.md) | 15_Responsible_Gambling_負責任博彩.md | ~400 |
| 16 | [事件回應](./16_Incident_Response_事件回應.md) | 16_Incident_Response_事件回應.md | ~1,680 |

---

## v2.2 新增支援文檔

| 文檔 | 檔案 | 行數 | 說明 |
|------|------|------|------|
| 可配置參數系統技術規格 | [Technical_Configurable_Parameters_System_可配置參數系統技術規格.md](./Technical_Configurable_Parameters_System_可配置參數系統技術規格.md) | ~2,500 | 三層覆蓋架構、DB Schema、快取策略、Admin API、Maker-Checker |
| v2.2 實作指南 | [Technical_Sprint_v2.2_Implementation_Guide_實作指南.md](./Technical_Sprint_v2.2_Implementation_Guide_實作指南.md) | ~4,200 | GAP-1~10、F-03~F-05、N-05 共 14 項技術實作規格 |
| 跨模組邊界情境技術規格 | [Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md) | ~1,200 | BS-01~BS-12 全部 12 情境序列圖、API、狀態機、測試案例 |

---

## ADR (架構決策記錄)

| ADR | 標題 | 狀態 |
|-----|------|------|
| [ADR-001](./ADR/ADR-001_Naming_Convention_命名規範.md) | 命名規範 (Singular Standard) | Accepted |
| [ADR-012](./ADR/ADR-012_Async_Risk_Flag_Mode_非同步風控提案.md) | 非同步風控提案系統 (Flag Mode) | Accepted |
| [ADR-013](./ADR/ADR-013_Manager_Boundary_Manager邊界.md) | Manager 邊界 (@Transactional) | Accepted |
| [ADR-014](./ADR/ADR-014_TenantIgnore_Whitelist_租戶忽略白名單.md) | @TenantIgnore 白名單 | Accepted |
| [ADR-015](./ADR/ADR-015_Three_Layer_Idempotency_三層冪等防護.md) | 三層冪等防護 | Accepted |

---

## 按角色閱讀指南

| 角色 | 建議閱讀順序 |
|------|-------------|
| **後端工程師** | Ch0 → ADR-001/013 → 所負責模組章節 |
| **前端工程師** | Ch0 (技術棧) → Ch11 → Ch4 (遊戲啟動) |
| **DevOps** | Ch10 → Ch16 (事件回應) → Ch0 (部署架構) → Ch9 (監控) |
| **QA** | Ch0 (總覽) → 所測試模組章節 |
| **Tech Lead** | Ch0 → 全部 ADR → 各模組概述 |
| **安全工程師** | Ch13 → Ch6 → ADR-014/015 |

---

## 文檔原則

1. **技術導向**：專注於「如何實作」，含架構圖、程式碼範例、資料庫 Schema、API 規格
2. **ADR 驅動**：重大技術決策記錄為 ADR，附帶背景、決策、後果
3. **可驗證**：關鍵規則附 ArchUnit 測試，確保程式碼合規
4. **與業務對齊**：每章末尾連結對應 requirements/ 業務文檔

---

## 版本歷史

| 版本 | 日期 | 變更 |
|------|------|------|
| v2.2 | 2026-03-25 | 新增 Ch16 事件回應；新增可配置參數系統技術規格、v2.2 實作指南、跨模組邊界情境技術規格 3 份支援文檔；與 requirements v2.5 對齊 |
| v2.0 | 2026-03-24 | 完整重寫，從 414 份源文檔提煉為 16 章 + 5 ADR |
| v1.0 | 2026-01-15 | 初始版本 (分散於 architecture/ 目錄) |
