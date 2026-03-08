/**
 * Login Page Tests
 */
import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/slices/userSlice';
import appConfigReducer from '@/store/slices/appConfigSlice';
import Login from '../Login';

// Mock useLoginForm hook — form must be undefined so Ant Design creates its own internal FormInstance
vi.mock('@/hooks/useLoginForm', () => ({
  useLoginForm: () => ({
    form: undefined,
    loading: false,
    rememberPwd: false,
    setRememberPwd: vi.fn(),
    captchaImage: 'data:image/png;base64,test',
    captchaUuid: 'test-uuid',
    refreshCaptcha: vi.fn(),
    onLogin: vi.fn(),
    twoFactorEnabled: false,
    emailCodeTips: '获取验证码',
    emailCodeDisabled: false,
    sendEmailCode: vi.fn(),
  }),
}));

function renderLogin() {
  const store = configureStore({
    reducer: { user: userReducer, appConfig: appConfigReducer },
  });
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    </Provider>,
  );
}

describe('Login', () => {
  beforeEach(() => vi.clearAllMocks());

  it('should render login form', () => {
    renderLogin();
    expect(screen.getByPlaceholderText('请输入用户名')).toBeDefined();
    expect(screen.getByPlaceholderText('请输入密码')).toBeDefined();
    expect(screen.getByPlaceholderText('请输入验证码')).toBeDefined();
  });

  it('should render captcha image', () => {
    renderLogin();
    const captchaImg = screen.getByAltText('验证码');
    expect(captchaImg).toBeDefined();
  });

  it('should render remember password checkbox', () => {
    renderLogin();
    expect(screen.getByText('记住密码')).toBeDefined();
  });

  it('should render login button', () => {
    renderLogin();
    expect(screen.getByRole('button', { name: /登.*录/ })).toBeDefined();
  });

  it('should show app description', () => {
    renderLogin();
    expect(screen.getByText('SmartAdmin')).toBeDefined();
    expect(screen.getByText(/企业级/)).toBeDefined();
  });
});
