/**
 * Department Management Page Tests
 * 部門管理頁面集成測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DepartmentPage from './index';
import { departmentApi } from '@/api/system/departmentApi';
import type { DepartmentVO } from './types';

// 增加測試超時時間（樹形表格渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock departmentApi
vi.mock('@/api/system/departmentApi', () => ({
  departmentApi: {
    queryAllDepartment: vi.fn(),
    deleteDepartment: vi.fn(),
  },
}));

// Mock antd message
vi.mock('antd', async () => {
  const actual = (await vi.importActual('antd')) as Record<string, unknown>;
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  };
});

// Mock usePrivilege hook
vi.mock('@/hooks/usePrivilege', () => ({
  usePrivilege: () => true,
}));

// Mock PrivilegeButton component
vi.mock('@/components/PrivilegeButton', () => ({
  default: ({ children, onClick }: { children: React.ReactNode; onClick?: () => void }) => (
    <button onClick={onClick}>{children}</button>
  ),
}));

// Mock DepartmentFormModal component
vi.mock('./components/DepartmentFormModal', () => ({
  default: ({ visible, onCancel }: { visible: boolean; onCancel: () => void }) =>
    visible ? (
      <div data-testid="department-form-modal">
        <button onClick={onCancel}>Cancel</button>
      </div>
    ) : null,
}));

describe('DepartmentPage', () => {
  const mockDepartmentList: DepartmentVO[] = [
    {
      departmentId: 1,
      departmentName: '公司總部',
      parentId: 0,
      managerId: 1,
      managerName: '張總',
      sort: 1,
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
    {
      departmentId: 2,
      departmentName: '技術部',
      parentId: 1,
      managerId: 2,
      managerName: '李經理',
      sort: 2,
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
    {
      departmentId: 3,
      departmentName: '研發組',
      parentId: 2,
      managerName: '王組長',
      sort: 3,
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(departmentApi.queryAllDepartment).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockDepartmentList,
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該渲染頁面標題和搜索框',
      async () => {
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getByText('部門管理')).toBeInTheDocument();
            expect(screen.getByPlaceholderText('請輸入部門名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染操作按鈕',
      async () => {
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getByText('查詢')).toBeInTheDocument();
            expect(screen.getByText('重置')).toBeInTheDocument();
            expect(screen.getByText('新建')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染表格',
      async () => {
        render(<DepartmentPage />);

        // 等待數據加載完成
        await waitFor(
          () => {
            expect(screen.getByText('公司總部')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 驗證至少有一個表格列標題
        const columnHeaders = screen.getAllByRole('columnheader');
        expect(columnHeaders.length).toBeGreaterThan(0);
      },
      TEST_TIMEOUT
    );
  });

  describe('數據加載', () => {
    it(
      '應該在初始化時加載部門列表',
      async () => {
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(departmentApi.queryAllDepartment).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示部門列表數據',
      async () => {
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getByText('公司總部')).toBeInTheDocument();
            expect(screen.getByText('技術部')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理 API 錯誤',
      async () => {
        const { message } = await import('antd');
        vi.mocked(departmentApi.queryAllDepartment).mockRejectedValue(new Error('網絡錯誤'));

        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('查詢部門列表失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('搜索功能', () => {
    it(
      '應該支持按部門名稱搜索',
      async () => {
        const user = userEvent.setup();
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入部門名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('請輸入部門名稱');
        await user.type(searchInput, '技術');

        const searchButton = screen.getByText('查詢');
        await user.click(searchButton);

        // 搜索後應該仍然顯示技術部
        await waitFor(
          () => {
            expect(screen.getByText('技術部')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該支持重置搜索',
      async () => {
        const user = userEvent.setup();
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入部門名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('請輸入部門名稱');
        await user.type(searchInput, '測試關鍵字');

        const resetButton = screen.getByText('重置');
        await user.click(resetButton);

        // 驗證搜索框已清空
        await waitFor(
          () => {
            expect((searchInput as HTMLInputElement).value).toBe('');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('新建部門', () => {
    it(
      '應該打開部門表單 Modal',
      async () => {
        const user = userEvent.setup();
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getByText('新建')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('新建'));

        await waitFor(
          () => {
            expect(screen.getByTestId('department-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在取消時關閉 Modal',
      async () => {
        const user = userEvent.setup();
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getByText('新建')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 打開 Modal
        await user.click(screen.getByText('新建'));

        await waitFor(
          () => {
            expect(screen.getByTestId('department-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 點擊取消
        const cancelButton = screen.getByText('Cancel');
        await user.click(cancelButton);

        await waitFor(
          () => {
            expect(screen.queryByTestId('department-form-modal')).not.toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('添加下級部門', () => {
    it(
      '應該顯示「添加下級」按鈕',
      async () => {
        render(<DepartmentPage />);

        await waitFor(
          () => {
            const addSubButtons = screen.getAllByText('添加下級');
            expect(addSubButtons.length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該打開添加下級部門的 Modal',
      async () => {
        const user = userEvent.setup();
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getAllByText('添加下級').length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );

        const addSubButtons = screen.getAllByText('添加下級');
        await user.click(addSubButtons[0]);

        await waitFor(
          () => {
            expect(screen.getByTestId('department-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('編輯部門', () => {
    it(
      '應該顯示「編輯」按鈕',
      async () => {
        render(<DepartmentPage />);

        await waitFor(
          () => {
            const editButtons = screen.getAllByText('編輯');
            expect(editButtons.length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該打開編輯部門的 Modal',
      async () => {
        const user = userEvent.setup();
        render(<DepartmentPage />);

        await waitFor(
          () => {
            expect(screen.getAllByText('編輯').length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );

        const editButtons = screen.getAllByText('編輯');
        await user.click(editButtons[0]);

        await waitFor(
          () => {
            expect(screen.getByTestId('department-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('刪除部門', () => {
    it(
      '應該顯示「刪除」按鈕（非頂級部門）',
      async () => {
        render(<DepartmentPage />);

        await waitFor(
          () => {
            // 至少有一個刪除按鈕（非頂級部門）
            const deleteButtons = screen.getAllByText('刪除');
            expect(deleteButtons.length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理空列表',
      async () => {
        vi.mocked(departmentApi.queryAllDepartment).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: [],
        });

        render(<DepartmentPage />);

        await waitFor(
          () => {
            // 驗證表格已渲染，但沒有數據
            const table = document.querySelector('.ant-table-tbody');
            expect(table).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
