# 03 遊戲整合

> **Audience**: 架構師、後端開發人員、DevOps
> **Status**: Phase 6 完成 - 4 份拆分文件 + 來源索引

---

## 拆分文件

| 文件 | 說明 | 來源 |
|------|------|------|
| [Turnover Calculation Logic](Turnover_Calculation_Logic.md) | 三層驗證、SmartAdmin 對映、計算演算法 | [source](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md) |
| [Game Integration Implementation](Game_Integration_Implementation.md) | 無縫錢包 API Controller、Token 驗證、三層冪等性 | [source](../../source-archive/00_Foundation/guides/00-12_Game_Integration_Implementation.md) |
| [Game Integration Protocols](Game_Integration_Protocols.md) | API 協定、供應商適配層、資料正規化、Webhook 規格 | [source](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md) |
| [Game Lobby System](Game_Lobby_System.md) | 遊戲大廳服務架構、多層快取、Elasticsearch 搜尋、排序 | [source](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md) |

## 核心整合文件（來源索引）

| 文件 | 說明 | 來源 |
|------|------|------|
| [Game Integration Standard](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md) | 遊戲供應商整合標準協定 | [source](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md) |
| [Game Lobby Management](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md) | 遊戲大廳組織與管理 | [source](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md) |
| [Seamless Wallet Analysis](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) | 無縫錢包 (Seamless Wallet) 整合模式 | [source](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) |

## 有效投注額計算架構

| 文件 | 說明 | 來源 |
|------|------|------|
| [Turnover Core Logic](../../source-archive/03_Game_Center/03-04-01_Turnover_Core_Logic.md) | 核心有效投注額計算演算法 | [source](../../source-archive/03_Game_Center/03-04-01_Turnover_Core_Logic.md) |
| [Three Layer Validation](../../source-archive/03_Game_Center/03-04-02_Three_Layer_Validation.md) | 多層驗證架構 | [source](../../source-archive/03_Game_Center/03-04-02_Three_Layer_Validation.md) |
| [Reconciliation Model](../../source-archive/03_Game_Center/03-04-03_Reconciliation_Model.md) | 遊戲資料對帳框架 | [source](../../source-archive/03_Game_Center/03-04-03_Reconciliation_Model.md) |
| [SmartAdmin Mapping](../../source-archive/03_Game_Center/03-04-04_SmartAdmin_Mapping.md) | SmartAdmin 實體對映 | [source](../../source-archive/03_Game_Center/03-04-04_SmartAdmin_Mapping.md) |
| [Turnover Calculation](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md) | 有效投注額計算總覽 | [source](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md) |

## 合規與監控

| 文件 | 說明 | 來源 |
|------|------|------|
| [GLI Certification](../../source-archive/03_Game_Center/03-05_GLI_Certification.md) | GLI 認證需求 | [source](../../source-archive/03_Game_Center/03-05_GLI_Certification.md) |
| [Game Audit Trail](../../source-archive/03_Game_Center/03-06_Game_Audit_Trail.md) | 遊戲事件稽核日誌 | [source](../../source-archive/03_Game_Center/03-06_Game_Audit_Trail.md) |
| [RTP Monitoring](../../source-archive/03_Game_Center/03-07_RTP_Monitoring.md) | RTP (Return-to-Player) 監控系統 | [source](../../source-archive/03_Game_Center/03-07_RTP_Monitoring.md) |

---

**最後更新**: 2026-02-08
