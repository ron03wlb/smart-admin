/**
 * Serial Number Generate Form Modal
 *
 * Corresponds to Vue's support/serial-number/components/serial-number-generate-form.vue (107L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, InputNumber, Input, message } from 'antd';
import { serialNumberApi } from '@/api/support/serial-number-api';
import type { SerialNumberVO } from '@/api/support/serial-number-api';

interface Props {
  open: boolean;
  serialNumber: SerialNumberVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const SerialNumberGenerateFormModal: React.FC<Props> = ({ open, serialNumber, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState('');

  useEffect(() => {
    if (open) {
      form.setFieldsValue({ count: 1 });
      setResult('');
    }
  }, [open, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const res = await serialNumberApi.generate({
        serialNumberId: serialNumber.serialNumberId,
        count: values.count,
      });
      if (res.code === 1 && res.data) {
        setResult(res.data.join('\n'));
        message.success('生成成功');
        onSuccess();
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="生成序列号"
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="业务名称">
          <span>{serialNumber.businessName}</span>
        </Form.Item>
        <Form.Item label="格式">
          <span>{serialNumber.format}</span>
        </Form.Item>
        <Form.Item label="最后生成">
          <span>{serialNumber.lastNumber || '-'}</span>
        </Form.Item>
        <Form.Item label="生成数量" name="count" rules={[{ required: true, message: '请输入生成数量' }]}>
          <InputNumber min={1} max={100} style={{ width: '100%' }} />
        </Form.Item>
        {result && (
          <Form.Item label="生成结果">
            <Input.TextArea value={result} rows={4} readOnly />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default SerialNumberGenerateFormModal;
