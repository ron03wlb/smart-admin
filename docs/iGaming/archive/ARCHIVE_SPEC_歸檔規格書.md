---
title: "iGaming 文檔歸檔規格書"
version: v5.1.0
date: 2026-03-24
status: executing
---

# iGaming 文檔歸檔規格書

## 問題陳述

v5.0 重整後，根目錄同時存在新舊文檔目錄（requirements/ 與 requirements-v2/、architecture/ 與 technical-v2/），造成混淆。需要將所有舊檔歸檔，並將 v2 目錄重新命名為正式名稱。

## 目標

1. 根目錄只保留 `requirements/`（原 requirements-v2/）和 `technical/`（原 technical-v2/）
2. 所有舊目錄和舊摘要檔案移入 `archive/`
3. 根目錄僅保留 `README.md`，其餘 .md 全部歸檔

## 執行計畫

### Step 1: 移動舊目錄至 archive/

| 來源 | 目標 | 檔案數 |
|------|------|--------|
| requirements/ | archive/v4-requirements/ | ~67 |
| architecture/ | archive/v4-architecture/ | ~123 |
| source-archive/ | archive/v4-source-archive/ | ~191 |
| 00_Navigation/ | archive/v4-navigation/ | 3 |
| implementation/ | archive/v4-implementation/ | ~15 |
| 12_Technical_Operations/ | archive/v4-technical-operations/ | 0 |
| quality-reports/ | archive/v4-quality-reports/ | 1 |
| reports/ | archive/v4-reports/ | 1 |
| research/ | archive/v4-research/ | 1 |
| testing/ | archive/v4-testing/ | 1 |

### Step 2: 重新命名 v2 目錄

| 來源 | 目標 |
|------|------|
| requirements-v2/ | requirements/ |
| technical-v2/ | technical/ |

### Step 3: 移動根目錄舊 .md 至 archive/

移動所有非 README.md 的根目錄 .md 檔案至 archive/v4-root-docs/

### Step 4: 更新內部引用

- requirements/ 內的檔案：`../technical-v2/` → `../technical/`
- technical/ 內的檔案：`../requirements-v2/` → `../requirements/`
- README.md：更新所有路徑

### 最終結構

```
IGaming/
├── README.md                    # 唯一根目錄文件
├── requirements/                # 業務需求（原 requirements-v2/）
│   ├── 00_Overview_總覽.md
│   ├── 01–15 各模組.md
│   └── README.md
├── technical/                   # 技術實作（原 technical-v2/）
│   ├── 00_Architecture_Overview_架構總覽.md
│   ├── 01–15 各模組.md
│   ├── ADR/
│   └── README.md
└── archive/                     # 所有舊版文檔
    ├── v4-requirements/
    ├── v4-architecture/
    ├── v4-source-archive/
    ├── v4-navigation/
    ├── v4-implementation/
    ├── v4-root-docs/
    └── *.md (舊摘要)
```

## 非目標

- 不修改任何檔案內容（僅路徑引用更新）
- 不刪除任何檔案（全部保留在 archive/）
