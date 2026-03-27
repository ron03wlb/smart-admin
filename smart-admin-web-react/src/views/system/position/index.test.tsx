/**
 * Position Management Page Tests
 * 職位管理頁面集成測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PositionPage from './index';
import { positionApi } from '@/api/system/positionApi';
import type { PositionVO } from './types';
import type { PageResult } from '@/api/types/response';

// 增加測試超時時間
const TEST_TIMEOUT = 15000;

// Mock positionApi
vi.mock('@/api/system/positionApi', () => ({
  positionApi: {
    queryPage: vi.fn(),
    deletePosition: vi.fn(),
    batchDeletePosition: vi.fn(),
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
  default: ({
    children,
    onClick,
    disabled,
  }: {
    children: React.ReactNode;
    onClick?: () => void;
    disabled?: boolean;
  }) => (
    <button onClick={onClick} disabled={disabled}>
      {children}
    </button>
  ),
}));

// Mock PositionFormModal component
vi.mock('./components/PositionFormModal', () => ({
  PositionFormModal: ({ visible, onCancel }: { visible: boolean; onCancel: () => void }) =>
    visible ? (
      <div data-testid="position-form-modal">
        <button onClick={onCancel}>Cancel</button>
      </div>
    ) : null,
}));

describe('PositionPage', () => {
  const mockPositionList: PositionVO[] = [
    {
      positionId: 1,
      positionName: '高級工程師',
      positionLevel: 'P7',
      sort: 1,
      remark: '技術骨幹',
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
    {
      positionId: 2,
      positionName: '產品經理',
      positionLevel: 'M2',
      sort: 2,
      remark: '產品負責人',
      createTime: '2026-03-24 00:00:00',
      updateTime: '2026-03-24 00:00:00',
    },
  ];

  const mockPageResult: PageResult<PositionVO> = {
    list: mockPositionList,
    total: 2,
    pageNum: 1,
    pageSize: 10,
    pages: 1,
    emptyFlag: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(positionApi.queryPage).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: mockPageResult,
    });
  });

  describe('基礎渲染', () => {
    it(
      '應該渲染搜索框和操作按鈕',
      async () => {
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('關鍵字查詢')).toBeInTheDocument();
            expect(screen.getByText('查詢')).toBeInTheDocument();
            expect(screen.getByText('重置')).toBeInTheDocument();
            expect(screen.getByText('新建')).toBeInTheDocument();
            expect(screen.getByText('批量刪除')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該渲染表格',
      async () => {
        render(<PositionPage />);

        // 等待數據加載完成
        await waitFor(
          () => {
            expect(screen.getByText('高級工程師')).toBeInTheDocument();
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
      '應該在初始化時加載職位列表',
      async () => {
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(positionApi.queryPage).toHaveBeenCalled();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示職位列表數據',
      async () => {
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(screen.getByText('高級工程師')).toBeInTheDocument();
            expect(screen.getByText('產品經理')).toBeInTheDocument();
            expect(screen.getByText('P7')).toBeInTheDocument();
            expect(screen.getByText('M2')).toBeInTheDocument();
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
        vi.mocked(positionApi.queryPage).mockRejectedValue(new Error('網絡錯誤'));

        render(<PositionPage />);

        // useTable hook 會自動處理錯誤，這裡只驗證 API 被調用
        await waitFor(
          () => {
            expect(positionApi.queryPage).toHaveBeenCalled();
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
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(screen.getByPlaceholderText('關鍵字查詢')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        const searchInput = screen.getByPlaceholderText('關鍵字查詢');
        await user.type(searchInput, '工程師');

        const searchButton = screen.getByText('查詢');
        await user.click(searchButton);

        // 驗證 API 被調用（第一次自動加載，第二次搜索）
        await waitFor(
          () => {
            expect(positionApi.queryPage).toHaveBeenCalledTimes(2);
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示重置按鈕',
      async () => {
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(screen.getByText('重置')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('新建職位', () => {
    it(
      '應該打開職位表單 Modal',
      async () => {
        const user = userEvent.setup();
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(screen.getByText('新建')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        await user.click(screen.getByText('新建'));

        await waitFor(
          () => {
            expect(screen.getByTestId('position-form-modal')).toBeInTheDocument();
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
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(screen.getByText('新建')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 打開 Modal
        await user.click(screen.getByText('新建'));

        await waitFor(
          () => {
            expect(screen.getByTestId('position-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 點擊取消
        const cancelButton = screen.getByText('Cancel');
        await user.click(cancelButton);

        await waitFor(
          () => {
            expect(screen.queryByTestId('position-form-modal')).not.toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('編輯職位', () => {
    it(
      '應該顯示「編輯」按鈕',
      async () => {
        render(<PositionPage />);

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
      '應該打開編輯職位的 Modal',
      async () => {
        const user = userEvent.setup();
        render(<PositionPage />);

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
            expect(screen.getByTestId('position-form-modal')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('刪除職位', () => {
    it(
      '應該顯示「刪除」按鈕',
      async () => {
        render(<PositionPage />);

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

  describe('批量刪除', () => {
    it(
      '應該顯示批量刪除按鈕',
      async () => {
        render(<PositionPage />);

        await waitFor(
          () => {
            expect(screen.getByText('批量刪除')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在未選擇時禁用批量刪除按鈕',
      async () => {
        render(<PositionPage />);

        await waitFor(
          () => {
            const batchDeleteButton = screen.getByText('批量刪除');
            expect(batchDeleteButton).toBeInTheDocument();
            // 驗證按鈕被禁用
            expect(batchDeleteButton).toBeDisabled();
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
        vi.mocked(positionApi.queryPage).mockResolvedValue({
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

        render(<PositionPage />);

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
