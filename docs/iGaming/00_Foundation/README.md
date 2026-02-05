# 00_Foundation - 基礎知識

**狀態**: ✅ Phase 3 遷移完成
**版本**: v4.0.0
**更新日期**: 2026-02-05

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
├── concepts/                              # 基礎概念（原 00_Concept_&_Analysis）
│   ├── 00-00_Document_Map.md              # 完整文檔地圖
│   ├── 00-01_Solution_Overview.md         # 方案總覽
│   ├── 00-02_Industry_Terminology.md      # 行業術語
│   ├── 00-03_Terminology_Standards.md     # 術語標準化定義
│   ├── 00-04_Technology_Stack.md          # 技術棧選型
│   └── 00-05_Data_Model.md               # 數據模型
├── navigation/                            # 導航（原 00_Navigation）
│   └── README.md                          # 導航中心
├── 00-00_QUICKSTART.md                    # 10 分鐘快速入門
├── 00-00_BUSINESS_FLOWS.md                # 業務流程圖集
├── 00-00_IMPLEMENTATION_GUIDE.md          # 實作指南
├── 00-00_IMPLEMENTATION_GUIDE_INDEX.md    # 實作指南索引
└── implementation-guides/                 # 分模塊實作指南
```

---

## 與其他模塊的關係

- **對外提供**: 基礎概念定義、術語標準
- **依賴模塊**: 無（獨立模塊）
- **被依賴**: 所有業務模塊

---

## 遷移記錄

- [x] Phase 3: `00_Concept_&_Analysis` (6 files) → `concepts/`
- [x] Phase 3: `00_Navigation` (1 file) → `navigation/`
- [x] Phase 3: 更新 31 個文件中的交叉引用連結
