import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { createTestStore } from '@/test/utils/store-factory';
import Login3 from '../Login3';

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

function renderLogin3() {
  const store = createTestStore();
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/login3']}>
        <Login3 />
      </MemoryRouter>
    </Provider>
  );
}

describe('Login3', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render login form', async () => {
    renderLogin3();

    await waitFor(() => {
      expect(screen.getByText('账号登录')).toBeInTheDocument();
    });

    expect(screen.getByPlaceholderText('请输入用户名')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入密码')).toBeInTheDocument();
  });

  it('should render welcome animation image', async () => {
    renderLogin3();

    await waitFor(() => {
      const gif = screen.getByAltText('Welcome Animation');
      expect(gif).toBeInTheDocument();
    });
  });

  it('should render blue capsule badge text', async () => {
    renderLogin3();

    await waitFor(() => {
      expect(screen.getByText(/高质量代码/)).toBeInTheDocument();
    });
  });
});
