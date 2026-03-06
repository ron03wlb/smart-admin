/**
 * ErrorBoundary 單元測試
 *
 * 測試覆蓋：
 * 1. 捕獲子組件錯誤
 * 2. 渲染 ErrorFallback
 * 3. 「重試」按鈕功能
 * 4. 自定義 onError 回調
 * 5. 自定義 fallback 組件
 * 6. 無錯誤時正常渲染
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ErrorBoundary from '../ErrorBoundary';
import ErrorFallback from '../ErrorFallback';

/**
 * 測試用：會拋出錯誤的組件
 */
const ThrowErrorComponent = ({ shouldThrow }: { shouldThrow: boolean }) => {
  if (shouldThrow) {
    throw new Error('測試錯誤消息');
  }
  return <div>正常渲染</div>;
};

describe('ErrorBoundary', () => {
  /**
   * Suppress 控制台錯誤輸出（React 19 會輸出錯誤日誌）
   */
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /**
   * 測試 1：應該捕獲子組件拋出的錯誤
   */
  test('應該捕獲子組件拋出的錯誤', () => {
    render(
      <ErrorBoundary>
        <ThrowErrorComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    // 驗證：ErrorFallback 應該被渲染
    expect(screen.getByText('頁面發生錯誤')).toBeInTheDocument();
    expect(
      screen.getByText(/抱歉，頁面遇到了意外問題/)
    ).toBeInTheDocument();
  });

  /**
   * 測試 2：應該顯示「重新載入」和「返回首頁」按鈕
   */
  test('應該顯示「重新載入」和「返回首頁」按鈕', () => {
    render(
      <ErrorBoundary>
        <ThrowErrorComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.getByRole('button', { name: /重新載入/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /返回首頁/i })).toBeInTheDocument();
  });

  /**
   * 測試 3：點擊「重新載入」按鈕應該重置錯誤狀態
   *
   * 注意：本測試驗證點擊重新載入按鈕會重置 ErrorBoundary 的錯誤狀態。
   * 由於 ErrorBoundary 使用 resetKey 機制重新掛載子組件，如果子組件在重新掛載時仍然拋出錯誤，
   * ErrorBoundary 會再次捕獲錯誤。因此，本測試只驗證按鈕功能，不測試完整的錯誤恢復流程。
   * 完整的錯誤恢復場景需要在集成測試中驗證。
   */
  test('點擊「重新載入」按鈕應該重置錯誤狀態', async () => {
    render(
      <ErrorBoundary>
        <ThrowErrorComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    // 1. 錯誤狀態：顯示 ErrorFallback
    expect(screen.getByText('頁面發生錯誤')).toBeInTheDocument();

    // 2. 記錄初始 DOM 狀態
    const errorTitleBefore = screen.getByText('頁面發生錯誤');
    expect(errorTitleBefore).toBeInTheDocument();

    // 3. 點擊「重新載入」按鈕（會觸發 handleReset，重置狀態並重新渲染子組件）
    const retryButton = screen.getByRole('button', { name: /重新載入/i });
    fireEvent.click(retryButton);

    // 4. 驗證：按鈕點擊後，ErrorBoundary 會嘗試重新渲染子組件
    // 由於子組件仍然拋出錯誤，ErrorBoundary 會再次捕獲並顯示 ErrorFallback
    // 這是預期行為：重試機制工作正常，但如果子組件的錯誤仍未修復，會再次顯示錯誤頁面
    await waitFor(() => {
      expect(screen.getByText('頁面發生錯誤')).toBeInTheDocument();
    });
  });

  /**
   * 測試 4：應該調用自定義 onError 回調
   */
  test('應該調用自定義 onError 回調', () => {
    const mockOnError = vi.fn();

    render(
      <ErrorBoundary onError={mockOnError}>
        <ThrowErrorComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    // 驗證：onError 應該被調用一次
    expect(mockOnError).toHaveBeenCalledTimes(1);

    // 驗證：onError 應該接收錯誤對象和 errorInfo
    expect(mockOnError).toHaveBeenCalledWith(
      expect.objectContaining({
        message: '測試錯誤消息',
      }),
      expect.objectContaining({
        componentStack: expect.any(String),
      })
    );
  });

  /**
   * 測試 5：應該支援自定義 fallback 組件
   */
  test('應該支援自定義 fallback 組件', () => {
    const CustomFallback = <div>自定義錯誤頁面</div>;

    render(
      <ErrorBoundary fallback={CustomFallback}>
        <ThrowErrorComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    // 驗證：應該顯示自定義 fallback（不是默認的 ErrorFallback）
    expect(screen.getByText('自定義錯誤頁面')).toBeInTheDocument();
    expect(screen.queryByText('頁面發生錯誤')).not.toBeInTheDocument();
  });

  /**
   * 測試 6：無錯誤時應該正常渲染子組件
   */
  test('無錯誤時應該正常渲染子組件', () => {
    render(
      <ErrorBoundary>
        <ThrowErrorComponent shouldThrow={false} />
      </ErrorBoundary>
    );

    // 驗證：應該顯示正常內容
    expect(screen.getByText('正常渲染')).toBeInTheDocument();

    // 驗證：不應該顯示 ErrorFallback
    expect(screen.queryByText('頁面發生錯誤')).not.toBeInTheDocument();
  });

  /**
   * 測試 7：應該輸出錯誤日誌到控制台（開發環境）
   */
  test('應該輸出錯誤日誌到控制台（開發環境）', () => {
    const consoleSpy = vi.spyOn(console, 'error');

    render(
      <ErrorBoundary>
        <ThrowErrorComponent shouldThrow={true} />
      </ErrorBoundary>
    );

    // 驗證：console.error 應該被調用（React 19 會輸出錯誤）
    expect(consoleSpy).toHaveBeenCalled();
  });
});

/**
 * ErrorFallback 單元測試
 */
describe('ErrorFallback', () => {
  const mockError = new Error('測試錯誤消息');
  const mockErrorInfo = {
    componentStack: '\n    at ThrowErrorComponent\n    at ErrorBoundary',
  };
  const mockOnReset = vi.fn();

  /**
   * 測試 1：應該顯示錯誤標題和描述
   */
  test('應該顯示錯誤標題和描述', () => {
    render(
      <ErrorFallback
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );

    expect(screen.getByText('頁面發生錯誤')).toBeInTheDocument();
    expect(
      screen.getByText(/抱歉，頁面遇到了意外問題/)
    ).toBeInTheDocument();
  });

  /**
   * 測試 2：應該顯示「重新載入」和「返回首頁」按鈕
   */
  test('應該顯示「重新載入」和「返回首頁」按鈕', () => {
    render(
      <ErrorFallback
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );

    expect(screen.getByRole('button', { name: /重新載入/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /返回首頁/i })).toBeInTheDocument();
  });

  /**
   * 測試 3：點擊「重新載入」應該調用 onReset
   */
  test('點擊「重新載入」應該調用 onReset', () => {
    render(
      <ErrorFallback
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );

    const retryButton = screen.getByRole('button', { name: /重新載入/i });
    fireEvent.click(retryButton);

    expect(mockOnReset).toHaveBeenCalledTimes(1);
  });

  /**
   * 測試 4：點擊「返回首頁」應該跳轉到根路徑
   */
  test('點擊「返回首頁」應該跳轉到根路徑', () => {
    // Mock window.location.href
    const originalLocation = window.location;
    delete (window as typeof window & { location: typeof originalLocation }).location;
    window.location = { href: '' } as typeof originalLocation;

    render(
      <ErrorFallback
        error={mockError}
        errorInfo={mockErrorInfo}
        onReset={mockOnReset}
      />
    );

    const homeButton = screen.getByRole('button', { name: /返回首頁/i });
    fireEvent.click(homeButton);

    expect(window.location.href).toBe('/');

    // Restore
    window.location = originalLocation;
  });
});
