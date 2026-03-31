/**
 * Tab 2: Field List
 *
 * Editable table mapping database columns to Java/JS types, dict, and enum names.
 */
import { useState, useEffect, useImperativeHandle, forwardRef, useRef, useCallback } from 'react';
import { Table, Input, Select, Tag, Alert, Button, message } from 'antd';
import { SmileOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useCodeGeneratorContext } from '../../CodeGeneratorContext';
import {
  getJavaType, getJsType, JavaTypeList, JsTypeList,
  convertUpperCamel, convertLowerCamel, checkExistEnum, convertJavaEnumName,
} from '../../code-generator-util';
import DictSelect from '@/components/support/dict-select/DictSelect';
import type { FieldConfig } from '@/types/code-generator.types';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchAllDictData, selectDictLoading } from '@/store/slices/dictSlice';

export interface FieldListRef {
  getForm: () => FieldConfig[];
}

interface FieldRow extends FieldConfig {
  dataType: string;
  columnComment: string;
  nullableFlag: boolean;
}

const FieldList = forwardRef<FieldListRef>((_props, ref) => {
  const { tableInfo, tableColumns, tableConfig } = useCodeGeneratorContext();
  const [tableData, setTableData] = useState<FieldRow[]>([]);
  const initializedRef = useRef(false);
  const dispatch = useAppDispatch();
  const dictLoading = useAppSelector(selectDictLoading);

  const handleRefreshDict = useCallback(async () => {
    try {
      await dispatch(fetchAllDictData()).unwrap();
      message.success('字典刷新成功');
    } catch {
      message.error('字典刷新失败');
    }
  }, [dispatch]);

  useEffect(() => {
    if (!tableConfig || !tableColumns.length || initializedRef.current) return;
    initializedRef.current = true;

    const basic = tableConfig.basic;
    let removePrefixName = tableInfo.tableName;
    if (removePrefixName.startsWith('t_')) {
      removePrefixName = removePrefixName.slice(2);
    }
    const modName = basic?.moduleName ? basic.moduleName : convertUpperCamel(removePrefixName);

    const fields: FieldRow[] = tableColumns.map((col) => {
      const cfg = tableConfig.fields?.find((f) => f.columnName === col.columnName);
      return {
        columnName: col.columnName,
        columnComment: col.columnComment,
        dataType: col.dataType,
        nullableFlag: col.nullableFlag,
        primaryKeyFlag: col.primaryKeyFlag,
        autoIncreaseFlag: col.autoIncreaseFlag,
        label: cfg?.label || col.columnComment,
        fieldName: cfg?.fieldName || convertLowerCamel(col.columnName),
        javaType: cfg?.javaType || getJavaType(col.dataType),
        jsType: cfg?.jsType || getJsType(col.dataType),
        dict: cfg?.dict || undefined,
        enumName: cfg?.enumName || (checkExistEnum(col.columnComment) ? convertJavaEnumName(modName, col.columnName) : undefined),
      };
    });

    setTableData(fields);
  }, [tableConfig, tableColumns, tableInfo]);

  const updateRow = (index: number, key: keyof FieldRow, value: string | undefined) => {
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
        columnComment: e.columnComment,
        label: e.label,
        fieldName: e.fieldName,
        javaType: e.javaType,
        jsType: e.jsType,
        dict: e.dict,
        enumName: e.enumName,
        primaryKeyFlag: e.primaryKeyFlag,
        autoIncreaseFlag: e.autoIncreaseFlag,
      })),
  }));

  const columns: ColumnsType<FieldRow> = [
    {
      title: '列名', dataIndex: 'columnName', width: 120, ellipsis: true,
      render: (text, record) => (
        <span>
          {record.primaryKeyFlag && <Tag color="#f50" style={{ lineHeight: '12px' }}>主键</Tag>}
          {record.autoIncreaseFlag && <Tag color="#f50" style={{ lineHeight: '12px' }}>自增</Tag>}
          {(record.primaryKeyFlag || record.autoIncreaseFlag) && <br />}
          {text}
        </span>
      ),
    },
    { title: '列描述', dataIndex: 'columnComment', width: 120, ellipsis: true },
    { title: '列类型', dataIndex: 'dataType', width: 100, ellipsis: true },
    {
      title: '非空', dataIndex: 'nullableFlag', width: 60,
      render: (val) => !val ? <Tag color="error">非空</Tag> : null,
    },
    {
      title: '字段命名', dataIndex: 'fieldName', width: 150,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'fieldName', e.target.value)} />,
    },
    {
      title: '字段名词', dataIndex: 'label', width: 150,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'label', e.target.value)} />,
    },
    {
      title: 'Java类型', dataIndex: 'javaType', width: 150,
      render: (val, _r, idx) => (
        <Select value={val} onChange={(v) => updateRow(idx, 'javaType', v)} style={{ width: '100%' }}>
          {JavaTypeList.map((t) => <Select.Option key={t} value={t}>{t}</Select.Option>)}
        </Select>
      ),
    },
    {
      title: '前端类型', dataIndex: 'jsType', width: 130,
      render: (val, _r, idx) => (
        <Select value={val} onChange={(v) => updateRow(idx, 'jsType', v)} style={{ width: '100%' }}>
          {JsTypeList.map((t) => <Select.Option key={t} value={t}>{t}</Select.Option>)}
        </Select>
      ),
    },
    {
      title: '字典', dataIndex: 'dict', width: 150,
      render: (val, _r, idx) => (
        <DictSelect dictCode="DICT_KEY_LIST" value={val} onChange={(v) => updateRow(idx, 'dict', v as string)} />
      ),
    },
    {
      title: '枚举', dataIndex: 'enumName', width: 150,
      render: (val, _r, idx) => <Input value={val} onChange={(e) => updateRow(idx, 'enumName', e.target.value)} />,
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
      <div style={{ float: 'right', padding: '10px 0' }}>
        <Button type="primary" loading={dictLoading} onClick={handleRefreshDict}>
          刷新字典
        </Button>
      </div>
      <Table<FieldRow>
        scroll={{ x: 1300 }}
        size="small"
        bordered
        style={{ marginTop: 10 }}
        dataSource={tableData}
        columns={columns}
        rowKey="columnName"
        pagination={false}
      />
    </>
  );
});

FieldList.displayName = 'FieldList';
export default FieldList;
