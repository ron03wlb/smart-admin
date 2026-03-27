/**
 * DictDataDrawer Component Unit Tests
 * 字典值Drawer組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import DictDataDrawer from './DictDataDrawer';
import type { DictDataVO } from '../types';

// Mock dependencies
vi.mock('@/api/support/dictApi', () => ({
  dictApi: {
    queryDictData: vi.fn(),
    deleteDictData: vi.fn(),
    updateDisabled: vi.fn(),
  },
}));

vi.mock('@/hooks/usePrivilege', () => ({
  usePrivilege: vi.fn(() => true),
}));

import { dictApi } from '@/api/support/dictApi';

const mockDictDataList: DictDataVO[] = [
  {
    dictDataId: 1,
    dictId: 10,
    dictCode: 'GENDER',
    dataValue: '1',
    dataLabel: '男',
    sortOrder: 100,
    remark: '男性',
    disabledFlag: 0,
    enabled: true,
  },
  {
    dictDataId: 2,
    dictId: 10,
    dictCode: 'GENDER',
    dataValue: '2',
    dataLabel: '女',
    sortOrder: 90,
    remark: '女性',
    disabledFlag: 0,
    enabled: true,
  },
];

describe('DictDataDrawer', () => {
  const mockOnClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    (dictApi.queryDictData as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: mockDictDataList,
    });
  });

  describe('Basic Rendering', () => {
    it('should render drawer when visible', async () => {
      render(<DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />);

      await waitFor(() => {
        expect(screen.getByText('字典值')).toBeInTheDocument();
      });
    });

    it('should not render when visible is false', () => {
      render(
        <DictDataDrawer visible={false} dictId={10} dictCode="GENDER" onClose={mockOnClose} />
      );

      expect(screen.queryByText('字典值')).not.toBeInTheDocument();
    });

    it('should display dict data in table', async () => {
      render(<DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />);

      await waitFor(() => {
        expect(screen.getByText('男')).toBeInTheDocument();
        expect(screen.getByText('女')).toBeInTheDocument();
      });
    });
  });

  describe('Data Fetching', () => {
    it('should fetch dict data when drawer opens', async () => {
      render(<DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />);

      await waitFor(() => {
        expect(dictApi.queryDictData).toHaveBeenCalledWith(10);
      });
    });

    it('should not fetch data when dictId is missing', () => {
      render(<DictDataDrawer visible={true} dictCode="GENDER" onClose={mockOnClose} />);

      expect(dictApi.queryDictData).not.toHaveBeenCalled();
    });
  });

  describe('Search and Filter', () => {
    it('should render search input', async () => {
      render(<DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />);

      await waitFor(() => {
        // 實際組件的 placeholder 是 "關鍵字"，不是 "請輸入關鍵字"
        expect(screen.getByPlaceholderText('關鍵字')).toBeInTheDocument();
      });
    });

    it('should render disabled filter select', async () => {
      render(<DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />);

      await waitFor(() => {
        expect(screen.getByText('全部')).toBeInTheDocument();
      });
    });
  });

  describe('Table Actions', () => {
    it('should render add button', async () => {
      render(<DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />);

      await waitFor(() => {
        // 實際組件的按鈕文字是 "新建"，不是 "添加"
        expect(screen.getByRole('button', { name: /新建/i })).toBeInTheDocument();
      });
    });

    it('should render batch delete button', async () => {
      render(<DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /批量刪除/i })).toBeInTheDocument();
      });
    });
  });

  describe('Drawer Close', () => {
    it('should call onClose when drawer is closed', async () => {
      const { container } = render(
        <DictDataDrawer visible={true} dictId={10} dictCode="GENDER" onClose={mockOnClose} />
      );

      await waitFor(() => {
        expect(screen.getByText('字典值')).toBeInTheDocument();
      });

      // Find and click close button
      const closeButton = container.querySelector('.ant-drawer-close');
      if (closeButton) {
        closeButton.dispatchEvent(new MouseEvent('click', { bubbles: true }));
        expect(mockOnClose).toHaveBeenCalled();
      }
    });
  });
});
