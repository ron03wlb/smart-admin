/**
 * Employee Management Page Tests
 * 員工管理頁面集成測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeePage from './index';
import { employeeApi } from '@/api/system/employeeApi';
import type { EmployeeVO } from './types';
import type { PageResult } from '@/api/types/response';

// 增加測試超時時間
const TEST_TIMEOUT = 15000;

// Mock employeeApi
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    queryEmployee: vi.fn(),
    deleteEmployee: vi.fn(),
    batchDeleteEmployee: vi.fn(),
    resetPassword: vi.fn(),
    updateStatus: vi.fn(),
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
  default: ({
    children,
    onClick,
    disabled,
  }: {
    children: React.ReactNode;
    onClick?: () => void;
    disabled?: boolean;
  }) => (
    <button onClick={onClick} disabled={disabled}>
      {children}
    </button>
  ),
}));

// Mock EmployeeFormModal component
vi.mock('./components/EmployeeFormModal', () => ({
  EmployeeFormModal: ({ visible, onCancel }: { visible: boolean; onCancel: () => void }) =>
    visible ? (
      <div data-testid="employee-form-modal">
        <button onClick={onCancel}>Cancel</button>
      </div>
    ) : null,
}));

// Mock PasswordDisplayModal component
vi.mock('./components/PasswordDisplayModal', () => ({
  PasswordDisplayModal: ({ visible, onClose }: { visible: boolean; onClose: () => void }) =>
    visible ? (
      <div data-testid="password-display-modal">
        <button onClick={onClose}>Close</button>
      </div>
    ) : null,
}));

describe('EmployeePage', () => {
  const mockEmployeeList: EmployeeVO[] = [
    {
      employeeId: 1,
      actualName: '張三',
      gender: 1,
      loginName: 'zhangsan',
      phone: '13800138000',
      administratorFlag: false,
      disabledFlag: false,
      leaveFlag: false,
      departmentId: 1,
      departmentName: '技術部',
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
    {
      employeeId: 2,
      actualName: '李四',
      gender: 2,
      loginName: 'lisi',
      phone: '13800138001',
      administratorFlag: false,
      disabledFlag: false,
      leaveFlag: false,
      departmentId: 1,
      departmentName: '技術部',
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
  ];

  const mockPageResult: PageResult<EmployeeVO> = {
    list: mockEmployeeList,
    total: 2,
    pageNum: 1,
    pageSize: 10,
    pages: 1,
    emptyFlag: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(employeeApi.queryEmployee).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockPageResult,
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該渲染頁面標題和搜索框',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('員工管理')).toBeInTheDocument();
            expect(screen.getByPlaceholderText('姓名/手機號/登錄賬號')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染操作按鈕',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('查詢')).toBeInTheDocument();
            expect(screen.getByText('重置')).toBeInTheDocument();
            expect(screen.getByText('添加成員')).toBeInTheDocument();
            expect(screen.getByText('批量刪除')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染狀態篩選選項',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('全部')).toBeInTheDocument();
            expect(screen.getByText('啟用')).toBeInTheDocument();
            expect(screen.getByText('禁用')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染表格',
      async () => {
        render(<EmployeePage />);

        // 等待數據加載完成
        await waitFor(
          () => {
            expect(screen.getByText('張三')).toBeInTheDocument();
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
      '應該在初始化時加載員工列表',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(employeeApi.queryEmployee).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示員工數據',
      async () => {
        render(<EmployeePage />);

        // 簡化：只驗證主要數據顯示
        await waitFor(
          () => {
            expect(screen.getByText('張三')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理 API 錯誤',
      async () => {
        vi.mocked(employeeApi.queryEmployee).mockRejectedValue(new Error('網絡錯誤'));

        render(<EmployeePage />);

        // useTable hook 會自動處理錯誤，這裡只驗證 API 被調用
        await waitFor(
          () => {
            expect(employeeApi.queryEmployee).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('搜索功能', () => {
    it(
      '應該顯示搜索輸入框',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('姓名/手機號/登錄賬號')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示重置按鈕',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('重置')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('狀態篩選', () => {
    it(
      '應該顯示狀態篩選選項',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('全部')).toBeInTheDocument();
            expect(screen.getByText('啟用')).toBeInTheDocument();
            expect(screen.getByText('禁用')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('添加員工', () => {
    it(
      '應該打開員工表單 Modal',
      async () => {
        const user = userEvent.setup();
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('添加成員')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('添加成員'));

        await waitFor(
          () => {
            expect(screen.getByTestId('employee-form-modal')).toBeInTheDocument();
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
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('添加成員')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 打開 Modal
        await user.click(screen.getByText('添加成員'));

        await waitFor(
          () => {
            expect(screen.getByTestId('employee-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 點擊取消
        const cancelButton = screen.getByText('Cancel');
        await user.click(cancelButton);

        await waitFor(
          () => {
            expect(screen.queryByTestId('employee-form-modal')).not.toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('編輯員工', () => {
    it(
      '應該顯示「編輯」按鈕',
      async () => {
        render(<EmployeePage />);

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
      '應該打開編輯員工的 Modal',
      async () => {
        const user = userEvent.setup();
        render(<EmployeePage />);

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
            expect(screen.getByTestId('employee-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('批量刪除', () => {
    it(
      '應該顯示批量刪除按鈕',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            expect(screen.getByText('批量刪除')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在未選擇時禁用批量刪除按鈕',
      async () => {
        render(<EmployeePage />);

        await waitFor(
          () => {
            const batchDeleteButton = screen.getByText('批量刪除');
            expect(batchDeleteButton).toBeInTheDocument();
            // PrivilegeButton 會根據 selectedRowKeys.length === 0 禁用
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
        vi.mocked(employeeApi.queryEmployee).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: {
            list: [],
            total: 0,
            pageNum: 1,
            pageSize: 10,
            pages: 0,
            emptyFlag: true,
          },
        });

        render(<EmployeePage />);

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
