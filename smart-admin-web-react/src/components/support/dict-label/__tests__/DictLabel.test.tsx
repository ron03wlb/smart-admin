import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { createTestStore } from '@/test/utils/store-factory';
import DictLabel from '../DictLabel';

function renderWithStore(ui: React.ReactElement) {
  const store = createTestStore({
    dict: {
      dictList: [],
      dictMap: {
        GOODS_PLACE: [
          { dataValue: 'CN', dataLabel: '中国', disabledFlag: false },
          { dataValue: 'US', dataLabel: '美国', disabledFlag: false },
        ],
      },
    },
  });
  return render(<Provider store={store}>{ui}</Provider>);
}

describe('DictLabel', () => {
  it('should display label for a dict code and value', () => {
    renderWithStore(<DictLabel dictCode="GOODS_PLACE" dataValue="CN" />);
    expect(screen.getByText('中国')).toBeTruthy();
  });

  it('should display empty for unknown value', () => {
    const { container } = renderWithStore(<DictLabel dictCode="GOODS_PLACE" dataValue="XX" />);
    expect(container.querySelector('span')?.textContent).toBe('');
  });

  it('should handle null value', () => {
    const { container } = renderWithStore(<DictLabel dictCode="GOODS_PLACE" dataValue={null} />);
    expect(container.querySelector('span')?.textContent).toBe('');
  });
});
