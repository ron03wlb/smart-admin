/**
 * Role Management Page Integration Tests
 * 角色管理頁面集成測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RolePage from './index';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO } from './types';

// 增加測試超時時間（頁面集成測試較慢）
const TEST_TIMEOUT = 15000;

// Mock roleApi
vi.mock('@/api/system/roleApi', () => ({
  roleApi: {
    queryAll: vi.fn(),
    deleteRole: vi.fn(),
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

// Mock all child components to simplify integration testing
vi.mock('./components/RoleFormModal', () => ({
  default: ({ visible }: { visible: boolean }) => (visible ? <div data-testid="role-form-modal">RoleFormModal</div> : null),
}));

vi.mock('./components/RoleMenuModal', () => ({
  default: ({ visible }: { visible: boolean }) => (visible ? <div data-testid="role-menu-modal">RoleMenuModal</div> : null),
}));

vi.mock('./components/RoleEmployeeDrawer', () => ({
  default: ({ visible }: { visible: boolean }) => (visible ? <div data-testid="role-employee-drawer">RoleEmployeeDrawer</div> : null),
}));

vi.mock('./components/RoleDataScopeModal', () => ({
  default: ({ visible }: { visible: boolean }) => (visible ? <div data-testid="role-datascope-modal">RoleDataScopeModal</div> : null),
}));

// Mock PrivilegeButton to render children directly
vi.mock('@/components/PrivilegeButton', () => ({
  default: ({ children, onClick }: { children: React.ReactNode; onClick?: () => void }) => (
    <button onClick={onClick}>{children}</button>
  ),
}));

describe('RolePage Integration Test', () => {
  const mockRoleList: RoleVO[] = [
    {
      roleId: 1,
      roleName: '管理員',
      roleCode: 'admin',
      remark: '系統管理員',
      createTime: '2026-03-01 10:00:00',
      updateTime: '2026-03-01 10:00:00',
    },
    {
      roleId: 2,
      roleName: '普通用戶',
      roleCode: 'user',
      remark: '普通用戶角色',
      createTime: '2026-03-01 10:00:00',
      updateTime: '2026-03-01 10:00:00',
    },
    {
      roleId: 3,
      roleName: '訪客',
      roleCode: 'guest',
      remark: '訪客角色',
      createTime: '2026-03-01 10:00:00',
      updateTime: '2026-03-01 10:00:00',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(roleApi.queryAll).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockRoleList,
    });
  });

  describe('頁面初始化', () => {
    it(
      '應該在頁面加載時查詢角色列表',
      async () => {
        render(<RolePage />);

        await waitFor(
          () => {
            expect(roleApi.queryAll).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示角色列表表格',
      async () => {
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getByText('管理員')).toBeInTheDocument();
            expect(screen.getByText('普通用戶')).toBeInTheDocument();
            expect(screen.getByText('訪客')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示表格',
      async () => {
        render(<RolePage />);

        // 驗證表格渲染（通過角色數據驗證）
        await waitFor(
          () => {
            expect(screen.getByText('管理員')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 驗證至少有一個表格列標題
        const columnHeaders = screen.getAllByRole('columnheader');
        expect(columnHeaders.length).toBeGreaterThan(0);
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理 API 錯誤',
      async () => {
        const { message } = await import('antd');
        vi.mocked(roleApi.queryAll).mockRejectedValue(new Error('網絡錯誤'));

        render(<RolePage />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('查詢角色列表失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('搜尋功能', () => {
    it(
      '應該支持關鍵字搜尋',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('角色名稱/角色編碼/備註')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('角色名稱/角色編碼/備註');
        await user.type(searchInput, '管理員');
        await user.click(screen.getByText('搜尋'));

        await waitFor(
          () => {
            expect(screen.getByText('管理員')).toBeInTheDocument();
            expect(screen.queryByText('普通用戶')).not.toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該支持重置搜尋',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('角色名稱/角色編碼/備註')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('角色名稱/角色編碼/備註');
        await user.type(searchInput, '管理員');
        await user.click(screen.getByText('搜尋'));

        await waitFor(
          () => {
            expect(screen.queryByText('普通用戶')).not.toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 找到重置按鈕
        const buttons = screen.getAllByRole('button');
        const resetButton = buttons.find(btn => btn.textContent === '重置');

        if (resetButton) {
          await user.click(resetButton);

          await waitFor(
            () => {
              expect(screen.getByText('普通用戶')).toBeInTheDocument();
            },
            { timeout: TEST_TIMEOUT }
          );
        }
      },
      TEST_TIMEOUT
    );
  });

  describe('操作按鈕', () => {
    it(
      '應該顯示新增角色按鈕',
      async () => {
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getByText('新增角色')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該點擊新增按鈕打開表單 Modal',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getByText('新增角色')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('新增角色'));

        await waitFor(
          () => {
            expect(screen.getByTestId('role-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('表格操作', () => {
    it(
      '應該顯示所有操作按鈕',
      async () => {
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getAllByText('菜單權限').length).toBeGreaterThan(0);
            expect(screen.getAllByText('員工管理').length).toBeGreaterThan(0);
            expect(screen.getAllByText('數據範圍').length).toBeGreaterThan(0);
            expect(screen.getAllByText('編輯').length).toBeGreaterThan(0);
            expect(screen.getAllByText('刪除').length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該點擊菜單權限按鈕打開 Modal',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getAllByText('菜單權限').length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );

        const menuButtons = screen.getAllByText('菜單權限');
        await user.click(menuButtons[0]);

        await waitFor(
          () => {
            expect(screen.getByTestId('role-menu-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該點擊員工管理按鈕打開 Drawer',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getAllByText('員工管理').length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );

        const employeeButtons = screen.getAllByText('員工管理');
        await user.click(employeeButtons[0]);

        await waitFor(
          () => {
            expect(screen.getByTestId('role-employee-drawer')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該點擊數據範圍按鈕打開 Modal',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getAllByText('數據範圍').length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );

        const dataScopeButtons = screen.getAllByText('數據範圍');
        await user.click(dataScopeButtons[0]);

        await waitFor(
          () => {
            expect(screen.getByTestId('role-datascope-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該點擊編輯按鈕打開表單 Modal',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

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
            expect(screen.getByTestId('role-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理空角色列表',
      async () => {
        vi.mocked(roleApi.queryAll).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: [],
        });

        render(<RolePage />);

        await waitFor(
          () => {
            expect(roleApi.queryAll).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理搜尋無結果',
      async () => {
        const user = userEvent.setup();
        render(<RolePage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('角色名稱/角色編碼/備註')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('角色名稱/角色編碼/備註');
        await user.type(searchInput, '不存在的角色');
        await user.click(screen.getByText('搜尋'));

        await waitFor(
          () => {
            expect(screen.queryByText('管理員')).not.toBeInTheDocument();
            expect(screen.queryByText('普通用戶')).not.toBeInTheDocument();
            expect(screen.queryByText('訪客')).not.toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
