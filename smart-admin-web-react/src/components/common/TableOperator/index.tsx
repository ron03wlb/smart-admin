/**
 * TableOperator Component
 * 表格操作欄組件
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/goods/goods-list.vue (Line 64-97)
 *
 * 功能：
 * - 左側操作按鈕區（新建、刪除、導入、導出等）
 * - 右側工具按鈕區（刷新、全屏、列設置等）
 * - 支持權限控制
 * - 支持自定義按鈕
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import React from 'react';
import { Button, Space, Row } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  ImportOutlined,
  ExportOutlined,
  ReloadOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import PrivilegeButton from '@/components/PrivilegeButton';
import './index.css';

export interface TableOperatorButton {
  /**
   * 按鈕類型（預設類型）
   */
  type?: 'add' | 'delete' | 'import' | 'export' | 'custom';

  /**
   * 按鈕文本
   */
  text?: string;

  /**
   * 按鈕圖標
   */
  icon?: React.ReactNode;

  /**
   * 點擊事件
   */
  onClick?: () => void;

  /**
   * 權限碼
   */
  privilege?: string;

  /**
   * 禁用狀態
   */
  disabled?: boolean;

  /**
   * 按鈕樣式類型
   */
  buttonType?: 'default' | 'primary' | 'dashed' | 'link' | 'text';

  /**
   * 是否危險按鈕
   */
  danger?: boolean;
}

export interface TableOperatorProps {
  /**
   * 左側操作按鈕配置
   */
  buttons?: TableOperatorButton[];

  /**
   * 是否顯示刷新按鈕
   */
  showRefresh?: boolean;

  /**
   * 刷新回調
   */
  onRefresh?: () => void;

  /**
   * 是否顯示全屏按鈕
   */
  showFullscreen?: boolean;

  /**
   * 全屏狀態
   */
  fullscreen?: boolean;

  /**
   * 全屏切換回調
   */
  onFullscreenChange?: (fullscreen: boolean) => void;

  /**
   * 是否顯示列設置按鈕
   */
  showColumnSetting?: boolean;

  /**
   * 列設置點擊回調
   */
  onColumnSettingClick?: () => void;

  /**
   * 自定義右側工具欄
   */
  toolbarRender?: React.ReactNode;
}

/**
 * 獲取預設按鈕配置
 */
const getDefaultButtonConfig = (type: string): Partial<TableOperatorButton> => {
  const configs: Record<string, Partial<TableOperatorButton>> = {
    add: {
      text: '新建',
      icon: <PlusOutlined />,
      buttonType: 'primary',
    },
    delete: {
      text: '批量刪除',
      icon: <DeleteOutlined />,
      buttonType: 'default',
      danger: true,
    },
    import: {
      text: '導入',
      icon: <ImportOutlined />,
      buttonType: 'primary',
    },
    export: {
      text: '導出',
      icon: <ExportOutlined />,
      buttonType: 'primary',
    },
  };

  return configs[type] || {};
};

/**
 * TableOperator 組件
 */
const TableOperator: React.FC<TableOperatorProps> = props => {
  const {
    buttons = [],
    showRefresh = true,
    onRefresh,
    showFullscreen = false,
    fullscreen = false,
    onFullscreenChange,
    showColumnSetting = false,
    onColumnSettingClick,
    toolbarRender,
  } = props;

  /**
   * 渲染操作按鈕
   */
  const renderButton = (buttonConfig: TableOperatorButton, index: number) => {
    const {
      type = 'custom',
      text,
      icon,
      onClick,
      privilege,
      disabled = false,
      buttonType = 'default',
      danger = false,
    } = buttonConfig;

    // 獲取預設配置
    const defaultConfig = type !== 'custom' ? getDefaultButtonConfig(type) : {};

    // 合併配置
    const finalConfig = {
      text: text || defaultConfig.text || '',
      icon: icon || defaultConfig.icon,
      buttonType: buttonType || defaultConfig.buttonType || 'default',
      danger: danger || defaultConfig.danger || false,
    };

    const button = (
      <Button
        key={index}
        type={finalConfig.buttonType as any}
        icon={finalConfig.icon}
        onClick={onClick}
        disabled={disabled}
        danger={finalConfig.danger}
      >
        {finalConfig.text}
      </Button>
    );

    // 如果有權限碼，使用 PrivilegeButton 包裹
    if (privilege) {
      return (
        <PrivilegeButton key={index} privilege={privilege}>
          {button}
        </PrivilegeButton>
      );
    }

    return button;
  };

  /**
   * 處理全屏切換
   */
  const handleFullscreenToggle = () => {
    if (onFullscreenChange) {
      onFullscreenChange(!fullscreen);
    }
  };

  return (
    <Row className="table-operator-container">
      {/* 左側操作按鈕區 */}
      <div className="table-operator-buttons">
        <Space size="small">{buttons.map((button, index) => renderButton(button, index))}</Space>
      </div>

      {/* 右側工具按鈕區 */}
      <div className="table-operator-toolbar">
        <Space size="small">
          {/* 自定義工具欄 */}
          {toolbarRender}

          {/* 刷新按鈕 */}
          {showRefresh && (
            <Button
              type="text"
              size="small"
              icon={<ReloadOutlined />}
              onClick={onRefresh}
              title="刷新"
            />
          )}

          {/* 全屏按鈕 */}
          {showFullscreen && (
            <Button
              type="text"
              size="small"
              icon={fullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
              onClick={handleFullscreenToggle}
              title={fullscreen ? '退出全屏' : '全屏'}
            />
          )}

          {/* 列設置按鈕 */}
          {showColumnSetting && (
            <Button
              type="text"
              size="small"
              icon={<SettingOutlined />}
              onClick={onColumnSettingClick}
              title="列設置"
            />
          )}
        </Space>
      </div>
    </Row>
  );
};

export default TableOperator;
