/**
 * EmployeeTableSelectModal Component Tests (Optimized)
 * 員工選擇表格 Modal 組件測試（優化版）
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeTableSelectModal from './EmployeeTableSelectModal';
import { employeeApi } from '@/api/system/employeeApi';
import type { EmployeeVO } from '@/views/system/employee/types';

// 增加測試超時時間（Modal 和 Table 組件渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock employeeApi
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    queryEmployee: vi.fn(),
  },
}));

// Mock antd message
vi.mock('antd', async () => {
  const actual = (await vi.importActual('antd')) as Record<string, unknown>;
  return {
    ...actual,
    message: {
      warning: vi.fn(),
      error: vi.fn(),
      success: vi.fn(),
    },
  };
});

describe('EmployeeTableSelectModal', () => {
  const mockEmployeeList: EmployeeVO[] = [
    {
      employeeId: 1,
      loginName: 'admin',
      actualName: '管理員',
      phone: '13800138000',
      departmentId: 1,
      departmentName: '技術部',
      positionId: 1,
      positionName: '工程師',
      disabledFlag: false,
      administratorFlag: true,
      remark: '',
    },
    {
      employeeId: 2,
      loginName: 'user1',
      actualName: '張三',
      phone: '13800138001',
      departmentId: 1,
      departmentName: '技術部',
      positionId: 2,
      positionName: '高級工程師',
      disabledFlag: false,
      administratorFlag: false,
      remark: '',
    },
    {
      employeeId: 3,
      loginName: 'user2',
      actualName: '李四',
      phone: '13800138002',
      departmentId: 2,
      departmentName: '產品部',
      positionId: 3,
      positionName: '產品經理',
      disabledFlag: true,
      administratorFlag: false,
      remark: '',
    },
  ];

  const defaultProps = {
    visible: true,
    onCancel: vi.fn(),
    onConfirm: vi.fn(),
    excludeEmployeeIds: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(employeeApi.queryEmployee).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: {
        list: mockEmployeeList,
        total: 3,
        pageNum: 1,
        pageSize: 10,
      },
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該在 visible=true 時顯示 Modal',
      async () => {
        render(<EmployeeTableSelectModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('選擇員工')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 visible=false 時隱藏 Modal',
      () => {
        render(<EmployeeTableSelectModal {...defaultProps} visible={false} />);
        expect(screen.queryByText('選擇員工')).not.toBeInTheDocument();
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示搜索框',
      async () => {
        render(<EmployeeTableSelectModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示確認和取消按鈕',
      async () => {
        render(<EmployeeTableSelectModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('確認添加')).toBeInTheDocument();
            expect(screen.getByText('取消')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('數據加載', () => {
    it(
      '應該在 Modal 打開時加載員工列表',
      async () => {
        render(<EmployeeTableSelectModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(employeeApi.queryEmployee).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該正確傳遞查詢參數',
      async () => {
        render(<EmployeeTableSelectModal {...defaultProps} departmentId={1} />);

        await waitFor(
          () => {
            expect(employeeApi.queryEmployee).toHaveBeenCalledWith(
              expect.objectContaining({
                departmentId: 1,
                pageNum: 1,
                pageSize: 10,
              })
            );
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該過濾掉已排除的員工',
      async () => {
        render(<EmployeeTableSelectModal {...defaultProps} excludeEmployeeIds={[1, 2]} />);

        await waitFor(
          () => {
            expect(screen.getByText('李四')).toBeInTheDocument();
            expect(screen.queryByText('管理員')).not.toBeInTheDocument();
            expect(screen.queryByText('張三')).not.toBeInTheDocument();
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

        render(<EmployeeTableSelectModal {...defaultProps} />);

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
      '應該支持關鍵字搜索',
      async () => {
        const user = userEvent.setup();
        render(<EmployeeTableSelectModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索');
        await user.type(searchInput, '張三');
        await user.click(screen.getByText('搜索'));

        await waitFor(
          () => {
            expect(employeeApi.queryEmployee).toHaveBeenCalledWith(
              expect.objectContaining({
                keyword: '張三',
                pageNum: 1,
              })
            );
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('確認操作', () => {
    it(
      '應該在未選擇員工時顯示警告',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        render(<EmployeeTableSelectModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('確認添加')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('確認添加'));

        expect(message.warning).toHaveBeenCalledWith('請至少選擇一名員工');
      },
      TEST_TIMEOUT
    );
  });

  describe('取消操作', () => {
    it(
      '應該在點擊取消時調用 onCancel',
      async () => {
        const onCancel = vi.fn();
        const user = userEvent.setup();

        render(<EmployeeTableSelectModal {...defaultProps} onCancel={onCancel} />);

        await waitFor(
          () => {
            expect(screen.getByText('取消')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('取消'));

        expect(onCancel).toHaveBeenCalledTimes(1);
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理空員工列表',
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
          },
        });

        render(<EmployeeTableSelectModal {...defaultProps} />);

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
      '應該處理所有員工都被排除的情況',
      async () => {
        render(<EmployeeTableSelectModal {...defaultProps} excludeEmployeeIds={[1, 2, 3]} />);

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
});
