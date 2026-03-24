/**
 * Dict Form Modal
 * 字典表單 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { dictApi } from '@/api/support/dictApi';
import type { DictVO, DictFormData } from '../types';

export interface DictFormModalProps {
  visible: boolean;
  dict?: DictVO;
  onClose: () => void;
  onSuccess: () => void;
}

const DictFormModal: React.FC<DictFormModalProps> = ({ visible, dict, onClose, onSuccess }) => {
  const [form] = Form.useForm<DictFormData>();
  const [loading, setLoading] = useState(false);

  // 當 visible 或 dict 變化時，更新表單
  useEffect(() => {
    if (visible) {
      if (dict) {
        form.setFieldsValue({
          dictId: dict.dictId,
          dictCode: dict.dictCode,
          dictName: dict.dictName,
          remark: dict.remark,
        });
      } else {
        form.resetFields();
      }
    }
  }, [visible, dict, form]);

  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      setLoading(true);
      if (dict?.dictId) {
        await dictApi.updateDict(values as any);
        message.success('修改成功');
      } else {
        await dictApi.addDict(values as any);
        message.success('添加成功');
      }

      onSuccess();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據!');
      } else {
        console.error('Failed to save dict:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      open={visible}
      title={dict?.dictId ? '編輯字典' : '添加字典'}
      okText="確認"
      cancelText="取消"
      onOk={handleSubmit}
      onCancel={handleClose}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 19 }} style={{ marginTop: 20 }}>
        <Form.Item name="dictId" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          label="字典編碼"
          name="dictCode"
          rules={[{ required: true, message: '請輸入編碼' }]}
        >
          <Input placeholder="請輸入編碼" />
        </Form.Item>

        <Form.Item
          label="字典名稱"
          name="dictName"
          rules={[{ required: true, message: '請輸入名稱' }]}
        >
          <Input placeholder="請輸入名稱" />
        </Form.Item>

        <Form.Item label="備註" name="remark">
          <Input.TextArea rows={4} placeholder="請輸入備註" style={{ resize: 'none' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DictFormModal;
