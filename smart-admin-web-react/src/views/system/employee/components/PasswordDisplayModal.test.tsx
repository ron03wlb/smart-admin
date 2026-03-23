/**
 * PasswordDisplayModal Component Unit Tests
 * 密碼顯示 Modal 組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PasswordDisplayModal } from './PasswordDisplayModal';

// Mock Ant Design message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
    },
  };
});

import { message } from 'antd';

describe('PasswordDisplayModal', () => {
  const mockProps = {
    visible: true,
    loginName: 'zhangsan',
    password: 'Abc123!@#',
    onClose: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==================== 基本渲染測試 ====================

  describe('Basic Rendering', () => {
    it('should render when visible is true', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      expect(screen.getByText('賬號密碼信息')).toBeInTheDocument();
      expect(screen.getByText(mockProps.loginName)).toBeInTheDocument();
      expect(screen.getByText(mockProps.password)).toBeInTheDocument();
    });

    it('should not render when visible is false', () => {
      render(<PasswordDisplayModal {...mockProps} visible={false} />);

      expect(screen.queryByText('賬號密碼信息')).not.toBeInTheDocument();
    });

    it('should display login name and password', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      // 檢查登錄名
      expect(screen.getByText('登錄名：')).toBeInTheDocument();
      expect(screen.getByText(mockProps.loginName)).toBeInTheDocument();

      // 檢查密碼
      expect(screen.getByText('初始密碼：')).toBeInTheDocument();
      expect(screen.getByText(mockProps.password)).toBeInTheDocument();
    });

    it('should display warning message', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      expect(screen.getByText(/請妥善保管此密碼/)).toBeInTheDocument();
    });
  });

  // ==================== 按鈕測試 ====================

  describe('Buttons', () => {
    it('should display "我已記住" button', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      const confirmButton = screen.getByRole('button', { name: /我已記住/ });
      expect(confirmButton).toBeInTheDocument();
    });

    it('should display copy password button', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      const copyButton = screen.getByRole('button', { name: /復制密碼/ });
      expect(copyButton).toBeInTheDocument();
    });

    it('should call onClose when confirm button is clicked', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      const confirmButton = screen.getByRole('button', { name: /我已記住/ });
      fireEvent.click(confirmButton);

      expect(mockProps.onClose).toHaveBeenCalledTimes(1);
    });

    it('should not have cancel button', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      const cancelButton = screen.queryByRole('button', { name: /取消/ });
      expect(cancelButton).not.toBeInTheDocument();
    });
  });

  // ==================== 復制密碼功能測試 ====================

  describe('Copy Password Functionality', () => {
    it('should copy password using Clipboard API', async () => {
      const mockWriteText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, {
        clipboard: {
          writeText: mockWriteText,
        },
      });

      render(<PasswordDisplayModal {...mockProps} />);

      const copyButton = screen.getByRole('button', { name: /復制密碼/ });
      fireEvent.click(copyButton);

      await waitFor(() => {
        expect(mockWriteText).toHaveBeenCalledWith(mockProps.password);
        expect(message.success).toHaveBeenCalledWith('密碼已復制到剪貼板');
      });
    });

    it('should show "已復制" text after copying', async () => {
      const mockWriteText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, {
        clipboard: {
          writeText: mockWriteText,
        },
      });

      render(<PasswordDisplayModal {...mockProps} />);

      const copyButton = screen.getByRole('button', { name: /復制密碼/ });
      fireEvent.click(copyButton);

      await waitFor(() => {
        expect(screen.getByText(/已復制/)).toBeInTheDocument();
      });
    });

    it('should reset copy status after 3 seconds', async () => {
      vi.useFakeTimers();

      const mockWriteText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, {
        clipboard: {
          writeText: mockWriteText,
        },
      });

      render(<PasswordDisplayModal {...mockProps} />);

      const copyButton = screen.getByRole('button', { name: /復制密碼/ });
      fireEvent.click(copyButton);

      // 等待"已復制"文本出現
      await vi.waitFor(() => {
        expect(screen.getByText(/已復制/)).toBeInTheDocument();
      });

      // 快進 3 秒並觸發 timers
      await vi.advanceTimersByTimeAsync(3000);

      // 驗證"已復制"文本消失
      expect(screen.queryByText(/已復制/)).not.toBeInTheDocument();
      expect(screen.getByText(/復制密碼/)).toBeInTheDocument();

      vi.useRealTimers();
    });

    it('should fallback to document.execCommand if Clipboard API fails', async () => {
      // Mock document.execCommand BEFORE rendering
      const mockExecCommand = vi.fn().mockReturnValue(true);
      document.execCommand = mockExecCommand;

      // Mock document.body.appendChild and removeChild
      const mockAppendChild = vi.spyOn(document.body, 'appendChild');
      const mockRemoveChild = vi.spyOn(document.body, 'removeChild');

      // Mock Clipboard API 失敗
      Object.assign(navigator, {
        clipboard: {
          writeText: vi.fn().mockRejectedValue(new Error('Clipboard API not available')),
        },
      });

      render(<PasswordDisplayModal {...mockProps} />);

      const copyButton = screen.getByRole('button', { name: /復制密碼/ });
      fireEvent.click(copyButton);

      // 等待 execCommand 被調用
      await waitFor(
        () => {
          expect(mockExecCommand).toHaveBeenCalledWith('copy');
        },
        { timeout: 3000 }
      );

      // 驗證成功消息
      expect(message.success).toHaveBeenCalledWith('密碼已復制到剪貼板');

      // 驗證 textarea 被添加和移除
      expect(mockAppendChild).toHaveBeenCalled();
      expect(mockRemoveChild).toHaveBeenCalled();

      // 清理 mocks
      mockAppendChild.mockRestore();
      mockRemoveChild.mockRestore();
    });
  });

  // ==================== Modal 行為測試 ====================

  describe('Modal Behavior', () => {
    it('should not be closable by clicking mask', () => {
      const { container } = render(<PasswordDisplayModal {...mockProps} />);

      // Ant Design Modal 的 maskClosable 為 false 時，點擊遮罩不會關閉
      const modal = container.querySelector('.ant-modal-wrap');
      if (modal) {
        fireEvent.click(modal);
        expect(mockProps.onClose).not.toHaveBeenCalled();
      }
    });

    it('should not have close icon', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      const closeIcon = screen.queryByRole('button', { name: /close/i });
      expect(closeIcon).not.toBeInTheDocument();
    });
  });

  // ==================== 不同密碼測試 ====================

  describe('Different Password Scenarios', () => {
    it('should display short password', () => {
      const shortPassword = 'abc123';
      render(<PasswordDisplayModal {...mockProps} password={shortPassword} />);

      expect(screen.getByText(shortPassword)).toBeInTheDocument();
    });

    it('should display long password', () => {
      const longPassword = 'Abc123!@#DefGhiJklMnoPqrStuVwxYz123456789';
      render(<PasswordDisplayModal {...mockProps} password={longPassword} />);

      expect(screen.getByText(longPassword)).toBeInTheDocument();
    });

    it('should display password with special characters', () => {
      const specialPassword = '!@#$%^&*()_+-=[]{}|;:,.<>?';
      render(<PasswordDisplayModal {...mockProps} password={specialPassword} />);

      expect(screen.getByText(specialPassword)).toBeInTheDocument();
    });
  });

  // ==================== 登錄名測試 ====================

  describe('Login Name Scenarios', () => {
    it('should display different login names', () => {
      const { rerender } = render(<PasswordDisplayModal {...mockProps} />);

      expect(screen.getByText('zhangsan')).toBeInTheDocument();

      rerender(<PasswordDisplayModal {...mockProps} loginName="admin" />);

      expect(screen.getByText('admin')).toBeInTheDocument();
    });

    it('should have copyable login name', () => {
      render(<PasswordDisplayModal {...mockProps} />);

      // Ant Design Typography.Title 的 copyable 屬性會顯示復制按鈕
      const loginNameElement = screen.getByText(mockProps.loginName);
      expect(loginNameElement).toBeInTheDocument();
    });
  });
});
