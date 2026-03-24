/**
 * Icon Select Component
 * 圖標選擇器組件
 *
 * 簡化版圖標選擇器，支持：
 * - 常用圖標快速選擇
 * - 圖標預覽
 * - 搜索過濾
 * - 自定義圖標輸入
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import React from 'react';
import { Select, Space } from 'antd';
import * as AntdIcons from '@ant-design/icons';

interface IconSelectProps {
  /** 當前值（圖標名稱） */
  value?: string;

  /** 值變化回調 */
  onChange?: (value: string | undefined) => void;

  /** 是否禁用 */
  disabled?: boolean;

  /** 佔位符 */
  placeholder?: string;
}

/**
 * 常用圖標列表
 */
const COMMON_ICONS = [
  'HomeOutlined',
  'UserOutlined',
  'TeamOutlined',
  'SettingOutlined',
  'ToolOutlined',
  'FileTextOutlined',
  'FolderOutlined',
  'FileOutlined',
  'AppstoreOutlined',
  'MenuOutlined',
  'UnorderedListOutlined',
  'TableOutlined',
  'FormOutlined',
  'DashboardOutlined',
  'PieChartOutlined',
  'BarChartOutlined',
  'LineChartOutlined',
  'AreaChartOutlined',
  'FundOutlined',
  'StockOutlined',
  'ShoppingOutlined',
  'ShoppingCartOutlined',
  'WalletOutlined',
  'CreditCardOutlined',
  'BellOutlined',
  'MessageOutlined',
  'MailOutlined',
  'PhoneOutlined',
  'CalendarOutlined',
  'ClockCircleOutlined',
  'LockOutlined',
  'SafetyOutlined',
  'KeyOutlined',
  'IdcardOutlined',
  'CloudOutlined',
  'DatabaseOutlined',
  'ApiOutlined',
  'CodeOutlined',
  'BugOutlined',
  'RocketOutlined',
  'ThunderboltOutlined',
  'StarOutlined',
  'HeartOutlined',
  'TrophyOutlined',
  'GiftOutlined',
  'CrownOutlined',
  'PictureOutlined',
  'CameraOutlined',
  'VideoCameraOutlined',
  'AudioOutlined',
  'SoundOutlined',
  'GlobalOutlined',
  'CompassOutlined',
  'EnvironmentOutlined',
  'AimOutlined',
  'ScanOutlined',
  'QrcodeOutlined',
  'TagOutlined',
  'TagsOutlined',
  'BookOutlined',
  'ReadOutlined',
  'ContainerOutlined',
  'FileDoneOutlined',
  'ReconciliationOutlined',
  'FileSearchOutlined',
  'SolutionOutlined',
  'FileProtectOutlined',
  'ScheduleOutlined',
  'ProjectOutlined',
  'AuditOutlined',
];

/**
 * 圖標選擇器
 */
export default function IconSelect({
  value,
  onChange,
  disabled,
  placeholder = '請選擇圖標',
}: IconSelectProps) {
  /**
   * 渲染圖標選項
   */
  const renderOption = (iconName: string) => {
    const IconComponent = (AntdIcons as unknown as Record<string, React.ComponentType<{ style?: React.CSSProperties }>>)[iconName];

    if (!IconComponent) {
      return {
        label: iconName,
        value: iconName,
      };
    }

    return {
      label: (
        <Space>
          <IconComponent style={{ fontSize: 16 }} />
          <span>{iconName}</span>
        </Space>
      ),
      value: iconName,
    };
  };

  /**
   * 渲染選中的值（帶圖標）
   */
  const renderValue = (iconName: string) => {
    const IconComponent = (AntdIcons as unknown as Record<string, React.ComponentType<{ style?: React.CSSProperties }>>)[iconName];

    if (!IconComponent) {
      return iconName;
    }

    return (
      <Space>
        <IconComponent style={{ fontSize: 16 }} />
        <span>{iconName}</span>
      </Space>
    );
  };

  return (
    <Select
      value={value}
      onChange={onChange}
      disabled={disabled}
      placeholder={placeholder}
      showSearch
      allowClear
      style={{ width: '100%' }}
      optionLabelProp="label"
      filterOption={(input, option) => {
        const label = option?.value as string;
        return label.toLowerCase().includes(input.toLowerCase());
      }}
      options={COMMON_ICONS.map(renderOption)}
    >
      {value && renderValue(value)}
    </Select>
  );
}
