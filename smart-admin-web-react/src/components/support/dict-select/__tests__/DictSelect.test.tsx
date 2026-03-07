import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { createTestStore } from '@/test/utils/store-factory';
import DictSelect from '../DictSelect';

function renderWithStore(ui: React.ReactElement, storeOverrides?: any) {
  const store = createTestStore({
    dict: {
      dictList: [],
      dictMap: {
        GOODS_PLACE: [
          { dataValue: 'CN', dataLabel: '中国', disabledFlag: false },
          { dataValue: 'US', dataLabel: '美国', disabledFlag: false },
          { dataValue: 'JP', dataLabel: '日本', disabledFlag: true },
        ],
      },
    },
    ...storeOverrides,
  });
  return render(<Provider store={store}>{ui}</Provider>);
}

describe('DictSelect', () => {
  it('should render select component', () => {
    const { container } = renderWithStore(<DictSelect dictCode="GOODS_PLACE" />);
    expect(container.querySelector('.ant-select')).toBeTruthy();
  });

  it('should filter out disabled items', async () => {
    renderWithStore(<DictSelect dictCode="GOODS_PLACE" />);

    const select = document.querySelector('.ant-select-selector')!;
    await userEvent.click(select);

    // '日本' has disabledFlag=true so should not be in options
    expect(screen.queryByText('日本')).toBeNull();
    expect(await screen.findByText('中国')).toBeTruthy();
    expect(screen.getByText('美国')).toBeTruthy();
  });

  it('should hide options in hiddenOption list', async () => {
    renderWithStore(<DictSelect dictCode="GOODS_PLACE" hiddenOption={['US']} />);

    const select = document.querySelector('.ant-select-selector')!;
    await userEvent.click(select);

    expect(screen.queryByText('美国')).toBeNull();
    expect(await screen.findByText('中国')).toBeTruthy();
  });

  it('should call onChange when value changes', async () => {
    const onChange = vi.fn();
    renderWithStore(<DictSelect dictCode="GOODS_PLACE" onChange={onChange} />);

    const select = document.querySelector('.ant-select-selector')!;
    await userEvent.click(select);

    const option = await screen.findByText('中国');
    await userEvent.click(option);

    expect(onChange).toHaveBeenCalledWith('CN', expect.anything());
  });
});
