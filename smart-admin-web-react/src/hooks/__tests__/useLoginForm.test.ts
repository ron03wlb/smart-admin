import { renderHook, act } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';

// Mock dependencies before importing the hook
vi.mock('@/api/system/login.api', () => ({
  getCaptcha: vi.fn().mockResolvedValue({
    data: { captchaBase64Image: 'data:image/png;base64,abc', captchaUuid: 'uuid-123', expireSeconds: 120 },
  }),
  login: vi.fn().mockResolvedValue({
    data: { token: 'token-abc', employeeId: '1', employeeName: 'Admin', administratorFlag: true, menuList: [] },
  }),
  sendLoginEmailCode: vi.fn().mockResolvedValue({ data: null }),
  getTwoFactorLoginFlag: vi.fn().mockResolvedValue({ data: false }),
}));

vi.mock('@/lib/encrypt', () => ({
  encryptData: vi.fn((data: string) => `encrypted_${data}`),
}));

vi.mock('@/store/hooks', () => ({
  useAppDispatch: () => vi.fn(),
}));

vi.mock('@/store/slices/userSlice', () => ({
  setUserLoginInfo: vi.fn((data: unknown) => ({ type: 'user/setUserLoginInfo', payload: data })),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    message: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  };
});

import { useLoginForm } from '../useLoginForm';
import { getCaptcha, getTwoFactorLoginFlag, sendLoginEmailCode } from '@/api/system/login.api';
import { message } from 'antd';

describe('useLoginForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    localStorage.clear();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should load captcha on mount', async () => {
    let hookResult: { current: ReturnType<typeof useLoginForm> };
    await act(async () => {
      const { result } = renderHook(() => useLoginForm());
      hookResult = result;
    });

    expect(getCaptcha).toHaveBeenCalled();
    expect(hookResult!.current.captchaImage).toBe('data:image/png;base64,abc');
    expect(hookResult!.current.captchaUuid).toBe('uuid-123');
  });

  it('should check 2FA flag on mount', async () => {
    await act(async () => {
      renderHook(() => useLoginForm());
    });

    expect(getTwoFactorLoginFlag).toHaveBeenCalled();
  });

  it('should have twoFactorEnabled=false by default', async () => {
    let hookResult: { current: ReturnType<typeof useLoginForm> };
    await act(async () => {
      const { result } = renderHook(() => useLoginForm());
      hookResult = result;
    });

    expect(hookResult!.current.twoFactorEnabled).toBe(false);
  });

  it('should refresh captcha when refreshCaptcha called', async () => {
    let hookResult: { current: ReturnType<typeof useLoginForm> };
    await act(async () => {
      const { result } = renderHook(() => useLoginForm());
      hookResult = result;
    });

    vi.mocked(getCaptcha).mockResolvedValueOnce({
      data: { captchaBase64Image: 'data:image/png;base64,new', captchaUuid: 'uuid-456', expireSeconds: 60 },
    } as any);

    await act(async () => {
      await hookResult!.current.refreshCaptcha();
    });

    expect(hookResult!.current.captchaImage).toBe('data:image/png;base64,new');
    expect(hookResult!.current.captchaUuid).toBe('uuid-456');
  });

  it('should warn when sending email code without loginName', async () => {
    let hookResult: { current: ReturnType<typeof useLoginForm> };
    await act(async () => {
      const { result } = renderHook(() => useLoginForm());
      hookResult = result;
    });

    await act(async () => {
      await hookResult!.current.sendEmailCode();
    });

    expect(message.warning).toHaveBeenCalledWith('请先输入用户名');
    expect(sendLoginEmailCode).not.toHaveBeenCalled();
  });

  it('should toggle rememberPwd state', async () => {
    let hookResult: { current: ReturnType<typeof useLoginForm> };
    await act(async () => {
      const { result } = renderHook(() => useLoginForm());
      hookResult = result;
    });

    expect(hookResult!.current.rememberPwd).toBe(false);

    act(() => {
      hookResult!.current.setRememberPwd(true);
    });

    expect(hookResult!.current.rememberPwd).toBe(true);
  });
});
