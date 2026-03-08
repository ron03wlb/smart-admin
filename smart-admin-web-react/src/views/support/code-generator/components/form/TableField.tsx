/**
 * Tab 6: Table Field Configuration
 *
 * Configure which fields show in the list table, with label, width, and ellipsis toggle.
 */
import { useState, useEffect, useImperativeHandle, forwardRef, useRef } from 'react';
import { Table, Input, InputNumber, Checkbox, Switch, Alert } from 'antd';
import { SmileOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useCodeGeneratorContext } from '../../CodeGeneratorContext';
import { convertLowerCamel } from '../../code-generator-util';
import type { TableFieldConfig } from '@/types/code-generator.types';

export interface TableFieldRef {
  getForm: () => TableFieldConfig[];
}

interface TableFieldRow {
  columnName: string;
  columnComment: string;
  dataType: string;
  showFlag: boolean;
  label: string;
  fieldName: string;
  width: number | null;
  ellipsisFlag: boolean;
}

const TableField = forwardRef<TableFieldRef>((_props, ref) => {
  const { tableColumns, tableConfig } = useCodeGeneratorContext();
  const [tableData, setTableData] = useState<TableFieldRow[]>([]);
  const initializedRef = useRef(false);

  useEffect(() => {
    if (!tableConfig || !tableColumns.length || initializedRef.current) return;
    initializedRef.current = true;

    const cfgFields = tableConfig.tableFields || [];
    const fields: TableFieldRow[] = tableColumns.map((col) => {
      const cfg = cfgFields.find((f) => f.columnName === col.columnName);
      return {
        columnName: col.columnName,
        columnComment: col.columnComment,
        dataType: col.dataType,
        showFlag: cfg?.showFlag ?? cfg?.visible ?? true,
        label: cfg?.label || col.columnComment,
        fieldName: cfg?.fieldName || convertLowerCamel(col.columnName),
        width: cfg?.width ?? null,
        ellipsisFlag: cfg?.ellipsisFlag ?? true,
      };
    });
    setTableData(fields);
  }, [tableConfig, tableColumns]);

  const updateRow = (index: number, key: keyof TableFieldRow, value: unknown) => {
    setTableData((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [key]: value };
      return next;
    });
  };

  useImperativeHandle(ref, () => ({
    getForm: () =>
      tableData.map((e) => ({
        columnName: e.columnName,
        label: e.label,
        fieldName: e.fieldName,
        showFlag: e.showFlag,
        width: e.width,
        ellipsisFlag: e.ellipsisFlag,
      })),
  }));

  const columns: ColumnsType<TableFieldRow> = [
    {
      title: '序号', dataIndex: 'no', width: 60,
      render: (_v, _r, idx) => idx + 1,
    },
    { title: '列名', dataIndex: 'columnName', width: 120, ellipsis: true },
    { title: '列描述', dataIndex: 'columnComment', width: 120, ellipsis: true },
    {
      title: '显示', dataIndex: 'showFlag', width: 50,
      render: (val, _r, idx) => <Checkbox checked={val} onChange={(e) => updateRow(idx, 'showFlag', e.target.checked)} />,
    },
    {
      title: '字段名词', dataIndex: 'label', width: 120,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'label', e.target.value)} />,
    },
    {
      title: '字段命名', dataIndex: 'fieldName', width: 120,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'fieldName', e.target.value)} />,
    },
    {
      title: '宽度', dataIndex: 'width', width: 80,
      render: (val, _r, idx) => <InputNumber value={val} onChange={(v) => updateRow(idx, 'width', v)} />,
    },
    {
      title: 'ellipsis', dataIndex: 'ellipsisFlag', width: 130,
      render: (val, _r, idx) => (
        <Switch
          checked={val}
          onChange={(v) => updateRow(idx, 'ellipsisFlag', v)}
          checkedChildren="自动省略"
          unCheckedChildren="换行显示"
        />
      ),
    },
  ];

  return (
    <>
      <Alert
        closable
        message='请务必将每一个字段的 "字段名词" 填写完整！！！'
        type="success"
        showIcon
        icon={<SmileOutlined />}
      />
      <Table<TableFieldRow>
        size="small"
        bordered
        scroll={{ x: 1000 }}
        style={{ marginTop: 10 }}
        dataSource={tableData}
        columns={columns}
        rowKey="columnName"
        pagination={false}
      />
    </>
  );
});

TableField.displayName = 'TableField';
export default TableField;
