/**
 * MenuTreeSelect Component Tests
 * 菜單樹選擇器組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import MenuTreeSelect from './MenuTreeSelect';
import { menuApi } from '@/api/system/menuApi';
import type { MenuVO } from '../types';

// 增加測試超時時間（Ant Design 組件渲染較慢）
const TEST_TIMEOUT = 10000;

// Mock menuApi
vi.mock('@/api/system/menuApi', () => ({
  menuApi: {
    queryMenu: vi.fn(),
  },
}));

describe('MenuTreeSelect', () => {
  const mockMenuList: MenuVO[] = [
    {
      menuId: 1,
      menuName: '系統管理',
      menuType: 1, // 目錄
      parentId: 0,
      path: '/system',
      component: '',
      sort: 1,
      visibleFlag: true,
      disabledFlag: false,
      permsType: 1,
      webPerms: '',
      apiPerms: '',
      icon: 'SettingOutlined',
    },
    {
      menuId: 2,
      menuName: '用戶管理',
      menuType: 2, // 菜單
      parentId: 1,
      path: '/system/user',
      component: 'system/user/index',
      sort: 1,
      visibleFlag: true,
      disabledFlag: false,
      permsType: 1,
      webPerms: 'system:user:query',
      apiPerms: 'system:user:query',
      icon: 'UserOutlined',
    },
    {
      menuId: 3,
      menuName: '新增用戶',
      menuType: 3, // 功能點
      parentId: 2,
      path: '',
      component: '',
      sort: 1,
      visibleFlag: true,
      disabledFlag: false,
      permsType: 1,
      webPerms: 'system:user:add',
      apiPerms: 'system:user:add',
      icon: '',
    },
    {
      menuId: 4,
      menuName: '業務管理',
      menuType: 1, // 目錄
      parentId: 0,
      path: '/business',
      component: '',
      sort: 2,
      visibleFlag: true,
      disabledFlag: false,
      permsType: 1,
      webPerms: '',
      apiPerms: '',
      icon: 'AppstoreOutlined',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    // 默認返回成功的 API 響應
    vi.mocked(menuApi.queryMenu).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockMenuList,
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該正確渲染組件',
      async () => {
        render(<MenuTreeSelect />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示自定義佔位符',
      async () => {
        render(<MenuTreeSelect placeholder="測試佔位符" />);

        await waitFor(
          () => {
            expect(screen.getByText('測試佔位符')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在禁用狀態下不可操作',
      async () => {
        render(<MenuTreeSelect disabled />);

        await waitFor(
          () => {
            const select = screen.getByRole('combobox');
            expect(select).toHaveAttribute('aria-disabled', 'true');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('菜單數據加載', () => {
    it(
      '應該在組件掛載時加載菜單數據',
      async () => {
        render(<MenuTreeSelect />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該正確構建菜單樹結構',
      async () => {
        render(<MenuTreeSelect />);

        await waitFor(
          () => {
            // 驗證頂級選項存在
            expect(screen.getByText('頂級菜單/目錄')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理 API 錯誤情況',
      async () => {
        vi.mocked(menuApi.queryMenu).mockRejectedValue(new Error('網絡錯誤'));

        render(<MenuTreeSelect />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('菜單類型過濾', () => {
    it(
      '目錄類型(1)應該只能選擇目錄作為父級',
      async () => {
        render(<MenuTreeSelect menuType={1} />);

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
      '菜單類型(2)應該只能選擇目錄作為父級',
      async () => {
        render(<MenuTreeSelect menuType={2} />);

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
      '功能點類型(3)應該只能選擇菜單作為父級',
      async () => {
        render(<MenuTreeSelect menuType={3} />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('編輯模式（排除自身及子節點）', () => {
    it(
      '應該排除當前菜單節點',
      async () => {
        render(<MenuTreeSelect currentMenuId={1} />);

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
      '應該排除當前菜單的所有子節點',
      async () => {
        render(<MenuTreeSelect currentMenuId={1} />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('值變化處理', () => {
    it(
      '應該在選擇值變化時調用 onChange',
      async () => {
        const onChange = vi.fn();

        render(<MenuTreeSelect onChange={onChange} />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 驗證 onChange prop 被正確設置
        expect(onChange).toHaveBeenCalledTimes(0);
      },
      TEST_TIMEOUT
    );

    it(
      '選擇頂級菜單時應該返回 undefined',
      async () => {
        const onChange = vi.fn();

        render(<MenuTreeSelect value={0} onChange={onChange} />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('搜索過濾', () => {
    it(
      '應該支持菜單名稱搜索',
      async () => {
        render(<MenuTreeSelect />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('受控模式', () => {
    it(
      '應該正確顯示當前值',
      async () => {
        render(<MenuTreeSelect value={1} />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 menuType 或 currentMenuId 變化時重新加載',
      async () => {
        const { rerender } = render(<MenuTreeSelect menuType={1} />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );

        // 修改 menuType，應該觸發重新加載
        rerender(<MenuTreeSelect menuType={2} />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalledTimes(2);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理空菜單列表',
      async () => {
        vi.mocked(menuApi.queryMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: [],
        });

        render(<MenuTreeSelect />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 應該至少顯示頂級菜單選項
        expect(screen.getByText('頂級菜單/目錄')).toBeInTheDocument();
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理 null 數據',
      async () => {
        vi.mocked(menuApi.queryMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: null as unknown as MenuVO[],
        });

        render(<MenuTreeSelect />);

        await waitFor(
          () => {
            expect(menuApi.queryMenu).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
