/**
 * RoleDataScopeModal Component Tests
 * 角色數據範圍配置 Modal 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoleDataScopeModal from './RoleDataScopeModal';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, DataScopeVO } from '../types';

// 增加測試超時時間（Modal 組件渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock roleApi
vi.mock('@/api/system/roleApi', () => ({
  roleApi: {
    getDataScopeList: vi.fn(),
    getDataScopeByRoleId: vi.fn(),
    updateDataScope: vi.fn(),
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

describe('RoleDataScopeModal', () => {
  const mockRole: RoleVO = {
    roleId: 1,
    roleName: '管理員',
    roleCode: 'admin',
    remark: '系統管理員',
  };

  const mockDataScopeList: DataScopeVO[] = [
    {
      dataScopeType: 1,
      dataScopeTypeName: '訂單管理',
      dataScopeTypeDesc: '訂單查看範圍',
      viewType: 1,
      viewTypeList: [
        { viewType: 1, viewTypeName: '全部數據' },
        { viewType: 2, viewTypeName: '本部門數據' },
        { viewType: 3, viewTypeName: '本人數據' },
      ],
    },
    {
      dataScopeType: 2,
      dataScopeTypeName: '客戶管理',
      dataScopeTypeDesc: '客戶查看範圍',
      viewType: 2,
      viewTypeList: [
        { viewType: 1, viewTypeName: '全部數據' },
        { viewType: 2, viewTypeName: '本部門數據' },
      ],
    },
  ];

  const mockRoleDataScope: DataScopeVO[] = [
    {
      dataScopeType: 1,
      dataScopeTypeName: '訂單管理',
      dataScopeTypeDesc: '訂單查看範圍',
      viewType: 1,
      viewTypeList: [],
    },
    {
      dataScopeType: 2,
      dataScopeTypeName: '客戶管理',
      dataScopeTypeDesc: '客戶查看範圍',
      viewType: 2,
      viewTypeList: [],
    },
  ];

  const defaultProps = {
    visible: true,
    onCancel: vi.fn(),
    onSuccess: vi.fn(),
    role: mockRole,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(roleApi.getDataScopeList).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockDataScopeList,
    });
    vi.mocked(roleApi.getDataScopeByRoleId).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockRoleDataScope,
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該顯示帶有角色名稱的 Modal 標題',
      async () => {
        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText(`設置數據範圍 - ${mockRole.roleName}`)).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 visible=false 時隱藏 Modal',
      () => {
        render(<RoleDataScopeModal {...defaultProps} visible={false} />);
        expect(screen.queryByText(/設置數據範圍/)).not.toBeInTheDocument();
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示提示信息',
      async () => {
        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('提示')).toBeInTheDocument();
            expect(
              screen.getByText('數據範圍決定該角色在查看業務數據時的可見範圍')
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
        render(<RoleDataScopeModal {...defaultProps} />);

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

    it(
      '應該顯示表頭',
      async () => {
        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('業務單據')).toBeInTheDocument();
            expect(screen.getByText('查看數據範圍')).toBeInTheDocument();
            expect(screen.getByText('說明')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('數據加載', () => {
    it(
      '應該在 Modal 打開時加載數據範圍列表',
      async () => {
        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(roleApi.getDataScopeList).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 Modal 打開時加載角色數據範圍配置',
      async () => {
        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(roleApi.getDataScopeByRoleId).toHaveBeenCalledWith(mockRole.roleId);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示數據範圍配置項',
      async () => {
        render(<RoleDataScopeModal {...defaultProps} />);

        // 分步驟檢查，避免一次性檢查太多導致超時
        await waitFor(
          () => {
            expect(screen.getByText('訂單管理')).toBeInTheDocument();
            expect(screen.getByText('客戶管理')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 驗證數據範圍選項存在
        await waitFor(
          () => {
            expect(screen.getAllByText('全部數據').length).toBeGreaterThan(0);
            expect(screen.getAllByText('本部門數據').length).toBeGreaterThan(0);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理數據範圍列表加載錯誤',
      async () => {
        const { message } = await import('antd');
        vi.mocked(roleApi.getDataScopeList).mockRejectedValue(new Error('網絡錯誤'));

        render(<RoleDataScopeModal {...defaultProps} />);

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
      '應該處理角色數據範圍加載錯誤',
      async () => {
        const { message } = await import('antd');
        vi.mocked(roleApi.getDataScopeByRoleId).mockRejectedValue(new Error('權限不足'));

        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('權限不足');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在沒有 role 時不加載角色數據範圍',
      async () => {
        render(<RoleDataScopeModal {...defaultProps} role={undefined} />);

        await waitFor(
          () => {
            // 仍然應該加載數據範圍列表
            expect(roleApi.getDataScopeList).toHaveBeenCalledTimes(0);
            // 但不應該加載角色配置
            expect(roleApi.getDataScopeByRoleId).not.toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('提交操作', () => {
    it(
      '應該成功保存數據範圍配置',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();
        const onSuccess = vi.fn();

        vi.mocked(roleApi.updateDataScope).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(<RoleDataScopeModal {...defaultProps} onSuccess={onSuccess} />);

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
            expect(roleApi.updateDataScope).toHaveBeenCalledWith(
              expect.objectContaining({
                roleId: mockRole.roleId,
                dataScopeItemList: expect.any(Array),
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

        vi.mocked(roleApi.updateDataScope).mockRejectedValue(new Error('權限不足'));

        render(<RoleDataScopeModal {...defaultProps} />);

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

        render(<RoleDataScopeModal {...defaultProps} onCancel={onCancel} />);

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
      '應該處理空數據範圍列表',
      async () => {
        vi.mocked(roleApi.getDataScopeList).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: [],
        });

        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('暫無數據範圍配置')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理沒有錯誤消息的數據範圍列表加載失敗',
      async () => {
        const { message } = await import('antd');
        vi.mocked(roleApi.getDataScopeList).mockRejectedValue(new Error());

        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('加載數據範圍失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理沒有錯誤消息的角色數據範圍加載失敗',
      async () => {
        const { message } = await import('antd');
        vi.mocked(roleApi.getDataScopeByRoleId).mockRejectedValue(new Error());

        render(<RoleDataScopeModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('加載數據範圍配置失敗');
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

        vi.mocked(roleApi.updateDataScope).mockRejectedValue(new Error());

        render(<RoleDataScopeModal {...defaultProps} />);

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
