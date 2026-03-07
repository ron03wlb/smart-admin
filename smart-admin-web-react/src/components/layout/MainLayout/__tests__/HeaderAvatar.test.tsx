import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { createTestStore } from '@/test/utils/store-factory';
import HeaderAvatar from '../HeaderAvatar';

// Mock employee API for ChangePasswordModal
vi.mock('@/api/system/employee-api', () => ({
  employeeApi: {
    update: vi.fn().mockResolvedValue({ code: 1 }),
    queryAll: vi.fn().mockResolvedValue({ code: 1, data: [] }),
  },
}));

function renderWithProviders() {
  const store = createTestStore({
    user: { employeeName: '张三' },
  });
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <HeaderAvatar />
      </MemoryRouter>
    </Provider>,
  );
}

describe('HeaderAvatar', () => {
  it('should display employee name', () => {
    renderWithProviders();
    expect(screen.getByText('张三')).toBeTruthy();
  });

  it('should display first character as avatar', () => {
    renderWithProviders();
    expect(screen.getByText('张')).toBeTruthy();
  });
});
