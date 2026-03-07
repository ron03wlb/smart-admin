/**
 * Account Center - Personal Profile
 *
 * Corresponds to Vue's account/components/center/index.vue (307L)
 * Displays and edits user profile information.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Form, Input, Select, Button, message, Spin } from 'antd';
import { getLoginInfo } from '@/api/system/login.api';
import { employeeApi } from '@/api/system/employee-api';

const AccountCenter: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getLoginInfo();
      if (res.code === 1 && res.data) {
        form.setFieldsValue(res.data);
      }
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      await employeeApi.updateCenter(values);
      message.success('更新成功');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Spin spinning={loading}>
      <Form form={form} labelCol={{ span: 4 }} wrapperCol={{ span: 12 }} style={{ maxWidth: 600 }}>
        <Form.Item label="登录账号" name="loginName">
          <Input disabled />
        </Form.Item>
        <Form.Item label="部门" name="departmentName">
          <Input disabled />
        </Form.Item>
        <Form.Item label="姓名" name="actualName" rules={[{ required: true, message: '请输入姓名' }]}>
          <Input placeholder="请输入姓名" />
        </Form.Item>
        <Form.Item label="性别" name="gender" rules={[{ required: true, message: '请选择性别' }]}>
          <Select
            placeholder="请选择性别"
            options={[
              { label: '男', value: 1 },
              { label: '女', value: 2 },
              { label: '未知', value: 0 },
            ]}
          />
        </Form.Item>
        <Form.Item label="手机号" name="phone" rules={[{ required: true, message: '请输入手机号' }]}>
          <Input placeholder="请输入手机号" />
        </Form.Item>
        <Form.Item label="邮箱" name="email" rules={[{ required: true, message: '请输入邮箱' }]}>
          <Input placeholder="请输入邮箱" />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={3} placeholder="请输入备注" />
        </Form.Item>
        <Form.Item wrapperCol={{ offset: 4 }}>
          <Button type="primary" onClick={handleSubmit} loading={submitting}>
            保存
          </Button>
        </Form.Item>
      </Form>
    </Spin>
  );
};

export default AccountCenter;
