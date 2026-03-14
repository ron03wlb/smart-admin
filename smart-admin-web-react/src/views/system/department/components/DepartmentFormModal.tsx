/**
 * Department Form Modal Component
 * 部門表單 Modal 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { useModal } from '@/hooks/useModal';
import { departmentApi } from '@/api/system/departmentApi';
import type { DepartmentFormData, DepartmentAddForm, DepartmentUpdateForm } from '../types';
import { DEPARTMENT_VALIDATION, DEPARTMENT_CONSTANTS } from '@/constants/system/departmentConst';

interface DepartmentFormModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  initialData?: DepartmentFormData;
}

/**
 * 部門表單 Modal
 */
export const DepartmentFormModal: React.FC<DepartmentFormModalProps> = ({
  visible,
  onCancel,
  onSuccess,
  initialData,
}) => {
  const [form] = Form.useForm();

  // 使用 useModal hook 判斷新增/編輯模式
  const { isEditMode } = useModal({
    editIdField: 'departmentId',
    recordData: initialData,
  });

  // ==================== Form Initialization ====================

  useEffect(() => {
    if (visible) {
      if (initialData) {
        form.setFieldsValue(initialData);
      } else {
        form.resetFields();
      }
    }
  }, [visible, initialData, form]);

  // ==================== Form Validation Rules ====================

  const rules = {
    departmentName: [
      { required: true, message: '部門名稱不能為空' },
      {
        max: DEPARTMENT_VALIDATION.NAME_MAX_LENGTH,
        message: `部門名稱不能大於${DEPARTMENT_VALIDATION.NAME_MAX_LENGTH}個字符`,
      },
    ],
    parentId: initialData?.parentId !== DEPARTMENT_CONSTANTS.TOP_PARENT_ID
      ? [{ required: true, message: '上級部門不能為空' }]
      : [],
  };

  // ==================== Form Submission ====================

  /**
   * 處理表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      if (isEditMode) {
        // 編輯模式：檢查上級部門不能是自己
        if (values.parentId === initialData!.departmentId) {
          message.warning('上級部門不能為自己');
          return;
        }

        const updateForm: DepartmentUpdateForm = {
          departmentId: initialData!.departmentId!,
          departmentName: values.departmentName,
          parentId: values.parentId || DEPARTMENT_CONSTANTS.TOP_PARENT_ID,
          managerId: values.managerId,
          sort: values.sort ?? 0,
        };

        await departmentApi.updateDepartment(updateForm);
        message.success('更新成功');
        onSuccess();
      } else {
        // 新增模式
        const addForm: DepartmentAddForm = {
          departmentName: values.departmentName,
          parentId: values.parentId || DEPARTMENT_CONSTANTS.TOP_PARENT_ID,
          managerId: values.managerId,
          sort: values.sort ?? 0,
        };

        await departmentApi.addDepartment(addForm);
        message.success('添加成功');
        onSuccess();
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據！');
      } else {
        message.error(isEditMode ? '更新失敗' : '添加失敗');
      }
    }
  };

  /**
   * 處理取消
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  // ==================== Render ====================

  return (
    <Modal
      title={isEditMode ? '編輯部門' : '添加部門'}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      okText={isEditMode ? '更新' : '保存'}
      cancelText="取消"
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          sort: 0,
        }}
      >
        {/* 上級部門（僅非頂級部門顯示） */}
        {initialData?.parentId !== DEPARTMENT_CONSTANTS.TOP_PARENT_ID && (
          <Form.Item
            label="上級部門"
            name="parentId"
            rules={rules.parentId}
          >
            <Input placeholder="上級部門ID（後續實現 DepartmentTreeSelect）" disabled />
          </Form.Item>
        )}

        {/* 部門名稱 */}
        <Form.Item
          label="部門名稱"
          name="departmentName"
          rules={rules.departmentName}
        >
          <Input placeholder="請輸入部門名稱" />
        </Form.Item>

        {/* 部門負責人 */}
        <Form.Item label="部門負責人" name="managerId">
          <Input placeholder="負責人ID（後續實現 EmployeeSelect）" />
        </Form.Item>

        {/* 部門排序 */}
        <Form.Item
          label="部門排序（值越大越靠前）"
          name="sort"
        >
          <InputNumber
            style={{ width: '100%' }}
            min={0}
            placeholder="請輸入部門排序"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DepartmentFormModal;
