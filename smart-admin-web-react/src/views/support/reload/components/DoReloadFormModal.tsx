/**
 * Do Reload Form Modal
 * 執行 Reload 表單 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { reloadApi } from '@/api/support/reloadApi';
import type { ReloadFormData } from '../types';

export interface DoReloadFormModalProps {
  onRefresh: () => void;
}

export interface DoReloadFormModalRef {
  showModal: (tag: string) => void;
}

const DoReloadFormModal = forwardRef<DoReloadFormModalRef, DoReloadFormModalProps>(
  ({ onRefresh }, ref) => {
    const [form] = Form.useForm<ReloadFormData>();
    const [visible, setVisible] = useState(false);
    const [loading, setLoading] = useState(false);

    useImperativeHandle(ref, () => ({
      showModal: (tag: string) => {
        form.resetFields();
        form.setFieldsValue({
          tag,
          identification: '',
          args: '',
        });
        setVisible(true);
      },
    }));

    const handleSubmit = async () => {
      try {
        await form.validateFields();
        const values = form.getFieldsValue();

        setLoading(true);
        await reloadApi.reload(values);
        message.success('reload成功');
        setVisible(false);
        form.resetFields();
        onRefresh();
      } catch (error: any) {
        if (error.errorFields) {
          message.error('參數驗證錯誤，請仔細填寫表單數據!');
        } else {
          console.error('Failed to reload:', error);
        }
      } finally {
        setLoading(false);
      }
    };

    const handleCancel = () => {
      setVisible(false);
      form.resetFields();
    };

    return (
      <Modal
        open={visible}
        title="執行Reload"
        okText="確認"
        cancelText="取消"
        onOk={handleSubmit}
        onCancel={handleCancel}
        confirmLoading={loading}
      >
        <Form form={form} labelCol={{ span: 5 }} style={{ marginTop: 20 }}>
          <Form.Item label="標籤" name="tag">
            <Input disabled />
          </Form.Item>

          <Form.Item
            label="運行標識"
            name="identification"
            rules={[{ required: true, message: '請輸入運行標識' }]}
          >
            <Input placeholder="請輸入運行標識" />
          </Form.Item>

          <Form.Item label="參數" name="args" rules={[{ required: true, message: '請輸入參數值' }]}>
            <Input placeholder="請輸入參數" />
          </Form.Item>
        </Form>
      </Modal>
    );
  }
);

DoReloadFormModal.displayName = 'DoReloadFormModal';

export default DoReloadFormModal;
