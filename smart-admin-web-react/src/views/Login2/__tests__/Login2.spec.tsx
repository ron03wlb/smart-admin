import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { createTestStore } from '@/test/utils/store-factory';
import Login2 from '../Login2';

vi.mock('@/api/system/login.api', () => ({
  getCaptcha: vi.fn().mockResolvedValue({
    data: { captchaBase64Image: 'data:image/png;base64,abc', captchaUuid: 'uuid-1', expireSeconds: 120 },
  }),
  login: vi.fn(),
  sendLoginEmailCode: vi.fn(),
  getTwoFactorLoginFlag: vi.fn().mockResolvedValue({ data: false }),
}));

vi.mock('@/lib/encrypt', () => ({
  encryptData: vi.fn((v: string) => `enc_${v}`),
}));

function renderLogin2() {
  const store = createTestStore();
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/login2']}>
        <Login2 />
      </MemoryRouter>
    </Provider>
  );
}

describe('Login2', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render login form with welcome panel', async () => {
    renderLogin2();

    await waitFor(() => {
      expect(screen.getByText('账号登录')).toBeInTheDocument();
    });

    expect(screen.getByPlaceholderText('请输入用户名')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入密码')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入验证码')).toBeInTheDocument();
    expect(screen.getByText('记住密码')).toBeInTheDocument();
  });

  it('should render welcome text', async () => {
    renderLogin2();

    await waitFor(() => {
      expect(screen.getByText(/欢迎登录/)).toBeInTheDocument();
    });
  });

  it('should render QR code image', async () => {
    renderLogin2();

    await waitFor(() => {
      const qr = screen.getByAltText('QR');
      expect(qr).toBeInTheDocument();
    });
  });
});
