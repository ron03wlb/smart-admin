/**
 * Dict Form Modal
 *
 * Corresponds to Vue's support/dict/components/dict-form-modal.vue
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { dictApi } from '@/api/support/dict-api';
import type { DictVO } from '@/types/dict.types';

interface Props {
  open: boolean;
  dict?: DictVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const DictFormModal: React.FC<Props> = ({ open, dict, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!dict;

  useEffect(() => {
    if (open) {
      if (dict) {
        form.setFieldsValue(dict);
      } else {
        form.resetFields();
      }
    }
  }, [open, dict, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await dictApi.update({ ...values, dictId: dict!.dictId });
      } else {
        await dictApi.add(values);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? '编辑字典' : '添加字典'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} destroyOnClose>
      <Form form={form} labelCol={{ span: 5 }}>
        <Form.Item label="字典编码" name="dictCode" rules={[{ required: true, message: '请输入字典编码' }]}>
          <Input placeholder="请输入字典编码" />
        </Form.Item>
        <Form.Item label="字典名称" name="dictName" rules={[{ required: true, message: '请输入字典名称' }]}>
          <Input placeholder="请输入字典名称" />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={2} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DictFormModal;
