/**
 * Code Generator Tests
 * 代碼生成器測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import CodeGeneratorPage from './index';

// Mock API
vi.mock('@/api/support/codeGeneratorApi', () => ({
  codeGeneratorApi: {
    queryTableList: vi.fn().mockResolvedValue({
      data: {
        list: [
          {
            tableName: 't_user',
            tableComment: '用戶表',
            configTime: '2026-03-15 10:00:00',
          },
          {
            tableName: 't_role',
            tableComment: '角色表',
            configTime: '2026-03-15 11:00:00',
          },
        ],
        total: 2,
      },
    }),
    getTableColumns: vi.fn().mockResolvedValue({ data: [] }),
    getConfig: vi.fn().mockResolvedValue({ data: {} }),
    updateConfig: vi.fn().mockResolvedValue({ data: null }),
    preview: vi.fn().mockResolvedValue({ data: '// Generated code' }),
    downloadCode: vi.fn(),
  },
}));

describe('CodeGeneratorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('應該正確渲染頁面', async () => {
    render(
      <BrowserRouter>
        <CodeGeneratorPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      // 檢查是否有查詢表單
      expect(screen.getByPlaceholderText('請輸入表名關鍵字')).toBeInTheDocument();
      expect(screen.getByText('查詢')).toBeInTheDocument();
      expect(screen.getByText('重置')).toBeInTheDocument();
    });
  });

  it('應該顯示表格數據', async () => {
    render(
      <BrowserRouter>
        <CodeGeneratorPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('t_user')).toBeInTheDocument();
      expect(screen.getByText('用戶表')).toBeInTheDocument();
      expect(screen.getByText('t_role')).toBeInTheDocument();
      expect(screen.getByText('角色表')).toBeInTheDocument();
    });
  });

  it('應該顯示操作按鈕', async () => {
    render(
      <BrowserRouter>
        <CodeGeneratorPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      const configButtons = screen.getAllByText('代碼配置');
      expect(configButtons.length).toBeGreaterThan(0);

      const previewButtons = screen.getAllByText('代碼預覽');
      expect(previewButtons.length).toBeGreaterThan(0);

      const downloadButtons = screen.getAllByText('下載代碼');
      expect(downloadButtons.length).toBeGreaterThan(0);
    });
  });
});
