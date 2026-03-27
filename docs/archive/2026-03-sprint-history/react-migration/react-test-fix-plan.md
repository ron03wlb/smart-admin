# Task Plan: React 專案測試修復

**專案**: SmartAdmin React Web
**任務**: 修復 26 個失敗的測試文件
**分支**: `feature/igaming-infrastructure-sprint1`
**日期**: 2026-03-20
**狀態**: Phase 1 - 問題分析中

**進度摘要**: ⏳ Phase 1 待開始 | ⏸️ Phase 2 待開始 | ⏸️ Phase 3 待開始

---

## 當前狀態

**測試結果**:
- ❌ 失敗: 26 個測試文件
- ✅ 通過: 83 個測試文件
- 📊 總計: 109 個測試文件
- 🎯 目標: 100% 通過率

**已知問題分類**:

### 1. Ant Design 棄用警告 (P1 - 中優先級)
- `destroyOnClose` → 改用 `destroyOnHidden`
- `bordered` → 改用 `variant`
- `bodyStyle` → 改用 `styles.body`
- `Tabs.TabPane` → 改用 `items`
- `rowKey` function 的 index 參數已棄用

### 2. React Testing Library - act() 警告 (P0 - 高優先級)
- 大量組件狀態更新未包裝在 act() 中
- 影響組件: InternalFormItem, Field, CategoryFormModal, JobManagement, SmartLoadingComponent 等

### 3. Form 相關警告 (P1 - 中優先級)
- "Instance created by `useForm` is not connected to any Form element"
- 需要將 form 實例傳遞給 Form 組件

### 4. React 19 相容性警告 (P2 - 低優先級)
- "antd v5 support React is 16 ~ 18"
- 需要評估是否需要升級 Ant Design 或降級 React

---

## 失敗測試文件清單

**高影響測試** (P0 - 10+ 失敗測試):
1. ❌ src/views/support/job/index.test.tsx - 15 tests | **10 failed**
2. ❌ src/views/business/category/index.test.tsx - 13 tests | **6 failed**

**中影響測試** (P1 - 1-9 失敗測試):
3. ❌ src/views/support/serial-number/components/SerialNumberRecordModal.test.tsx - 10 tests | **1 failed**
4. ❌ src/views/support/config/components/ConfigFormModal.test.tsx - ? tests | **? failed**
5. ❌ src/views/system/account/components/Center.test.tsx - ? tests | **? failed**

**待完整分析**: 21 個其他失敗測試文件

---

## Phase 1: 問題診斷與分類 ⏳ pending

**目標**: 收集所有失敗測試的詳細錯誤信息,分類問題

**步驟**:
1. ⏳ 讀取完整測試輸出文件 (test-output.txt)
2. ⏳ 提取所有失敗測試的錯誤消息
3. ⏳ 按錯誤類型分類:
   - act() 警告導致的失敗
   - Ant Design API 變更導致的失敗
   - 測試斷言錯誤
   - 組件渲染錯誤
   - API Mock 錯誤
4. ⏳ 創建修復優先級矩陣

**完成標準**:
- [ ] 所有 26 個失敗測試文件的錯誤都已記錄在 findings.md
- [ ] 每個錯誤都已分類
- [ ] 優先級矩陣已建立

**預估時間**: 30 分鐘

---

## Phase 2: 高優先級修復 (P0) ⏸️ pending

**目標**: 修復 P0 高影響測試 (10+ 失敗)

**修復清單**:
1. ⏸️ src/views/support/job/index.test.tsx (10 failed)
   - 問題: act() 警告、Tabs.TabPane 棄用、API Mock 錯誤
   - 預計修復: 15 個測試全部通過

2. ⏸️ src/views/business/category/index.test.tsx (6 failed)
   - 問題: act() 警告、刪除確認 Modal、API 調用
   - 預計修復: 13 個測試全部通過

**完成標準**:
- [ ] job/index.test.tsx: 15/15 tests passed
- [ ] category/index.test.tsx: 13/13 tests passed
- [ ] 無 act() 警告
- [ ] 無 Ant Design 棄用警告

**預估時間**: 1-2 小時

---

## Phase 3: 中優先級修復 (P1) ⏸️ pending

**目標**: 修復 P1 中影響測試 (1-9 失敗)

**修復清單**:
1. ⏸️ SerialNumberRecordModal.test.tsx (1 failed)
2. ⏸️ ConfigFormModal.test.tsx
3. ⏸️ Center.test.tsx
4. ⏸️ 其他 18 個測試文件

**完成標準**:
- [ ] 所有 P1 測試通過
- [ ] act() 警告數量減少 80%
- [ ] Ant Design 棄用警告減少 80%

**預估時間**: 2-3 小時

---

## Phase 4: Ant Design 棄用警告修復 (P1) ⏸️ pending

**目標**: 系統性修復所有 Ant Design API 棄用警告

**修復清單**:
1. ⏸️ Modal: destroyOnClose → destroyOnHidden
2. ⏸️ Card: bordered → variant
3. ⏸️ Drawer: bodyStyle → styles.body
4. ⏸️ Tabs: TabPane → items
5. ⏸️ Table: rowKey index 參數

**完成標準**:
- [ ] 所有 Modal 組件更新
- [ ] 所有 Card 組件更新
- [ ] 所有 Drawer 組件更新
- [ ] 所有 Tabs 組件更新
- [ ] 所有 Table rowKey 更新
- [ ] 無 Ant Design 棄用警告

**預估時間**: 1-2 小時

---

## Phase 5: 驗證與回歸測試 ⏸️ pending

**目標**: 確保所有測試通過,無新引入的錯誤

**步驟**:
1. ⏸️ 運行完整測試套件
2. ⏸️ 驗證 109/109 測試通過
3. ⏸️ 檢查無 console 警告/錯誤
4. ⏸️ 更新測試覆蓋率報告
5. ⏸️ Git 提交測試修復

**完成標準**:
- [ ] Test Files: 0 failed | 109 passed
- [ ] 無 act() 警告
- [ ] 無 Ant Design 棄用警告
- [ ] 測試覆蓋率 ≥75%

**預估時間**: 30 分鐘

---

## Errors Encountered

| Error | Phase | Attempt | Resolution | Status |
|-------|-------|---------|------------|--------|
| test-output.txt 文件過大 (63174 tokens) | 0 | 1 | 使用 offset/limit 分段讀取 | ✅ Resolved |

---

## Progress Log

**2026-03-20 開始時間** - 開始 Phase 1: 問題診斷與分類
- 運行測試套件
- 發現 26 個測試文件失敗
- 創建修復計劃

---

## Notes

**測試環境**:
- Node.js: v20+
- Vitest: v4.0.18
- React: v19
- Ant Design: v5.x
- React Testing Library: v16+

**修復策略**:
- 優先修復高影響測試 (10+ 失敗)
- 系統性修復 act() 警告
- 批量修復 Ant Design 棄用警告
- 保持測試覆蓋率不降低

**Git 提交策略**:
- Phase 2 完成後提交一次 (P0 修復)
- Phase 3 完成後提交一次 (P1 修復)
- Phase 4 完成後提交一次 (Ant Design 更新)
- 每次提交前運行完整測試套件
