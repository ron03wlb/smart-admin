/**
 * Job Form Modal
 *
 * Corresponds to Vue's support/job/components/job-form-modal.vue (264L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Radio, Switch, message } from 'antd';
import { jobApi } from '@/api/support/job-api';
import type { JobVO } from '@/api/support/job-api';

interface Props {
  open: boolean;
  job?: JobVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const JobFormModal: React.FC<Props> = ({ open, job, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!job;

  useEffect(() => {
    if (open) {
      if (job) {
        form.setFieldsValue(job);
      } else {
        form.resetFields();
        form.setFieldsValue({ triggerType: 'CRON', enabledFlag: true, sort: 0 });
      }
    }
  }, [open, job, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await jobApi.update({ ...values, jobId: job!.jobId });
      } else {
        await jobApi.add(values);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  const triggerType = Form.useWatch('triggerType', form);

  return (
    <Modal title={isEdit ? '编辑任务' : '添加任务'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={650} destroyOnClose>
      <Form form={form} labelCol={{ span: 5 }}>
        <Form.Item label="任务名称" name="jobName" rules={[{ required: true, message: '请输入任务名称' }]}>
          <Input placeholder="请输入任务名称" maxLength={100} />
        </Form.Item>
        <Form.Item label="任务描述" name="remark">
          <Input.TextArea rows={2} maxLength={250} placeholder="请输入任务描述" />
        </Form.Item>
        <Form.Item label="排序" name="sort">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="任务类" name="jobClass" rules={[{ required: true, message: '请输入任务类' }]}>
          <Input.TextArea rows={2} maxLength={200} placeholder="请输入完整的Java类路径" />
        </Form.Item>
        <Form.Item label="执行参数" name="param">
          <Input.TextArea rows={2} maxLength={1000} placeholder="JSON格式参数" />
        </Form.Item>
        <Form.Item label="触发类型" name="triggerType" rules={[{ required: true }]}>
          <Radio.Group>
            <Radio value="CRON">CRON表达式</Radio>
            <Radio value="FIXED_DELAY">固定延迟</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="触发配置" name="triggerValue" rules={[{ required: true, message: '请输入触发配置' }]}>
          {triggerType === 'FIXED_DELAY' ? (
            <InputNumber min={1} addonAfter="秒" style={{ width: '100%' }} />
          ) : (
            <Input placeholder="例如: 0 0/5 * * * ?" />
          )}
        </Form.Item>
        <Form.Item label="是否启用" name="enabledFlag" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default JobFormModal;
