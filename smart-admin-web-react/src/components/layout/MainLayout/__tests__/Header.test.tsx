/**
 * Header 單元測試
 *
 * 測試覆蓋：
 * 1. Header 正確渲染
 * 2. Logo 顯示正確文字
 * 3. Logo 點擊跳轉首頁
 * 4. 搜尋框存在且可輸入
 * 5. 搜尋功能（按 Enter 鍵）
 * 6. 通知 Badge 顯示正確數量
 * 7. 點擊通知圖標顯示消息
 * 8. 用戶名稱顯示正確
 * 9. 用戶下拉菜單包含「退出登錄」選項
 * 10. 點擊「退出登錄」調用 logout action 並跳轉
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

    // 驗證：Header 組件應該被渲染
    expect(screen.getByText('SmartAdmin')).toBeInTheDocument();
  });

  /**
   * 測試 2：應該顯示正確的 Logo 文字
   */
  test('應該顯示正確的 Logo 文字', () => {
    renderWithProviders(<Header />);

    // 驗證：Logo 文字應該是 "SmartAdmin"
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

    // 驗證：應該調用 navigate('/home')
    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });

  /**
   * 測試 4：應該顯示搜尋框
   */
  test('應該顯示搜尋框', () => {
    renderWithProviders(<Header />);

    // 驗證：搜尋框應該存在
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

    // 1. 輸入搜尋文字
    fireEvent.change(searchInput, { target: { value: '玩家管理' } });
    expect(searchInput).toHaveValue('玩家管理');

    // 2. 按 Enter 鍵觸發搜尋
    fireEvent.keyDown(searchInput, { key: 'Enter', code: 'Enter' });

    // 3. 驗證：應該顯示搜尋消息
    await waitFor(() => {
      expect(message.info).toHaveBeenCalledWith('搜尋: 玩家管理');
    });
  });

  /**
   * 測試 6：應該顯示通知 Badge
   */
  test('應該顯示通知 Badge', () => {
    renderWithProviders(<Header />);

    // 驗證：通知 Badge 應該顯示數量 5
    const badge = screen.getByText('5');
    expect(badge).toBeInTheDocument();
  });

  /**
   * 測試 7：點擊通知圖標應該顯示消息
   */
  test('點擊通知圖標應該顯示消息', async () => {
    const { message } = await import('antd');
    renderWithProviders(<Header />);

    // 1. 找到通知圖標（BellOutlined）
    const notificationIcon = document.querySelector('.header-action-icon');
    expect(notificationIcon).toBeInTheDocument();

    // 2. 點擊通知圖標
    if (notificationIcon) {
      fireEvent.click(notificationIcon);
    }

    // 3. 驗證：應該顯示通知消息
    await waitFor(() => {
      expect(message.info).toHaveBeenCalledWith('通知中心開發中...');
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

    // 驗證：應該顯示用戶名稱 "張三"
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

    // 驗證：應該顯示默認文字 "用戶"
    expect(screen.getByText('用戶')).toBeInTheDocument();
  });

  /**
   * 測試 10：用戶下拉菜單應該包含「退出登錄」選項
   */
  test('用戶下拉菜單應該包含「退出登錄」選項', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Header />, {
      preloadedState: {
        employeeName: '測試用戶',
      },
    });

    // 1. 找到用戶菜單區域
    const userMenu = document.querySelector('.header-user-menu');
    expect(userMenu).toBeInTheDocument();

    // 2. 點擊用戶菜單打開下拉菜單（使用 userEvent 模擬真實交互）
    if (userMenu) {
      await user.click(userMenu);
    }

    // 3. 等待下拉菜單渲染完成（增加 timeout）
    await waitFor(
      () => {
        expect(screen.getByText('退出登錄')).toBeInTheDocument();
      },
      { timeout: 3000 }
    );
  });

  /**
   * 測試 11：點擊「退出登錄」應該調用 logout action 並跳轉
   */
  test('點擊「退出登錄」應該調用 logout action 並跳轉', async () => {
    const user = userEvent.setup();
    const { message } = await import('antd');
    const { store } = renderWithProviders(<Header />, {
      preloadedState: {
        token: 'test-token',
        employeeId: 'test-employee-id',
        employeeName: '測試用戶',
      },
    });

    // 1. 打開用戶下拉菜單
    const userMenu = document.querySelector('.header-user-menu');
    if (userMenu) {
      await user.click(userMenu);
    }

    // 2. 等待下拉菜單渲染，然後點擊「退出登錄」
    await waitFor(
      () => {
        expect(screen.getByText('退出登錄')).toBeInTheDocument();
      },
      { timeout: 3000 }
    );

    const logoutButton = screen.getByText('退出登錄');
    await user.click(logoutButton);

    // 3. 驗證：Redux Store 應該清空用戶信息
    await waitFor(() => {
      const state = store.getState();
      expect(state.user.token).toBe('');
      expect(state.user.employeeName).toBe('');
    });

    // 4. 驗證：應該跳轉到登錄頁
    expect(mockNavigate).toHaveBeenCalledWith('/');

    // 5. 驗證：應該顯示成功消息
    expect(message.success).toHaveBeenCalledWith('已退出登錄');
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

    // 1. 輸入空白字符
    fireEvent.change(searchInput, { target: { value: '   ' } });

    // 2. 按 Enter 鍵觸發 onPressEnter
    fireEvent.keyDown(searchInput, { key: 'Enter', code: 'Enter', keyCode: 13 });

    // 3. 等待一小段時間，確保如果有異步操作也能捕獲
    await new Promise((resolve) => setTimeout(resolve, 100));

    // 4. 驗證：不應該調用 message.info
    expect(mockInfo).not.toHaveBeenCalled();
  });
});
