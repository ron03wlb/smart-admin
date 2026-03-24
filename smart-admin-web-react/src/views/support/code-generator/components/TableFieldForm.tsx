/**
 * Code Generator Table Field Form - 列表表單（簡化版）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/components/form/code-generator-table-config-form-table-field.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useImperativeHandle, forwardRef } from 'react';
import { Alert, Table } from 'antd';
import { TableOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

export interface TableFieldFormRef {
  setData: (columns: any[], config: any) => void;
  getFormData: () => any[];
}

interface TableFieldData {
  columnName: string;
  columnComment: string;
  dataType: string;
  fieldName?: string;
  label?: string;
  showFlag?: boolean;
}

const TableFieldForm = forwardRef<TableFieldFormRef>((_props, ref) => {
  const [dataSource, setDataSource] = useState<TableFieldData[]>([]);

  /**
   * 設置數據
   */
  const setData = (columns: any[], _config: any) => {
    // 簡化版：直接使用原始列數據
    setDataSource(columns || []);
  };

  /**
   * 獲取表單數據
   */
  const getFormData = () => {
    return dataSource;
  };

  // 暴露方法給父組件
  useImperativeHandle(ref, () => ({
    setData,
    getFormData,
  }));

  /**
   * 列定義
   */
  const columns: ColumnsType<TableFieldData> = [
    {
      title: '序號',
      width: 60,
      align: 'center',
      render: (_text, _record, index) => index + 1,
    },
    {
      title: '列名',
      dataIndex: 'columnName',
      width: 150,
      ellipsis: true,
    },
    {
      title: '列描述',
      dataIndex: 'columnComment',
      width: 200,
      ellipsis: true,
    },
    {
      title: '列類型',
      dataIndex: 'dataType',
      width: 120,
      ellipsis: true,
    },
  ];

  return (
    <div>
      <Alert
        closable
        message="完整列表字段配置功能（字段選擇、顯示順序、寬度配置、排序等）將在 Phase 3 實現"
        type="info"
        showIcon
        icon={<TableOutlined />}
        style={{ marginBottom: 16 }}
      />

      <Table
        size="small"
        bordered
        columns={columns}
        dataSource={dataSource}
        rowKey="columnName"
        scroll={{ x: 600 }}
        pagination={false}
      />
    </div>
  );
});

TableFieldForm.displayName = 'TableFieldForm';

export default TableFieldForm;
