# Vue to React 遷移專案 - 任務計劃

**計劃版本**: 1.0.0
**創建日期**: 2026-03-10
**最後更新**: 2026-03-10
**專案狀態**: Phase 1 進行中（Week 2 Day 5）
**整體進度**: 15-20%

---

## 🎯 專案目標

將 SmartAdmin 前端從 Vue 3.4.27 全面遷移至 React 19.2.0，保持所有功能完整性的同時優化架構設計。

**關鍵指標**:
- 195 個頁面完全遷移
- 單元測試覆蓋率 ≥ 75%
- 首屏加載時間 < 2s
- 12 週內完成部署

---

## 📅 階段計劃

### Phase 0: 準備階段 (Week 0) ✅ 已完成

**狀態**: ✅ 完成 (100%)
**完成日期**: 2026-03-09

**已完成任務**:
- [x] 創建遷移計劃文檔 v1.1.0
- [x] 創建進度追蹤文檔
- [x] 設置專案目錄結構

**交付物**:
- ✅ docs/migration/vue-to-react-migration-plan.md (v1.1.0)
- ✅ docs/migration/vue-to-react-PROGRESS-TRACKING.md

---

### Phase 1: Foundation & POC (Week 1-2) 🔄 進行中

**狀態**: 🔄 進行中 (70%)
**當前**: Week 2 Day 5
**預計完成**: Week 2 結束

#### Week 1: 項目搭建與技術驗證 ✅ 已完成

- [x] Day 1: Vite + React + TypeScript 腳手架搭建
- [x] Day 1-2: 核心依賴安裝 (Redux Toolkit, Ant Design, React Router)
- [x] Day 2-3: API 層封裝 (axios 配置、ResponseDTO 類型定義)
- [x] Day 4-5: 權限系統 POC (usePrivilege Hook、PrivilegeButton 組件)

**已完成**:
- ✅ TypeScript 嚴格模式配置
- ✅ Redux Toolkit + Redux Persist 配置
- ✅ Axios 攔截器實現
- ✅ ResponseDTO/PageResult 類型定義
- ✅ usePrivilege/usePrivileges/useAnyPrivilege Hooks
- ✅ PrivilegeButton 組件

#### Week 2: 核心功能遷移 🔄 進行中

- [x] Day 1-2: 登錄頁遷移 (LoginForm、Sa-Token 集成、MFA 雙因子認證)
- [x] Day 3: userSlice 完整遷移 (427 行，菜單樹構建邏輯)
- [x] Day 4: 首頁遷移 + 側邊欄菜單（動態菜單渲染）
- [x] Day 5: 動態路由生成 (React.lazy 懶加載) - **當前進行中**

**已完成**:
- ✅ 登錄頁面 (314+ 行，含 MFA)
- ✅ userSlice (427 行)
- ✅ dictSlice (295 行)
- ✅ BasicLayout 側邊欄 (164 行)
- ✅ 動態路由映射框架 (72 行)
- ✅ ProtectedRoute 路由守衛
- ✅ DynamicPage 動態頁面加載

**待完成**:
- [ ] 完善動態路由懶加載優化
- [ ] 測試 6 個已註冊路由
- [ ] 準備 M1 里程碑驗收

#### M1 檢查清單 (Week 2 結束)

**登錄與認證**:
- [x] 登錄頁渲染正常
- [x] 用戶名/密碼登錄成功
- [x] Token 存儲正確 (localStorage)
- [x] Token 攜帶正確 (Axios 攔截器)
- [x] 登錄失敗提示正確
- [x] MFA 雙因子認證流程

**首頁與導航**:
- [x] 首頁渲染正常
- [x] 側邊菜單正確（支持 3 層嵌套）
- [x] 菜單動態生成（基於後端菜單樹）
- [x] 路由跳轉正常
- [ ] TagNav 標籤導航（待實現）

**權限系統**:
- [x] usePrivilege Hook 正確
- [x] PrivilegeButton 顯示/隱藏正確
- [x] 管理員用戶全部按鈕可見
- [x] 普通用戶部分按鈕隱藏
- [x] 路由權限正確 (ProtectedRoute)

**性能測試**:
- [ ] 首屏加載時間 < 4s（POC 階段）
- [ ] 無 console 錯誤
- [ ] 無 React Warning
- [ ] 內存佔用 < 150MB

**代碼質量**:
- [ ] ESLint 無錯誤（待配置）
- [ ] TypeScript 編譯無錯誤
- [ ] 單元測試通過（待編寫）
- [ ] E2E 測試通過（待編寫）

---

### Phase 2: Core Infrastructure (Week 3-4) ⬜ 未開始

**狀態**: ⬜ 未開始 (0%)
**預計開始**: Week 3 開始

#### 目標
- 10+ 個通用組件
- 8 個 Redux Slices (目前 2/8)
- 國際化配置
- Keep-alive 機制

#### Week 3: 通用組件庫建設

**計劃任務**:
- [ ] Day 1-2: 表單組件 (SmartEnumSelect、DictSelect、CategoryTreeSelect)
- [ ] Day 3: 表格組件 (TableOperator、useTable Hook)
- [ ] Day 4-5: 業務組件 (FileUpload、SmartLoading、EmployeeSelect)

**待開發組件清單**:
1. SmartEnumSelect - 枚舉選擇器
2. ~~DictSelect~~ - ✅ 已完成
3. CategoryTreeSelect - 分類樹選擇器
4. TableOperator - 表格操作欄
5. useTable Hook - 表格邏輯 Hook
6. FileUpload - 文件上傳組件
7. SmartLoading - 加載指示器
8. EmployeeSelect - 員工選擇器
9. useModal Hook - Modal 邏輯 Hook
10. usePagination Hook - 分頁邏輯 Hook

#### Week 4: 狀態管理與國際化

**計劃任務**:
- [ ] Day 1-3: Redux Slices 遷移 (appConfig、role、tenant、spin、tagNav、menu)
- [ ] Day 4: 國際化 (react-i18next 配置、語言包遷移)
- [ ] Day 5: Keep-alive 機制 (KeepAliveOutlet 組件)

**待實現 Slices** (6/8):
1. ~~userSlice~~ - ✅ 已完成 (427 行)
2. ~~dictSlice~~ - ✅ 已完成 (295 行)
3. appConfigSlice - 應用配置
4. roleSlice - 角色狀態
5. tenantSlice - 多租戶
6. spinSlice - 加載狀態
7. tagNavSlice - 標籤導航
8. menuSlice - 菜單管理

#### M2 檢查清單 (Week 4 結束)

**通用組件**:
- [ ] 10+ 個組件全部完成
- [ ] Storybook 文檔
- [ ] 單元測試覆蓋率 ≥ 80%

**Redux Slices**:
- [ ] 8/8 Slices 完成
- [ ] 所有狀態邏輯與 Vue 版本一致

**功能測試**:
- [ ] 語言切換無刷新生效
- [ ] 頁面緩存正確工作
- [ ] 狀態持久化正常

---

### Phase 3: Component Migration (Week 5-8) ⬜ 未開始

**狀態**: ⬜ 未開始 (3.6% - 7/195 頁面)

#### 目標
完成所有 195 個 Vue 頁面遷移

**已完成頁面** (7 個):
1. ✅ /system/login - 登錄頁 (314+ 行)
2. ✅ /home - 首頁 (75 行)
3. ✅ /system/employee - 員工管理（路由已註冊）
4. ✅ /system/role - 角色管理（路由已註冊）
5. ✅ /system/menu - 菜單管理（路由已註冊）
6. ✅ /business/goods - 商品管理（路由已註冊）
7. ✅ /support/file - 文件管理（路由已註冊）

**待完成頁面**: 188 個

#### Week 5: System 模塊遷移

**P0 優先級**:
- [ ] employee-list.vue (412 行) → EmployeeList.tsx
- [ ] menu-list.vue (278 行) → MenuList.tsx

**P1 優先級**:
- [ ] role-list.vue → RoleList.tsx
- [ ] department-list.vue → DepartmentList.tsx

#### Week 6: Business 模塊遷移

**P0 優先級**:
- [ ] goods-list.vue (529 行) → GoodsList.tsx
- [ ] enterprise-list.vue (288 行) → EnterpriseList.tsx

**P1 優先級**:
- [ ] notice-list.vue (358 行) → NoticeList.tsx

#### Week 7-8: Support 模塊遷移

**P0 優先級**:
- [ ] job-list.vue (379 行) → JobList.tsx
- [ ] file-list.vue (296 行) → FileList.tsx

**P1 優先級**:
- [ ] change-log-list.vue (326 行) → ChangeLogList.tsx
- [ ] help-doc.vue (328 行) → HelpDoc.tsx

#### M3 檢查清單 (Week 6 結束 - 50% 遷移)

- [ ] 頁面遷移完成率 ≥ 50% (97/195 個)
- [ ] E2E 測試覆蓋核心流程
- [ ] 單元測試覆蓋率 ≥ 60%

#### M4 檢查清單 (Week 8 結束 - 100% 遷移)

- [ ] System 模塊 100% (12 個頁面)
- [ ] Business 模塊 100% (30+ 個頁面)
- [ ] Support 模塊 100% (150+ 個頁面)
- [ ] 頁面遷移完成率 = 100% (195/195 個)
- [ ] 單元測試覆蓋率 ≥ 75%
- [ ] E2E 測試覆蓋率 100%
- [ ] 無 P0/P1 級別 Bug

---

### Phase 4: Integration & Testing (Week 9-10) ⬜ 未開始

**狀態**: ⬜ 未開始 (0%)

#### Week 9: 集成測試與 Bug 修復

- [ ] 功能測試（權限系統、CRUD 流程、導入/導出）
- [ ] 兼容性測試（Chrome、Edge、Firefox、Safari）
- [ ] 性能測試（首屏加載、TTI、LCP）
- [ ] Bug 修復

#### Week 10: UAT 與文檔完善

- [ ] UAT 測試（產品經理、測試團隊、業務用戶）
- [ ] 開發者文檔編寫
- [ ] 部署文檔編寫
- [ ] 遷移報告編寫

#### M5 檢查清單 (Week 10 結束 - UAT 通過)

**功能測試**:
- [ ] 登錄與認證（5 個場景）
- [ ] CRUD 操作（10 個場景）
- [ ] 權限控制（8 個場景）
- [ ] 導入/導出（5 個場景）
- [ ] 國際化（3 個場景）

**性能測試**:
- [ ] 首屏加載 < 2.5s
- [ ] TTI < 3.5s
- [ ] 表格渲染（1000 行）< 1.2s

**UAT 驗收**:
- [ ] 產品經理驗收通過
- [ ] 測試團隊驗收通過
- [ ] 無 P0/P1 級別 Bug

---

### Phase 5: Optimization & Deployment (Week 11-12) ⬜ 未開始

**狀態**: ⬜ 未開始 (0%)

#### Week 11: 性能優化

- [ ] 代碼拆分（路由懶加載、第三方庫拆分）
- [ ] 資源優化（圖片壓縮、字體子集化、CDN）
- [ ] 運行時優化（useMemo、虛擬列表、防抖/節流）
- [ ] Lighthouse CI 集成

**性能目標**:
- 首屏加載 < 2s
- TTI < 3s
- Lighthouse Performance ≥ 90

#### Week 12: 灰度發布與監控

- [ ] 灰度發布（10% 流量）
- [ ] 監控與調優（10% 流量）
- [ ] 灰度發布（50% 流量）
- [ ] 監控與調優（50% 流量）
- [ ] 全量發布（100% 流量）

#### M6 檢查清單 (Week 12 結束 - 生產發布)

**性能優化**:
- [ ] 首屏加載 < 2s
- [ ] Lighthouse Performance ≥ 90
- [ ] Bundle Size < 900KB (gzip)

**灰度發布**:
- [ ] 10% 流量切換成功（無錯誤）
- [ ] 50% 流量切換成功（錯誤率 < 0.5%）
- [ ] 100% 流量切換成功

**監控配置**:
- [ ] Sentry 錯誤監控正常
- [ ] Grafana 儀表板配置完成
- [ ] Lighthouse CI 集成到 CI/CD

---

## ⚠️ 當前風險與問題

### 高優先級風險

**R001: Redux Slices 進度落後**
- **影響**: 狀態管理 75% 缺失 (6/8 待實現)
- **概率**: 中
- **應對措施**: Week 3 加速實現 appConfigSlice/roleSlice

**R002: 通用組件缺失**
- **影響**: 後續頁面開發效率受阻
- **概率**: 高
- **應對措施**: Week 3 必須完成 Table/Form/Upload 組件

**R003: 測試覆蓋率為 0%**
- **影響**: 質量保障缺失
- **概率**: 確定
- **應對措施**: 立即配置測試框架，同步開發測試 (TDD)

**R004: React Router 7 版本升級風險**
- **影響**: 潛在 API 不相容
- **概率**: 中
- **應對措施**: 驗證現有路由，查閱 Migration Guide

**R005: Vite 7 大版本升級風險**
- **影響**: 構建工具不相容
- **概率**: 中
- **應對措施**: 驗證構建配置，測試 HMR

### 中優先級風險

**R006: ESLint/Prettier 配置缺失**
- **影響**: 代碼質量無法檢查
- **應對措施**: 本週內補充配置文件

**R007: 缺失 @ant-design/icons**
- **影響**: 無法使用 Ant Design 圖標
- **應對措施**: 立即安裝依賴

---

## 📊 關鍵指標追蹤

| 指標 | 目標值 | 當前值 | 達成率 | 狀態 |
|------|--------|--------|--------|------|
| **頁面遷移完成率** | 100% | 3.6% | 3.6% | 🔴 |
| **Redux Slices** | 8 個 | 2 個 | 25% | 🟡 |
| **通用組件** | 10+ 個 | 4 個 | 40% | 🟡 |
| **單元測試覆蓋率** | 75% | 0% | 0% | 🔴 |
| **E2E 測試覆蓋率** | 100% | 0% | 0% | 🔴 |
| **首屏加載時間** | < 2s | 未測試 | - | ⬜ |
| **Lighthouse 評分** | ≥ 90 | 未測試 | - | ⬜ |

---

## 📝 下一步行動

### 本週內（立即行動）

1. **完成 Week 2 Day 5 任務**
   - [ ] 完善動態路由懶加載優化
   - [ ] 測試 6 個已註冊路由
   - [ ] 準備 M1 里程碑驗收

2. **補充關鍵配置**
   - [ ] 添加 ESLint 配置文件
   - [ ] 添加 Prettier 配置
   - [ ] 安裝 @ant-design/icons
   - [ ] 驗證 React Router 7 相容性

3. **建立測試框架**
   - [ ] 配置 Vitest (vitest.config.ts)
   - [ ] 編寫第一個單元測試 (usePrivilege.test.ts)
   - [ ] 設置 CI/CD 測試流程

### Week 3（下週）

1. **通用組件開發**
   - [ ] SmartEnumSelect
   - [ ] CategoryTreeSelect
   - [ ] TableOperator + useTable Hook
   - [ ] FileUpload
   - [ ] SmartLoading
   - [ ] EmployeeSelect

2. **Redux Slices 實現**
   - [ ] appConfigSlice
   - [ ] roleSlice
   - [ ] tenantSlice

---

## 📚 相關文檔

- [Vue to React 遷移計劃 v1.1.0](docs/migration/vue-to-react-migration-plan.md)
- [進度追蹤文檔](docs/migration/vue-to-react-PROGRESS-TRACKING.md)
- [SmartAdmin 架構規則](.agent/rules/foundation/F04-architecture-rules.md)
- [React CRUD 技能](.claude/skills/foundation/frontend/smartadmin-react-crud/)

---

**計劃維護**:
- 每日更新當前任務狀態
- 每週更新階段進度
- 遇到阻塞問題立即記錄
- 里程碑完成後更新驗收清單
