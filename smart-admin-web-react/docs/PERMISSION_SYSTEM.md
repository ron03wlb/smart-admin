# React 權限系統使用指南

## 概述

SmartAdmin React 權限系統完整複製了 Vue 版本的 `v-privilege` 指令功能，通過 Redux Store + Custom Hooks + Permission Components 實現聲明式權限控制。

## 架構設計

```
Vue v-privilege 指令
    ↓ (React 等效實現)
usePrivilege Hook + Permission Components
    ↓ (權限數據源)
Redux Store (userSlice.pointsList)
    ↓ (權限過濾)
pointsList.some(point => point.webPerms === permissionCode)
```

## 核心組件

### 1. usePrivilege Hook

**功能**：檢查當前用戶是否擁有指定權限

**簽名**：
```typescript
function usePrivilege(permissionCode: string): boolean
```

**邏輯**：
1. 超級管理員 (`administratorFlag === true`) → 直接返回 `true`
2. 普通用戶 → 檢查 `pointsList.some(point => point.webPerms === permissionCode)`

**使用示例**：
```tsx
import { usePrivilege } from '@/hooks/usePrivilege';

export default function UserList() {
  const canAdd = usePrivilege('system:user:add');
  const canEdit = usePrivilege('system:user:edit');
  const canDelete = usePrivilege('system:user:delete');

  return (
    <div>
      {canAdd && <Button type="primary">新增用戶</Button>}
      {canEdit && <span>您有編輯權限</span>}
      {canDelete && <Popconfirm title="確認刪除？">刪除</Popconfirm>}
    </div>
  );
}
```

### 2. usePrivileges Hook

**功能**：批量檢查多個權限

**簽名**：
```typescript
function usePrivileges(permissionCodes: string[]): Record<string, boolean>
```

**使用示例**：
```tsx
import { usePrivileges } from '@/hooks/usePrivilege';

export default function UserActions() {
  const permissions = usePrivileges([
    'system:user:add',
    'system:user:edit',
    'system:user:delete',
  ]);

  return (
    <Space>
      {permissions['system:user:add'] && <Button>新增</Button>}
      {permissions['system:user:edit'] && <Button>編輯</Button>}
      {permissions['system:user:delete'] && <Button danger>刪除</Button>}
    </Space>
  );
}
```

### 3. PrivilegeButton 組件

**功能**：對應 Vue 的 `<a-button v-privilege>`

**Props**：
```typescript
interface PrivilegeButtonProps extends ButtonProps {
  permissionCode: string;
  mode?: 'hide' | 'disable'; // 默認 'hide'
}
```

**模式說明**：
- `hide`（默認）：無權限時完全隱藏按鈕（對應 Vue 的 `removeChild(el)`）
- `disable`：無權限時禁用按鈕（保留視覺反饋）

**使用示例**：
```tsx
import { PrivilegeButton } from '@/components/framework/privilege';

// 無權限時隱藏
<PrivilegeButton permissionCode="system:user:add" type="primary">
  新增用戶
</PrivilegeButton>

// 無權限時禁用
<PrivilegeButton
  permissionCode="system:user:edit"
  mode="disable"
  type="link"
>
  編輯
</PrivilegeButton>

// 表格操作列
<Space>
  <PrivilegeButton permissionCode="system:user:edit" type="link">
    編輯
  </PrivilegeButton>
  <PrivilegeButton permissionCode="system:user:delete" type="link" danger>
    刪除
  </PrivilegeButton>
</Space>
```

### 4. PrivilegeDiv 組件

**功能**：對應 Vue 的 `<div v-privilege>`

**Props**：
```typescript
interface PrivilegeDivProps extends React.HTMLAttributes<HTMLDivElement> {
  permissionCode: string;
}
```

**使用示例**：
```tsx
import { PrivilegeDiv } from '@/components/framework/privilege';

<PrivilegeDiv permissionCode="system:user:query" className="user-info-panel">
  <Card title="用戶資訊">
    <p>姓名：{user.name}</p>
    <p>部門：{user.department}</p>
  </Card>
</PrivilegeDiv>
```

### 5. PrivilegeFragment 組件

**功能**：對應 Vue 的 `<template v-privilege>`（無額外 DOM 節點）

**Props**：
```typescript
interface PrivilegeFragmentProps {
  permissionCode: string;
  children: React.ReactNode;
}
```

**使用場景**：
- 需要權限控制但不希望添加額外 `<div>` 包裹
- 避免破壞佈局（如 Flex/Grid 子元素）

**使用示例**：
```tsx
import { PrivilegeFragment } from '@/components/framework/privilege';

<Flex>
  <div>總是顯示的內容</div>

  {/* Fragment 不會添加額外 DOM 節點 */}
  <PrivilegeFragment permissionCode="system:user:query">
    <div>需要權限的內容1</div>
    <div>需要權限的內容2</div>
  </PrivilegeFragment>
</Flex>
```

## Vue vs React 對照表

| Vue 寫法 | React 等效 | 說明 |
|---------|-----------|------|
| `<a-button v-privilege="'system:user:add'">新增</a-button>` | `<PrivilegeButton permissionCode="system:user:add">新增</PrivilegeButton>` | 無權限時隱藏按鈕 |
| `<div v-privilege="'system:user:query'">內容</div>` | `<PrivilegeDiv permissionCode="system:user:query">內容</PrivilegeDiv>` | 無權限時隱藏 div |
| `<template v-privilege="'system:user:query'">內容</template>` | `<PrivilegeFragment permissionCode="system:user:query">內容</PrivilegeFragment>` | 無額外 DOM 節點 |
| `v-if="userStore.checkPermission('system:user:add')"` | `{usePrivilege('system:user:add') && <Component />}` | 命令式權限檢查 |

## 完整使用示例

### 示例 1：用戶列表頁面

```tsx
import { useState } from 'react';
import { Table, Space, Input, Form } from 'antd';
import { PrivilegeButton } from '@/components/framework/privilege';
import { usePrivilege } from '@/hooks/usePrivilege';

export default function UserList() {
  const [form] = Form.useForm();
  const canQuery = usePrivilege('system:user:query');
  const canExport = usePrivilege('system:user:export');

  const columns = [
    { title: '用戶名', dataIndex: 'username' },
    { title: '姓名', dataIndex: 'name' },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <PrivilegeButton
            permissionCode="system:user:edit"
            type="link"
            onClick={() => handleEdit(record)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            permissionCode="system:user:delete"
            type="link"
            danger
            onClick={() => handleDelete(record.id)}
          >
            刪除
          </PrivilegeButton>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* 查詢表單（有查詢權限才顯示） */}
      {canQuery && (
        <Form form={form}>
          <Form.Item label="用戶名" name="username">
            <Input placeholder="請輸入用戶名" />
          </Form.Item>
          <PrivilegeButton permissionCode="system:user:query" type="primary">
            查詢
          </PrivilegeButton>
        </Form>
      )}

      {/* 工具欄 */}
      <Space style={{ marginBottom: 16 }}>
        <PrivilegeButton permissionCode="system:user:add" type="primary">
          新增用戶
        </PrivilegeButton>
        <PrivilegeButton permissionCode="system:user:import">
          批量導入
        </PrivilegeButton>
        {canExport && (
          <PrivilegeButton onClick={handleExport}>導出</PrivilegeButton>
        )}
      </Space>

      {/* 表格 */}
      <Table columns={columns} dataSource={users} />
    </div>
  );
}
```

### 示例 2：表格操作列

```tsx
import { Space, Popconfirm } from 'antd';
import { PrivilegeButton, PrivilegeFragment } from '@/components/framework/privilege';

const columns = [
  // ... 其他列
  {
    title: '操作',
    key: 'action',
    render: (_, record) => (
      <Space split={<Divider type="vertical" />}>
        {/* 查看（總是顯示） */}
        <Button type="link" onClick={() => handleView(record)}>
          查看
        </Button>

        {/* 需要權限的操作（使用 Fragment 避免額外 DOM） */}
        <PrivilegeFragment permissionCode="system:user:edit">
          <Button type="link" onClick={() => handleEdit(record)}>
            編輯
          </Button>
        </PrivilegeFragment>

        {/* 刪除（帶確認彈窗） */}
        <PrivilegeButton
          permissionCode="system:user:delete"
          type="link"
          danger
        >
          <Popconfirm
            title="確認刪除？"
            onConfirm={() => handleDelete(record.id)}
          >
            刪除
          </Popconfirm>
        </PrivilegeButton>
      </Space>
    ),
  },
];
```

### 示例 3：複雜權限邏輯

```tsx
import { usePrivilege, usePrivileges } from '@/hooks/usePrivilege';

export default function EmployeeDetail() {
  const canEdit = usePrivilege('system:employee:edit');
  const canViewSalary = usePrivilege('system:employee:salary:query');
  const canEditSalary = usePrivilege('system:employee:salary:edit');

  // 批量檢查
  const permissions = usePrivileges([
    'system:employee:attendance:query',
    'system:employee:attendance:edit',
    'system:employee:performance:query',
  ]);

  return (
    <Tabs>
      <TabPane tab="基本資料" key="basic">
        <Descriptions>
          <Descriptions.Item label="姓名">{employee.name}</Descriptions.Item>
          {/* 更多欄位 */}
        </Descriptions>

        {canEdit && (
          <Button type="primary" onClick={handleEdit}>
            編輯資料
          </Button>
        )}
      </TabPane>

      {/* 薪資頁籤（有權限才顯示） */}
      {canViewSalary && (
        <TabPane tab="薪資資訊" key="salary">
          <SalaryTable data={salaryData} />

          <PrivilegeButton
            permissionCode="system:employee:salary:edit"
            type="primary"
            onClick={handleEditSalary}
          >
            調整薪資
          </PrivilegeButton>
        </TabPane>
      )}

      {/* 考勤頁籤 */}
      {permissions['system:employee:attendance:query'] && (
        <TabPane tab="考勤記錄" key="attendance">
          <AttendanceTable />
        </TabPane>
      )}
    </Tabs>
  );
}
```

## 權限數據流

### 1. 登錄時設置權限

```typescript
import { useAppDispatch } from '@/store/hooks';
import { setUserLoginInfo } from '@/store/slices/userSlice';
import { login } from '@/api/system/login.api';

export default function Login() {
  const dispatch = useAppDispatch();

  const handleLogin = async (values) => {
    const res = await login(values);

    // 設置用戶登錄資訊（包含權限列表）
    dispatch(setUserLoginInfo(res.data));

    // Redux Persist 會自動將 userSlice 持久化到 localStorage
  };
}
```

### 2. Redux Store 自動過濾權限點

```typescript
// userSlice.ts 中的邏輯
setUserLoginInfo: (state, action: PayloadAction<LoginResult>) => {
  const { token, employeeId, administratorFlag, menuList } = action.payload;

  state.token = token;
  state.employeeId = employeeId;
  state.administratorFlag = administratorFlag;

  // 過濾出功能點（對應 Vue 的邏輯）
  state.pointsList = menuList.filter(
    (menu) =>
      menu.menuType === 'POINTS' &&
      menu.visibleFlag &&
      !menu.disabledFlag
  );
}
```

### 3. 組件中使用權限

```typescript
// usePrivilege Hook 內部邏輯
export function usePrivilege(permissionCode: string): boolean {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  return useMemo(() => {
    // 超級管理員直接放行
    if (administratorFlag) {
      return true;
    }

    // 檢查權限點列表
    return pointsList.some((point) => point.webPerms === permissionCode);
  }, [administratorFlag, pointsList, permissionCode]);
}
```

## 測試覆蓋

權限系統包含完整的單元測試（29 個測試用例）：

### usePrivilege Hook 測試（8 個）
- ✅ 超級管理員應該擁有所有權限
- ✅ 普通用戶有權限時應該返回 true
- ✅ 普通用戶無權限時應該返回 false
- ✅ 空權限列表應該返回 false
- ✅ 權限編碼完全匹配時應該返回 true
- ✅ 權限編碼部分匹配時應該返回 false（精確匹配）
- ✅ 權限列表為 null 時應該返回 false
- ✅ 多個權限點包含相同 webPerms 時應該正確檢查

### usePrivileges Hook 測試（5 個）
- ✅ 超級管理員應該對所有權限返回 true
- ✅ 普通用戶應該根據權限列表返回正確結果
- ✅ 空權限編碼數組應該返回空對象
- ✅ 空權限列表應該對所有權限返回 false
- ✅ 單個權限編碼應該正確檢查

### PrivilegeButton 測試（7 個）
- ✅ 有權限時應該顯示按鈕
- ✅ 無權限且 hide 模式時不應該渲染
- ✅ 無權限且 disable 模式時應該禁用按鈕
- ✅ 超級管理員應該看到所有按鈕
- ✅ 有權限時應該保留原始 disabled 狀態
- ✅ 應該正確傳遞 Ant Design Button props
- ✅ 默認 mode 應該是 hide

### PrivilegeDiv 測試（4 個）
- ✅ 有權限時應該渲染 div
- ✅ 無權限時不應該渲染
- ✅ 超級管理員應該看到所有內容
- ✅ 應該正確傳遞 div props

### PrivilegeFragment 測試（5 個）
- ✅ 有權限時應該渲染子元素
- ✅ 無權限時不應該渲染
- ✅ 超級管理員應該看到所有內容
- ✅ 不應該添加額外的 DOM 節點（使用 Fragment）
- ✅ 應該支持多種類型的子元素

### 運行測試

```bash
# 運行所有權限系統測試
npm test -- usePrivilege.test.tsx PrivilegeComponents.test.tsx --run

# 帶覆蓋率報告
npm test:coverage -- usePrivilege.test.tsx PrivilegeComponents.test.tsx
```

## 常見問題

### Q1: 什麼時候使用 Hook，什麼時候使用 Component？

**答**：
- **使用 Hook**：需要根據權限渲染不同內容、複雜條件邏輯
  ```tsx
  const canEdit = usePrivilege('system:user:edit');
  return canEdit ? <EditForm /> : <ViewOnly />;
  ```

- **使用 Component**：簡單的按鈕/區域顯示/隱藏（推薦，聲明式）
  ```tsx
  <PrivilegeButton permissionCode="system:user:add">新增</PrivilegeButton>
  ```

### Q2: PrivilegeDiv vs PrivilegeFragment 如何選擇？

**答**：
- **PrivilegeDiv**：可以添加 className、style 等 div 屬性
- **PrivilegeFragment**：不希望添加額外 DOM 節點（如 Flex/Grid 佈局）

### Q3: 如何調試權限問題？

**答**：使用 Redux DevTools 查看 `state.user.pointsList`：
```javascript
// 瀏覽器 Console
JSON.stringify(
  window.__REDUX_DEVTOOLS_EXTENSION__.store.getState().user.pointsList,
  null,
  2
);
```

### Q4: 超級管理員如何關閉權限檢查？

**答**：超級管理員標識由後端返回，無需前端配置：
```typescript
// 後端返回的登錄數據
{
  "token": "xxx",
  "administratorFlag": true  // 超級管理員
}
```

## 與 Vue 版本的差異

| 特性 | Vue | React | 說明 |
|-----|-----|-------|------|
| 權限控制方式 | 自定義指令 | Custom Hooks + 組件 | React 無指令機制 |
| 無權限行為 | `removeChild(el)` 移除 DOM | 返回 `null` | 等效效果 |
| 狀態管理 | Pinia | Redux Toolkit | 功能一致 |
| 持久化 | 手動 localStorage | Redux Persist | React 自動化 |
| TypeScript 支持 | 部分 | 完整 | React 全面類型化 |

## 最佳實踐

1. **優先使用聲明式組件**（PrivilegeButton/Div/Fragment）而非命令式 Hook
2. **批量權限檢查**使用 `usePrivileges` 避免多次 selector 調用
3. **表格操作列**使用 `PrivilegeFragment` 避免破壞 Space 佈局
4. **複雜邏輯**才使用 `usePrivilege` Hook 進行條件渲染
5. **權限編碼**保持與後端一致（如 `system:user:add`）

## 相關文件

- **Hook 實現**：[src/hooks/usePrivilege.ts](../src/hooks/usePrivilege.ts)
- **組件實現**：[src/components/framework/privilege/](../src/components/framework/privilege/)
- **Redux Store**：[src/store/slices/userSlice.ts](../src/store/slices/userSlice.ts)
- **測試文件**：[src/hooks/__tests__/usePrivilege.test.tsx](../src/hooks/__tests__/usePrivilege.test.tsx)
