/**
 * TableOperator Component Tests
 * TableOperator 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import TableOperator from './index';
import type { TableOperatorButton } from './index';

// Mock PrivilegeButton component (both named and default exports)
vi.mock('@/components/PrivilegeButton', () => {
  const MockPrivilegeButton = ({ children }: { children: React.ReactNode }) => <div>{children}</div>;
  return {
    default: MockPrivilegeButton,
    PrivilegeButton: MockPrivilegeButton,
  };
});

describe('TableOperator', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('基本功能', () => {
    it('應該渲染組件', () => {
      const { container } = render(<TableOperator />);

      const operatorContainer = container.querySelector('.table-operator-container');
      expect(operatorContainer).toBeInTheDocument();
    });

    it('應該渲染左側按鈕區和右側工具區', () => {
      const { container } = render(<TableOperator />);

      const buttonsArea = container.querySelector('.table-operator-buttons');
      const toolbarArea = container.querySelector('.table-operator-toolbar');

      expect(buttonsArea).toBeInTheDocument();
      expect(toolbarArea).toBeInTheDocument();
    });
  });

  describe('預設按鈕類型', () => {
    it('應該渲染新建按鈕（add 類型）', () => {
      const handleClick = vi.fn();
      const buttons: TableOperatorButton[] = [
        {
          type: 'add',
          onClick: handleClick,
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const addButton = screen.getByRole('button', { name: /新建/i });
      expect(addButton).toBeInTheDocument();

      fireEvent.click(addButton);
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('應該渲染批量刪除按鈕（delete 類型）', () => {
      const handleClick = vi.fn();
      const buttons: TableOperatorButton[] = [
        {
          type: 'delete',
          onClick: handleClick,
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const deleteButton = screen.getByRole('button', { name: /批量刪除/i });
      expect(deleteButton).toBeInTheDocument();
      expect(deleteButton).toHaveClass('ant-btn-dangerous');
    });

    it('應該渲染導入按鈕（import 類型）', () => {
      const handleClick = vi.fn();
      const buttons: TableOperatorButton[] = [
        {
          type: 'import',
          onClick: handleClick,
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const importButton = screen.getByRole('button', { name: /導入/i });
      expect(importButton).toBeInTheDocument();
    });

    it('應該渲染導出按鈕（export 類型）', () => {
      const handleClick = vi.fn();
      const buttons: TableOperatorButton[] = [
        {
          type: 'export',
          onClick: handleClick,
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const exportButton = screen.getByRole('button', { name: /導出/i });
      expect(exportButton).toBeInTheDocument();
    });
  });

  describe('自定義按鈕', () => {
    it('應該支持自定義按鈕文本和圖標', () => {
      const handleClick = vi.fn();
      const buttons: TableOperatorButton[] = [
        {
          type: 'custom',
          text: '自定義按鈕',
          onClick: handleClick,
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const customButton = screen.getByRole('button', { name: /自定義按鈕/i });
      expect(customButton).toBeInTheDocument();
    });

    it('應該支持覆蓋預設按鈕文本', () => {
      const buttons: TableOperatorButton[] = [
        {
          type: 'add',
          text: '添加項目',
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const addButton = screen.getByRole('button', { name: /添加項目/i });
      expect(addButton).toBeInTheDocument();
    });

    it('應該支持禁用按鈕', () => {
      const buttons: TableOperatorButton[] = [
        {
          type: 'delete',
          disabled: true,
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const deleteButton = screen.getByRole('button', { name: /批量刪除/i });
      expect(deleteButton).toBeDisabled();
    });
  });

  describe('權限控制', () => {
    it('應該支持按鈕權限碼', () => {
      const buttons: TableOperatorButton[] = [
        {
          type: 'add',
          privilege: 'system:user:add',
        },
      ];

      const { container } = render(<TableOperator buttons={buttons} />);

      // PrivilegeButton 應該被渲染
      expect(container.querySelector('.table-operator-buttons')).toBeInTheDocument();
    });
  });

  describe('工具欄按鈕', () => {
    it('應該顯示刷新按鈕（預設）', () => {
      const handleRefresh = vi.fn();

      render(<TableOperator onRefresh={handleRefresh} />);

      const refreshButton = screen.getByTitle('刷新');
      expect(refreshButton).toBeInTheDocument();

      fireEvent.click(refreshButton);
      expect(handleRefresh).toHaveBeenCalledTimes(1);
    });

    it('應該隱藏刷新按鈕（showRefresh=false）', () => {
      render(<TableOperator showRefresh={false} />);

      const refreshButton = screen.queryByTitle('刷新');
      expect(refreshButton).not.toBeInTheDocument();
    });

    it('應該顯示全屏按鈕（showFullscreen=true）', () => {
      const handleFullscreenChange = vi.fn();

      render(<TableOperator showFullscreen onFullscreenChange={handleFullscreenChange} />);

      const fullscreenButton = screen.getByTitle('全屏');
      expect(fullscreenButton).toBeInTheDocument();

      fireEvent.click(fullscreenButton);
      expect(handleFullscreenChange).toHaveBeenCalledWith(true);
    });

    it('應該切換全屏按鈕圖標', () => {
      const handleFullscreenChange = vi.fn();

      const { rerender } = render(
        <TableOperator
          showFullscreen
          fullscreen={false}
          onFullscreenChange={handleFullscreenChange}
        />
      );

      let fullscreenButton = screen.getByTitle('全屏');
      expect(fullscreenButton).toBeInTheDocument();

      // 切換為全屏狀態
      rerender(
        <TableOperator
          showFullscreen
          fullscreen={true}
          onFullscreenChange={handleFullscreenChange}
        />
      );

      fullscreenButton = screen.getByTitle('退出全屏');
      expect(fullscreenButton).toBeInTheDocument();
    });

    it('應該顯示列設置按鈕（showColumnSetting=true）', () => {
      const handleColumnSettingClick = vi.fn();

      render(<TableOperator showColumnSetting onColumnSettingClick={handleColumnSettingClick} />);

      const columnSettingButton = screen.getByTitle('列設置');
      expect(columnSettingButton).toBeInTheDocument();

      fireEvent.click(columnSettingButton);
      expect(handleColumnSettingClick).toHaveBeenCalledTimes(1);
    });
  });

  describe('自定義工具欄', () => {
    it('應該支持自定義工具欄渲染', () => {
      const customToolbar = <button>自定義工具</button>;

      render(<TableOperator toolbarRender={customToolbar} />);

      const customButton = screen.getByRole('button', { name: /自定義工具/i });
      expect(customButton).toBeInTheDocument();
    });
  });

  describe('多按鈕組合', () => {
    it('應該支持多個按鈕組合', () => {
      const buttons: TableOperatorButton[] = [
        { type: 'add' },
        { type: 'delete' },
        { type: 'import' },
        { type: 'export' },
      ];

      render(<TableOperator buttons={buttons} />);

      expect(screen.getByRole('button', { name: /新建/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /批量刪除/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /導入/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /導出/i })).toBeInTheDocument();
    });

    it('應該支持所有工具按鈕組合', () => {
      render(<TableOperator showRefresh showFullscreen showColumnSetting />);

      expect(screen.getByTitle('刷新')).toBeInTheDocument();
      expect(screen.getByTitle('全屏')).toBeInTheDocument();
      expect(screen.getByTitle('列設置')).toBeInTheDocument();
    });
  });

  describe('邊界情況', () => {
    it('應該處理空按鈕數組', () => {
      const { container } = render(<TableOperator buttons={[]} />);

      const buttonsArea = container.querySelector('.table-operator-buttons');
      expect(buttonsArea).toBeInTheDocument();
      expect(buttonsArea?.textContent).toBe('');
    });

    it('應該處理未定義的回調函數', () => {
      const buttons: TableOperatorButton[] = [
        {
          type: 'add',
          // onClick 未定義
        },
      ];

      render(<TableOperator buttons={buttons} />);

      const addButton = screen.getByRole('button', { name: /新建/i });

      // 點擊不應該拋出錯誤
      expect(() => fireEvent.click(addButton)).not.toThrow();
    });
  });
});
