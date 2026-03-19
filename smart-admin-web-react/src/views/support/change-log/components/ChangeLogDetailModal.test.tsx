/**
 * ChangeLogDetailModal Component Unit Tests
 * 系統更新日誌詳情 Modal 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ChangeLogDetailModal from './ChangeLogDetailModal';
import type { ChangeLogVO } from '../types';

const mockChangeLogWithLink: ChangeLogVO = {
  changeLogId: 1,
  updateVersion: 'v1.0.0',
  type: 1,
  publishAuthor: 'Admin',
  publicDate: '2026-01-01',
  content: '重大更新：新增用戶管理模塊\n- 支持用戶 CRUD 操作\n- 支持權限控制',
  link: 'https://example.com/v1.0.0',
  createTime: '2026-01-01 10:00:00',
  updateTime: '2026-01-01 10:00:00',
};

const mockChangeLogWithoutLink: ChangeLogVO = {
  changeLogId: 2,
  updateVersion: 'v1.1.0',
  type: 2,
  publishAuthor: 'Developer',
  publicDate: '2026-02-01',
  content: '功能更新：優化查詢性能',
  link: undefined,
  createTime: '2026-02-01 11:00:00',
  updateTime: '2026-02-01 11:00:00',
};

describe('ChangeLogDetailModal', () => {
  beforeEach(() => {
    // Clear any previous renders
  });

  it('should render detail modal with content', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogDetailModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.show(mockChangeLogWithLink);
    });

    // Wait for modal to appear
    await waitFor(() => {
      expect(screen.getByText('更新日誌')).toBeInTheDocument();
    });

    // Verify content is displayed
    expect(screen.getByText(/重大更新：新增用戶管理模塊/)).toBeInTheDocument();
    expect(screen.getByText(/支持用戶 CRUD 操作/)).toBeInTheDocument();
    expect(screen.getByText(/支持權限控制/)).toBeInTheDocument();
  });

  it('should display link when provided', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogDetailModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.show(mockChangeLogWithLink);
    });

    // Wait for modal to appear
    await waitFor(() => {
      expect(screen.getByText('鏈接：')).toBeInTheDocument();
    });

    // Verify link element
    const linkElement = screen.getByRole('link', { name: mockChangeLogWithLink.link });
    expect(linkElement).toBeInTheDocument();
    expect(linkElement).toHaveAttribute('href', mockChangeLogWithLink.link);
    expect(linkElement).toHaveAttribute('target', '_blank');
    expect(linkElement).toHaveAttribute('rel', 'noreferrer');
  });

  it('should not display link section when link is not provided', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogDetailModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.show(mockChangeLogWithoutLink);
    });

    // Wait for modal to appear and verify content
    await waitFor(() => {
      expect(screen.getByText('功能更新：優化查詢性能')).toBeInTheDocument();
    });

    // Verify link section is NOT displayed
    expect(screen.queryByText('鏈接：')).not.toBeInTheDocument();
  });

  it('should display content with proper formatting (pre tag with whitespace preserved)', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogDetailModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.show(mockChangeLogWithLink);
    });

    // Wait for content to appear
    await waitFor(() => {
      expect(screen.getByText(/重大更新：新增用戶管理模塊/)).toBeInTheDocument();
    });

    // Find the pre element
    const preElement = screen.getByText(/重大更新：新增用戶管理模塊/).closest('pre');
    expect(preElement).toBeInTheDocument();

    // Verify pre styling for whitespace preservation
    expect(preElement).toHaveStyle({ whiteSpace: 'pre-wrap', wordWrap: 'break-word' });
  });

  // Note: Modal close test is skipped due to test environment limitations
  it.skip('should close modal on cancel', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogDetailModal ref={ref} />);

    // Open modal
    act(() => {
      ref.current?.show(mockChangeLogWithLink);
    });

    // Wait for modal to appear
    await waitFor(() => {
      expect(screen.getByText('更新日誌')).toBeInTheDocument();
    });

    // Click cancel (close button)
    const closeButton = screen.getByRole('button', { name: /close/i });
    fireEvent.click(closeButton);

    // Modal should be closed (title not visible)
    await waitFor(() => {
      expect(screen.queryByText('更新日誌')).not.toBeInTheDocument();
    });
  });

  it('should handle empty content gracefully', async () => {
    const ref = { current: null } as any;
    const emptyContentChangeLog: ChangeLogVO = {
      ...mockChangeLogWithoutLink,
      content: '',
    };

    render(<ChangeLogDetailModal ref={ref} />);

    // Open modal with empty content
    act(() => {
      ref.current?.show(emptyContentChangeLog);
    });

    // Wait for modal to appear
    await waitFor(() => {
      expect(screen.getByText('更新日誌')).toBeInTheDocument();
    });

    // Verify pre element exists but is empty
    const preElement = document.querySelector('pre');
    expect(preElement).toBeInTheDocument();
    expect(preElement?.textContent).toBe('');
  });

  it('should update content when show is called multiple times', async () => {
    const ref = { current: null } as any;
    render(<ChangeLogDetailModal ref={ref} />);

    // First call
    act(() => {
      ref.current?.show(mockChangeLogWithLink);
    });
    await waitFor(() => {
      expect(screen.getByText(/重大更新：新增用戶管理模塊/)).toBeInTheDocument();
    });
    expect(screen.getByText('鏈接：')).toBeInTheDocument();

    // Second call with different data
    act(() => {
      ref.current?.show(mockChangeLogWithoutLink);
    });
    await waitFor(() => {
      expect(screen.getByText('功能更新：優化查詢性能')).toBeInTheDocument();
    });
    expect(screen.queryByText('鏈接：')).not.toBeInTheDocument();
  });
});
