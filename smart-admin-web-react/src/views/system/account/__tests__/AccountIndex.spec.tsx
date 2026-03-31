/**
 * AccountIndex Spec Tests
 * 個人中心主頁面測試
 *
 * Now tests against Account* full implementations (not placeholders).
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

// Mock login API (used by AccountCenter)
vi.mock('@/api/system/login.api', () => ({
  getLoginInfo: vi.fn().mockResolvedValue({
    code: 1,
    data: {
      loginName: 'admin',
      departmentName: '技术部',
      actualName: '管理员',
      gender: 1,
      phone: '13800138000',
      email: 'admin@test.com',
      positionId: 1,
      remark: '',
      avatar: '',
    },
    msg: 'Success',
    success: true,
  }),
}));

// Mock employee-api (used by AccountCenter, AccountPassword)
vi.mock('@/api/system/employee-api', () => ({
  employeeApi: {
    updateCenter: vi.fn().mockResolvedValue({ code: 1, data: null, msg: 'Success', success: true }),
    updateAvatar: vi.fn().mockResolvedValue({ code: 1, data: null, msg: 'Success', success: true }),
    updatePassword: vi.fn().mockResolvedValue({ code: 1, data: null, msg: 'Success', success: true }),
    getPasswordComplexityEnabled: vi.fn().mockResolvedValue({ code: 1, data: false, msg: 'Success', success: true }),
  },
}));

// Mock file-api (used by AccountCenter avatar upload)
vi.mock('@/api/support/file-api', () => ({
  fileApi: {
    uploadFile: vi.fn().mockResolvedValue({ code: 1, data: { fileKey: 'key', fileUrl: 'url' }, msg: 'Success', success: true }),
  },
}));

// Mock message-api (used by AccountMessage)
vi.mock('@/api/support/message-api', () => ({
  messageApi: {
    queryMessage: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 }, msg: 'Success', success: true }),
    read: vi.fn().mockResolvedValue({ code: 1, data: null, msg: 'Success', success: true }),
  },
}));

// Mock login-log-api (used by AccountLoginLog)
vi.mock('@/api/support/login-log-api', () => ({
  loginLogApi: {
    queryListLogin: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 }, msg: 'Success', success: true }),
  },
}));

// Mock operate-log-api (used by AccountOperateLog)
vi.mock('@/api/support/operate-log-api', () => ({
  operateLogApi: {
    queryListLogin: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 }, msg: 'Success', success: true }),
  },
}));

// Mock notice-api (used by AccountNotice -> NoticeEmployeeList)
vi.mock('@/api/business/oa/notice-api', () => ({
  noticeApi: {
    queryEmployeeNotice: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 }, msg: 'Success', success: true }),
  },
}));

// Mock mfa-api (used by AccountMfa)
vi.mock('@/api/system/mfa-api', () => ({
  mfaApi: {
    getStatus: vi.fn().mockResolvedValue({ code: 1, data: { mfaEnabled: false }, msg: 'Success', success: true }),
    getBackupCodeCount: vi.fn().mockResolvedValue({ code: 1, data: 0, msg: 'Success', success: true }),
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
        employeeName: '管理员',
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
      expect(screen.getByText('个人中心')).toBeInTheDocument();
      expect(screen.getByText('修改密码')).toBeInTheDocument();
      expect(screen.getByText('我的消息')).toBeInTheDocument();
      expect(screen.getByText('登录日志')).toBeInTheDocument();
      expect(screen.getByText('操作日志')).toBeInTheDocument();
      expect(screen.getByText('多因素认证')).toBeInTheDocument();
    });
  });

  it('should show AccountCenter by default', async () => {
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      // AccountCenter has form fields populated from getLoginInfo
      expect(screen.getByText('登录账号')).toBeInTheDocument();
    }, { timeout: 5000 });
  });

  it('should switch to password tab when clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      expect(screen.getByText('修改密码')).toBeInTheDocument();
    });

    // Click the menu item
    const menuItems = screen.getAllByText('修改密码');
    await user.click(menuItems[0]);

    await waitFor(() => {
      expect(screen.getByText('原密码')).toBeInTheDocument();
      expect(screen.getByText('新密码')).toBeInTheDocument();
      expect(screen.getByText('确认密码')).toBeInTheDocument();
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
      // AccountMessage shows a table with search input
      expect(screen.getByPlaceholderText('搜索标题/内容')).toBeInTheDocument();
    });
  });
});
