/**
 * Change Log Form Modal
 *
 * Corresponds to Vue's support/change-log/change-log-form.vue
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, DatePicker, Radio, message } from 'antd';
import { changeLogApi } from '@/api/support/change-log-api';
import type { ChangeLogVO } from '@/api/support/change-log-api';
import dayjs from 'dayjs';

interface Props {
  open: boolean;
  changeLog?: ChangeLogVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const ChangeLogForm: React.FC<Props> = ({ open, changeLog, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!changeLog;

  useEffect(() => {
    if (open) {
      if (changeLog) {
        form.setFieldsValue({
          ...changeLog,
          publicDate: changeLog.publicDate ? dayjs(changeLog.publicDate) : undefined,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ type: 2 });
      }
    }
  }, [open, changeLog, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const payload = {
        ...values,
        publicDate: values.publicDate?.format('YYYY-MM-DD'),
      };
      if (isEdit) {
        await changeLogApi.update({ ...payload, changeLogId: changeLog!.changeLogId });
      } else {
        await changeLogApi.add(payload);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? '编辑更新日志' : '添加更新日志'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={700} destroyOnClose>
      <Form form={form} labelCol={{ span: 5 }}>
        <Form.Item label="版本号" name="updateVersion" rules={[{ required: true, message: '请输入版本号' }]}>
          <Input placeholder="例如: v1.0.0" maxLength={50} />
        </Form.Item>
        <Form.Item label="更新类型" name="type" rules={[{ required: true }]}>
          <Radio.Group>
            <Radio value={1}>重大更新</Radio>
            <Radio value={2}>功能更新</Radio>
            <Radio value={3}>Bug修复</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="发布人" name="publishAuthor" rules={[{ required: true, message: '请输入发布人' }]}>
          <Input placeholder="请输入发布人" maxLength={50} />
        </Form.Item>
        <Form.Item label="发布日期" name="publicDate" rules={[{ required: true, message: '请选择发布日期' }]}>
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="跳转链接" name="link">
          <Input placeholder="可选，例如 https://example.com" maxLength={500} />
        </Form.Item>
        <Form.Item label="更新内容" name="content" rules={[{ required: true, message: '请输入更新内容' }]}>
          <Input.TextArea rows={12} maxLength={5000} placeholder="请输入更新内容" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ChangeLogForm;
