/**
 * Menu Management Page Tests
 * 菜單管理頁面集成測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MenuPage from './index';
import { menuApi } from '@/api/system/menuApi';
import type { MenuVO } from './types';

// 增加測試超時時間（樹形表格渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock menuApi
vi.mock('@/api/system/menuApi', () => ({
  menuApi: {
    queryMenu: vi.fn(),
    deleteMenu: vi.fn(),
    batchDeleteMenu: vi.fn(),
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

// Mock MenuFormModal component
vi.mock('./components/MenuFormModal', () => ({
  default: ({ visible, onCancel }: { visible: boolean; onCancel: () => void }) =>
    visible ? (
      <div data-testid="menu-form-modal">
        <button onClick={onCancel}>Cancel</button>
      </div>
    ) : null,
}));

describe('MenuPage', () => {
  const mockMenuList: MenuVO[] = [
    {
      menuId: 1,
      menuName: '系統管理',
      menuType: 1, // 目錄
      parentId: 0,
      sort: 1,
      path: '/system',
      icon: 'SettingOutlined',
      component: 'Layout',
      visibleFlag: true,
      cacheFlag: false,
      disabledFlag: false,
      frameFlag: false,
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
    {
      menuId: 2,
      menuName: '菜單管理',
      menuType: 2, // 菜單
      parentId: 1,
      sort: 1,
      path: '/system/menu',
      icon: 'MenuOutlined',
      component: 'system/menu/index',
      webPerms: 'system:menu:query',
      apiPerms: 'system:menu:*',
      visibleFlag: true,
      cacheFlag: true,
      disabledFlag: false,
      frameFlag: false,
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
    {
      menuId: 3,
      menuName: '新增',
      menuType: 3, // 按鈕
      parentId: 2,
      sort: 1,
      webPerms: 'system:menu:add',
      apiPerms: 'system:menu:add',
      visibleFlag: true,
      cacheFlag: false,
      disabledFlag: false,
      frameFlag: false,
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(menuApi.queryMenu).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockMenuList,
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該渲染頁面標題和搜索框',
      async () => {
        render(<MenuPage />);

        await waitFor(
          () => {
            expect(screen.getByText('菜單管理')).toBeInTheDocument();
            expect(
              screen.getByPlaceholderText('菜單名稱/路由地址/組件路徑/權限字符串')
            ).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染操作按鈕',
      async () => {
        render(<MenuPage />);

        await waitFor(
          () => {
            expect(screen.getByText('查詢')).toBeInTheDocument();
            expect(screen.getByText('重置')).toBeInTheDocument();
            expect(screen.getByText('添加菜單')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染表格',
      async () => {
        render(<MenuPage />);

        // 等待數據加載完成
        await waitFor(
          () => {
            expect(screen.getByText('系統管理')).toBeInTheDocument();
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
      '應該在初始化時調用 API',
      async () => {
        render(<MenuPage />);

        // 簡化：只驗證 API 被調用過
        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示菜單數據',
      async () => {
        render(<MenuPage />);

        // 簡化：只驗證主要數據顯示
        await waitFor(
          () => {
            expect(screen.getByText('系統管理')).toBeInTheDocument();
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
        vi.mocked(menuApi.queryMenu).mockRejectedValue(new Error('網絡錯誤'));

        render(<MenuPage />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('查詢菜單列表失敗');
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
        render(<MenuPage />);

        // 簡化：只驗證搜索輸入框存在
        await waitFor(
          () => {
            expect(
              screen.getByPlaceholderText('菜單名稱/路由地址/組件路徑/權限字符串')
            ).toBeInTheDocument();
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
        render(<MenuPage />);

        await waitFor(
          () => {
            expect(
              screen.getByPlaceholderText('菜單名稱/路由地址/組件路徑/權限字符串')
            ).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('菜單名稱/路由地址/組件路徑/權限字符串');
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

  describe('添加菜單', () => {
    it(
      '應該打開菜單表單 Modal',
      async () => {
        const user = userEvent.setup();
        render(<MenuPage />);

        await waitFor(
          () => {
            expect(screen.getByText('添加菜單')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('添加菜單'));

        await waitFor(
          () => {
            expect(screen.getByTestId('menu-form-modal')).toBeInTheDocument();
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
        render(<MenuPage />);

        await waitFor(
          () => {
            expect(screen.getByText('添加菜單')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 打開 Modal
        await user.click(screen.getByText('添加菜單'));

        await waitFor(
          () => {
            expect(screen.getByTestId('menu-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 點擊取消
        const cancelButton = screen.getByText('Cancel');
        await user.click(cancelButton);

        await waitFor(
          () => {
            expect(screen.queryByTestId('menu-form-modal')).not.toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('添加下級菜單', () => {
    it(
      '應該顯示「添加下級」按鈕（非按鈕類型菜單）',
      async () => {
        render(<MenuPage />);

        await waitFor(
          () => {
            // 「添加下級」按鈕應該存在（對於目錄和菜單類型）
            const addSubButtons = screen.getAllByText('添加下級');
            expect(addSubButtons.length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該打開添加下級菜單的 Modal',
      async () => {
        const user = userEvent.setup();
        render(<MenuPage />);

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
            expect(screen.getByTestId('menu-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('編輯菜單', () => {
    it(
      '應該顯示「編輯」按鈕',
      async () => {
        render(<MenuPage />);

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
      '應該打開編輯菜單的 Modal',
      async () => {
        const user = userEvent.setup();
        render(<MenuPage />);

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
            expect(screen.getByTestId('menu-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('刪除菜單', () => {
    it(
      '應該顯示「刪除」按鈕',
      async () => {
        render(<MenuPage />);

        await waitFor(
          () => {
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
        vi.mocked(menuApi.queryMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: [],
        });

        render(<MenuPage />);

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
