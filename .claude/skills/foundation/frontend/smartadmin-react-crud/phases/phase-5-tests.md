# Phase 5: 測試生成

**預計時間**: ~3 分鐘
**輸出文件**: `smart-admin-web-react/src/views/{module}/{entity}/__tests__/`
**依賴**: Phase 3（列表組件）、Phase 4（表單模態框）

---

## 概述

Phase 5 生成 Vitest + React Testing Library 測試，覆蓋列表組件和表單模態框的核心功能，目標覆蓋率 >= 80%。

---

## 測試文件結構

```
smart-admin-web-react/src/views/{module}/{entity}/
└── __tests__/
    ├── {Entity}List.spec.tsx          # 列表組件測試
    └── {Entity}FormModal.spec.tsx     # 表單模態框測試
```

---

## 列表組件測試

```tsx
// smart-admin-web-react/src/views/system/employee/__tests__/EmployeeList.spec.tsx

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { EmployeeList } from '../EmployeeList';
import { employeeApi } from '@/api/system/employee-api';
import userReducer from '@/store/slices/userSlice';

// Mock API
vi.mock('@/api/system/employee-api');

// 創建測試 Store
const createTestStore = () => {
  return configureStore({
    reducer: {
      user: userReducer,
    },
    preloadedState: {
      user: {
        token: 'test-token',
        employeeId: '1',
        administratorFlag: true,
        pointsList: [],
        menuTree: [],
      },
    },
  });
};

describe('EmployeeList Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should fetch and display employee data on mount', async () => {
    const mockData = {
      success: true,
      data: {
        list: [
          {
            employeeId: 1,
            employeeName: 'John Doe',
            email: 'john@example.com',
            createTime: '2026-03-06T10:00:00Z',
          },
        ],
        total: 1,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
        emptyFlag: false,
      },
    };
    vi.mocked(employeeApi.query).mockResolvedValue(mockData);

    const store = createTestStore();
    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('john@example.com')).toBeInTheDocument();
    });

    expect(employeeApi.query).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 10,
      keyword: '',
    });
  });

  it('should handle search keyword input and query', async () => {
    const user = userEvent.setup();
    const store = createTestStore();

    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    const searchInput = screen.getByPlaceholderText('請輸入搜索關鍵字');
    await user.type(searchInput, 'test');

    const queryButton = screen.getByRole('button', { name: '查詢' });
    await user.click(queryButton);

    await waitFor(() => {
      expect(employeeApi.query).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: 'test' })
      );
    });
  });

  it('should open form modal when add button clicked', async () => {
    const user = userEvent.setup();
    const store = createTestStore();

    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    const addButton = screen.getByRole('button', { name: /新增/i });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText(/新增Employee/i)).toBeInTheDocument();
    });
  });

  it('should call delete API and refresh data when delete confirmed', async () => {
    const mockQueryData = {
      success: true,
      data: {
        list: [
          {
            employeeId: 1,
            employeeName: 'John Doe',
            email: 'john@example.com',
            createTime: '2026-03-06T10:00:00Z',
          },
        ],
        total: 1,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
        emptyFlag: false,
      },
    };
    vi.mocked(employeeApi.query).mockResolvedValue(mockQueryData);
    vi.mocked(employeeApi.delete).mockResolvedValue({ success: true });

    const user = userEvent.setup();
    const store = createTestStore();

    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
    });

    const deleteButton = screen.getByRole('button', { name: /刪除/i });
    await user.click(deleteButton);

    const confirmButton = screen.getByRole('button', { name: '確定' });
    await user.click(confirmButton);

    await waitFor(() => {
      expect(employeeApi.delete).toHaveBeenCalledWith(1);
    });
  });

  it('should handle pagination change', async () => {
    const store = createTestStore();

    render(
      <Provider store={store}>
        <EmployeeList />
      </Provider>
    );

    // 模擬翻頁
    const nextPageButton = screen.getByRole('button', { name: '下一頁' });
    await userEvent.click(nextPageButton);

    await waitFor(() => {
      expect(employeeApi.query).toHaveBeenCalledWith(
        expect.objectContaining({ pageNum: 2 })
      );
    });
  });
});
```

---

## 表單模態框測試

```tsx
// smart-admin-web-react/src/views/system/employee/__tests__/EmployeeFormModal.spec.tsx

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { EmployeeFormModal } from '../EmployeeFormModal';
import { employeeApi } from '@/api/system/employee-api';

vi.mock('@/api/system/employee-api');

describe('EmployeeFormModal Component', () => {
  const mockOnCancel = vi.fn();
  const mockOnSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render in add mode when record is null', () => {
    render(
      <EmployeeFormModal
        visible={true}
        record={null}
        onCancel={mockOnCancel}
        onSuccess={mockOnSuccess}
      />
    );

    expect(screen.getByText(/新增Employee/i)).toBeInTheDocument();
  });

  it('should render in edit mode when record is provided', () => {
    const mockRecord = {
      employeeId: 1,
      employeeName: 'John Doe',
      email: 'john@example.com',
      createTime: '2026-03-06T10:00:00Z',
    };

    render(
      <EmployeeFormModal
        visible={true}
        record={mockRecord}
        onCancel={mockOnCancel}
        onSuccess={mockOnSuccess}
      />
    );

    expect(screen.getByText(/編輯Employee/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
    expect(screen.getByDisplayValue('john@example.com')).toBeInTheDocument();
  });

  it('should call add API when submitting in add mode', async () => {
    const user = userEvent.setup();
    vi.mocked(employeeApi.add).mockResolvedValue({ success: true });

    render(
      <EmployeeFormModal
        visible={true}
        record={null}
        onCancel={mockOnCancel}
        onSuccess={mockOnSuccess}
      />
    );

    // 填寫表單
    const nameInput = screen.getByLabelText('姓名');
    await user.type(nameInput, 'John Doe');

    const emailInput = screen.getByLabelText('郵箱');
    await user.type(emailInput, 'john@example.com');

    // 提交表單
    const okButton = screen.getByRole('button', { name: '確定' });
    await user.click(okButton);

    await waitFor(() => {
      expect(employeeApi.add).toHaveBeenCalledWith(
        expect.objectContaining({
          employeeName: 'John Doe',
          email: 'john@example.com',
        })
      );
      expect(mockOnSuccess).toHaveBeenCalled();
    });
  });

  it('should call update API when submitting in edit mode', async () => {
    const user = userEvent.setup();
    const mockRecord = {
      employeeId: 1,
      employeeName: 'John Doe',
      email: 'john@example.com',
      createTime: '2026-03-06T10:00:00Z',
    };
    vi.mocked(employeeApi.update).mockResolvedValue({ success: true });

    render(
      <EmployeeFormModal
        visible={true}
        record={mockRecord}
        onCancel={mockOnCancel}
        onSuccess={mockOnSuccess}
      />
    );

    // 修改表單
    const nameInput = screen.getByDisplayValue('John Doe');
    await user.clear(nameInput);
    await user.type(nameInput, 'Jane Doe');

    // 提交表單
    const okButton = screen.getByRole('button', { name: '確定' });
    await user.click(okButton);

    await waitFor(() => {
      expect(employeeApi.update).toHaveBeenCalledWith(
        expect.objectContaining({
          employeeId: 1,
          employeeName: 'Jane Doe',
        })
      );
      expect(mockOnSuccess).toHaveBeenCalled();
    });
  });

  it('should show validation errors for invalid form data', async () => {
    const user = userEvent.setup();

    render(
      <EmployeeFormModal
        visible={true}
        record={null}
        onCancel={mockOnCancel}
        onSuccess={mockOnSuccess}
      />
    );

    // 不填寫必填字段，直接提交
    const okButton = screen.getByRole('button', { name: '確定' });
    await user.click(okButton);

    await waitFor(() => {
      expect(screen.getByText(/請輸入姓名/i)).toBeInTheDocument();
    });

    expect(employeeApi.add).not.toHaveBeenCalled();
  });
});
```

---

## 測試覆蓋範圍

### 列表組件測試

- ✅ 組件渲染
- ✅ 數據獲取（Mock API）
- ✅ 數據展示（Table）
- ✅ 搜索功能（輸入關鍵字、點擊查詢）
- ✅ 分頁功能（翻頁、改變頁大小）
- ✅ 新增操作（打開模態框）
- ✅ 編輯操作（打開模態框、預填數據）
- ✅ 刪除操作（確認框、API 調用）
- ✅ 批量刪除（選中行、確認框、API 調用）

### 表單模態框測試

- ✅ Add 模式渲染
- ✅ Edit 模式渲染（預填數據）
- ✅ 表單提交（Add API）
- ✅ 表單提交（Update API）
- ✅ 表單驗證（必填字段、格式驗證）
- ✅ 錯誤處理（API 失敗）
- ✅ 成功回調（onSuccess）

---

## 測試工具

### 1. Vitest

```bash
npm run test               # 運行所有測試
npm run test:coverage      # 生成覆蓋率報告
npm run test:ui            # 可視化測試 UI
```

### 2. React Testing Library

```tsx
import { render, screen, waitFor } from '@testing-library/react';

// 渲染組件
render(<Component />);

// 查詢元素
screen.getByText('文本');
screen.getByRole('button', { name: '按鈕' });
screen.getByLabelText('標籤');
screen.getByPlaceholderText('佔位符');

// 等待異步操作
await waitFor(() => {
  expect(screen.getByText('數據')).toBeInTheDocument();
});
```

### 3. userEvent

```tsx
import userEvent from '@testing-library/user-event';

const user = userEvent.setup();

// 點擊
await user.click(button);

// 輸入
await user.type(input, '文本');

// 清空
await user.clear(input);

// 鍵盤事件
await user.keyboard('{Enter}');
```

---

## 驗證清單

- [ ] 測試文件路徑正確：`src/views/{module}/{entity}/__tests__/`
- [ ] 所有 API 方法已 Mock（`vi.mock`）
- [ ] 測試使用 Redux Provider 包裹組件
- [ ] 異步操作使用 `waitFor`
- [ ] 用戶交互使用 `userEvent.setup()`
- [ ] 測試覆蓋率 >= 80%
- [ ] 所有測試通過（`npm run test`）
- [ ] TypeScript 編譯無錯誤

---

**Phase 5 完成標準**：
- ✅ 測試文件創建成功
- ✅ 所有測試通過
- ✅ 測試覆蓋率 >= 80%
- ✅ 關鍵功能覆蓋（CRUD、分頁、搜索、驗證）

**所有 Phase 完成！**下一步進行完整的質量檢查。
