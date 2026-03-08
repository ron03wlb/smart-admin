/**
 * HomeHeader Tests
 */
import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { Provider } from 'react-redux';
import { createTestStore } from '@/test/utils/store-factory';
import HomeHeader from '../HomeHeader';

function renderHomeHeader(employeeName = '管理员') {
  const store = createTestStore({
    user: { employeeName },
  });
  return render(
    <Provider store={store}>
      <HomeHeader />
    </Provider>,
  );
}

describe('HomeHeader', () => {
  beforeEach(() => vi.clearAllMocks());

  it('should render greeting with employee name', () => {
    renderHomeHeader('张三');
    expect(screen.getByText(/张三/)).toBeDefined();
  });

  it('should display a time-based greeting', () => {
    renderHomeHeader();
    // One of these greetings should be present based on current hour
    const greetings = ['午夜好', '早上好', '中午好', '下午好', '晚上好'];
    const hasGreeting = greetings.some((g) => screen.queryByText(new RegExp(g)));
    expect(hasGreeting).toBe(true);
  });

  it('should display current date', () => {
    renderHomeHeader();
    const year = new Date().getFullYear().toString();
    expect(screen.getByText(new RegExp(year))).toBeDefined();
  });

  it('should display a motivational quote', () => {
    renderHomeHeader();
    // There's always a secondary text with a quote (the component always renders one)
    const secondaryTexts = document.querySelectorAll('.ant-typography-secondary');
    expect(secondaryTexts.length).toBeGreaterThan(0);
  });

  it('should display environment tag', () => {
    renderHomeHeader();
    expect(screen.getByText('本地開發')).toBeDefined();
  });
});
