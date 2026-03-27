# 11_Frontend_CMS - 前端與內容管理

> **模塊定位**: 前端架構與內容管理
> **最後更新**: 2026-02-07

---

## 📋 模塊職責

前端佈局引擎、Banner 管理、SEO、移動端、A/B 測試、國際化。

**核心功能**：
- 前端佈局引擎
- Banner 與公告管理
- SEO 與性能優化
- 移動 App 架構
- A/B 測試框架
- 國際化與本地化

**職責邊界**：
- ✅ 包含：前端架構、CMS、i18n
- ❌ 不包含：後端 API 設計（09_Technical_Infrastructure）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 |
|------|---------|------|--------|
| 11-01 | [Frontend_Layout_Engine.md](11-01_Frontend_Layout_Engine.md) | 前端佈局引擎 | P0 |
| 11-02 | [Banner_&_Announcement.md](11-02_Banner_&_Announcement.md) | Banner 與公告 | P1 |
| 11-03 | [SEO_&_Performance.md](11-03_SEO_&_Performance.md) | SEO 與性能 | P2 |
| 11-04 | [Mobile_App_Architecture.md](11-04_Mobile_App_Architecture.md) | 移動 App 架構 | P1 |
| 11-06 | [AB_Testing_Framework.md](11-06_AB_Testing_Framework.md) | A/B 測試框架 | P1 |
| 11-07 | [i18n_Localization.md](11-07_i18n_Localization.md) | 國際化基礎 | P0 |
| 11-08 | [Dynamic_Content_Localization.md](11-08_Dynamic_Content_Localization.md) | 動態內容本地化 | P1 |
| 11-09 | [Localization_Workflow.md](11-09_Localization_Workflow.md) | 本地化工作流 | P2 |
| 11-10 | [Localization_API.md](11-10_Localization_API.md) | 本地化 API | P2 |

---

## 🔗 核心依賴

```text
11_Frontend_CMS
    ↓
    ├─► 04_Activity_Center - 活動 Banner
    ├─► 09_Technical_Infrastructure (09-03) - API 設計
    ├─► 10_Platform_Management (10-03) - 通知推送
    └─► 06_Platform_Governance (06-01) - 多租戶 UI
```

---

## 🌍 本地化文檔系列

```text
11_Frontend_CMS/
├── 11-07_i18n_Localization.md        # 基礎架構
├── 11-08_Dynamic_Content_Localization.md  # 動態內容
├── 11-09_Localization_Workflow.md    # 翻譯工作流
└── 11-10_Localization_API.md         # API 設計
```

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| 佈局引擎設計 | 11-01 Frontend_Layout | §2.1 |
| i18n 架構 | 11-07 i18n_Localization | §3.1 |
| A/B 測試設計 | 11-06 AB_Testing | §2.1 |

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: Frontend Team
