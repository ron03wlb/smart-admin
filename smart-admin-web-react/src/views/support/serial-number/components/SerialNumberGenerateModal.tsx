/**
 * Serial Number Generate Modal
 * 單號生成 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { serialNumberApi } from '@/api/support/serialNumberApi';
import type { SerialNumberVO, SerialNumberGenerateForm } from '../types';

export interface SerialNumberGenerateModalProps {
  onRefresh?: () => void;
}

export interface SerialNumberGenerateModalRef {
  show: (record: SerialNumberVO) => void;
}

const SerialNumberGenerateModal = forwardRef<
  SerialNumberGenerateModalRef,
  SerialNumberGenerateModalProps
>(({ onRefresh }, ref) => {
  const [form] = Form.useForm<SerialNumberGenerateForm & SerialNumberVO>();
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [generateResult, setGenerateResult] = useState<string>('');
  const [currentRecord, setCurrentRecord] = useState<SerialNumberVO | null>(null);

  // 暴露 show 方法
  useImperativeHandle(ref, () => ({
    show: (record: SerialNumberVO) => {
      setCurrentRecord(record);
      form.setFieldsValue({
        ...record,
        count: 1,
      });
      setGenerateResult('');
      setVisible(true);
    },
  }));

  const handleClose = () => {
    setVisible(false);
    form.resetFields();
    setGenerateResult('');
    setCurrentRecord(null);
    onRefresh?.();
  };

  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      setLoading(true);
      const res = await serialNumberApi.generate({
        serialNumberId: currentRecord!.serialNumberId,
        count: values.count,
      });

      message.success('生成成功');
      setGenerateResult(res.data.join(', '));
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據!');
      } else {
        console.error('Failed to generate serial number:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={visible}
      title="生成單號"
      okText="生成"
      cancelText="關閉"
      onOk={handleSubmit}
      onCancel={handleClose}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 5 }}>
        <Form.Item label="業務" name="businessName">
          <Input disabled />
        </Form.Item>

        <Form.Item label="格式" name="format">
          <Input disabled />
        </Form.Item>

        <Form.Item label="循環週期" name="ruleType">
          <Input disabled />
        </Form.Item>

        <Form.Item label="上次產生單號" name="lastNumber">
          <Input disabled />
        </Form.Item>

        <Form.Item
          label="生成數量"
          name="count"
          rules={[{ required: true, message: '請輸入數量' }]}
        >
          <InputNumber min={1} max={100} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="生成結果">
          <Input.TextArea
            value={generateResult}
            rows={4}
            readOnly
            placeholder="點擊「生成」按鈕後將顯示結果"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
});

SerialNumberGenerateModal.displayName = 'SerialNumberGenerateModal';

export default SerialNumberGenerateModal;
