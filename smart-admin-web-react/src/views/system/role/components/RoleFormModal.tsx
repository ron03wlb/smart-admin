/**
 * Role Form Modal Component
 * 角色表單 Modal 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect } from 'react';
import { Form, Input, message, Modal } from 'antd';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, RoleAddForm, RoleUpdateForm, RoleFormData } from '../types';
import { useModal } from '@/hooks/useModal';
import { ROLE_VALIDATION } from '@/constants/system/roleConst';

interface RoleFormModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  initialData?: RoleVO;
}

export default function RoleFormModal({
  visible,
  onCancel,
  onSuccess,
  initialData,
}: RoleFormModalProps) {
  const [form] = Form.useForm<RoleFormData>();
  const [loading, setLoading] = React.useState(false);

  const { isEdit } = useModal<RoleVO>({
    defaultFormData: initialData,
  });

  /**
   * 表單驗證規則
   */
  const rules = {
    roleName: [
      { required: true, message: '請輸入角色名稱' },
      { max: ROLE_VALIDATION.NAME_MAX_LENGTH, message: `角色名稱最多${ROLE_VALIDATION.NAME_MAX_LENGTH}個字符` },
    ],
    roleCode: [
      { required: true, message: '請輸入角色編碼' },
      { max: ROLE_VALIDATION.CODE_MAX_LENGTH, message: `角色編碼最多${ROLE_VALIDATION.CODE_MAX_LENGTH}個字符` },
      {
        pattern: /^[a-zA-Z0-9_]+$/,
        message: '角色編碼只能包含字母、數字和下劃線',
      },
    ],
    remark: [
      { max: ROLE_VALIDATION.REMARK_MAX_LENGTH, message: `備註最多${ROLE_VALIDATION.REMARK_MAX_LENGTH}個字符` },
    ],
  };

  /**
   * 表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit && initialData) {
        // 編輯模式
        const updateForm: RoleUpdateForm = {
          roleId: initialData.roleId,
          roleName: values.roleName!,
          roleCode: values.roleCode!,
          remark: values.remark,
        };

        const res = await roleApi.updateRole(updateForm);
        if (res.ok) {
          message.success('更新成功');
          onSuccess();
        }
      } else {
        // 新增模式
        const addForm: RoleAddForm = {
          roleName: values.roleName!,
          roleCode: values.roleCode!,
          remark: values.remark,
        };

        const res = await roleApi.addRole(addForm);
        if (res.ok) {
          message.success('新增成功');
          onSuccess();
        }
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || (isEdit ? '更新失敗' : '新增失敗'));
      }
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Modal 關閉處理
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  /**
   * 初始化表單數據（編輯模式）
   */
  useEffect(() => {
    if (visible && initialData) {
      form.setFieldsValue({
        roleName: initialData.roleName,
        roleCode: initialData.roleCode,
        remark: initialData.remark,
      });
    } else if (visible && !initialData) {
      form.resetFields();
    }
  }, [visible, initialData, form]);

  return (
    <Modal
      title={isEdit ? '編輯角色' : '新增角色'}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        preserve={false}
        style={{ marginTop: 16 }}
      >
        <Form.Item
          label="角色名稱"
          name="roleName"
          rules={rules.roleName}
        >
          <Input placeholder="請輸入角色名稱" maxLength={ROLE_VALIDATION.NAME_MAX_LENGTH} />
        </Form.Item>

        <Form.Item
          label="角色編碼"
          name="roleCode"
          rules={rules.roleCode}
          tooltip="只能包含字母、數字和下劃線"
        >
          <Input
            placeholder="請輸入角色編碼（如：admin, manager）"
            maxLength={ROLE_VALIDATION.CODE_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item
          label="備註"
          name="remark"
          rules={rules.remark}
        >
          <Input.TextArea
            placeholder="請輸入備註（可選）"
            maxLength={ROLE_VALIDATION.REMARK_MAX_LENGTH}
            showCount
            rows={4}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
