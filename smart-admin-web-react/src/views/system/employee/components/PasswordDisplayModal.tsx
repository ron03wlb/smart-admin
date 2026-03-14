/**
 * Password Display Modal Component
 * 密碼顯示 Modal 組件
 *
 * 用於顯示新建員工或重置密碼後的密碼信息
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/employee/components/employee-password-dialog/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useState } from 'react';
import { Modal, Typography, Space, Button, message } from 'antd';
import { CopyOutlined, CheckCircleOutlined } from '@ant-design/icons';

const { Text, Title } = Typography;

/**
 * 密碼顯示 Modal Props
 */
export interface PasswordDisplayModalProps {
  /**
   * Modal 顯示狀態
   */
  visible: boolean;
  /**
   * 登錄名
   */
  loginName: string;
  /**
   * 密碼
   */
  password: string;
  /**
   * 關閉 Modal 的回調
   */
  onClose: () => void;
}

/**
 * 密碼顯示 Modal 組件
 */
export const PasswordDisplayModal: React.FC<PasswordDisplayModalProps> = ({
  visible,
  loginName,
  password,
  onClose,
}) => {
  const [copied, setCopied] = useState(false);

  /**
   * 復制密碼到剪貼板
   */
  const handleCopyPassword = async () => {
    try {
      // 使用 Clipboard API 復制密碼
      await navigator.clipboard.writeText(password);
      setCopied(true);
      message.success('密碼已復制到剪貼板');

      // 3秒後重置復制狀態
      setTimeout(() => {
        setCopied(false);
      }, 3000);
    } catch (error) {
      // 降級方案：使用傳統方式復制
      const textArea = document.createElement('textarea');
      textArea.value = password;
      textArea.style.position = 'fixed';
      textArea.style.opacity = '0';
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        setCopied(true);
        message.success('密碼已復制到剪貼板');
        setTimeout(() => {
          setCopied(false);
        }, 3000);
      } catch (err) {
        message.error('復制失敗，請手動復制');
      }
      document.body.removeChild(textArea);
    }
  };

  /**
   * 處理 Modal 關閉
   */
  const handleClose = () => {
    setCopied(false);
    onClose();
  };

  return (
    <Modal
      title="賬號密碼信息"
      open={visible}
      onOk={handleClose}
      onCancel={handleClose}
      okText="我已記住"
      cancelButtonProps={{ style: { display: 'none' } }}
      closable={false}
      maskClosable={false}
      width={480}
    >
      <div style={{ padding: '16px 0' }}>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          {/* 登錄名 */}
          <div>
            <Text type="secondary">登錄名：</Text>
            <Title level={4} copyable style={{ marginTop: 8, marginBottom: 0 }}>
              {loginName}
            </Title>
          </div>

          {/* 密碼 */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text type="secondary">初始密碼：</Text>
              <Button
                type="link"
                icon={copied ? <CheckCircleOutlined /> : <CopyOutlined />}
                onClick={handleCopyPassword}
              >
                {copied ? '已復制' : '復制密碼'}
              </Button>
            </div>
            <div
              style={{
                marginTop: 8,
                padding: '12px 16px',
                backgroundColor: '#f5f5f5',
                borderRadius: '4px',
                fontSize: '20px',
                fontWeight: 'bold',
                letterSpacing: '2px',
                textAlign: 'center',
                fontFamily: 'monospace',
              }}
            >
              {password}
            </div>
          </div>

          {/* 提示信息 */}
          <div
            style={{
              padding: '12px',
              backgroundColor: '#fff7e6',
              border: '1px solid #ffd591',
              borderRadius: '4px',
            }}
          >
            <Text type="warning" style={{ fontSize: '14px' }}>
              ⚠️ 請妥善保管此密碼，關閉後將無法再次查看！
            </Text>
          </div>
        </Space>
      </div>
    </Modal>
  );
};
