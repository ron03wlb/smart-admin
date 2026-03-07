/**
 * Role Form Modal
 *
 * Corresponds to Vue's role/components/role-form-modal/index.vue
 * Add/edit role with roleName, roleCode, remark fields.
 */
import React, { useEffect } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { roleApi } from '@/api/system/role-api';
import type { RoleVO } from '@/types/role.types';

interface RoleFormModalProps {
  open: boolean;
  role?: RoleVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const RoleFormModal: React.FC<RoleFormModalProps> = ({ open, role, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const isEdit = !!role?.roleId;

  useEffect(() => {
    if (open) {
      if (role) {
        form.setFieldsValue(role);
      } else {
        form.resetFields();
      }
    }
  }, [open, role, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (isEdit) {
      await roleApi.update({ ...values, roleId: role!.roleId });
    } else {
      await roleApi.add(values);
    }
    message.success(`${isEdit ? '编辑' : '添加'}成功`);
    onSuccess();
  };

  return (
    <Modal
      title={isEdit ? '编辑角色' : '添加角色'}
      open={open}
      width={600}
      onCancel={onCancel}
      onOk={handleSubmit}
      destroyOnClose
    >
      <Form form={form} labelCol={{ span: 4 }}>
        <Form.Item label="角色名称" name="roleName" rules={[{ required: true, message: '请输入角色名称' }]}>
          <Input placeholder="请输入角色名称" />
        </Form.Item>
        <Form.Item label="角色编码" name="roleCode" rules={[{ required: true, message: '请输入角色编码' }]}>
          <Input placeholder="请输入角色编码" />
        </Form.Item>
        <Form.Item label="角色备注" name="remark">
          <Input placeholder="请输入角色备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default RoleFormModal;
