/**
 * MenuFormModal Component Tests
 * 菜單表單 Modal 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MenuFormModal from './MenuFormModal';
import { menuApi } from '@/api/system/menuApi';
import type { MenuFormData } from '../types';
import { MenuTypeEnum, PermsTypeEnum } from '../types';
import { MENU_VALIDATION } from '@/constants/system/menuConst';

// 增加測試超時時間（Drawer 和 Form 組件渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock menuApi
vi.mock('@/api/system/menuApi', () => ({
  menuApi: {
    addMenu: vi.fn(),
    updateMenu: vi.fn(),
    queryMenuList: vi.fn(),
    queryMenu: vi.fn().mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: [],
    }),
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

// Mock useModal hook
vi.mock('@/hooks/useModal', () => ({
  useModal: ({ defaultFormData }: { defaultFormData?: MenuFormData }) => ({
    isEdit: !!defaultFormData,
  }),
}));

describe('MenuFormModal', () => {
  const mockCatalogData: MenuFormData = {
    menuId: 1,
    menuType: MenuTypeEnum.CATALOG,
    menuName: '系統管理',
    parentId: 0,
    icon: 'SettingOutlined',
    sort: 1,
    visibleFlag: true,
    disabledFlag: false,
    cacheFlag: false,
    frameFlag: false,
  };

  const mockMenuData: MenuFormData = {
    menuId: 2,
    menuType: MenuTypeEnum.MENU,
    menuName: '菜單管理',
    parentId: 1,
    path: '/system/menu',
    component: '/system/menu/index',
    icon: 'MenuOutlined',
    sort: 1,
    visibleFlag: true,
    disabledFlag: false,
    cacheFlag: true,
    frameFlag: false,
  };

  const mockPointsData: MenuFormData = {
    menuId: 3,
    menuType: MenuTypeEnum.POINTS,
    menuName: '添加菜單',
    parentId: 2,
    contextMenuId: 2,
    permsType: PermsTypeEnum.SA_TOKEN,
    webPerms: 'system:menu:add',
    apiPerms: 'system:menu:add,system:menu:update',
    sort: 1,
    disabledFlag: false,
  };

  const defaultProps = {
    visible: true,
    onCancel: vi.fn(),
    onSuccess: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('基礎渲染', () => {
    it(
      '應該在新增模式下顯示 Drawer',
      async () => {
        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('添加菜單')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在編輯模式下顯示 Drawer',
      async () => {
        render(<MenuFormModal {...defaultProps} initialData={mockCatalogData} />);

        await waitFor(
          () => {
            expect(screen.getByText('編輯菜單')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 visible=false 時隱藏 Drawer',
      () => {
        render(<MenuFormModal {...defaultProps} visible={false} />);
        expect(screen.queryByText('添加菜單')).not.toBeInTheDocument();
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示菜單類型選擇',
      async () => {
        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('目錄')).toBeInTheDocument();
            expect(screen.getByText('菜單')).toBeInTheDocument();
            expect(screen.getByText('功能點')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在新增模式下顯示「提交並添加下一個」按鈕',
      async () => {
        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('提交並添加下一個')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在編輯模式下隱藏「提交並添加下一個」按鈕',
      async () => {
        render(<MenuFormModal {...defaultProps} initialData={mockCatalogData} />);

        await waitFor(
          () => {
            expect(screen.getByText('編輯菜單')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        expect(screen.queryByText('提交並添加下一個')).not.toBeInTheDocument();
      },
      TEST_TIMEOUT
    );
  });

  describe('菜單類型', () => {
    it(
      '應該顯示 3 種菜單類型選項',
      async () => {
        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('目錄')).toBeInTheDocument();
            expect(screen.getByText('菜單')).toBeInTheDocument();
            expect(screen.getByText('功能點')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('表單驗證', () => {
    it(
      '應該在菜單名稱為空時顯示錯誤',
      async () => {
        const user = userEvent.setup();
        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('提交')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('提交'));

        await waitFor(
          () => {
            expect(screen.getByText('菜單名稱不能為空')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該驗證菜單名稱最大長度',
      async () => {
        const user = userEvent.setup();
        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入菜單名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const nameInput = screen.getByPlaceholderText('請輸入菜單名稱');
        const longName = 'a'.repeat(MENU_VALIDATION.NAME_MAX_LENGTH + 1);

        fireEvent.change(nameInput, { target: { value: longName } });
        await user.click(screen.getByText('提交'));

        await waitFor(
          () => {
            expect(
              screen.getByText(`菜單名稱不能大於${MENU_VALIDATION.NAME_MAX_LENGTH}個字符`)
            ).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在菜單類型為「菜單」且路由地址為空時顯示錯誤',
      async () => {
        const user = userEvent.setup();
        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('菜單')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('菜單'));

        await waitFor(
          () => {
            expect(screen.getByLabelText('路由地址')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('提交'));

        await waitFor(
          () => {
            expect(screen.getByText('路由地址不能為空')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

  });

  describe('新增菜單', () => {
    it(
      '應該成功新增目錄',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();
        const onSuccess = vi.fn();

        vi.mocked(menuApi.addMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(<MenuFormModal {...defaultProps} onSuccess={onSuccess} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入菜單名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.type(screen.getByPlaceholderText('請輸入菜單名稱'), '系統管理');
        await user.click(screen.getByText('提交'));

        await waitFor(
          () => {
            expect(menuApi.addMenu).toHaveBeenCalled();
            expect(message.success).toHaveBeenCalledWith('添加成功');
            expect(onSuccess).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理新增失敗',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(menuApi.addMenu).mockRejectedValue(new Error('菜單名稱已存在'));

        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入菜單名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.type(screen.getByPlaceholderText('請輸入菜單名稱'), '系統管理');
        await user.click(screen.getByText('提交'));

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

  describe('編輯菜單', () => {
    it(
      '應該在編輯模式下初始化目錄數據',
      async () => {
        render(<MenuFormModal {...defaultProps} initialData={mockCatalogData} />);

        await waitFor(
          () => {
            const nameInput = screen.getByPlaceholderText(
              '請輸入菜單名稱'
            ) as HTMLInputElement;
            expect(nameInput.value).toBe('系統管理');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在編輯模式下初始化菜單數據',
      async () => {
        render(<MenuFormModal {...defaultProps} initialData={mockMenuData} />);

        await waitFor(
          () => {
            const nameInput = screen.getByPlaceholderText(
              '請輸入菜單名稱'
            ) as HTMLInputElement;
            const pathInput = screen.getByPlaceholderText(
              '請輸入路由地址'
            ) as HTMLInputElement;
            expect(nameInput.value).toBe('菜單管理');
            expect(pathInput.value).toBe('/system/menu');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該成功更新菜單',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();
        const onSuccess = vi.fn();

        vi.mocked(menuApi.updateMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(
          <MenuFormModal {...defaultProps} initialData={mockCatalogData} onSuccess={onSuccess} />
        );

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入菜單名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const nameInput = screen.getByPlaceholderText('請輸入菜單名稱');
        await user.clear(nameInput);
        await user.type(nameInput, '系統設置');

        await user.click(screen.getByText('提交'));

        await waitFor(
          () => {
            expect(menuApi.updateMenu).toHaveBeenCalled();
            expect(message.success).toHaveBeenCalledWith('更新成功');
            expect(onSuccess).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('連續添加功能', () => {
    it(
      '應該在點擊「提交並添加下一個」後重置表單但保留菜單類型',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(menuApi.addMenu).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(<MenuFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入菜單名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 選擇菜單類型並填寫表單
        await user.click(screen.getByText('菜單'));

        await waitFor(
          () => {
            expect(screen.getByLabelText('路由地址')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.type(screen.getByPlaceholderText('請輸入菜單名稱'), '菜單管理');
        await user.type(screen.getByPlaceholderText('請輸入路由地址'), '/system/menu');

        // 點擊「提交並添加下一個」
        await user.click(screen.getByText('提交並添加下一個'));

        await waitFor(
          () => {
            expect(message.success).toHaveBeenCalledWith('添加成功');
          },
          { timeout: TEST_TIMEOUT }
        );

        // 驗證表單已重置但菜單類型保留
        await waitFor(
          () => {
            const nameInput = screen.getByPlaceholderText(
              '請輸入菜單名稱'
            ) as HTMLInputElement;
            expect(nameInput.value).toBe('');
            // 菜單類型應該仍然是「菜單」，所以路由地址字段應該存在
            expect(screen.getByLabelText('路由地址')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('Cancel操作', () => {
    it(
      '應該在點擊「取消」時調用 onCancel 並重置表單',
      async () => {
        const onCancel = vi.fn();
        const user = userEvent.setup();

        render(<MenuFormModal {...defaultProps} onCancel={onCancel} />);

        await waitFor(
          () => {
            expect(screen.getByText('取消')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.type(screen.getByPlaceholderText('請輸入菜單名稱'), '測試');
        await user.click(screen.getByText('取消'));

        expect(onCancel).toHaveBeenCalledTimes(1);
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理 Drawer 重新打開時的表單重置',
      async () => {
        const { rerender } = render(<MenuFormModal {...defaultProps} visible={false} />);

        rerender(<MenuFormModal {...defaultProps} visible={true} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入菜單名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const user = userEvent.setup();
        await user.type(screen.getByPlaceholderText('請輸入菜單名稱'), '測試');

        rerender(<MenuFormModal {...defaultProps} visible={false} />);
        rerender(<MenuFormModal {...defaultProps} visible={true} />);

        await waitFor(
          () => {
            const nameInput = screen.getByPlaceholderText(
              '請輸入菜單名稱'
            ) as HTMLInputElement;
            expect(nameInput.value).toBe('');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
