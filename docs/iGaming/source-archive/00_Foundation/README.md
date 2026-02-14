# 00_Foundation - 基礎知識

**狀態**: ✅ Phase 4 重構完成
**版本**: v4.0.0
**更新日期**: 2026-02-07

---

## 模塊職責

本模塊提供 iGaming 平台的基礎知識和概念，是所有其他模塊的入口。

**核心內容**：
- 方案總覽（整體架構）
- 行業術語標準化定義
- 數據模型設計
- 技術棧選型

---

## 目錄結構

```
00_Foundation/
├── README.md                              # 本文件
├── 00-01_Quickstart.md                    # 10 分鐘快速入門
├── 00-02_Business_Flows.md                # 業務流程圖集
├── 00-03_Implementation_Guide.md          # 實作指南
├── 00-04_Implementation_Index.md          # 實作指南索引
└── guides/                                # 概念與實作指南
    ├── 00-05_Document_Map.md              # 完整文檔地圖
    ├── 00-06_Solution_Overview.md         # 方案總覽
    ├── 00-07_Industry_Terminology.md      # 行業術語
    ├── 00-08_Terminology_Standards.md     # 術語標準化定義
    ├── 00-09_Technology_Stack.md          # 技術棧選型
    ├── 00-10_Data_Model.md                # 數據模型
    ├── 00-11_Financial_Implementation.md  # 財務實作
    ├── 00-12_Game_Integration_Implementation.md  # 遊戲整合
    ├── 00-13_Promotion_Implementation.md  # 活動系統實作
    ├── 00-14_Risk_Implementation.md       # 風控實作
    ├── 00-15_Governance_Implementation.md # 治理實作
    └── 00-16_Infrastructure_Implementation.md  # 基礎設施實作
```

---

## 與其他模塊的關係

- **對外提供**: 基礎概念定義、術語標準
- **依賴模塊**: 無（獨立模塊）
- **被依賴**: 所有業務模塊

---

## 遷移記錄

- [x] Phase 3: `00_Concept_&_Analysis` (6 files) → `concepts/`
- [x] Phase 3: 更新 31 個文件中的交叉引用連結
- [x] Phase 4: 刪除 `navigation/` 目錄，導航功能整合至主 README.md
- [x] Phase 5: 修正 `00-00_` 命名為順序編號
- [x] Phase 5: 合併 `concepts/` + `implementation-guides/` → `guides/`
