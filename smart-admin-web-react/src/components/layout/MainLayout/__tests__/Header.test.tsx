/**
 * Header 單元測試
 *
 * 測試覆蓋：
 * 1. Header 正確渲染
 * 2. Logo 顯示正確文字
 * 3. Logo 點擊跳轉首頁
 * 4. 搜尋框存在且可輸入
 * 5. 搜尋功能（按 Enter 鍵）
 * 6. 消息通知圖標渲染（HeaderMessage 整合）
 * 7. 設置圖標點擊打開設置抽屜（HeaderSetting 整合）
 * 8. 用戶名稱顯示正確
 * 9. 用戶名稱為空時顯示默認文字
 * 10. 用戶下拉菜單包含退出登錄選項
 * 11. 點擊退出登錄調用 logout action 並跳轉
 * 12. 空搜尋不觸發搜尋
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import { describe, test, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/utils/test-utils';
import Header from '../Header';

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      info: vi.fn(),
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  };
});

// Mock message API (used by HeaderMessage)
vi.mock('@/api/support/message-api', () => ({
  messageApi: {
    getUnreadCount: vi.fn().mockResolvedValue({ code: 1, data: 3 }),
    queryMessage: vi.fn().mockResolvedValue({ code: 1, data: { list: [] } }),
  },
}));

// Mock login API (used by logout thunk)
vi.mock('@/api/system/loginApi', () => ({
  loginApi: {
    login: vi.fn().mockResolvedValue({ ok: true, data: {} }),
    logout: vi.fn().mockResolvedValue({ ok: true, data: null, msg: '', code: 200 }),
    getLoginInfo: vi.fn().mockResolvedValue({ ok: true, data: {} }),
  },
}));

// Mock employee API (used by ChangePasswordModal in HeaderAvatar)
vi.mock('@/api/system/employee-api', () => ({
  employeeApi: {
    update: vi.fn().mockResolvedValue({ code: 1 }),
    queryAll: vi.fn().mockResolvedValue({ code: 1, data: [] }),
  },
}));

describe('Header', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  /**
   * 測試 1：應該正確渲染 Header 組件
   */
  test('應該正確渲染 Header 組件', () => {
    renderWithProviders(<Header />, {
      preloadedState: {
        employeeName: '測試用戶',
      },
    });

    expect(screen.getByText('SmartAdmin')).toBeInTheDocument();
  });

  /**
   * 測試 2：應該顯示正確的 Logo 文字
   */
  test('應該顯示正確的 Logo 文字', () => {
    renderWithProviders(<Header />);

    const logo = screen.getByText('SmartAdmin');
    expect(logo).toBeInTheDocument();
  });

  /**
   * 測試 3：點擊 Logo 應該跳轉到首頁
   */
  test('點擊 Logo 應該跳轉到首頁', () => {
    renderWithProviders(<Header />);

    const logo = screen.getByText('SmartAdmin');
    fireEvent.click(logo);

    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });

  /**
   * 測試 4：應該顯示搜尋框
   */
  test('應該顯示搜尋框', () => {
    renderWithProviders(<Header />);

    const searchInput = screen.getByPlaceholderText('搜尋菜單、功能...');
    expect(searchInput).toBeInTheDocument();
  });

  /**
   * 測試 5：搜尋框應該可以輸入文字並觸發搜尋
   */
  test('搜尋框應該可以輸入文字並觸發搜尋', async () => {
    const { message } = await import('antd');
    renderWithProviders(<Header />);

    const searchInput = screen.getByPlaceholderText('搜尋菜單、功能...');

    fireEvent.change(searchInput, { target: { value: '玩家管理' } });
    expect(searchInput).toHaveValue('玩家管理');

    fireEvent.keyDown(searchInput, { key: 'Enter', code: 'Enter' });

    await waitFor(() => {
      expect(message.info).toHaveBeenCalledWith('搜尋: 玩家管理');
    });
  });

  /**
   * 測試 6：應該渲染消息通知圖標（HeaderMessage 整合）
   */
  test('應該渲染消息通知圖標', () => {
    renderWithProviders(<Header />);

    const bellIcon = screen.getByRole('img', { name: 'bell' });
    expect(bellIcon).toBeInTheDocument();
  });

  /**
   * 測試 7：點擊設置圖標應該打開設置抽屜（HeaderSetting 整合）
   */
  test('點擊設置圖標應該打開設置抽屜', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Header />);

    const settingIcon = screen.getByRole('img', { name: 'setting' });
    await user.click(settingIcon);

    await waitFor(() => {
      expect(screen.getByText('系统设置')).toBeInTheDocument();
    });
  });

  /**
   * 測試 8：應該顯示用戶名稱
   */
  test('應該顯示用戶名稱', () => {
    renderWithProviders(<Header />, {
      preloadedState: {
        employeeName: '張三',
      },
    });

    expect(screen.getByText('張三')).toBeInTheDocument();
  });

  /**
   * 測試 9：用戶名稱為空時應該顯示默認文字
   */
  test('用戶名稱為空時應該顯示默認文字', () => {
    renderWithProviders(<Header />, {
      preloadedState: {
        employeeName: '',
      },
    });

    // HeaderAvatar uses simplified Chinese '用户'
    expect(screen.getByText('用户')).toBeInTheDocument();
  });

  /**
   * 測試 10：用戶下拉菜單應該包含退出登錄選項（HeaderAvatar 整合）
   */
  test('用戶下拉菜單應該包含退出登錄選項', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Header />, {
      preloadedState: {
        employeeName: '測試用戶',
      },
    });

    // Hover on avatar area to trigger Dropdown
    const avatarTrigger = screen.getByText('測試用戶');
    await user.hover(avatarTrigger);

    // Wait for dropdown menu to render (simplified Chinese)
    await waitFor(
      () => {
        expect(screen.getByText('退出登录')).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });

  /**
   * 測試 11：點擊退出登錄應該調用 logout action 並跳轉
   */
  test('點擊退出登錄應該調用 logout action 並跳轉', async () => {
    const user = userEvent.setup();
    const { store } = renderWithProviders(<Header />, {
      preloadedState: {
        token: 'test-token',
        employeeId: 'test-employee-id',
        employeeName: '測試用戶',
      },
    });

    // 1. Hover to open avatar dropdown
    const avatarTrigger = screen.getByText('測試用戶');
    await user.hover(avatarTrigger);

    // 2. Wait for dropdown and click logout
    await waitFor(
      () => {
        expect(screen.getByText('退出登录')).toBeInTheDocument();
      },
      { timeout: 3000 }
    );

    const logoutButton = screen.getByText('退出登录');
    await user.click(logoutButton);

    // 3. Verify Redux Store cleared
    await waitFor(() => {
      const state = store.getState();
      expect(state.user.token).toBe('');
      expect(state.user.employeeName).toBe('');
    });

    // 4. Verify navigation to login page
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  /**
   * 測試 12：空搜尋不應該觸發搜尋
   */
  test('空搜尋不應該觸發搜尋', async () => {
    const { message } = await import('antd');
    const mockInfo = vi.fn();
    message.info = mockInfo;

    renderWithProviders(<Header />);

    const searchInput = screen.getByPlaceholderText('搜尋菜單、功能...');

    fireEvent.change(searchInput, { target: { value: '   ' } });
    fireEvent.keyDown(searchInput, { key: 'Enter', code: 'Enter', keyCode: 13 });

    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(mockInfo).not.toHaveBeenCalled();
  });
});
