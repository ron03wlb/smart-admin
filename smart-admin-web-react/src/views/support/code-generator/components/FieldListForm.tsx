/**
 * Code Generator Field List Form - 字段列表表單（簡化版）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/components/form/code-generator-table-config-form-field.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useImperativeHandle, forwardRef } from 'react';
import { Alert, Table, Button } from 'antd';
import { SmileOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

export interface FieldListFormRef {
  setData: (columns: any[], config: any) => void;
  getFormData: () => any[];
}

interface FieldData {
  columnName: string;
  columnComment: string;
  dataType: string;
  nullable: string;
  columnKey: string;
  extra: string;
  fieldName?: string;
  label?: string;
  javaType?: string;
  jsType?: string;
  dict?: string;
  enumName?: string;
}

const FieldListForm = forwardRef<FieldListFormRef>((_props, ref) => {
  const [dataSource, setDataSource] = useState<FieldData[]>([]);

  /**
   * 設置數據
   */
  const setData = (columns: any[], config: any) => {
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
  const columns: ColumnsType<FieldData> = [
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
      width: 150,
      ellipsis: true,
    },
    {
      title: '列類型',
      dataIndex: 'dataType',
      width: 120,
      ellipsis: true,
    },
    {
      title: '是否為空',
      dataIndex: 'nullable',
      width: 100,
      align: 'center',
    },
    {
      title: '鍵',
      dataIndex: 'columnKey',
      width: 100,
      align: 'center',
    },
  ];

  return (
    <div>
      <Alert
        closable
        message="完整字段編輯功能（字段命名、Java類型、前端類型、字典、枚舉配置等）將在 Phase 3 實現"
        type="info"
        showIcon
        icon={<SmileOutlined />}
        style={{ marginBottom: 16 }}
      />

      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button icon={<ReloadOutlined />} onClick={() => setData(dataSource, {})}>
          刷新字典
        </Button>
      </div>

      <Table
        size="small"
        bordered
        columns={columns}
        dataSource={dataSource}
        rowKey="columnName"
        scroll={{ x: 800 }}
        pagination={false}
      />
    </div>
  );
});

FieldListForm.displayName = 'FieldListForm';

export default FieldListForm;
