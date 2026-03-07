/**
 * Department Form Modal (Add/Edit)
 *
 * Corresponds to Vue's views/system/department/components/department-form-modal.vue
 */
import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { departmentApi } from '@/api/system/department-api';
import type { DepartmentVO, DepartmentAddForm, DepartmentUpdateForm } from '@/types/department.types';
import DepartmentTreeSelect from '@/components/system/department-tree-select/DepartmentTreeSelect';
import EmployeeSelect from '@/components/system/employee-select/EmployeeSelect';

interface DepartmentFormModalProps {
  visible: boolean;
  record: Partial<DepartmentVO> | null;
  onCancel: () => void;
  onSuccess: () => void;
}

const DepartmentFormModal: React.FC<DepartmentFormModalProps> = ({
  visible,
  record,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);
  const isEdit = record !== null && !!record.departmentId;
  const isTopLevel = record?.parentId === 0 || record?.parentId === undefined;

  useEffect(() => {
    if (visible && record) {
      form.setFieldsValue({
        parentId: record.parentId,
        departmentName: record.departmentName || '',
        managerId: record.managerId,
        sort: record.sort ?? 0,
      });
    } else if (visible) {
      form.resetFields();
    }
  }, [visible, record, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit) {
        if (values.parentId === record!.departmentId) {
          message.warning('上级部门不能为自己');
          return;
        }
        const res = await departmentApi.update({
          ...values,
          departmentId: record!.departmentId,
        } as DepartmentUpdateForm);
        if (res.code === 1) {
          message.success('更新成功');
          onSuccess();
        }
      } else {
        const res = await departmentApi.add(values as DepartmentAddForm);
        if (res.code === 1) {
          message.success('添加成功');
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
      title={isEdit ? '编辑部门' : '添加部门'}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} layout="vertical" autoComplete="off">
        {!isTopLevel && (
          <Form.Item label="上级部门" name="parentId" rules={[{ required: true, message: '上级部门不能为空' }]}>
            <DepartmentTreeSelect />
          </Form.Item>
        )}
        <Form.Item
          label="部门名称"
          name="departmentName"
          rules={[
            { required: true, message: '部门名称不能为空' },
            { max: 50, message: '部门名称不能大于50个字符' },
          ]}
        >
          <Input placeholder="请输入部门名称" />
        </Form.Item>
        <Form.Item label="部门负责人" name="managerId">
          <EmployeeSelect placeholder="请选择部门负责人" />
        </Form.Item>
        <Form.Item label="部门排序 （值越大越靠前！）" name="sort">
          <InputNumber style={{ width: '100%' }} min={0} placeholder="请输入部门排序" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DepartmentFormModal;
