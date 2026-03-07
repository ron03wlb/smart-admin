/**
 * Do Reload Form Modal
 *
 * Corresponds to Vue's support/reload/components/do-reload-form.vue (90L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { reloadApi } from '@/api/support/reload-api';
import type { ReloadVO } from '@/api/support/reload-api';

interface Props {
  open: boolean;
  reload: ReloadVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const DoReloadFormModal: React.FC<Props> = ({ open, reload, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      form.setFieldsValue({
        identification: reload.identification,
        args: reload.args,
      });
    }
  }, [open, reload, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      await reloadApi.reload({
        tag: reload.tag,
        identification: values.identification,
        args: values.args,
      });
      message.success('执行成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="执行Reload"
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="Tag">
          <span>{reload.tag}</span>
        </Form.Item>
        <Form.Item label="Identification" name="identification" rules={[{ required: true, message: '请输入Identification' }]}>
          <Input placeholder="请输入Identification" />
        </Form.Item>
        <Form.Item label="Args" name="args" rules={[{ required: true, message: '请输入Args' }]}>
          <Input placeholder="请输入Args" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DoReloadFormModal;
