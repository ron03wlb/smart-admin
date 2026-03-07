import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { createTestStore } from '@/test/utils/store-factory';
import HeaderSetting from '../HeaderSetting';

function renderWithProviders(visible: boolean) {
  const store = createTestStore();
  return render(
    <Provider store={store}>
      <HeaderSetting visible={visible} onClose={() => {}} />
    </Provider>,
  );
}

describe('HeaderSetting', () => {
  it('should not render drawer content when not visible', () => {
    renderWithProviders(false);
    expect(screen.queryByText('系统设置')).toBeNull();
  });

  it('should render drawer content when visible', () => {
    renderWithProviders(true);
    expect(screen.getByText('系统设置')).toBeTruthy();
    expect(screen.getByText('主题颜色')).toBeTruthy();
    expect(screen.getByText('布局')).toBeTruthy();
    expect(screen.getByText('暗黑模式')).toBeTruthy();
  });
});
