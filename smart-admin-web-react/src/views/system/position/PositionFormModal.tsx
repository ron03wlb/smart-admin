/**
 * Position Form Modal (Add/Edit)
 *
 * Corresponds to Vue's views/system/position/position-form.vue
 */
import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { positionApi } from '@/api/system/position-api';
import type { PositionVO, PositionAddForm, PositionUpdateForm } from '@/types/position.types';

interface PositionFormModalProps {
  visible: boolean;
  record: PositionVO | null;
  onCancel: () => void;
  onSuccess: () => void;
}

const PositionFormModal: React.FC<PositionFormModalProps> = ({
  visible,
  record,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);
  const isEdit = record !== null;

  useEffect(() => {
    if (visible && record) {
      form.setFieldsValue(record);
    } else if (visible) {
      form.resetFields();
    }
  }, [visible, record, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit) {
        const res = await positionApi.update({
          ...values,
          positionId: record.positionId,
        } as PositionUpdateForm);
        if (res.code === 1) {
          message.success('操作成功');
          onSuccess();
        }
      } else {
        const res = await positionApi.add(values as PositionAddForm);
        if (res.code === 1) {
          message.success('操作成功');
          onSuccess();
        }
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑' : '添加'}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 6 }} autoComplete="off">
        <Form.Item
          label="职务名称"
          name="positionName"
          rules={[{ required: true, message: '请输入职务名称' }]}
        >
          <Input placeholder="职务名称" />
        </Form.Item>
        <Form.Item label="职级" name="positionLevel">
          <Input placeholder="职级" />
        </Form.Item>
        <Form.Item label="排序" name="sort" initialValue={0}>
          <InputNumber min={0} step={1} precision={0} style={{ width: '100%' }} placeholder="排序" />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input placeholder="备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default PositionFormModal;
