/**
 * Account Password Change
 *
 * Corresponds to Vue's account/components/password/index.vue (127L)
 * Includes password complexity check (matches Vue behavior).
 */
import React, { useState, useEffect } from 'react';
import { Form, Input, Button, message } from 'antd';
import { employeeApi } from '@/api/system/employee-api';

/**
 * Password complexity regex (matches Vue version)
 */
const PASSWORD_COMPLEX_REGEX =
  /^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\W_!@#$%^&*`~()-+=]+$)(?![a-z0-9]+$)(?![a-z\W_!@#$%^&*`~()-+=]+$)(?![0-9\W_!@#$%^&*`~()-+=]+$)[a-zA-Z0-9\W_!@#$%^&*`~()-+=]{8,20}$/;

const AccountPassword: React.FC = () => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [passwordComplexityEnabled, setPasswordComplexityEnabled] = useState(false);

  /** Fetch password complexity setting */
  useEffect(() => {
    const fetchComplexity = async () => {
      try {
        const res = await employeeApi.getPasswordComplexityEnabled();
        if (res.code === 1) {
          setPasswordComplexityEnabled(res.data);
        }
      } catch {
        // ignore
      }
    };
    fetchComplexity();
  }, []);

  const tips = passwordComplexityEnabled
    ? '密码长度8-20位，必须包含字母、数字、特殊符号（如：@#$%^&*()_+-=）等三种字符'
    : '密码长度至少8位';

  const passwordRules = passwordComplexityEnabled
    ? [{ required: true, pattern: PASSWORD_COMPLEX_REGEX, message: '密码格式错误' }]
    : [
        { required: true, message: '密码格式错误' },
        { min: 8, message: '密码长度至少8位' },
      ];

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (values.newPassword !== values.confirmPassword) {
      message.error('新密码与确认密码不一致');
      return;
    }
    setSubmitting(true);
    try {
      await employeeApi.updatePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      message.success('修改成功');
      form.resetFields();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('参数验证错误，请仔细填写表单数据!');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Form form={form} layout="vertical" style={{ maxWidth: 550 }}>
      <Form.Item
        label="原密码"
        name="oldPassword"
        rules={[{ required: true, message: '请输入原密码' }]}
      >
        <Input.Password placeholder="请输入原密码" autoComplete="off" />
      </Form.Item>
      <Form.Item label="新密码" name="newPassword" rules={passwordRules} help={tips}>
        <Input.Password placeholder="请输入新密码" autoComplete="off" />
      </Form.Item>
      <Form.Item label="确认密码" name="confirmPassword" rules={passwordRules} help={tips}>
        <Input.Password placeholder="请输入确认密码" autoComplete="off" />
      </Form.Item>
      <Form.Item>
        <Button type="primary" onClick={handleSubmit} loading={submitting} style={{ marginTop: 20 }}>
          修改密码
        </Button>
      </Form.Item>
    </Form>
  );
};

export default AccountPassword;
