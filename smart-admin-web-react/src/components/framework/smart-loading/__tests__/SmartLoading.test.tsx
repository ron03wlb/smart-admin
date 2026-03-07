import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';
import { createTestStore } from '@/test/utils/store-factory';
import SmartLoadingOverlay from '../SmartLoading';

describe('SmartLoadingOverlay', () => {
  it('should not render when loading is false', () => {
    const store = createTestStore({ spin: { loading: false } });
    const { container } = render(
      <Provider store={store}>
        <SmartLoadingOverlay />
      </Provider>,
    );
    expect(container.querySelector('.ant-spin')).toBeNull();
  });

  it('should render spinner when loading is true', () => {
    const store = createTestStore({ spin: { loading: true } });
    const { container } = render(
      <Provider store={store}>
        <SmartLoadingOverlay />
      </Provider>,
    );
    expect(container.querySelector('.ant-spin')).toBeTruthy();
  });
});
