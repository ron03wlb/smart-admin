/**
 * ErrorFallback 錯誤提示頁面
 *
 * 功能：
 * 1. 顯示友好的錯誤提示 UI（Ant Design Result）
 * 2. 開發環境顯示完整錯誤堆棧（componentStack）
 * 3. 提供「重試」按鈕（重新渲染）
 * 4. 提供「返回首頁」按鈕（用戶逃生通道）
 *
 * 使用場景：
 * - ErrorBoundary 內部使用（不直接暴露給業務代碼）
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

import React from 'react';
import type { ErrorInfo } from 'react';
import { Result, Button, Typography, Collapse, Space } from 'antd';
import { HomeOutlined, ReloadOutlined } from '@ant-design/icons';
import './ErrorFallback.module.css';

const { Paragraph, Text } = Typography;

interface ErrorFallbackProps {
  /**
   * 錯誤對象
   */
  error: Error | null;

  /**
   * React 錯誤信息（包含 componentStack）
   */
  errorInfo: ErrorInfo | null;

  /**
   * 重試回調函數（由 ErrorBoundary 提供）
   */
  onReset: () => void;
}

/**
 * ErrorFallback 錯誤提示頁面
 *
 * @param props - ErrorFallbackProps
 * @returns React Element
 */
const ErrorFallback: React.FC<ErrorFallbackProps> = ({
  error,
  errorInfo,
  onReset,
}) => {
  /**
   * 處理返回首頁
   */
  const handleGoHome = (): void => {
    window.location.href = '/';
  };

  /**
   * 是否為開發環境
   */
  const isDev = import.meta.env.DEV;

  return (
    <div className="error-fallback-container">
      <Result
        status="error"
        title="頁面發生錯誤"
        subTitle="抱歉，頁面遇到了意外問題。您可以嘗試重新載入，或返回首頁繼續使用。"
        extra={
          <Space>
            <Button
              type="primary"
              icon={<ReloadOutlined />}
              onClick={onReset}
            >
              重新載入
            </Button>
            <Button icon={<HomeOutlined />} onClick={handleGoHome}>
              返回首頁
            </Button>
          </Space>
        }
      >
        {/* 開發環境：顯示錯誤詳情 */}
        {isDev && error && (
          <div className="error-details">
            <Collapse
              defaultActiveKey={['1']}
              ghost
              items={[
                {
                  key: '1',
                  label: '錯誤詳情（開發模式）',
                  children: (
                    <>
                      <Paragraph>
                        <Text strong>錯誤消息：</Text>
                      </Paragraph>
                      <Paragraph
                        code
                        copyable
                        style={{
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                        }}
                      >
                        {error.message}
                      </Paragraph>

                      <Paragraph>
                        <Text strong>錯誤堆棧：</Text>
                      </Paragraph>
                      <Paragraph
                        code
                        copyable
                        style={{
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                          maxHeight: '200px',
                          overflow: 'auto',
                        }}
                      >
                        {error.stack}
                      </Paragraph>

                      {errorInfo && errorInfo.componentStack && (
                        <>
                          <Paragraph>
                            <Text strong>組件堆棧：</Text>
                          </Paragraph>
                          <Paragraph
                            code
                            copyable
                            style={{
                              whiteSpace: 'pre-wrap',
                              wordBreak: 'break-word',
                              maxHeight: '200px',
                              overflow: 'auto',
                            }}
                          >
                            {errorInfo.componentStack}
                          </Paragraph>
                        </>
                      )}
                    </>
                  ),
                },
              ]}
            />
          </div>
        )}
      </Result>
    </div>
  );
};

export default ErrorFallback;
