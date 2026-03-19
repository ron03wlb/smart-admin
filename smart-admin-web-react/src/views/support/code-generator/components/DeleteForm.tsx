/**
 * Code Generator Delete Form - 刪除表單（簡化版）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/components/form/code-generator-table-config-form-delete.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useImperativeHandle, forwardRef } from 'react';
import { Form, Radio, Alert } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';

export interface DeleteFormRef {
  setData: (columns: any[], config: any) => void;
  getFormData: () => any;
  validateForm: () => Promise<boolean>;
}

const DeleteForm = forwardRef<DeleteFormRef>((_props, ref) => {
  const [form] = Form.useForm();

  /**
   * 設置數據
   */
  const setData = (columns: any[], config: any) => {
    const deleteInfo = config?.deleteInfo || {};

    const formData = {
      isSupportDelete: deleteInfo.isSupportDelete !== false,
      isBatchDelete: deleteInfo.isBatchDelete || false,
    };

    form.setFieldsValue(formData);
  };

  /**
   * 獲取表單數據
   */
  const getFormData = () => {
    return form.getFieldsValue();
  };

  /**
   * 驗證表單
   */
  const validateForm = (): Promise<boolean> => {
    return new Promise((resolve) => {
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

  // 暴露方法給父組件
  useImperativeHandle(ref, () => ({
    setData,
    getFormData,
    validateForm,
  }));

  return (
    <div>
      <Alert
        closable
        message="完整刪除配置功能（單條刪除、批量刪除、字段選擇等）將在 Phase 3 實現"
        type="info"
        showIcon
        icon={<DeleteOutlined />}
        style={{ marginBottom: 16 }}
      />

      <Form form={form} labelCol={{ span: 4 }} wrapperCol={{ span: 16 }} style={{ maxWidth: 600 }}>
        <Form.Item label="是否支持刪除" name="isSupportDelete" initialValue={true}>
          <Radio.Group buttonStyle="solid">
            <Radio.Button value={true}>支持</Radio.Button>
            <Radio.Button value={false}>不支持刪除</Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item label="是否批量刪除" name="isBatchDelete" initialValue={false}>
          <Radio.Group buttonStyle="solid">
            <Radio.Button value={true}>支持</Radio.Button>
            <Radio.Button value={false}>不支持</Radio.Button>
          </Radio.Group>
        </Form.Item>
      </Form>
    </div>
  );
});

DeleteForm.displayName = 'DeleteForm';

export default DeleteForm;
