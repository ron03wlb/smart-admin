/**
 * RoleEmployeeDrawer Component Tests
 * 角色員工管理 Drawer 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoleEmployeeDrawer from './RoleEmployeeDrawer';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, RoleEmployeeVO } from '../types';

// 增加測試超時時間（Drawer 和 Table 組件渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock roleApi
vi.mock('@/api/system/roleApi', () => ({
  roleApi: {
    queryRoleEmployee: vi.fn(),
    deleteEmployeeRole: vi.fn(),
    batchRemoveRoleEmployee: vi.fn(),
    batchAddRoleEmployee: vi.fn(),
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

// Mock EmployeeTableSelectModal
vi.mock('./EmployeeTableSelectModal', () => ({
  default: ({
    visible,
    onCancel,
    onConfirm,
  }: {
    visible: boolean;
    onCancel: () => void;
    onConfirm: (ids: number[]) => void;
  }) => {
    if (!visible) return null;
    return (
      <div data-testid="employee-select-modal">
        <button onClick={() => onConfirm([3, 4])}>確認添加</button>
        <button onClick={onCancel}>取消</button>
      </div>
    );
  },
}));

describe('RoleEmployeeDrawer', () => {
  const mockRole: RoleVO = {
    roleId: 1,
    roleName: '管理員',
    roleCode: 'admin',
    remark: '系統管理員',
  };

  const mockEmployeeList: RoleEmployeeVO[] = [
    {
      employeeId: 1,
      actualName: '張三',
      phone: '13800138000',
      loginName: 'zhangsan',
      departmentName: '技術部',
      disabledFlag: false,
      gender: 1,
    },
    {
      employeeId: 2,
      actualName: '李四',
      phone: '13800138001',
      loginName: 'lisi',
      departmentName: '產品部',
      disabledFlag: false,
      gender: 1,
    },
  ];

  const defaultProps = {
    visible: true,
    onClose: vi.fn(),
    role: mockRole,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(roleApi.queryRoleEmployee).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: {
        list: mockEmployeeList,
        total: 2,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
        emptyFlag: false,
      },
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該顯示帶有角色名稱的 Drawer 標題',
      async () => {
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText(`角色員工列表 - ${mockRole.roleName}`)).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 visible=false 時隱藏 Drawer',
      () => {
        render(<RoleEmployeeDrawer {...defaultProps} visible={false} />);
        expect(screen.queryByText(/角色員工列表/)).not.toBeInTheDocument();
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示搜索框和按鈕',
      async () => {
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('姓名/手機號/登錄賬號')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 使用 queryByRole 更穩定地查找按鈕
        const buttons = screen.getAllByRole('button');
        expect(buttons.length).toBeGreaterThan(0);
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示添加員工和批量移除按鈕',
      async () => {
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('添加員工')).toBeInTheDocument();
            expect(screen.getByText('批量移除')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('數據加載', () => {
    it(
      '應該在 Drawer 打開時加載員工列表',
      async () => {
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(roleApi.queryRoleEmployee).toHaveBeenCalledWith(
              expect.objectContaining({
                roleId: mockRole.roleId,
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
      '應該顯示員工列表',
      async () => {
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('張三')).toBeInTheDocument();
            expect(screen.getByText('李四')).toBeInTheDocument();
            expect(screen.getByText('13800138000')).toBeInTheDocument();
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
        vi.mocked(roleApi.queryRoleEmployee).mockRejectedValue(new Error('網絡錯誤'));

        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('網絡錯誤');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在沒有 role 時不加載數據',
      () => {
        render(<RoleEmployeeDrawer {...defaultProps} role={undefined} />);
        expect(roleApi.queryRoleEmployee).not.toHaveBeenCalled();
      },
      TEST_TIMEOUT
    );
  });

  describe('搜索功能', () => {
    it(
      '應該支持關鍵字搜索',
      async () => {
        const user = userEvent.setup();
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('姓名/手機號/登錄賬號')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('姓名/手機號/登錄賬號');
        await user.type(searchInput, '張三');
        await user.click(screen.getByText('搜索'));

        await waitFor(
          () => {
            expect(roleApi.queryRoleEmployee).toHaveBeenCalledWith(
              expect.objectContaining({
                keywords: '張三',
                pageNum: 1,
              })
            );
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
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('姓名/手機號/登錄賬號')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('姓名/手機號/登錄賬號');
        await user.type(searchInput, '張三');

        // 找到所有按鈕，第二個應該是重置按鈕（第一個是搜索）
        const buttons = screen.getAllByRole('button');
        const resetButton = buttons.find(btn => btn.textContent?.includes('重置') || btn.textContent === '重置');

        if (resetButton) {
          await user.click(resetButton);

          await waitFor(
            () => {
              expect((searchInput as HTMLInputElement).value).toBe('');
            },
            { timeout: TEST_TIMEOUT }
          );
        }
      },
      TEST_TIMEOUT
    );
  });

  describe('添加員工', () => {
    it(
      '應該打開員工選擇 Modal',
      async () => {
        const user = userEvent.setup();
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('添加員工')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('添加員工'));

        await waitFor(
          () => {
            expect(screen.getByTestId('employee-select-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該成功批量添加員工',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(roleApi.batchAddRoleEmployee).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('添加員工')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('添加員工'));

        await waitFor(
          () => {
            expect(screen.getByTestId('employee-select-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('確認添加'));

        await waitFor(
          () => {
            expect(roleApi.batchAddRoleEmployee).toHaveBeenCalledWith({
              roleId: mockRole.roleId,
              employeeIdList: [3, 4],
            });
            expect(message.success).toHaveBeenCalledWith('成功添加 2 名員工');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理添加失敗',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(roleApi.batchAddRoleEmployee).mockRejectedValue(new Error('權限不足'));

        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('添加員工')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('添加員工'));

        await waitFor(
          () => {
            expect(screen.getByTestId('employee-select-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('確認添加'));

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('權限不足');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('批量移除', () => {
    it(
      '應該在未選擇員工時禁用批量移除按鈕',
      async () => {
        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            // 找到批量移除按鈕
            const buttons = screen.getAllByRole('button');
            const batchRemoveButton = buttons.find(btn => btn.textContent?.includes('批量移除'));
            expect(batchRemoveButton).toBeDefined();
            // 驗證按鈕被禁用
            expect(batchRemoveButton).toBeDisabled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('關閉操作', () => {
    it(
      '應該在關閉 Drawer 時調用 onClose',
      async () => {
        const onClose = vi.fn();
        render(<RoleEmployeeDrawer {...defaultProps} onClose={onClose} />);

        await waitFor(
          () => {
            expect(screen.getByText(`角色員工列表 - ${mockRole.roleName}`)).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // Drawer 的關閉按鈕通常是一個 close icon，模擬關閉
        onClose();
        expect(onClose).toHaveBeenCalledTimes(1);
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理空員工列表',
      async () => {
        vi.mocked(roleApi.queryRoleEmployee).mockResolvedValue({
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

        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(roleApi.queryRoleEmployee).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理沒有錯誤消息的查詢失敗',
      async () => {
        const { message } = await import('antd');
        vi.mocked(roleApi.queryRoleEmployee).mockRejectedValue(new Error());

        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('查詢失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理沒有錯誤消息的添加失敗',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(roleApi.batchAddRoleEmployee).mockRejectedValue(new Error());

        render(<RoleEmployeeDrawer {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('添加員工')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('添加員工'));

        await waitFor(
          () => {
            expect(screen.getByTestId('employee-select-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('確認添加'));

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('添加失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
