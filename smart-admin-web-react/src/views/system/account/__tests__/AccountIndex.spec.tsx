import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import AccountIndex from '../index';

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
    },
  }),
}));

vi.mock('@/api/system/employee-api', () => ({
  employeeApi: {
    updateCenter: vi.fn().mockResolvedValue({ code: 1, success: true }),
    updatePassword: vi.fn().mockResolvedValue({ code: 1, success: true }),
    query: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 } }),
    queryAll: vi.fn().mockResolvedValue({ code: 1, data: [] }),
  },
}));

vi.mock('@/api/support/login-log-api', () => ({
  loginLogApi: {
    queryListLogin: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 } }),
  },
}));

vi.mock('@/api/support/operate-log-api', () => ({
  operateLogApi: {
    queryListLogin: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 } }),
  },
}));

vi.mock('@/api/support/message-api', () => ({
  messageApi: {
    queryMessage: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 } }),
  },
}));

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
    });
  });

  it('should show AccountCenter by default', async () => {
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      // AccountCenter has a form with "登录账号" field
      expect(screen.getByText('登录账号')).toBeInTheDocument();
    });
  });

  it('should switch to password tab when clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AccountIndex />);

    await waitFor(() => {
      expect(screen.getByText('修改密码')).toBeInTheDocument();
    });

    await user.click(screen.getByText('修改密码'));

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

    await user.click(screen.getByText('我的消息'));

    await waitFor(() => {
      expect(screen.getByPlaceholderText('搜索标题/内容')).toBeInTheDocument();
    });
  });
});
