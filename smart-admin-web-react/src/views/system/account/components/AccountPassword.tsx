/**
 * Account Password Change
 *
 * Corresponds to Vue's account/components/password/index.vue (127L)
 */
import React, { useState } from 'react';
import { Form, Input, Button, message } from 'antd';
import { employeeApi } from '@/api/system/employee-api';

const AccountPassword: React.FC = () => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的密码不一致');
      return;
    }
    setSubmitting(true);
    try {
      await employeeApi.updatePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      message.success('密码修改成功');
      form.resetFields();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Form form={form} labelCol={{ span: 4 }} wrapperCol={{ span: 12 }} style={{ maxWidth: 600 }}>
      <Form.Item label="原密码" name="oldPassword" rules={[{ required: true, message: '请输入原密码' }]}>
        <Input.Password placeholder="请输入原密码" />
      </Form.Item>
      <Form.Item
        label="新密码"
        name="newPassword"
        rules={[
          { required: true, message: '请输入新密码' },
          { min: 8, message: '密码不少于8位' },
        ]}
      >
        <Input.Password placeholder="请输入新密码" />
      </Form.Item>
      <Form.Item
        label="确认密码"
        name="confirmPassword"
        rules={[{ required: true, message: '请再次输入新密码' }]}
      >
        <Input.Password placeholder="请再次输入新密码" />
      </Form.Item>
      <Form.Item wrapperCol={{ offset: 4 }}>
        <Button type="primary" onClick={handleSubmit} loading={submitting}>
          修改密码
        </Button>
      </Form.Item>
    </Form>
  );
};

export default AccountPassword;
