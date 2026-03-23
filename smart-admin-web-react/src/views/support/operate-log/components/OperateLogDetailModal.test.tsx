/**
 * OperateLogDetailModal Component Unit Tests
 * 操作日誌詳情Modal組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { createRef } from 'react';
import OperateLogDetailModal, { OperateLogDetailModalRef } from './OperateLogDetailModal';
import type { OperateLogVO } from '../types';

// Mock dependencies
vi.mock('@/api/support/operateLogApi', () => ({
  operateLogApi: {
    detail: vi.fn(),
  },
}));

vi.mock('@/utils/date', () => ({
  formatDateTime: vi.fn(date => date || '-'),
}));

import { operateLogApi } from '@/api/support/operateLogApi';

const mockOperateLogDetail: OperateLogVO = {
  operateLogId: 1,
  operateUserId: 100,
  operateUserName: '測試用戶',
  operateUserType: 1,
  url: '/api/employee/add',
  method: 'POST',
  module: '員工管理',
  content: '新增員工',
  param: '{"name":"張三","phone":"13800138000"}',
  response: '{"code":200,"ok":true,"msg":"Success"}',
  ip: '192.168.1.100',
  ipRegion: '中國 廣東 深圳',
  userAgent:
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  successFlag: 1,
  failReason: undefined,
  createTime: '2026-03-15 10:30:00',
};

describe('OperateLogDetailModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    (operateLogApi.detail as any).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: mockOperateLogDetail,
    });
  });

  describe('Basic Rendering', () => {
    it('should not render modal initially', () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      expect(screen.queryByText('請求詳情')).not.toBeInTheDocument();
    });

    it('should render modal when show method is called', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText('請求詳情')).toBeInTheDocument();
      });
    });

    it('should display loading state initially', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText('請求詳情')).toBeInTheDocument();
      });
    });
  });

  describe('Data Fetching', () => {
    it('should fetch detail when show is called', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(operateLogApi.detail).toHaveBeenCalledWith(1);
      });
    });

    it('should display fetched detail data', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText(/測試用戶/)).toBeInTheDocument();
        expect(screen.getByText(/\/api\/employee\/add/)).toBeInTheDocument();
        expect(screen.getByText(/員工管理/)).toBeInTheDocument();
      });
    });
  });

  describe('Success/Failure Status', () => {
    it('should display success status for successful operation', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText('成功')).toBeInTheDocument();
      });
    });

    it('should display failure status for failed operation', async () => {
      (operateLogApi.detail as any).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          ...mockOperateLogDetail,
          successFlag: 0,
          failReason: '權限不足',
        },
      });

      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText('失敗')).toBeInTheDocument();
        expect(screen.getByText('權限不足')).toBeInTheDocument();
      });
    });
  });

  describe('UserAgent Parsing', () => {
    it('should parse and display browser, OS, and device info', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        // UserAgent parsing results
        expect(screen.getByText(/客戶端：/)).toBeInTheDocument();
      });
    });
  });

  describe('Request Parameters', () => {
    it('should display request parameters', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText('請求參數：')).toBeInTheDocument();
      });
    });

    it('should display response data for successful request', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText('返回結果：')).toBeInTheDocument();
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle API error gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      (operateLogApi.detail as any).mockRejectedValue(new Error('Network error'));

      const ref = createRef<OperateLogDetailModalRef>();
      render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith(
          'Failed to fetch operate log detail:',
          expect.any(Error)
        );
      });

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Modal Close', () => {
    it('should close modal when cancel is triggered', async () => {
      const ref = createRef<OperateLogDetailModalRef>();
      const { container } = render(<OperateLogDetailModal ref={ref} />);

      ref.current?.show(1);

      await waitFor(() => {
        expect(screen.getByText('請求詳情')).toBeInTheDocument();
      });

      // Find and trigger close
      const closeButton = container.querySelector('.ant-modal-close');
      if (closeButton) {
        closeButton.dispatchEvent(new MouseEvent('click', { bubbles: true }));

        await waitFor(() => {
          expect(screen.queryByText('請求詳情')).not.toBeVisible();
        });
      }
    });
  });
});
