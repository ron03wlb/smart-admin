/**
 * Dict Data Form Modal
 *
 * Corresponds to Vue's support/dict/components/dict-data-form-modal.vue
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { dictApi } from '@/api/support/dict-api';
import type { DictDataVO } from '@/types/dict.types';

interface Props {
  open: boolean;
  dictId: number;
  dictData?: DictDataVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const DictDataFormModal: React.FC<Props> = ({ open, dictId, dictData, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!dictData;

  useEffect(() => {
    if (open) {
      if (dictData) {
        form.setFieldsValue(dictData);
      } else {
        form.resetFields();
      }
    }
  }, [open, dictData, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await dictApi.updateDictData({ ...values, dictDataId: dictData!.dictDataId, dictId });
      } else {
        await dictApi.addDictData({ ...values, dictId });
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? '编辑字典数据' : '添加字典数据'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} destroyOnClose>
      <Form form={form} labelCol={{ span: 5 }}>
        <Form.Item label="显示文本" name="dataLabel" rules={[{ required: true, message: '请输入显示文本' }]}>
          <Input placeholder="请输入显示文本" />
        </Form.Item>
        <Form.Item label="值" name="dataValue" rules={[{ required: true, message: '请输入值' }]}>
          <Input placeholder="请输入值" />
        </Form.Item>
        <Form.Item label="排序" name="sortValue">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={2} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DictDataFormModal;
