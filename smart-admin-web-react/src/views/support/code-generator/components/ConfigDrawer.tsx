/**
 * Code Generator Config Drawer - 代碼配置抽屜
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/components/form/code-generator-table-config-form.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useImperativeHandle, forwardRef, useRef } from 'react';
import { Drawer, Tabs, Button, Space, message } from 'antd';
import {
  InfoCircleOutlined,
  UnorderedListOutlined,
  SaveOutlined,
  DeleteOutlined,
  FileSearchOutlined,
  TableOutlined,
} from '@ant-design/icons';
import type { TableInfo } from '@/api/support/codeGeneratorApi';
import { codeGeneratorApi } from '@/api/support/codeGeneratorApi';
import BasicForm from './BasicForm';

interface ConfigDrawerProps {
  onReload?: () => void;
}

export interface ConfigDrawerRef {
  showDrawer: (table: TableInfo) => void;
}

const ConfigDrawer = forwardRef<ConfigDrawerRef, ConfigDrawerProps>(({ onReload }, ref) => {
  const [open, setOpen] = useState(false);
  const [activeKey, setActiveKey] = useState('1');
  const [tableInfo, setTableInfo] = useState<TableInfo | null>(null);
  const [tableColumns, setTableColumns] = useState<any[]>([]);
  const [tableConfig, setTableConfig] = useState<any>({});

  const basicFormRef = useRef<any>(null);

  /**
   * 顯示 Drawer
   */
  const showDrawer = async (table: TableInfo) => {
    setTableInfo(table);
    setActiveKey('1');
    setOpen(true);

    // 加載表列和配置
    try {
      const [columnsResult, configResult] = await Promise.all([
        codeGeneratorApi.getTableColumns(table.tableName),
        codeGeneratorApi.getConfig(table.tableName),
      ]);

      setTableColumns(columnsResult.data || []);
      setTableConfig(configResult.data || {});

      // 設置基礎表單數據
      basicFormRef.current?.setData(configResult.data, table);
    } catch (error) {
      message.error('加載配置失敗');
      console.error('Load config error:', error);
    }
  };

  /**
   * 關閉 Drawer
   */
  const handleClose = () => {
    setOpen(false);
  };

  /**
   * 保存配置
   */
  const handleSave = async () => {
    try {
      // 驗證基礎表單
      const basicValid = await basicFormRef.current?.validateForm();
      if (!basicValid) {
        message.error('請檢查【1.基礎命名】表單，有參數驗證錯誤');
        return;
      }

      // 獲取基礎表單數據
      const basicData = basicFormRef.current?.getFormData();

      // 提交配置
      await codeGeneratorApi.updateConfig({
        tableName: tableInfo!.tableName,
        basic: basicData,
        fields: [],
        insertAndUpdate: {},
        deleteInfo: {},
        queryFields: [],
        tableFields: [],
      });

      message.success('保存成功');
      onReload?.();
      handleClose();
    } catch (error) {
      message.error('保存失敗');
      console.error('Save config error:', error);
    }
  };

  // 暴露方法給父組件
  useImperativeHandle(ref, () => ({
    showDrawer,
  }));

  /**
   * Tabs 配置
   */
  const tabItems = [
    {
      key: '1',
      label: (
        <span>
          <InfoCircleOutlined />
          1.基礎命名
        </span>
      ),
      children: <BasicForm ref={basicFormRef} />,
    },
    {
      key: '2',
      label: (
        <span>
          <UnorderedListOutlined />
          2.字段列表
        </span>
      ),
      children: <div>字段列表（Phase 2 實現）</div>,
    },
    {
      key: '3',
      label: (
        <span>
          <SaveOutlined />
          3.增加、修改
        </span>
      ),
      children: <div>增加、修改（Phase 2 實現）</div>,
    },
    {
      key: '4',
      label: (
        <span>
          <DeleteOutlined />
          4.刪除
        </span>
      ),
      children: <div>刪除（Phase 2 實現）</div>,
    },
    {
      key: '5',
      label: (
        <span>
          <FileSearchOutlined />
          5.查詢條件
        </span>
      ),
      children: <div>查詢條件（Phase 2 實現）</div>,
    },
    {
      key: '6',
      label: (
        <span>
          <TableOutlined />
          6.列表
        </span>
      ),
      children: <div>列表（Phase 2 實現）</div>,
    },
  ];

  return (
    <Drawer
      title="代碼配置"
      open={open}
      width={1200}
      onClose={handleClose}
      maskClosable={false}
      destroyOnClose
      footer={
        <div style={{ textAlign: 'right' }}>
          <Space>
            <Button onClick={handleClose}>取消</Button>
            <Button type="primary" onClick={handleSave}>
              保存
            </Button>
          </Space>
        </div>
      }
    >
      <Tabs activeKey={activeKey} onChange={setActiveKey} items={tabItems} />
    </Drawer>
  );
});

ConfigDrawer.displayName = 'ConfigDrawer';

export default ConfigDrawer;
