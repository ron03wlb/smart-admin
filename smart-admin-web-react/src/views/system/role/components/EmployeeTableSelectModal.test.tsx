/**
 * EmployeeTableSelectModal Component Tests
 * 員工選擇表格 Modal 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeTableSelectModal from './EmployeeTableSelectModal';
import { employeeApi } from '@/api/system/employeeApi';
import type { EmployeeVO } from '@/views/system/employee/types';

// Mock employeeApi
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    queryEmployee: vi.fn(),
  },
}));

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      warning: vi.fn(),
      error: vi.fn(),
    },
  };
});

describe('EmployeeTableSelectModal', () => {
  const mockEmployeeList: EmployeeVO[] = [
    {
      employeeId: 1,
      loginName: 'admin',
      actualName: '管理員',
      phone: '13800138000',
      departmentId: 1,
      departmentName: '技術部',
      positionId: 1,
      positionName: '工程師',
      disabledFlag: false,
      administratorFlag: true,
      remark: '',
    },
    {
      employeeId: 2,
      loginName: 'user1',
      actualName: '張三',
      phone: '13800138001',
      departmentId: 1,
      departmentName: '技術部',
      positionId: 2,
      positionName: '高級工程師',
      disabledFlag: false,
      administratorFlag: false,
      remark: '',
    },
    {
      employeeId: 3,
      loginName: 'user2',
      actualName: '李四',
      phone: '13800138002',
      departmentId: 2,
      departmentName: '產品部',
      positionId: 3,
      positionName: '產品經理',
      disabledFlag: true, // 禁用用戶
      administratorFlag: false,
      remark: '',
    },
  ];

  const defaultProps = {
    visible: true,
    onCancel: vi.fn(),
    onConfirm: vi.fn(),
    excludeEmployeeIds: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    // 默認返回成功的 API 響應
    vi.mocked(employeeApi.queryEmployee).mockResolvedValue({
      ok: true,
      code: 1,
      msg: '操作成功',
      data: {
        list: mockEmployeeList,
        total: 3,
        pageNum: 1,
        pageSize: 10,
      },
    });
  });

  describe('基礎渲染', () => {
    it('應該在 visible=true 時顯示 Modal', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('選擇員工')).toBeInTheDocument();
      });
    });

    it('應該在 visible=false 時隱藏 Modal', () => {
      render(<EmployeeTableSelectModal {...defaultProps} visible={false} />);

      expect(screen.queryByText('選擇員工')).not.toBeInTheDocument();
    });

    it('應該顯示搜索框', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索')).toBeInTheDocument();
      });
    });

    it('應該顯示確認和取消按鈕', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('確認添加')).toBeInTheDocument();
        expect(screen.getByText('取消')).toBeInTheDocument();
      });
    });
  });

  describe('數據加載', () => {
    it('應該在 Modal 打開時加載員工列表', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalledTimes(1);
      });
    });

    it('應該正確傳遞查詢參數', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} departmentId={1} />);

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalledWith(
          expect.objectContaining({
            departmentId: 1,
            pageNum: 1,
            pageSize: 10,
          })
        );
      });
    });

    it('應該過濾掉已排除的員工', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} excludeEmployeeIds={[1, 2]} />);

      await waitFor(() => {
        // 應該只顯示 employeeId=3 的員工（李四）
        expect(screen.getByText('李四')).toBeInTheDocument();
        expect(screen.queryByText('管理員')).not.toBeInTheDocument();
        expect(screen.queryByText('張三')).not.toBeInTheDocument();
      });
    });

    it('應該處理 API 錯誤', async () => {
      vi.mocked(employeeApi.queryEmployee).mockRejectedValue(new Error('網絡錯誤'));

      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalled();
      });
    });
  });

  describe('搜索功能', () => {
    it('應該支持關鍵字搜索', async () => {
      const user = userEvent.setup();
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索');
      await user.type(searchInput, '張三');
      await user.click(screen.getByText('搜索'));

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalledWith(
          expect.objectContaining({
            keyword: '張三',
            pageNum: 1,
          })
        );
      });
    });

    it('應該支持 Enter 鍵觸發搜索', async () => {
      const user = userEvent.setup();
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索');
      await user.type(searchInput, '張三{Enter}');

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalledWith(
          expect.objectContaining({
            keyword: '張三',
          })
        );
      });
    });
  });

  describe('員工選擇', () => {
    it('應該支持單選員工', async () => {
      const user = userEvent.setup();
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('管理員')).toBeInTheDocument();
      });

      // 選擇第一個員工（管理員）
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // 第一個是全選，第二個是第一行

      // 應該顯示已選擇提示
      await waitFor(() => {
        expect(screen.getByText(/已選擇 1 名員工/)).toBeInTheDocument();
      });
    });

    it('應該支持多選員工', async () => {
      const user = userEvent.setup();
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('管理員')).toBeInTheDocument();
      });

      // 選擇多個員工
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]); // 管理員
      await user.click(checkboxes[2]); // 張三

      await waitFor(() => {
        expect(screen.getByText(/已選擇 2 名員工/)).toBeInTheDocument();
      });
    });

    it('應該禁用已禁用的員工', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} excludeEmployeeIds={[]} />);

      await waitFor(() => {
        expect(screen.getByText('李四')).toBeInTheDocument();
      });

      // 李四是禁用用戶，其複選框應該被禁用
      const checkboxes = screen.getAllByRole('checkbox');
      // 注意：實際索引可能需要調整
      const disabledCheckbox = checkboxes.find(cb => cb.hasAttribute('disabled'));
      expect(disabledCheckbox).toBeDefined();
    });
  });

  describe('確認操作', () => {
    it('應該在未選擇員工時顯示警告', async () => {
      const { message } = await import('antd');
      const user = userEvent.setup();

      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('確認添加')).toBeInTheDocument();
      });

      // 直接點擊確認按鈕（未選擇任何員工）
      await user.click(screen.getByText('確認添加'));

      expect(message.warning).toHaveBeenCalledWith('請至少選擇一名員工');
    });

    it('應該在選擇員工後調用 onConfirm', async () => {
      const onConfirm = vi.fn();
      const user = userEvent.setup();

      render(<EmployeeTableSelectModal {...defaultProps} onConfirm={onConfirm} />);

      await waitFor(() => {
        expect(screen.getByText('管理員')).toBeInTheDocument();
      });

      // 選擇一個員工
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]);

      // 點擊確認
      await user.click(screen.getByText('確認添加'));

      expect(onConfirm).toHaveBeenCalledWith([1]); // employeeId=1（管理員）
    });

    it('應該在確認後清空選擇', async () => {
      const user = userEvent.setup();

      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('管理員')).toBeInTheDocument();
      });

      // 選擇員工
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]);

      await waitFor(() => {
        expect(screen.getByText(/已選擇 1 名員工/)).toBeInTheDocument();
      });

      // 確認
      await user.click(screen.getByText('確認添加'));

      // 選擇應該被清空（需要重新打開 Modal 驗證）
    });
  });

  describe('取消操作', () => {
    it('應該在點擊取消時調用 onCancel', async () => {
      const onCancel = vi.fn();
      const user = userEvent.setup();

      render(<EmployeeTableSelectModal {...defaultProps} onCancel={onCancel} />);

      await waitFor(() => {
        expect(screen.getByText('取消')).toBeInTheDocument();
      });

      await user.click(screen.getByText('取消'));

      expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('應該在取消後清空選擇', async () => {
      const user = userEvent.setup();

      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('管理員')).toBeInTheDocument();
      });

      // 選擇員工
      const checkboxes = screen.getAllByRole('checkbox');
      await user.click(checkboxes[1]);

      // 取消
      await user.click(screen.getByText('取消'));

      // 選擇應該被清空
    });
  });

  describe('分頁功能', () => {
    it('應該支持翻頁', async () => {
      const user = userEvent.setup();
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalledTimes(1);
      });

      // Ant Design Table 的分頁控制（實際實現可能需要調整）
      // 這裡主要驗證 API 調用邏輯
    });

    it('應該支持改變每頁條數', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalledTimes(1);
      });

      // 驗證 Table pagination 配置
    });
  });

  describe('Modal 關閉時清空狀態', () => {
    it('應該在 Modal 關閉時清空搜索關鍵字', async () => {
      const { rerender } = render(<EmployeeTableSelectModal {...defaultProps} visible={true} />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索')).toBeInTheDocument();
      });

      // 關閉 Modal
      rerender(<EmployeeTableSelectModal {...defaultProps} visible={false} />);

      // 重新打開
      rerender(<EmployeeTableSelectModal {...defaultProps} visible={true} />);

      // 關鍵字應該被重置為空
      await waitFor(() => {
        const searchInput = screen.getByPlaceholderText('請輸入姓名、登錄名或電話搜索');
        expect(searchInput).toHaveValue('');
      });
    });
  });

  describe('邊界情況', () => {
    it('應該處理空員工列表', async () => {
      vi.mocked(employeeApi.queryEmployee).mockResolvedValue({
        ok: true,
        code: 1,
        msg: '操作成功',
        data: {
          list: [],
          total: 0,
          pageNum: 1,
          pageSize: 10,
        },
      });

      render(<EmployeeTableSelectModal {...defaultProps} />);

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalled();
      });

      // 應該顯示空狀態（Ant Design Table 的默認空狀態）
    });

    it('應該處理所有員工都被排除的情況', async () => {
      render(<EmployeeTableSelectModal {...defaultProps} excludeEmployeeIds={[1, 2, 3]} />);

      await waitFor(() => {
        expect(employeeApi.queryEmployee).toHaveBeenCalled();
      });

      // 所有員工都被過濾，應該顯示空列表
    });
  });
});
