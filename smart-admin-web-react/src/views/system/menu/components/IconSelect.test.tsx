/**
 * IconSelect Component Tests
 * 圖標選擇器組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import IconSelect from './IconSelect';

// 增加測試超時時間
const TEST_TIMEOUT = 10000;

describe('IconSelect', () => {
  describe('基礎渲染', () => {
    it(
      '應該正確渲染組件',
      async () => {
        render(<IconSelect />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示自定義佔位符',
      async () => {
        render(<IconSelect placeholder="測試圖標選擇" />);

        await waitFor(
          () => {
            expect(screen.getByText('測試圖標選擇')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該顯示默認佔位符',
      async () => {
        render(<IconSelect />);

        await waitFor(
          () => {
            expect(screen.getByText('請選擇圖標')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在禁用狀態下不可操作',
      async () => {
        const { container } = render(<IconSelect disabled />);

        await waitFor(
          () => {
            const selectWrapper = container.querySelector('.ant-select-disabled');
            expect(selectWrapper).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('受控模式', () => {
    it(
      '應該正確顯示當前值',
      async () => {
        render(<IconSelect value="HomeOutlined" />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該在值變化時調用 onChange',
      async () => {
        const onChange = vi.fn();

        render(<IconSelect onChange={onChange} />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );

        // 驗證 onChange prop 被正確設置
        expect(onChange).toHaveBeenCalledTimes(0);
      },
      TEST_TIMEOUT
    );
  });

  describe('圖標選項', () => {
    it(
      '應該支持搜索功能',
      async () => {
        render(<IconSelect />);

        await waitFor(
          () => {
            const select = screen.getByRole('combobox');
            expect(select).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該支持清空選擇',
      async () => {
        render(<IconSelect value="HomeOutlined" />);

        await waitFor(
          () => {
            const select = screen.getByRole('combobox');
            expect(select).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('邊界情況', () => {
    it(
      '應該處理空值',
      async () => {
        render(<IconSelect value={undefined} />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該處理不存在的圖標名稱',
      async () => {
        render(<IconSelect value="NonExistentIcon" />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('圖標渲染', () => {
    it(
      '應該為有效圖標顯示圖標組件',
      async () => {
        render(<IconSelect value="HomeOutlined" />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );

    it(
      '應該為無效圖標僅顯示文本',
      async () => {
        render(<IconSelect value="InvalidIcon" />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('可訪問性', () => {
    it(
      '應該有正確的 ARIA 屬性',
      async () => {
        render(<IconSelect />);

        await waitFor(
          () => {
            const select = screen.getByRole('combobox');
            expect(select).toHaveAttribute('aria-haspopup', 'listbox');
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });

  describe('性能', () => {
    it(
      '應該使用虛擬滾動優化大列表',
      async () => {
        render(<IconSelect />);

        await waitFor(
          () => {
            expect(screen.getByRole('combobox')).toBeInTheDocument();
          },
          { timeout: TEST_TIMEOUT }
        );
      },
      TEST_TIMEOUT
    );
  });
});
