/**
 * ErrorBoundary 全局錯誤邊界組件
 *
 * 功能：
 * 1. 捕獲子組件樹中的 React 錯誤（componentDidCatch）
 * 2. 渲染 ErrorFallback 替代崩潰的組件
 * 3. 提供「重試」按鈕重新渲染
 * 4. 開發環境輸出完整錯誤堆棧，生產環境僅記錄
 *
 * 使用場景：
 * - App.tsx 全局包裹（捕獲所有未預期的錯誤）
 * - 高風險組件區域包裹（細粒度錯誤隔離）
 *
 * 技術約束：
 * - 必須使用 Class Component（React 19 無 Hook 替代）
 * - 無法捕獲異步錯誤（setTimeout, Promise.catch）
 * - 無法捕獲事件處理器錯誤（需手動 try-catch）
 *
 * @example
 * ```typescript
 * // 全局包裹
 * <ErrorBoundary>
 *   <App />
 * </ErrorBoundary>
 *
 * // 細粒度包裹
 * <ErrorBoundary>
 *   <RiskyComponent />
 * </ErrorBoundary>
 * ```
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import React, { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import ErrorFallback from './ErrorFallback';

interface ErrorBoundaryProps {
  /**
   * 子組件
   */
  children: ReactNode;

  /**
   * 自定義錯誤回調（可選，用於錯誤上報）
   *
   * @param error - 錯誤對象
   * @param errorInfo - React 錯誤信息（包含 componentStack）
   *
   * @example
   * ```typescript
   * const handleError = (error: Error, errorInfo: ErrorInfo) => {
   *   // 上報到 Sentry 或其他監控服務
   *   Sentry.captureException(error, { extra: errorInfo });
   * };
   *
   * <ErrorBoundary onError={handleError}>
   *   <App />
   * </ErrorBoundary>
   * ```
   */
  onError?: (error: Error, errorInfo: ErrorInfo) => void;

  /**
   * 自定義 Fallback 組件（可選）
   *
   * @example
   * ```typescript
   * <ErrorBoundary fallback={<CustomErrorPage />}>
   *   <App />
   * </ErrorBoundary>
   * ```
   */
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  /**
   * 是否發生錯誤
   */
  hasError: boolean;

  /**
   * 錯誤對象（用於顯示錯誤信息）
   */
  error: Error | null;

  /**
   * React 錯誤信息（包含 componentStack）
   */
  errorInfo: ErrorInfo | null;

  /**
   * 重置計數器（用於強制重新掛載子組件）
   */
  resetKey: number;
}

/**
 * ErrorBoundary 全局錯誤邊界組件
 */
class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      resetKey: 0,
    };
  }

  /**
   * 靜態方法：從錯誤派生狀態（getDerivedStateFromError）
   *
   * React 19 推薦使用此方法更新狀態，而非在 componentDidCatch 中調用 setState
   *
   * @param error - 錯誤對象
   * @returns 新的狀態對象
   */
  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    // 更新狀態以觸發 fallback UI 渲染
    return {
      hasError: true,
      error,
    };
  }

  /**
   * 生命週期方法：捕獲子組件錯誤（componentDidCatch）
   *
   * 用於錯誤日誌記錄和上報，不應在此方法中調用 setState
   *
   * @param error - 錯誤對象
   * @param errorInfo - React 錯誤信息（包含 componentStack）
   */
  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // 1. 更新 errorInfo 狀態（用於顯示 componentStack）
    this.setState({ errorInfo });

    // 2. 開發環境：輸出完整錯誤堆棧
    if (import.meta.env.DEV) {
      console.error('🔴 ErrorBoundary 捕獲到錯誤：', error);
      console.error('📍 組件堆棧：', errorInfo.componentStack);
    }

    // 3. 調用自定義錯誤回調（如有）
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }

    // 4. 生產環境：可擴展為 API 上報
    if (import.meta.env.PROD) {
      // 示例：上報到錯誤監控服務
      // errorReportingService.logError(error, errorInfo);
    }
  }

  /**
   * 重試處理函數：重置錯誤狀態，重新渲染子組件
   */
  handleReset = (): void => {
    this.setState((prevState) => ({
      hasError: false,
      error: null,
      errorInfo: null,
      resetKey: prevState.resetKey + 1,
    }));
  };

  render(): ReactNode {
    const { hasError, error, errorInfo, resetKey } = this.state;
    const { children, fallback } = this.props;

    if (hasError) {
      // 1. 如果提供了自定義 fallback，使用自定義 fallback
      if (fallback) {
        return fallback;
      }

      // 2. 否則使用默認的 ErrorFallback 組件
      return (
        <ErrorFallback
          error={error}
          errorInfo={errorInfo}
          onReset={this.handleReset}
        />
      );
    }

    // 3. 無錯誤時，正常渲染子組件（使用 key 強制重新掛載）
    return <React.Fragment key={resetKey}>{children}</React.Fragment>;
  }
}

export default ErrorBoundary;
