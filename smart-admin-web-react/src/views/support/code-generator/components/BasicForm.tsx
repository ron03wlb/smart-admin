/**
 * Code Generator Basic Form - 基礎命名表單
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/components/form/code-generator-table-config-form-basic.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useImperativeHandle, forwardRef, useMemo } from 'react';
import { Form, Input, DatePicker, Alert, Row, Col, Tabs } from 'antd';
import { SmileOutlined } from '@ant-design/icons';
import type { TableInfo, BasicConfig, CodeGeneratorConfig } from '@/api/support/codeGeneratorApi';
import { convertUpperCamel, convertLowerHyphen } from '../utils/codeGeneratorUtils';
import dayjs, { type Dayjs } from 'dayjs';
import './BasicForm.css';

export interface BasicFormRef {
  setData: (config: CodeGeneratorConfig, table: TableInfo) => void;
  getFormData: () => BasicConfig;
  validateForm: () => Promise<boolean>;
}

interface FormData {
  moduleName?: string;
  javaPackageName?: string;
  description?: string;
  frontAuthor?: string;
  frontDate?: Dayjs;
  backendAuthor?: string;
  backendDate?: Dayjs;
  copyright?: string;
}

const BasicForm = forwardRef<BasicFormRef>((_props, ref) => {
  const [form] = Form.useForm<FormData>();
  const [tablePrefix, setTablePrefix] = useState('t_');
  const [currentTable, setCurrentTable] = useState<TableInfo | null>(null);
  const [previewActiveKey, setPreviewActiveKey] = useState('1');

  // 監聽表單值變化以更新預覽
  const [formValues, setFormValues] = useState<FormData>({});

  /**
   * 設置表單數據
   */
  const setData = (config: CodeGeneratorConfig, table: TableInfo) => {
    setCurrentTable(table);
    const basic = config?.basic;

    // 處理表前綴和模塊名稱
    let removePrefixTableName = table.tableName;
    if (table.tableName.startsWith(tablePrefix)) {
      removePrefixTableName = table.tableName.replace(tablePrefix, '');
    }

    const moduleName = basic?.moduleName || convertUpperCamel(removePrefixTableName);
    const description = basic?.description || table.tableComment;
    const frontDate = basic?.frontDate
      ? dayjs(basic.frontDate)
      : table.createTime
        ? dayjs(table.createTime)
        : dayjs();
    const backendDate = basic?.backendDate
      ? dayjs(basic.backendDate)
      : table.createTime
        ? dayjs(table.createTime)
        : dayjs();

    const initialValues: FormData = {
      moduleName,
      javaPackageName: basic?.javaPackageName || undefined,
      description,
      frontAuthor: basic?.frontAuthor || undefined,
      frontDate,
      backendAuthor: basic?.backendAuthor || undefined,
      backendDate,
      copyright: basic?.copyright || undefined,
    };

    form.setFieldsValue(initialValues);
    setFormValues(initialValues);
  };

  /**
   * 獲取表單數據
   */
  const getFormData = (): BasicConfig => {
    const values = form.getFieldsValue();
    return {
      ...values,
      frontDate: values.frontDate?.format('YYYY-MM-DD HH:mm:ss'),
      backendDate: values.backendDate?.format('YYYY-MM-DD HH:mm:ss'),
    };
  };

  /**
   * 驗證表單
   */
  const validateForm = (): Promise<boolean> => {
    return new Promise(resolve => {
      form
        .validateFields()
        .then(() => {
          resolve(true);
        })
        .catch(() => {
          resolve(false);
        });
    });
  };

  /**
   * 表前綴變化
   */
  const handleTablePrefixChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newPrefix = e.target.value;
    setTablePrefix(newPrefix);

    if (currentTable) {
      let removePrefixTableName = currentTable.tableName;
      if (currentTable.tableName.startsWith(newPrefix)) {
        removePrefixTableName = currentTable.tableName.replace(newPrefix, '');
      }
      const newModuleName = convertUpperCamel(removePrefixTableName);
      form.setFieldValue('moduleName', newModuleName);
      setFormValues({ ...formValues, moduleName: newModuleName });
    }
  };

  /**
   * 表單值變化
   */
  const handleValuesChange = (_changedValues: any, allValues: FormData) => {
    setFormValues(allValues);
  };

  // 暴露方法給父組件
  useImperativeHandle(ref, () => ({
    setData,
    getFormData,
    validateForm,
  }));

  /**
   * 前端文件名預覽
   */
  const frontName = useMemo(() => {
    return convertLowerHyphen(formValues.moduleName || '');
  }, [formValues.moduleName]);

  const frontNameList = useMemo(() => {
    return [
      `請求：${frontName}-api.js`,
      `常量：${frontName}-const.js`,
      `列表：${frontName}-list.vue`,
      `表單：${frontName}-form-modal.vue`,
      `詳情：${frontName}-detail.vue`,
    ];
  }, [frontName]);

  /**
   * 後端文件名預覽
   */
  const backendMvcNameList = useMemo(() => {
    const moduleName = formValues.moduleName || '';
    return [
      `控制層：${moduleName}Controller.java`,
      `業務層：${moduleName}Service.java`,
      `中間層：${moduleName}Manager.java`,
      `持久層：${moduleName}Dao.java`,
      `SQL層：${moduleName}Mapper.xml`,
    ];
  }, [formValues.moduleName]);

  const backendJavaBeanNameList = useMemo(() => {
    const moduleName = formValues.moduleName || '';
    return [
      `實體類：${moduleName}Entity.java`,
      `表現類：${moduleName}VO.java`,
      `新建類：${moduleName}AddForm.java`,
      `更新類：${moduleName}UpdateForm.java`,
      `查詢類：${moduleName}QueryForm.java`,
    ];
  }, [formValues.moduleName]);

  const backendConstNameList = useMemo(() => {
    const moduleName = formValues.moduleName || '';
    return [`枚舉類：${moduleName}Enum.java`, `常量類：${moduleName}Const.java`];
  }, [formValues.moduleName]);

  /**
   * 注釋預覽
   */
  const frontVueComment = useMemo(() => {
    return `<!--
  * ${formValues.description || ''}
  *
  * @Author:     ${formValues.frontAuthor || ''}
  * @Date:       ${formValues.frontDate?.format('YYYY-MM-DD HH:mm:ss') || ''}
  * @Copyright   ${formValues.copyright || ''}
-->`;
  }, [formValues.description, formValues.frontAuthor, formValues.frontDate, formValues.copyright]);

  const frontJsComment = useMemo(() => {
    return `/*
 * ${formValues.description || ''}
 *
 * @Author:     ${formValues.frontAuthor || ''}
 * @Date:       ${formValues.frontDate?.format('YYYY-MM-DD HH:mm:ss') || ''}
 * @Copyright   ${formValues.copyright || ''}
 */`;
  }, [formValues.description, formValues.frontAuthor, formValues.frontDate, formValues.copyright]);

  const backendComment = useMemo(() => {
    return `/**
 * ${formValues.description || ''}
 *
 * @Author:     ${formValues.backendAuthor || ''}
 * @Date:       ${formValues.backendDate?.format('YYYY-MM-DD HH:mm:ss') || ''}
 * @Copyright   ${formValues.copyright || ''}
 */`;
  }, [
    formValues.description,
    formValues.backendAuthor,
    formValues.backendDate,
    formValues.copyright,
  ]);

  /**
   * 預覽 Tabs
   */
  const previewTabs = [
    {
      key: '1',
      label: '前端文件命名',
      children: (
        <>
          <div className="preview-title">前端文件名</div>
          <div className="preview-block">
            {frontNameList.map((item, index) => (
              <div key={index}>{item}</div>
            ))}
          </div>
          <div className="preview-title">前端Vue文件注釋</div>
          <pre className="preview-block">{frontVueComment}</pre>
          <div className="preview-title">前端Js文件注釋</div>
          <pre className="preview-block">{frontJsComment}</pre>
        </>
      ),
    },
    {
      key: '2',
      label: '後端文件命名',
      children: (
        <>
          <div className="preview-title">後端-四層代碼：</div>
          <div className="preview-block">
            {backendMvcNameList.map((item, index) => (
              <div key={index}>{item}</div>
            ))}
          </div>
          <div className="preview-title">JavaBean代碼：</div>
          <div className="preview-block">
            {backendJavaBeanNameList.map((item, index) => (
              <div key={index}>{item}</div>
            ))}
          </div>
          <div className="preview-title">常量代碼：</div>
          <div className="preview-block">
            {backendConstNameList.map((item, index) => (
              <div key={index}>{item}</div>
            ))}
          </div>
          <div className="preview-title">注釋：</div>
          <pre className="preview-block">{backendComment}</pre>
        </>
      ),
    },
  ];

  return (
    <Row className="basic-form-container">
      {/* 左側：表單 */}
      <Col flex="350px">
        <Alert
          closable
          message="默認數據庫表名前綴為：t_， 如果想修改默認前綴，請修改表前綴輸入框"
          type="success"
          showIcon
          icon={<SmileOutlined />}
          style={{ marginBottom: 16 }}
        />
        <Form
          form={form}
          labelCol={{ span: 5 }}
          wrapperCol={{ span: 16 }}
          onValuesChange={handleValuesChange}
        >
          <Form.Item label="表">{currentTable?.tableName || ''}</Form.Item>
          <Form.Item label="表備註">{currentTable?.tableComment || ''}</Form.Item>
          <Form.Item label="表前綴">
            <Input
              value={tablePrefix}
              onChange={handleTablePrefixChange}
              placeholder="請輸入表前綴"
            />
          </Form.Item>
          <Form.Item
            label="單詞命名"
            name="moduleName"
            rules={[{ required: true, message: '請輸入單詞命名' }]}
          >
            <Input placeholder="請輸入單詞命名" />
          </Form.Item>
          <Form.Item
            label="Java包名"
            name="javaPackageName"
            rules={[{ required: true, message: '請輸入Java包名' }]}
          >
            <Input placeholder="請輸入Java包名" />
          </Form.Item>
          <Form.Item label="註釋信息" name="description">
            <Input placeholder="請輸入註釋信息" />
          </Form.Item>
          <Form.Item
            label="前端作者"
            name="frontAuthor"
            rules={[{ required: true, message: '請輸入前端作者' }]}
          >
            <Input placeholder="請輸入前端作者" />
          </Form.Item>
          <Form.Item
            label="前端時間"
            name="frontDate"
            rules={[{ required: true, message: '請輸入前端時間' }]}
          >
            <DatePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="請輸入前端時間"
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item
            label="後端作者"
            name="backendAuthor"
            rules={[{ required: true, message: '請輸入後端作者' }]}
          >
            <Input placeholder="請輸入後端作者" />
          </Form.Item>
          <Form.Item
            label="後端時間"
            name="backendDate"
            rules={[{ required: true, message: '請輸入後端時間' }]}
          >
            <DatePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="請輸入後端時間"
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item
            label="版權信息"
            name="copyright"
            rules={[{ required: true, message: '請輸入版權信息' }]}
          >
            <Input placeholder="請輸入版權信息" />
          </Form.Item>
        </Form>
      </Col>

      {/* 右側：預覽 */}
      <Col flex="auto" style={{ height: '100vh', overflowY: 'scroll', paddingLeft: 16 }}>
        <Tabs
          activeKey={previewActiveKey}
          onChange={setPreviewActiveKey}
          size="small"
          items={previewTabs}
        />
      </Col>
    </Row>
  );
});

BasicForm.displayName = 'BasicForm';

export default BasicForm;
