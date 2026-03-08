/**
 * Tab 1: Basic Naming
 *
 * Left panel: form for module naming, package, authors, dates, copyright.
 * Right panel: file name preview with frontend/backend sub-tabs.
 */
import { useState, useEffect, useImperativeHandle, forwardRef, useMemo, useRef } from 'react';
import { Form, Input, DatePicker, Row, Col, Tabs, Alert, message } from 'antd';
import { SmileOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { useCodeGeneratorContext } from '../../CodeGeneratorContext';
import { convertUpperCamel, convertLowerHyphen } from '../../code-generator-util';
import type { BasicConfig } from '@/types/code-generator.types';

export interface BasicNamingRef {
  validateForm: () => Promise<boolean>;
  getForm: () => BasicConfig;
}

const TABLE_PREFIX = 't_';
const TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const BasicNaming = forwardRef<BasicNamingRef>((_props, ref) => {
  const { tableInfo, tableConfig } = useCodeGeneratorContext();
  const [form] = Form.useForm();
  const [tablePrefix, setTablePrefix] = useState(TABLE_PREFIX);
  const [previewTab, setPreviewTab] = useState('1');
  const initializedRef = useRef(false);

  // Initialize form data when config loads
  useEffect(() => {
    if (!tableConfig || !tableInfo.tableName || initializedRef.current) return;
    initializedRef.current = true;

    const basic = tableConfig.basic;
    let removePrefixName = tableInfo.tableName;
    if (removePrefixName.startsWith(tablePrefix)) {
      removePrefixName = removePrefixName.slice(tablePrefix.length);
    }

    form.setFieldsValue({
      moduleName: basic?.moduleName ? basic.moduleName : convertUpperCamel(removePrefixName),
      javaPackageName: basic?.javaPackageName || undefined,
      description: basic?.description || tableInfo.tableComment,
      frontAuthor: basic?.frontAuthor || undefined,
      frontDate: basic?.frontDate ? dayjs(basic.frontDate) : dayjs(tableInfo.createTime),
      backendAuthor: basic?.backendAuthor || undefined,
      backendDate: basic?.backendDate ? dayjs(basic.backendDate) : dayjs(tableInfo.createTime),
      copyright: basic?.copyright || undefined,
    });
  }, [tableConfig, tableInfo, tablePrefix, form]);

  const onChangeTablePrefix = (value: string) => {
    setTablePrefix(value);
    let removePrefixName = tableInfo.tableName;
    if (removePrefixName.startsWith(value)) {
      removePrefixName = removePrefixName.slice(value.length);
    }
    form.setFieldValue('moduleName', convertUpperCamel(removePrefixName));
  };

  useImperativeHandle(ref, () => ({
    validateForm: async () => {
      try {
        await form.validateFields();
        return true;
      } catch {
        message.error('请检查【1.基础命名】表单，有参数验证错误');
        return false;
      }
    },
    getForm: () => {
      const values = form.getFieldsValue();
      return {
        ...values,
        frontDate: values.frontDate ? dayjs(values.frontDate).format(TIME_FORMAT) : '',
        backendDate: values.backendDate ? dayjs(values.backendDate).format(TIME_FORMAT) : '',
      };
    },
  }));

  // Preview computed values
  const moduleName = Form.useWatch('moduleName', form) || '';
  const description = Form.useWatch('description', form) || '';
  const frontAuthor = Form.useWatch('frontAuthor', form) || '';
  const frontDate = Form.useWatch('frontDate', form);
  const backendAuthor = Form.useWatch('backendAuthor', form) || '';
  const backendDate = Form.useWatch('backendDate', form);
  const copyright = Form.useWatch('copyright', form) || '';

  const frontName = useMemo(() => convertLowerHyphen(moduleName), [moduleName]);
  const frontDateStr = frontDate ? dayjs(frontDate).format(TIME_FORMAT) : '';
  const backendDateStr = backendDate ? dayjs(backendDate).format(TIME_FORMAT) : '';

  const frontNameList = useMemo(() => [
    `请求：${frontName}-api.js`,
    `常量：${frontName}-const.js`,
    `列表：${frontName}-list.vue`,
    `表单：${frontName}-form-modal.vue`,
    `详情：${frontName}-detail.vue`,
  ], [frontName]);

  const backendMvcNameList = useMemo(() => [
    `控制层：${moduleName}Controller.java`,
    `业务层：${moduleName}Service.java`,
    `中间层：${moduleName}Manager.java`,
    `持久层：${moduleName}Dao.java`,
    `SQL层：${moduleName}Mapper.xml`,
  ], [moduleName]);

  const backendJavaBeanNameList = useMemo(() => [
    `实体类：${moduleName}Entity.java`,
    `表现类：${moduleName}VO.java`,
    `新建类：${moduleName}AddForm.java`,
    `更新类：${moduleName}UpdateForm.java`,
    `查询类：${moduleName}QueryForm.java`,
  ], [moduleName]);

  const backendConstNameList = useMemo(() => [
    `枚举类：${moduleName}Enum.java`,
    `常量类：${moduleName}Const.java`,
  ], [moduleName]);

  const previewStyle: React.CSSProperties = { padding: '10px 5px', background: '#f5f5f5', fontSize: 14, marginBottom: 8 };

  return (
    <>
      <Alert
        closable
        message="默认数据库表名前缀为：t_， 如果想修改默认前缀，请在下方修改表前缀"
        type="success"
        showIcon
        icon={<SmileOutlined />}
        style={{ marginBottom: 10 }}
      />
      <Row style={{ marginTop: 10 }}>
        <Col flex="350px">
          <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 16 }}>
            <Form.Item label="表">{tableInfo.tableName}</Form.Item>
            <Form.Item label="表备注">{tableInfo.tableComment}</Form.Item>
            <Form.Item label="表前缀">
              <Input
                value={tablePrefix}
                onChange={(e) => onChangeTablePrefix(e.target.value)}
                placeholder="请输入 表前缀"
              />
            </Form.Item>
            <Form.Item label="单词命名" name="moduleName" rules={[{ required: true, message: '请输入 单词命名' }]}>
              <Input placeholder="请输入 单词命名" />
            </Form.Item>
            <Form.Item label="Java包名" name="javaPackageName" rules={[{ required: true, message: '请输入 java包名' }]}>
              <Input placeholder="请输入 Java包名" />
            </Form.Item>
            <Form.Item label="注释信息" name="description">
              <Input placeholder="请输入 注释信息" />
            </Form.Item>
            <Form.Item label="前端作者" name="frontAuthor" rules={[{ required: true, message: '请输入 前端作者' }]}>
              <Input placeholder="请输入 前端作者" />
            </Form.Item>
            <Form.Item label="前端时间" name="frontDate" rules={[{ required: true, message: '请输入 前端时间' }]}>
              <DatePicker style={{ width: '100%' }} showTime format={TIME_FORMAT} placeholder="请输入 前端时间" />
            </Form.Item>
            <Form.Item label="后端作者" name="backendAuthor" rules={[{ required: true, message: '请输入 后端作者' }]}>
              <Input placeholder="请输入 后端作者" />
            </Form.Item>
            <Form.Item label="后端时间" name="backendDate" rules={[{ required: true, message: '请输入 后端时间' }]}>
              <DatePicker style={{ width: '100%' }} showTime format={TIME_FORMAT} placeholder="请输入 后端时间" />
            </Form.Item>
            <Form.Item label="版权信息" name="copyright" rules={[{ required: true, message: '请输入 版权' }]}>
              <Input placeholder="请输入 版权信息" />
            </Form.Item>
          </Form>
        </Col>
        <Col flex="auto" style={{ maxHeight: '100vh', overflowY: 'auto' }}>
          <Tabs
            activeKey={previewTab}
            onChange={setPreviewTab}
            size="small"
            items={[
              {
                key: '1',
                label: '前端文件命名',
                children: (
                  <>
                    <div style={{ fontWeight: 600, margin: '5px 0' }}>前端文件名</div>
                    <div style={previewStyle}>
                      {frontNameList.map((item) => <div key={item}>{item}</div>)}
                    </div>
                    <div style={{ fontWeight: 600, margin: '5px 0' }}>前端Vue文件注释</div>
                    <pre style={previewStyle}>{`<!--
  * ${description}
  *
  * @Author:     ${frontAuthor}
  * @Date:       ${frontDateStr}
  * @Copyright   ${copyright}
-->`}</pre>
                    <div style={{ fontWeight: 600, margin: '5px 0' }}>前端Js文件注释</div>
                    <pre style={previewStyle}>{`/*
 * ${description}
 *
 * @Author:     ${frontAuthor}
 * @Date:       ${frontDateStr}
 * @Copyright   ${copyright}
 */`}</pre>
                  </>
                ),
              },
              {
                key: '2',
                label: '后端文件命名',
                children: (
                  <>
                    <div style={{ fontWeight: 600, margin: '5px 0' }}>后端-四层代码：</div>
                    <div style={previewStyle}>
                      {backendMvcNameList.map((item) => <div key={item}>{item}</div>)}
                    </div>
                    <div style={{ fontWeight: 600, margin: '5px 0' }}>JavaBean代码：</div>
                    <div style={previewStyle}>
                      {backendJavaBeanNameList.map((item) => <div key={item}>{item}</div>)}
                    </div>
                    <div style={{ fontWeight: 600, margin: '5px 0' }}>常量代码：</div>
                    <div style={previewStyle}>
                      {backendConstNameList.map((item) => <div key={item}>{item}</div>)}
                    </div>
                    <div style={{ fontWeight: 600, margin: '5px 0' }}>注释：</div>
                    <pre style={previewStyle}>{`/**
 * ${description}
 *
 * @Author:     ${backendAuthor}
 * @Date:       ${backendDateStr}
 * @Copyright   ${copyright}
 */`}</pre>
                  </>
                ),
              },
            ]}
          />
        </Col>
      </Row>
    </>
  );
});

BasicNaming.displayName = 'BasicNaming';
export default BasicNaming;
