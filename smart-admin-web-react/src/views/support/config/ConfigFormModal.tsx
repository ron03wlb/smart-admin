/**
 * Config Form Modal
 *
 * Corresponds to Vue's support/config/components/config-form.vue (99L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { configApi } from '@/api/support/config-api';
import type { ConfigVO } from '@/api/support/config-api';

interface Props {
  open: boolean;
  config?: ConfigVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const ConfigFormModal: React.FC<Props> = ({ open, config, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!config;

  useEffect(() => {
    if (open) {
      if (config) {
        form.setFieldsValue(config);
      } else {
        form.resetFields();
      }
    }
  }, [open, config, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await configApi.updateConfig({ ...values, configId: config!.configId });
      } else {
        await configApi.addConfig(values);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑参数' : '添加参数'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 5 }}>
        <Form.Item label="参数Key" name="configKey" rules={[{ required: true, message: '请输入参数Key' }]}>
          <Input placeholder="请输入参数Key" />
        </Form.Item>
        <Form.Item label="参数名称" name="configName" rules={[{ required: true, message: '请输入参数名称' }]}>
          <Input placeholder="请输入参数名称" />
        </Form.Item>
        <Form.Item label="参数值" name="configValue" rules={[{ required: true, message: '请输入参数值' }]}>
          <Input.TextArea rows={3} placeholder="请输入参数值" />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={2} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ConfigFormModal;
