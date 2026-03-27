# React 測試錯誤診斷結果

**診斷日期**: 2026-03-20
**測試框架**: Vitest 4.0.18
**總測試文件**: 109
**失敗文件**: 26
**通過文件**: 83

---

## 失敗測試文件完整清單 (按失敗數排序)

### P0 - 極高影響 (10+ 失敗測試)

| 文件 | 總測試 | 失敗 | 失敗率 | 執行時間 | 優先級 |
|------|--------|------|--------|----------|--------|
| src/views/support/job/components/**JobFormModal.test.tsx** | 14 | **10** | 71% | 111s | P0 ⚠️ |
| src/views/support/job/**index.test.tsx** | 15 | **10** | 67% | 168s | P0 ⚠️ |

**影響**: 2 個文件,20 個失敗測試,佔總失敗數的 ~30%

---

### P1 - 高影響 (5-9 失敗測試)

| 文件 | 總測試 | 失敗 | 失敗率 | 執行時間 | 優先級 |
|------|--------|------|--------|----------|--------|
| src/views/support/change-log/**index.test.tsx** | 7 | **7** | 100% | 3s | P1 🔴 |
| src/views/business/category/**index.test.tsx** | 13 | **6** | 46% | 31s | P1 |
| src/views/business/category/components/**CategoryFormModal.test.tsx** | 12 | **6** | 50% | 48s | P1 |
| src/views/support/operate-log/components/**OperateLogDetailModal.test.tsx** | 12 | **6** | 50% | 9s | P1 |
| src/views/support/change-log/components/**ChangeLogFormModal.test.tsx** | 8 | **5** | 63% | 40s | P1 |
| src/views/system/employee/components/**EmployeeFormModal.test.tsx** | 18 | **5** | 28% | 49s | P1 |
| src/views/support/message/components/**MessageReceiverModal.test.tsx** | 12 | **5** | 42% | 57s | P1 |

**影響**: 7 個文件,40 個失敗測試,佔總失敗數的 ~60%

---

### P2 - 中影響 (1-4 失敗測試)

| 文件 | 總測試 | 失敗 | 失敗率 | 執行時間 | 優先級 |
|------|--------|------|--------|----------|--------|
| src/views/business/enterprise/**index.test.tsx** | 4 | **4** | 100% | 0.2s | P2 |
| src/views/support/dict/components/**DictDataFormModal.test.tsx** | 6 | **4** | 67% | 36s | P2 |
| src/views/support/dict/components/**DictDataDrawer.test.tsx** | 10 | **4** | 40% | 51s | P2 |
| src/views/system/position/components/**PositionFormModal.test.tsx** | 16 | **4** | 25% | 26s | P2 |
| src/views/system/account/components/**Password.test.tsx** | 14 | **4** | 29% | 42s | P2 |
| src/views/support/job/components/**JobExecuteModal.test.tsx** | 8 | **4** | 50% | 34s | P2 |
| src/views/support/config/components/**ConfigFormModal.test.tsx** | 15 | **3** | 20% | 24s | P2 |
| src/views/support/job/components/**JobLogDrawer.test.tsx** | 12 | **3** | 25% | 58s | P2 |
| src/views/system/department/components/**DepartmentFormModal.test.tsx** | 14 | **1** | 7% | 18s | P2 |
| src/views/system/account/components/**Center.test.tsx** | 12 | **1** | 8% | 28s | P2 |
| src/views/support/code-generator/**index.test.tsx** | 3 | **1** | 33% | 11s | P2 |
| src/components/common/**TableOperator/index.test.tsx** | 20 | **1** | 5% | 9s | P2 |
| src/views/support/message/components/**MessageSendForm.test.tsx** | 10 | **1** | 10% | 24s | P2 |
| src/views/support/reload/**index.test.tsx** | 8 | **1** | 13% | 20s | P2 |
| src/views/support/message/**index.test.tsx** | 5 | **1** | 20% | 15s | P2 |
| src/views/support/serial-number/**index.test.tsx** | 10 | **1** | 10% | 30s | P2 |
| src/views/support/serial-number/components/**SerialNumberRecordModal.test.tsx** | 10 | **1** | 10% | 10s | P2 |

**影響**: 17 個文件,35 個失敗測試,佔總失敗數的 ~53%

---

## 錯誤類型分析

### 1. React Testing Library - act() 警告 ⚠️

**影響範圍**: 幾乎所有失敗測試
**錯誤示例**:
```
An update to XXX inside a test was not wrapped in act(...).

When testing, code that causes React state updates should be wrapped into act(...):

act(() => {
  /* fire events that update state */
});
```

**受影響組件**:
- InternalFormItem
- Field
- CategoryFormModal
- JobManagement
- SmartLoadingComponent
- ItemHolder
- 等等...

**根本原因**:
- 測試中的異步狀態更新未正確包裝
- userEvent 操作後未等待狀態更新完成
- Modal/Drawer 開啟/關閉動畫導致的異步更新

**修復策略**:
1. 使用 `waitFor()` 等待異步操作完成
2. 使用 `await userEvent.click()` 等待事件處理完成
3. 在測試結束前添加 `await waitFor(() => {})` 清理副作用

---

### 2. Ant Design 5.x 棄用警告 ⚠️

**影響範圍**: 中等 (影響測試警告數量,但不直接導致測試失敗)

#### 2.1 Modal.destroyOnClose → destroyOnHidden
```tsx
// ❌ 舊 API (已棄用)
<Modal destroyOnClose={true} />

// ✅ 新 API
<Modal destroyOnHidden={true} />
```

#### 2.2 Card.bordered → variant
```tsx
// ❌ 舊 API
<Card bordered={false} />

// ✅ 新 API
<Card variant="borderless" />
```

#### 2.3 Drawer.bodyStyle → styles.body
```tsx
// ❌ 舊 API
<Drawer bodyStyle={{ padding: 0 }} />

// ✅ 新 API
<Drawer styles={{ body: { padding: 0 } }} />
```

#### 2.4 Tabs.TabPane → items
```tsx
// ❌ 舊 API
<Tabs>
  <Tabs.TabPane tab="Tab 1" key="1">Content 1</Tabs.TabPane>
</Tabs>

// ✅ 新 API
<Tabs items={[{ label: 'Tab 1', key: '1', children: 'Content 1' }]} />
```

#### 2.5 Table rowKey index 參數
```tsx
// ❌ 舊 API
<Table rowKey={(record, index) => `${record.id}-${index}`} />

// ✅ 新 API
<Table rowKey={(record) => record.id} />
```

---

### 3. Form useForm 未連接警告

**錯誤示例**:
```
Warning: Instance created by `useForm` is not connected to any Form element.
Forget to pass `form` prop?
```

**根本原因**:
- 測試中創建了 useForm 實例,但未傳遞給 Form 組件
- Modal 中的 Form 在 Modal 關閉時被銷毀,但 form 實例仍存在

**修復策略**:
- 確保 `<Form form={form}>` 正確傳遞
- 測試清理時重置 form 實例

---

### 4. React 19 相容性警告

**錯誤示例**:
```
Warning: [antd: compatible] antd v5 support React is 16 ~ 18.
see https://u.ant.design/v5-for-19 for compatible.
```

**影響**: 低 (僅警告,功能正常)
**解決方案**: 等待 Ant Design v6 或暫時忽略

---

### 5. Select options value 為 null 警告

**錯誤示例**:
```
Warning: `value` in Select options should not be `null`.
```

**根本原因**: SmartEnumSelect 組件允許 null 值選項
**修復策略**: 將 null 改為空字符串 "" 或數字 0

---

### 6. jsdom 未實現警告

**錯誤示例**:
```
Not implemented: Window's getComputedStyle() method: with pseudo-elements
```

**影響**: 極低 (測試環境限制,不影響測試結果)
**解決方案**: 可忽略或 mock getComputedStyle

---

## 具體失敗測試示例分析

### 示例 1: job/index.test.tsx (10 failed)

**失敗測試**:
1. ❌ should render job management page with table columns
2. ❌ should switch between active and deleted tabs
3. ❌ should trigger search when keyword is entered
4. ❌ should filter by trigger type
5. ❌ should filter by enabled status
6. ❌ should reset search form when reset button is clicked
7. ❌ should open JobFormModal when add button is clicked
8. ❌ should open JobFormModal with job data when edit button is clicked
9. ❌ should show delete confirmation modal when delete button is clicked
10. ❌ should open JobLogDrawer when view log button is clicked

**共同問題**:
- Tabs.TabPane 棄用 (所有測試)
- act() 警告 (幾乎所有測試)
- API Mock 錯誤: `Cannot read properties of undefined (reading 'ok')`

**修復計劃**:
1. 替換 Tabs.TabPane 為 items API
2. 修復 JobLogDrawer API Mock (queryJobLog)
3. 添加 waitFor() 等待異步操作

---

### 示例 2: category/index.test.tsx (6 failed)

**失敗測試**:
1. ❌ should render category management page (5224ms)
2. ❌ should open form modal when add child button is clicked (3181ms)
3. ❌ should show confirm modal when delete button is clicked (2907ms)
4. ❌ should call delete API when confirm is clicked (3638ms)
5. ❌ should not delete when cancel is clicked (3488ms)
6. ❌ should show error message when delete fails (2839ms)

**共同問題**:
- Card.bordered 棄用
- Modal.destroyOnClose 棄用
- act() 警告
- 刪除確認 Modal 測試邏輯錯誤

**修復計劃**:
1. 替換 Card bordered → variant
2. 替換 Modal destroyOnClose → destroyOnHidden
3. 修復刪除確認 Modal 測試邏輯 (使用正確的 selector)

---

### 示例 3: change-log/index.test.tsx (7 failed, 100%)

**失敗測試**:
1. ❌ should render change log page
2. ❌ should load and display change log list
3. ❌ should filter by publicType
4. ❌ should open ChangeLogFormModal when add button is clicked
5. ❌ should open ChangeLogFormModal when edit button is clicked
6. ❌ should open ChangeLogDetailModal when view detail button is clicked
7. ❌ should handle API error gracefully

**失敗率**: 100% (所有測試失敗)
**執行時間**: 3.4s (非常短,可能測試環境初始化失敗)

**可能原因**:
- 測試文件配置錯誤
- Mock 數據缺失
- 組件渲染失敗

**修復計劃**:
- 優先檢查測試文件基本配置
- 檢查 Mock 數據是否完整

---

## 修復優先級矩陣

| 優先級 | 文件數 | 失敗測試數 | 預計時間 | 影響範圍 |
|--------|--------|------------|----------|----------|
| **P0** | 2 | 20 | 2-3h | Job 模塊核心功能 |
| **P1** | 7 | 40 | 3-4h | 分類、更新日誌、員工、消息 |
| **P2** | 17 | 35 | 2-3h | 其他模塊 |
| **總計** | **26** | **95** | **7-10h** | - |

---

## 修復建議

### 快速勝利 (Quick Wins)
1. ✅ 批量替換 Ant Design 棄用 API (影響所有測試,修復簡單)
2. ✅ 修復 100% 失敗率測試 (change-log/index.test.tsx)
3. ✅ 修復單一失敗測試 (P2 中 7 個文件,每個僅 1 個失敗)

### 高ROI修復
1. ✅ 修復 job 模塊 (2 個文件,20 個失敗,佔 30%)
2. ✅ 修復 category 模塊 (2 個文件,12 個失敗,佔 18%)

### 系統性修復
1. ✅ 建立統一的 act() 處理模式
2. ✅ 建立統一的 Modal 測試模式
3. ✅ 建立統一的 Form 測試模式

---

## 測試環境問題

### jsdom 限制
- ❌ 不支持 getComputedStyle() with pseudo-elements
- ❌ 不完全支持某些 CSS 特性

### Ant Design 5.x + React 19
- ⚠️ Ant Design 官方僅支持 React 16-18
- ⚠️ React 19 兼容性警告 (功能正常,僅警告)

---

## 下一步行動

1. ✅ **立即執行**: 修復 change-log/index.test.tsx (100% 失敗,快速勝利)
2. ✅ **P0 修復**: job 模塊 (2 個文件,20 個失敗)
3. ✅ **P1 修復**: category 模塊 (2 個文件,12 個失敗)
4. ✅ **批量修復**: Ant Design 棄用 API (影響所有文件)
5. ✅ **驗證**: 運行完整測試套件,確保 109/109 通過

---

**診斷完成時間**: 2026-03-20
**預計修復完成時間**: 2026-03-20 (7-10 小時工作)
