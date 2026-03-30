/**
 * AccountIndex Spec Tests
 * 個人中心主頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { BrowserRouter } from 'react-router-dom';
import AccountIndex from '../index';
import userReducer from '@/store/slices/userSlice';

// Mock employeeApi (used by Center and Password)
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    getEmployee: vi.fn().mockResolvedValue({
      ok: true,
      code: 200,
      msg: 'Success',
      data: {
        employeeId: 1,
        loginName: 'admin',
        departmentId: 1,
        actualName: '管理員',
        gender: 1,
        phone: '13800138000',
        email: 'admin@test.com',
        positionId: 1,
        remark: '',
      },
    }),
    updateEmployee: vi.fn().mockResolvedValue({ ok: true, code: 200, msg: 'Success', data: undefined }),
    getPasswordComplexityEnabled: vi.fn().mockResolvedValue({ ok: true, code: 200, msg: 'Success', data: false }),
    updateEmployeePassword: vi.fn().mockResolvedValue({ ok: true, code: 200, msg: 'Success', data: undefined }),
  },
}));

/**
 * Helper: render with Redux store + BrowserRouter
 */
function renderWithProviders(ui: React.ReactElement) {
  const store = configureStore({
    reducer: {
      user: userReducer as any,
    },
    preloadedState: {
      user: {
        token: 'test-token',
        employeeId: '1',
        employeeName: '管理員',
        loginName: 'admin',
        administratorFlag: true,
        menuTree: [],
        displayMenuTree: [],
        pointsList: [],
        menuRouterList: [],
        menuParentIdListMap: {},
        unreadMessageCount: 0,
        loading: false,
        error: null,
      },
    },
  });

  return render(
    <Provider store={store}>
      <BrowserRouter>{ui}</BrowserRouter>
    </Provider>
  );
}

describe('AccountIndex', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should display left menu with all items', async () => {
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      expect(screen.getByText('個人中心')).toBeInTheDocument();
      expect(screen.getByText('修改密碼')).toBeInTheDocument();
      expect(screen.getByText('我的消息')).toBeInTheDocument();
      expect(screen.getByText('登錄日誌')).toBeInTheDocument();
      expect(screen.getByText('操作日誌')).toBeInTheDocument();
    });
  });

  it('should show AccountCenter by default', async () => {
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      // Center component has a form with "登錄賬號" field
      expect(screen.getByText('登錄賬號')).toBeInTheDocument();
    });
  });

  it('should switch to password tab when clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      expect(screen.getByText('修改密碼')).toBeInTheDocument();
    });

    // Click the menu item (in the left nav)
    const menuItems = screen.getAllByText('修改密碼');
    await user.click(menuItems[0]);

    await waitFor(() => {
      expect(screen.getByText('原密碼')).toBeInTheDocument();
      expect(screen.getByText('新密碼')).toBeInTheDocument();
      expect(screen.getByText('確認密碼')).toBeInTheDocument();
    });
  });

  it('should switch to message tab when clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      expect(screen.getByText('我的消息')).toBeInTheDocument();
    });

    // Click the menu item
    const menuItems = screen.getAllByText('我的消息');
    await user.click(menuItems[0]);

    await waitFor(() => {
      // Message component shows a Result with title "我的消息"
      // and a button "前往消息管理"
      expect(screen.getByText('前往消息管理')).toBeInTheDocument();
    });
  });
});
