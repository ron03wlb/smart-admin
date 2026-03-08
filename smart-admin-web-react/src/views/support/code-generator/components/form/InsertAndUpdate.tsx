/**
 * Tab 3: Insert and Update Configuration
 *
 * Left panel: support toggle, page type, width, countPerLine.
 * Center panel: form layout preview.
 * Bottom table: required/insert/update flags and frontend component per field.
 */
import { useState, useEffect, useImperativeHandle, forwardRef, useMemo, useRef } from 'react';
import { Form, Radio, Input, InputNumber, Row, Col, Table, Checkbox, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCodeGeneratorContext } from '../../CodeGeneratorContext';
import { checkExistEnum, getFrontComponent, CODE_FRONT_COMPONENT_LIST, CODE_PAGE_TYPE_LIST } from '../../code-generator-util';
import { Select } from 'antd';

export interface InsertAndUpdateRef {
  validateForm: () => Promise<boolean>;
  getForm: () => InsertAndUpdateFormData;
}

interface InsertAndUpdateFormData {
  isSupportInsertAndUpdate: boolean;
  pageType: string;
  width?: string;
  countPerLine: number;
  fieldList?: FieldRow[];
}

interface FieldRow {
  columnName: string;
  columnComment: string;
  dataType: string;
  nullableFlag: boolean;
  primaryKeyFlag: boolean;
  autoIncreaseFlag: boolean;
  requiredFlag: boolean;
  insertFlag: boolean;
  updateFlag: boolean;
  frontComponent: string;
}

const InsertAndUpdate = forwardRef<InsertAndUpdateRef>((_props, ref) => {
  const { tableColumns, tableConfig } = useCodeGeneratorContext();
  const [form] = Form.useForm();
  const [isSupportInsertAndUpdate, setIsSupportInsertAndUpdate] = useState(true);
  const [tableData, setTableData] = useState<FieldRow[]>([]);
  const initializedRef = useRef(false);

  useEffect(() => {
    if (!tableConfig || !tableColumns.length || initializedRef.current) return;
    initializedRef.current = true;

    const cfg = tableConfig.insertAndUpdate;
    const support = cfg?.isSupportInsertAndUpdate ?? true;
    setIsSupportInsertAndUpdate(support);

    form.setFieldsValue({
      isSupportInsertAndUpdate: support,
      pageType: cfg?.pageType || 'modal',
      width: cfg?.width || undefined,
      countPerLine: cfg?.countPerLine || 1,
    });

    // Build field rows
    const insertAndUpdateFields = tableConfig.insertAndUpdateFields || [];
    const fields: FieldRow[] = tableColumns.map((col) => {
      const cfgField = insertAndUpdateFields.find((f) => f.columnName === col.columnName);
      const frontComponent = cfgField?.frontComponent
        || (checkExistEnum(col.columnComment) ? 'SmartEnumSelect' : getFrontComponent(col.dataType));

      return {
        columnName: col.columnName,
        columnComment: col.columnComment,
        dataType: col.dataType,
        nullableFlag: col.nullableFlag,
        primaryKeyFlag: col.primaryKeyFlag,
        autoIncreaseFlag: col.autoIncreaseFlag,
        requiredFlag: cfgField ? cfgField.requiredFlag : !col.nullableFlag,
        insertFlag: cfgField ? cfgField.insertFlag : !col.nullableFlag,
        updateFlag: cfgField?.updateFlag ?? false,
        frontComponent,
      };
    });
    setTableData(fields);
  }, [tableConfig, tableColumns, form]);

  const updateRow = (index: number, key: keyof FieldRow, value: boolean | string) => {
    setTableData((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [key]: value };
      return next;
    });
  };

  const countPerLine = Form.useWatch('countPerLine', form) || 1;
  const spanPerLine = useMemo(() => Math.floor(20 / countPerLine), [countPerLine]);

  useImperativeHandle(ref, () => ({
    validateForm: async () => {
      if (!isSupportInsertAndUpdate) return true;
      try {
        await form.validateFields();
        return true;
      } catch {
        message.error('请检查【3.增加、修改】表单，有参数验证错误');
        return false;
      }
    },
    getForm: () => {
      const values = form.getFieldsValue();
      return {
        isSupportInsertAndUpdate: values.isSupportInsertAndUpdate,
        pageType: values.pageType,
        width: values.width,
        countPerLine: values.countPerLine,
        fieldList: tableData.map((e) => ({
          columnName: e.columnName,
          requiredFlag: e.requiredFlag,
          insertFlag: e.insertFlag,
          updateFlag: e.updateFlag,
          frontComponent: e.frontComponent,
        })),
      } as InsertAndUpdateFormData;
    },
  }));

  const tableColumns_: ColumnsType<FieldRow> = [
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
      title: '必填', dataIndex: 'requiredFlag', width: 50,
      render: (val, _r, idx) => <Checkbox checked={val} onChange={(e) => updateRow(idx, 'requiredFlag', e.target.checked)} />,
    },
    {
      title: '新增', dataIndex: 'insertFlag', width: 50,
      render: (val, _r, idx) => <Checkbox checked={val} onChange={(e) => updateRow(idx, 'insertFlag', e.target.checked)} />,
    },
    {
      title: '更新', dataIndex: 'updateFlag', width: 50,
      render: (val, _r, idx) => <Checkbox checked={val} onChange={(e) => updateRow(idx, 'updateFlag', e.target.checked)} />,
    },
    {
      title: '前端组件', dataIndex: 'frontComponent', width: 150,
      render: (val, _r, idx) => (
        <Select value={val} onChange={(v) => updateRow(idx, 'frontComponent', v)} style={{ width: '100%' }}>
          {CODE_FRONT_COMPONENT_LIST.map((item) => (
            <Select.Option key={item.value} value={item.value}>{item.label}</Select.Option>
          ))}
        </Select>
      ),
    },
  ];

  const previewBoxStyle: React.CSSProperties = { background: '#00a0e9', padding: '5px 0', textAlign: 'center', color: 'white' };

  return (
    <>
      <Row style={{ marginTop: 10 }}>
        <Col flex="350px">
          <Form form={form} style={{ width: 350 }} labelCol={{ span: 5 }} wrapperCol={{ span: 16 }}>
            <Form.Item label="是否支持" name="isSupportInsertAndUpdate" rules={[{ required: true }]}>
              <Radio.Group buttonStyle="solid" onChange={(e) => setIsSupportInsertAndUpdate(e.target.value)}>
                <Radio.Button value={true}>支持</Radio.Button>
                <Radio.Button value={false}>不支持添加、修改</Radio.Button>
              </Radio.Group>
            </Form.Item>
            {isSupportInsertAndUpdate && (
              <>
                <Form.Item label="页面方式" name="pageType" rules={[{ required: true }]}>
                  <Radio.Group buttonStyle="solid">
                    {CODE_PAGE_TYPE_LIST.map((item) => (
                      <Radio.Button key={item.value} value={item.value}>{item.label}</Radio.Button>
                    ))}
                  </Radio.Group>
                </Form.Item>
                <Form.Item label="页面宽度" name="width" rules={[{ required: true, message: '请输入 宽度' }]}>
                  <Input placeholder="Modal或者Drawer的width属性" />
                </Form.Item>
                <Form.Item label="每行数量" name="countPerLine" rules={[{ required: true }]}>
                  <InputNumber style={{ width: '100%' }} max={24} placeholder="请输入 每行数量" />
                </Form.Item>
              </>
            )}
          </Form>
        </Col>
        {isSupportInsertAndUpdate && (
          <Col flex="auto" style={{ width: 500 }}>
            <div style={{ background: '#efefef', padding: '15px 20px' }}>
              {[0, 1, 2].map((rowIdx) => (
                <Row key={rowIdx} gutter={20} justify="space-around" style={rowIdx > 0 ? { marginTop: 10 } : undefined}>
                  {Array.from({ length: countPerLine }).map((_, colIdx) => (
                    <Col key={colIdx} span={spanPerLine}>
                      <div style={previewBoxStyle}>字段</div>
                    </Col>
                  ))}
                </Row>
              ))}
            </div>
          </Col>
        )}
      </Row>

      {isSupportInsertAndUpdate && (
        <Table<FieldRow>
          size="small"
          scroll={{ x: 1000 }}
          bordered
          style={{ marginTop: 10 }}
          dataSource={tableData}
          columns={tableColumns_}
          rowKey="columnName"
          pagination={false}
        />
      )}
    </>
  );
});

InsertAndUpdate.displayName = 'InsertAndUpdate';
export default InsertAndUpdate;
