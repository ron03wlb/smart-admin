/**
 * RoleMenuModal Component Tests
 * 角色菜單權限 Modal 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoleMenuModal from './RoleMenuModal';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, RoleMenuSelectedVO } from '../types';
import type { MenuVO } from '@/views/system/menu/types';
import { MenuTypeEnum } from '@/views/system/menu/types';

// 增加測試超時時間（Modal 和 Tree 組件渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock roleApi
vi.mock('@/api/system/roleApi', () => ({
  roleApi: {
    getRoleSelectedMenu: vi.fn(),
    updateRoleMenu: vi.fn(),
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

describe('RoleMenuModal', () => {
  const mockRole: RoleVO = {
    roleId: 1,
    roleName: '管理員',
    roleCode: 'admin',
    remark: '系統管理員',
  };

  const mockMenuTree: MenuVO[] = [
    {
      menuId: 1,
      menuName: '系統管理',
      menuType: MenuTypeEnum.CATALOG,
      parentId: 0,
      sort: 1,
      children: [
        {
          menuId: 11,
          menuName: '用戶管理',
          menuType: MenuTypeEnum.MENU,
          parentId: 1,
          path: '/system/user',
          sort: 1,
        },
        {
          menuId: 12,
          menuName: '角色管理',
          menuType: MenuTypeEnum.MENU,
          parentId: 1,
          path: '/system/role',
          sort: 2,
        },
      ],
    },
    {
      menuId: 2,
      menuName: '業務管理',
      menuType: MenuTypeEnum.CATALOG,
      parentId: 0,
      sort: 2,
      children: [
        {
          menuId: 21,
          menuName: '訂單管理',
          menuType: MenuTypeEnum.MENU,
          parentId: 2,
          path: '/business/order',
          sort: 1,
        },
      ],
    },
  ];

  const mockRoleMenuData: RoleMenuSelectedVO = {
    menuTreeList: mockMenuTree,
    selectedMenuId: [11, 12],
  };

  const defaultProps = {
    visible: true,
    onCancel: vi.fn(),
    onSuccess: vi.fn(),
    role: mockRole,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(roleApi.getRoleSelectedMenu).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockRoleMenuData,
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該顯示帶有角色名稱的 Modal 標題',
      async () => {
        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText(`設置角色功能權限 - ${mockRole.roleName}`)).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 visible=false 時隱藏 Modal',
      () => {
        render(<RoleMenuModal {...defaultProps} visible={false} />);
        expect(screen.queryByText(/設置角色功能權限/)).not.toBeInTheDocument();
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示提示信息',
      async () => {
        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('提示')).toBeInTheDocument();
            expect(
              screen.getByText('勾選菜單項後，該角色將擁有對應的功能操作權限')
            ).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示 OK 和 Cancel 按鈕',
      async () => {
        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('OK')).toBeInTheDocument();
            expect(screen.getByText('Cancel')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('數據加載', () => {
    it(
      '應該在 Modal 打開時加載菜單數據',
      async () => {
        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(roleApi.getRoleSelectedMenu).toHaveBeenCalledWith(mockRole.roleId);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示菜單樹結構',
      async () => {
        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('系統管理')).toBeInTheDocument();
            expect(screen.getByText('用戶管理')).toBeInTheDocument();
            expect(screen.getByText('角色管理')).toBeInTheDocument();
            expect(screen.getByText('業務管理')).toBeInTheDocument();
            expect(screen.getByText('訂單管理')).toBeInTheDocument();
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
        vi.mocked(roleApi.getRoleSelectedMenu).mockRejectedValue(new Error('網絡錯誤'));

        render(<RoleMenuModal {...defaultProps} />);

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
        render(<RoleMenuModal {...defaultProps} role={undefined} />);
        expect(roleApi.getRoleSelectedMenu).not.toHaveBeenCalled();
      },
      TEST_TIMEOUT
    );
  });

  describe('樹形選擇', () => {
    it(
      '應該初始化選中的菜單節點',
      async () => {
        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            // 驗證 API 被調用並返回了選中的菜單
            expect(roleApi.getRoleSelectedMenu).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );

        // Tree 組件應該渲染
        await waitFor(
          () => {
            expect(screen.getByText('用戶管理')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('提交操作', () => {
    it(
      '應該在未選擇菜單時顯示警告',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        // Mock 返回空的選中菜單
        vi.mocked(roleApi.getRoleSelectedMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: {
            menuTreeList: mockMenuTree,
            selectedMenuId: [],
          },
        });

        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('OK')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(message.warning).toHaveBeenCalledWith('請至少選擇一個菜單權限');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該成功保存菜單權限',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();
        const onSuccess = vi.fn();

        vi.mocked(roleApi.updateRoleMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(<RoleMenuModal {...defaultProps} onSuccess={onSuccess} />);

        await waitFor(
          () => {
            expect(screen.getByText('OK')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 點擊保存
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(roleApi.updateRoleMenu).toHaveBeenCalledWith(
              expect.objectContaining({
                roleId: mockRole.roleId,
                menuIdList: expect.any(Array),
              })
            );
            expect(message.success).toHaveBeenCalledWith('保存成功');
            expect(onSuccess).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理保存失敗',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(roleApi.updateRoleMenu).mockRejectedValue(new Error('權限不足'));

        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('OK')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('OK'));

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

  describe('取消操作', () => {
    it(
      '應該在點擊 Cancel 時調用 onCancel',
      async () => {
        const onCancel = vi.fn();
        const user = userEvent.setup();

        render(<RoleMenuModal {...defaultProps} onCancel={onCancel} />);

        await waitFor(
          () => {
            expect(screen.getByText('Cancel')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('Cancel'));

        expect(onCancel).toHaveBeenCalledTimes(1);
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理空菜單數據',
      async () => {
        vi.mocked(roleApi.getRoleSelectedMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: {
            menuTreeList: [],
            selectedMenuId: [],
          },
        });

        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('暫無菜單數據')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理沒有子菜單的情況',
      async () => {
        const flatMenuTree: MenuVO[] = [
          {
            menuId: 1,
            menuName: '首頁',
            menuType: MenuTypeEnum.MENU,
            parentId: 0,
            path: '/home',
            sort: 1,
          },
        ];

        vi.mocked(roleApi.getRoleSelectedMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: {
            menuTreeList: flatMenuTree,
            selectedMenuId: [],
          },
        });

        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('首頁')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理沒有錯誤消息的加載失敗',
      async () => {
        const { message } = await import('antd');
        vi.mocked(roleApi.getRoleSelectedMenu).mockRejectedValue(new Error());

        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('加載菜單權限失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理沒有錯誤消息的保存失敗',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(roleApi.updateRoleMenu).mockRejectedValue(new Error());

        render(<RoleMenuModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('OK')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('保存失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
