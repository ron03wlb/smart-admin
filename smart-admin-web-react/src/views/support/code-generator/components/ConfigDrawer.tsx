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
import FieldListForm from './FieldListForm';
import InsertUpdateForm from './InsertUpdateForm';
import DeleteForm from './DeleteForm';
import QueryFieldForm from './QueryFieldForm';
import TableFieldForm from './TableFieldForm';

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
  const fieldListFormRef = useRef<any>(null);
  const insertUpdateFormRef = useRef<any>(null);
  const deleteFormRef = useRef<any>(null);
  const queryFieldFormRef = useRef<any>(null);
  const tableFieldFormRef = useRef<any>(null);

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

      // 設置所有表單數據
      basicFormRef.current?.setData(configResult.data, table);
      fieldListFormRef.current?.setData(columnsResult.data, configResult.data);
      insertUpdateFormRef.current?.setData(columnsResult.data, configResult.data);
      deleteFormRef.current?.setData(columnsResult.data, configResult.data);
      queryFieldFormRef.current?.setData(columnsResult.data, configResult.data);
      tableFieldFormRef.current?.setData(columnsResult.data, configResult.data);
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
      // 驗證所有表單
      const basicValid = await basicFormRef.current?.validateForm();
      const insertUpdateValid = await insertUpdateFormRef.current?.validateForm();
      const deleteValid = await deleteFormRef.current?.validateForm();

      if (!basicValid || !insertUpdateValid || !deleteValid) {
        message.error('請檢查表單，有參數驗證錯誤');
        return;
      }

      // 獲取所有表單數據
      const basicData = basicFormRef.current?.getFormData();
      const fieldsData = fieldListFormRef.current?.getFormData() || [];
      const insertAndUpdateData = insertUpdateFormRef.current?.getFormData() || {};
      const deleteData = deleteFormRef.current?.getFormData() || {};
      const queryFieldsData = queryFieldFormRef.current?.getFormData() || [];
      const tableFieldsData = tableFieldFormRef.current?.getFormData() || [];

      // 提交配置
      await codeGeneratorApi.updateConfig({
        tableName: tableInfo!.tableName,
        basic: basicData,
        fields: fieldsData,
        insertAndUpdate: insertAndUpdateData,
        deleteInfo: deleteData,
        queryFields: queryFieldsData,
        tableFields: tableFieldsData,
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
      children: <FieldListForm ref={fieldListFormRef} />,
    },
    {
      key: '3',
      label: (
        <span>
          <SaveOutlined />
          3.增加、修改
        </span>
      ),
      children: <InsertUpdateForm ref={insertUpdateFormRef} />,
    },
    {
      key: '4',
      label: (
        <span>
          <DeleteOutlined />
          4.刪除
        </span>
      ),
      children: <DeleteForm ref={deleteFormRef} />,
    },
    {
      key: '5',
      label: (
        <span>
          <FileSearchOutlined />
          5.查詢條件
        </span>
      ),
      children: <QueryFieldForm ref={queryFieldFormRef} />,
    },
    {
      key: '6',
      label: (
        <span>
          <TableOutlined />
          6.列表
        </span>
      ),
      children: <TableFieldForm ref={tableFieldFormRef} />,
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
