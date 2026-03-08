/**
 * Home Page Tests
 */
import { render } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';

// Mock all child components to isolate Home layout testing
vi.mock('../components/HomeHeader', () => ({ default: () => <div data-testid="home-header">HomeHeader</div> }));
vi.mock('../components/HomeNotice', () => ({ default: ({ title }: { title: string }) => <div data-testid={`home-notice-${title}`}>{title}</div> }));
vi.mock('../components/charts/PieChart', () => ({ default: () => <div data-testid="pie-chart">PieChart</div> }));
vi.mock('../components/charts/CategoryChart', () => ({ default: () => <div data-testid="category-chart">CategoryChart</div> }));
vi.mock('../components/charts/GradientChart', () => ({ default: () => <div data-testid="gradient-chart">GradientChart</div> }));
vi.mock('../components/ToBeDoneCard', () => ({ default: () => <div data-testid="to-be-done">ToBeDoneCard</div> }));
vi.mock('../components/ChangelogCard', () => ({ default: () => <div data-testid="changelog">ChangelogCard</div> }));

import Home from '../Home';

describe('Home', () => {
  it('should render all dashboard sections', () => {
    const { getByTestId } = render(<Home />);
    expect(getByTestId('home-header')).toBeDefined();
    expect(getByTestId('home-notice-公告')).toBeDefined();
    expect(getByTestId('home-notice-通知')).toBeDefined();
    expect(getByTestId('pie-chart')).toBeDefined();
    expect(getByTestId('category-chart')).toBeDefined();
    expect(getByTestId('gradient-chart')).toBeDefined();
  });

  it('should render sidebar cards', () => {
    const { getByTestId } = render(<Home />);
    expect(getByTestId('changelog')).toBeDefined();
    expect(getByTestId('to-be-done')).toBeDefined();
  });

  it('should use responsive grid layout', () => {
    const { container } = render(<Home />);
    const cols = container.querySelectorAll('.ant-col');
    expect(cols.length).toBeGreaterThan(0);
  });
});
