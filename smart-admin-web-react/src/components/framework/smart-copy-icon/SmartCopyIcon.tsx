/**
 * Smart Copy Icon Component
 *
 * A copy-to-clipboard button with success feedback.
 */
import React from 'react';
import { Tooltip, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';

interface SmartCopyIconProps {
  text: string;
  tooltip?: string;
  style?: React.CSSProperties;
}

const SmartCopyIcon: React.FC<SmartCopyIconProps> = ({ text, tooltip = '复制', style }) => {
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      message.success('复制成功');
    } catch {
      message.error('复制失败');
    }
  };

  return (
    <Tooltip title={tooltip}>
      <CopyOutlined onClick={handleCopy} style={{ cursor: 'pointer', ...style }} />
    </Tooltip>
  );
};

export default SmartCopyIcon;
