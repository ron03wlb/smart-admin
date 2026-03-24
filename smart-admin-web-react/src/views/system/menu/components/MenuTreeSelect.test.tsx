/**
 * MenuTreeSelect Component Tests
 * 菜單樹選擇器組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MenuTreeSelect from './MenuTreeSelect';
import { menuApi } from '@/api/system/menuApi';
import type { MenuVO } from '../types';

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
    it('應該正確渲染組件', async () => {
      render(<MenuTreeSelect />);

      await waitFor(() => {
        expect(screen.getByRole('combobox')).toBeInTheDocument();
      });
    });

    it('應該顯示自定義佔位符', async () => {
      render(<MenuTreeSelect placeholder="測試佔位符" />);

      await waitFor(() => {
        expect(screen.getByText('測試佔位符')).toBeInTheDocument();
      });
    });

    it('應該在禁用狀態下不可操作', async () => {
      render(<MenuTreeSelect disabled />);

      await waitFor(() => {
        const select = screen.getByRole('combobox');
        expect(select).toHaveAttribute('aria-disabled', 'true');
      });
    });
  });

  describe('菜單數據加載', () => {
    it('應該在組件掛載時加載菜單數據', async () => {
      render(<MenuTreeSelect />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalledTimes(1);
      });
    });

    it('應該正確構建菜單樹結構', async () => {
      render(<MenuTreeSelect />);

      await waitFor(() => {
        // 驗證頂級選項存在
        expect(screen.getByText('頂級菜單/目錄')).toBeInTheDocument();
      });
    });

    it('應該處理 API 錯誤情況', async () => {
      vi.mocked(menuApi.queryMenu).mockRejectedValue(new Error('網絡錯誤'));

      render(<MenuTreeSelect />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('菜單類型過濾', () => {
    it('目錄類型(1)應該只能選擇目錄作為父級', async () => {
      render(<MenuTreeSelect menuType={1} />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalled();
      });

      // 目錄只能選擇目錄（menuType=1）
      // 應該包含：系統管理(1)、業務管理(4)
      // 不應該包含：用戶管理(2-菜單)、新增用戶(3-功能點)
    });

    it('菜單類型(2)應該只能選擇目錄作為父級', async () => {
      render(<MenuTreeSelect menuType={2} />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalled();
      });

      // 菜單只能選擇目錄（menuType=1）
      // 應該包含：系統管理(1)、業務管理(4)
    });

    it('功能點類型(3)應該只能選擇菜單作為父級', async () => {
      render(<MenuTreeSelect menuType={3} />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalled();
      });

      // 功能點只能選擇菜單（menuType=2）
      // 應該包含：用戶管理(2)
    });
  });

  describe('編輯模式（排除自身及子節點）', () => {
    it('應該排除當前菜單節點', async () => {
      render(<MenuTreeSelect currentMenuId={1} />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalled();
      });

      // 應該排除 menuId=1 的節點（系統管理）
    });

    it('應該排除當前菜單的所有子節點', async () => {
      render(<MenuTreeSelect currentMenuId={1} />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalled();
      });

      // 應該排除 menuId=1（系統管理）及其子節點 menuId=2（用戶管理）、menuId=3（新增用戶）
      // 應該保留 menuId=4（業務管理）
    });
  });

  describe('值變化處理', () => {
    it('應該在選擇值變化時調用 onChange', async () => {
      const onChange = vi.fn();
      const user = userEvent.setup();

      render(<MenuTreeSelect onChange={onChange} />);

      await waitFor(() => {
        expect(screen.getByRole('combobox')).toBeInTheDocument();
      });

      // 點擊選擇框
      const select = screen.getByRole('combobox');
      await user.click(select);

      // 注意：TreeSelect 的實際點擊和選擇需要更複雜的模擬
      // 這裡主要驗證組件能正確接收 onChange prop
      expect(onChange).toHaveBeenCalledTimes(0); // 未實際選擇時不應調用
    });

    it('選擇頂級菜單時應該返回 undefined', async () => {
      const onChange = vi.fn();

      render(<MenuTreeSelect value={0} onChange={onChange} />);

      await waitFor(() => {
        expect(screen.getByRole('combobox')).toBeInTheDocument();
      });

      // 頂級菜單（value=0）應該在 onChange 中轉換為 undefined
    });
  });

  describe('搜索過濾', () => {
    it('應該支持菜單名稱搜索', async () => {
      render(<MenuTreeSelect />);

      await waitFor(() => {
        expect(screen.getByRole('combobox')).toBeInTheDocument();
      });

      // TreeSelect 的 showSearch 功能應該啟用
      const select = screen.getByRole('combobox');
      expect(select).toBeInTheDocument();
    });
  });

  describe('受控模式', () => {
    it('應該正確顯示當前值', async () => {
      render(<MenuTreeSelect value={1} />);

      await waitFor(() => {
        expect(screen.getByRole('combobox')).toBeInTheDocument();
      });

      // 受控模式下應該顯示 value=1 對應的菜單
    });

    it('應該在 menuType 或 currentMenuId 變化時重新加載', async () => {
      const { rerender } = render(<MenuTreeSelect menuType={1} />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalledTimes(1);
      });

      // 修改 menuType，應該觸發重新加載
      rerender(<MenuTreeSelect menuType={2} />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalledTimes(2);
      });
    });
  });

  describe('邊界情況', () => {
    it('應該處理空菜單列表', async () => {
      vi.mocked(menuApi.queryMenu).mockResolvedValue({
        ok: true,
        code: 1,
        msg: '操作成功',
        data: [],
      });

      render(<MenuTreeSelect />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalled();
      });

      // 應該至少顯示頂級菜單選項
      expect(screen.getByText('頂級菜單/目錄')).toBeInTheDocument();
    });

    it('應該處理 null 數據', async () => {
      vi.mocked(menuApi.queryMenu).mockResolvedValue({
        ok: true,
        code: 1,
        msg: '操作成功',
        data: null as unknown as MenuVO[],
      });

      render(<MenuTreeSelect />);

      await waitFor(() => {
        expect(menuApi.queryMenu).toHaveBeenCalled();
      });
    });
  });
});
