/**
 * Change Password Modal
 *
 * Corresponds to Vue's header-reset-password-modal/index.vue
 */
import React from 'react';
import { Modal, Form, Input, message } from 'antd';
import { employeeApi } from '@/api/system/employee-api';
import { SmartLoading } from '@/components/framework/smart-loading';

interface ChangePasswordModalProps {
  visible: boolean;
  onClose: () => void;
}

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,20}$/;

const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({ visible, onClose }) => {
  const [form] = Form.useForm();

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      SmartLoading.show();
      const res = await employeeApi.updatePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      if (res.code === 1) {
        message.success('密码修改成功');
        form.resetFields();
        onClose();
      }
    } catch {
      // validation error or API error
    } finally {
      SmartLoading.hide();
    }
  };

  return (
    <Modal title="修改密码" open={visible} onOk={handleOk} onCancel={onClose} destroyOnHidden>
      <Form form={form} layout="vertical">
        <Form.Item name="oldPassword" label="原密码" rules={[{ required: true, message: '请输入原密码' }]}>
          <Input.Password placeholder="请输入原密码" />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[
            { required: true, message: '请输入新密码' },
            {
              pattern: PASSWORD_REGEX,
              message: '密码需8-20位，包含大小写字母和数字',
            },
          ]}
        >
          <Input.Password placeholder="8-20位，包含大小写字母和数字" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="确认密码"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '请确认新密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('两次输入的密码不一致'));
              },
            }),
          ]}
        >
          <Input.Password placeholder="请再次输入新密码" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ChangePasswordModal;
