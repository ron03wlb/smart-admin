/**
 * RoleFormModal Component Tests
 * 角色表單 Modal 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RoleFormModal from './RoleFormModal';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO } from '../types';
import { ROLE_VALIDATION } from '@/constants/system/roleConst';

// 增加測試超時時間（Modal 和 Form 組件渲染較慢）
const TEST_TIMEOUT = 15000;

// Mock roleApi
vi.mock('@/api/system/roleApi', () => ({
  roleApi: {
    addRole: vi.fn(),
    updateRole: vi.fn(),
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
  useModal: ({ defaultFormData }: { defaultFormData?: RoleVO }) => ({
    isEdit: !!defaultFormData,
  }),
}));

describe('RoleFormModal', () => {
  const mockRoleVO: RoleVO = {
    roleId: 1,
    roleName: '管理員',
    roleCode: 'admin',
    remark: '系統管理員角色',
    createTime: '2026-03-24 00:00:00',
    updateTime: '2026-03-24 00:00:00',
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
      '應該在新增模式下顯示 Modal',
      async () => {
        render(<RoleFormModal {...defaultProps} />);

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
      '應該在編輯模式下顯示 Modal',
      async () => {
        render(<RoleFormModal {...defaultProps} initialData={mockRoleVO} />);

        await waitFor(
          () => {
            expect(screen.getByText('編輯角色')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在 visible=false 時隱藏 Modal',
      () => {
        render(<RoleFormModal {...defaultProps} visible={false} />);
        expect(screen.queryByText('新增角色')).not.toBeInTheDocument();
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示所有表單項',
      async () => {
        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByLabelText('角色名稱')).toBeInTheDocument();
            expect(screen.getByLabelText('角色編碼')).toBeInTheDocument();
            expect(screen.getByLabelText('備註')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('表單驗證', () => {
    it(
      '應該在角色名稱為空時顯示錯誤',
      async () => {
        const user = userEvent.setup();
        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('OK')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 點擊OK按鈕觸發驗證
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(screen.getByText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在角色編碼為空時顯示錯誤',
      async () => {
        const user = userEvent.setup();
        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByText('OK')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(screen.getByText('請輸入角色編碼')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該驗證角色名稱最大長度',
      async () => {
        const user = userEvent.setup();
        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const nameInput = screen.getByPlaceholderText('請輸入角色名稱');
        const longName = 'a'.repeat(ROLE_VALIDATION.NAME_MAX_LENGTH + 1);

        // 使用 fireEvent.change 直接設置值以提高性能
        fireEvent.change(nameInput, { target: { value: longName } });
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(
              screen.getByText(`角色名稱最多${ROLE_VALIDATION.NAME_MAX_LENGTH}個字符`)
            ).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該驗證角色編碼格式',
      async () => {
        const user = userEvent.setup();
        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText(/請輸入角色編碼/)).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const codeInput = screen.getByPlaceholderText(/請輸入角色編碼/);

        // 測試非法字符（包含中文）
        await user.type(codeInput, '管理員@123');
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(screen.getByText('角色編碼只能包含字母、數字和下劃線')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該驗證備註最大長度',
      async () => {
        const user = userEvent.setup();
        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入備註（可選）')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const remarkInput = screen.getByPlaceholderText('請輸入備註（可選）');
        const longRemark = 'a'.repeat(ROLE_VALIDATION.REMARK_MAX_LENGTH + 1);

        // 使用 fireEvent.change 直接設置值以提高性能
        fireEvent.change(remarkInput, { target: { value: longRemark } });
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(
              screen.getByText(`備註最多${ROLE_VALIDATION.REMARK_MAX_LENGTH}個字符`)
            ).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('新增角色', () => {
    it(
      '應該成功新增角色',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();
        const onSuccess = vi.fn();

        vi.mocked(roleApi.addRole).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(<RoleFormModal {...defaultProps} onSuccess={onSuccess} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 填寫表單
        await user.type(screen.getByPlaceholderText('請輸入角色名稱'), '測試角色');
        await user.type(screen.getByPlaceholderText(/請輸入角色編碼/), 'test_role');
        await user.type(screen.getByPlaceholderText('請輸入備註（可選）'), '測試備註');

        // 提交
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(roleApi.addRole).toHaveBeenCalledWith({
              roleName: '測試角色',
              roleCode: 'test_role',
              remark: '測試備註',
            });
            expect(message.success).toHaveBeenCalledWith('新增成功');
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

        vi.mocked(roleApi.addRole).mockRejectedValue(new Error('角色編碼已存在'));

        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.type(screen.getByPlaceholderText('請輸入角色名稱'), '測試角色');
        await user.type(screen.getByPlaceholderText(/請輸入角色編碼/), 'admin');
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('角色編碼已存在');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('編輯角色', () => {
    it(
      '應該在編輯模式下初始化表單數據',
      async () => {
        render(<RoleFormModal {...defaultProps} initialData={mockRoleVO} />);

        await waitFor(
          () => {
            const nameInput = screen.getByPlaceholderText('請輸入角色名稱') as HTMLInputElement;
            const codeInput = screen.getByPlaceholderText(/請輸入角色編碼/) as HTMLInputElement;
            const remarkInput = screen.getByPlaceholderText(
              '請輸入備註（可選）'
            ) as HTMLTextAreaElement;

            expect(nameInput.value).toBe('管理員');
            expect(codeInput.value).toBe('admin');
            expect(remarkInput.value).toBe('系統管理員角色');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該成功更新角色',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();
        const onSuccess = vi.fn();

        vi.mocked(roleApi.updateRole).mockResolvedValue({
          ok: true,
          code: 1,
          msg: '操作成功',
          data: undefined,
        });

        render(<RoleFormModal {...defaultProps} initialData={mockRoleVO} onSuccess={onSuccess} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 修改表單數據
        const nameInput = screen.getByPlaceholderText('請輸入角色名稱');
        await user.clear(nameInput);
        await user.type(nameInput, '超級管理員');

        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(roleApi.updateRole).toHaveBeenCalledWith({
              roleId: 1,
              roleName: '超級管理員',
              roleCode: 'admin',
              remark: '系統管理員角色',
            });
            expect(message.success).toHaveBeenCalledWith('更新成功');
            expect(onSuccess).toHaveBeenCalledTimes(1);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理更新失敗',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(roleApi.updateRole).mockRejectedValue(new Error('網絡錯誤'));

        render(<RoleFormModal {...defaultProps} initialData={mockRoleVO} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('網絡錯誤');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('Cancel操作', () => {
    it(
      '應該在點擊Cancel時調用 onCancel 並重置表單',
      async () => {
        const onCancel = vi.fn();
        const user = userEvent.setup();

        render(<RoleFormModal {...defaultProps} onCancel={onCancel} />);

        await waitFor(
          () => {
            expect(screen.getByText('Cancel')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 填寫表單
        await user.type(screen.getByPlaceholderText('請輸入角色名稱'), '測試');

        // 點擊Cancel
        await user.click(screen.getByText('Cancel'));

        expect(onCancel).toHaveBeenCalledTimes(1);
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理無錯誤消息的失敗情況',
      async () => {
        const { message } = await import('antd');
        const user = userEvent.setup();

        vi.mocked(roleApi.addRole).mockRejectedValue(new Error());

        render(<RoleFormModal {...defaultProps} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.type(screen.getByPlaceholderText('請輸入角色名稱'), '測試');
        await user.type(screen.getByPlaceholderText(/請輸入角色編碼/), 'test');
        await user.click(screen.getByText('OK'));

        await waitFor(
          () => {
            expect(message.error).toHaveBeenCalledWith('新增失敗');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理 Modal 重新打開時的表單重置',
      async () => {
        const { rerender } = render(<RoleFormModal {...defaultProps} visible={false} />);

        // 第一次打開，填寫數據
        rerender(<RoleFormModal {...defaultProps} visible={true} />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('請輸入角色名稱')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const user = userEvent.setup();
        await user.type(screen.getByPlaceholderText('請輸入角色名稱'), '測試');

        // 關閉
        rerender(<RoleFormModal {...defaultProps} visible={false} />);

        // 重新打開（無 initialData）
        rerender(<RoleFormModal {...defaultProps} visible={true} />);

        await waitFor(
          () => {
            const nameInput = screen.getByPlaceholderText('請輸入角色名稱') as HTMLInputElement;
            expect(nameInput.value).toBe('');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
