/**
 * Tab 4: Delete Configuration
 *
 * Configure whether the module supports deletion, physical vs logical delete,
 * and delete strategy (single, batch, or both).
 */
import { useState, useEffect, useImperativeHandle, forwardRef, useRef } from 'react';
import { Form, Radio, message } from 'antd';
import { useCodeGeneratorContext } from '../../CodeGeneratorContext';
import { CODE_DELETE_TYPE_LIST } from '../../code-generator-util';
import { Select } from 'antd';

export interface DeleteConfigRef {
  validateForm: () => Promise<boolean>;
  getForm: () => DeleteFormData;
}

interface DeleteFormData {
  isSupportDelete: boolean;
  isPhysicallyDeleted?: boolean;
  deleteType?: string;
}

const DeleteConfig = forwardRef<DeleteConfigRef>((_props, ref) => {
  const { tableInfo, tableColumns, tableConfig } = useCodeGeneratorContext();
  const [form] = Form.useForm();
  const [isSupportDelete, setIsSupportDelete] = useState(true);
  const [isPhysicallyDeleted, setIsPhysicallyDeleted] = useState<boolean | undefined>(undefined);
  const [deleteFlagColumnName, setDeleteFlagColumnName] = useState('');
  const initializedRef = useRef(false);

  useEffect(() => {
    if (!tableConfig || !tableColumns.length || initializedRef.current) return;
    initializedRef.current = true;

    // Check for deleted_flag column
    const deletedFlagCol = tableColumns.find(
      (c) => c.columnName.startsWith('deleted_flag') || c.columnName.startsWith('delete_flag'),
    );
    if (deletedFlagCol) {
      setDeleteFlagColumnName(deletedFlagCol.columnName);
    }

    const deleteInfo = tableConfig.deleteInfo;
    const support = deleteInfo?.isSupportDelete ?? true;
    const physically = deleteInfo?.isPhysicallyDeleted ?? !deletedFlagCol;

    setIsSupportDelete(support);
    setIsPhysicallyDeleted(physically);

    form.setFieldsValue({
      isSupportDelete: support,
      isPhysicallyDeleted: physically,
      deleteType: deleteInfo?.deleteType || 'SingleAndBatch',
    });
  }, [tableConfig, tableColumns, form]);

  useImperativeHandle(ref, () => ({
    validateForm: async () => {
      if (!isSupportDelete) return true;
      try {
        await form.validateFields();
        return true;
      } catch {
        message.error('请检查【4.删除】表单，有参数验证错误');
        return false;
      }
    },
    getForm: () => {
      const values = form.getFieldsValue();
      return {
        isSupportDelete: values.isSupportDelete,
        isPhysicallyDeleted: values.isPhysicallyDeleted,
        deleteType: values.deleteType,
      };
    },
  }));

  return (
    <Form form={form} labelCol={{ span: 6 }} style={{ width: 600, marginTop: 10 }}>
      <Form.Item label="数据库表名词">{tableInfo.tableName}</Form.Item>
      <Form.Item label="数据库表备注">{tableInfo.tableComment}</Form.Item>
      <Form.Item label="是否允许删除" name="isSupportDelete">
        <Radio.Group
          buttonStyle="solid"
          onChange={(e) => setIsSupportDelete(e.target.value)}
        >
          <Radio.Button value={true}>支持删除</Radio.Button>
          <Radio.Button value={false}>不允许删除</Radio.Button>
        </Radio.Group>
      </Form.Item>
      {isSupportDelete && (
        <>
          <Form.Item label="是否为物理删除" name="isPhysicallyDeleted" rules={[{ required: true, message: '请输入 是否为物理删除' }]}>
            <Radio.Group
              buttonStyle="solid"
              onChange={(e) => setIsPhysicallyDeleted(e.target.value)}
            >
              <Radio.Button value={true}>物理删除</Radio.Button>
              <Radio.Button value={false}>假删</Radio.Button>
            </Radio.Group>
          </Form.Item>
          {isPhysicallyDeleted === false && (
            <Form.Item label=" " colon={false}>
              {deleteFlagColumnName ? (
                <span>假删字段为：{deleteFlagColumnName}</span>
              ) : (
                <span style={{ color: 'red' }}>
                  系统未检测出假删字段，假删字段名词应该为：<strong>deleted_flag</strong>
                </span>
              )}
            </Form.Item>
          )}
          <Form.Item label="删除类型" name="deleteType" rules={[{ required: true, message: '请输入 删除类型' }]}>
            <Select style={{ width: 200 }}>
              {CODE_DELETE_TYPE_LIST.map((item) => (
                <Select.Option key={item.value} value={item.value}>{item.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
        </>
      )}
    </Form>
  );
});

DeleteConfig.displayName = 'DeleteConfig';
export default DeleteConfig;
