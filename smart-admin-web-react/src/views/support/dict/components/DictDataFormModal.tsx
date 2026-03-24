/**
 * Dict Data Form Modal
 * 字典值表單 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { dictApi } from '@/api/support/dictApi';
import type { DictDataVO, DictDataFormData } from '../types';

export interface DictDataFormModalProps {
  visible: boolean;
  dictData?: DictDataVO;
  dictId: number;
  dictCode: string;
  onClose: () => void;
  onSuccess: () => void;
}

const DictDataFormModal: React.FC<DictDataFormModalProps> = ({
  visible,
  dictData,
  dictId,
  dictCode,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm<DictDataFormData>();
  const [loading, setLoading] = useState(false);

  // 當 visible 或 dictData 變化時，更新表單
  useEffect(() => {
    if (visible) {
      if (dictData) {
        form.setFieldsValue({
          dictDataId: dictData.dictDataId,
          dictId: dictData.dictId,
          dictCode: dictData.dictCode,
          dataValue: dictData.dataValue,
          dataLabel: dictData.dataLabel,
          sortOrder: dictData.sortOrder,
          remark: dictData.remark,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          dictId,
          dictCode,
          sortOrder: 0,
        });
      }
    }
  }, [visible, dictData, dictId, dictCode, form]);

  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      setLoading(true);
      if (dictData?.dictDataId) {
        await dictApi.updateDictData(values as any);
        message.success('修改成功');
      } else {
        await dictApi.addDictData(values as any);
        message.success('添加成功');
      }

      onSuccess();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據!');
      } else {
        console.error('Failed to save dict data:', error);
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
      title={dictData?.dictDataId ? '編輯字典值' : '添加字典值'}
      okText="確認"
      cancelText="取消"
      onOk={handleSubmit}
      onCancel={handleClose}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 16 }} style={{ marginTop: 20 }}>
        <Form.Item name="dictDataId" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="dictId" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="dictCode" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          label="字典項名稱"
          name="dataLabel"
          rules={[{ required: true, message: '請輸入 字典項名稱' }]}
        >
          <Input placeholder="請輸入 字典項名稱" />
        </Form.Item>

        <Form.Item
          label="字典項值"
          name="dataValue"
          rules={[{ required: true, message: '請輸入 字典項值' }]}
        >
          <Input placeholder="請輸入 字典項值" />
        </Form.Item>

        <Form.Item
          label="排序"
          name="sortOrder"
          rules={[{ required: true, message: '請輸入排序' }]}
          help="值越大越靠前"
        >
          <InputNumber min={0} max={1000} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="備註" name="remark">
          <Input.TextArea rows={4} placeholder="請輸入備註" style={{ resize: 'none' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DictDataFormModal;
