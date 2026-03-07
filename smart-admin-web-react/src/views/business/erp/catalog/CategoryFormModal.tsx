/**
 * Category Form Modal
 *
 * Corresponds to Vue's business/erp/catalog/components/category-form-modal.vue (111L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { categoryApi } from '@/api/business/erp/catalog-api';
import type { CategoryVO } from '@/api/business/erp/catalog-api';

interface Props {
  open: boolean;
  category?: CategoryVO;
  categoryType: number;
  parentId: number;
  onCancel: () => void;
  onSuccess: () => void;
}

const CategoryFormModal: React.FC<Props> = ({ open, category, categoryType, parentId, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!category;

  useEffect(() => {
    if (open) {
      if (category) {
        form.setFieldsValue({ categoryName: category.categoryName, sort: category.sort });
      } else {
        form.resetFields();
        form.setFieldsValue({ sort: 0 });
      }
    }
  }, [open, category, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await categoryApi.update({ ...values, categoryId: category!.categoryId, categoryType, parentId: category!.parentId });
      } else {
        await categoryApi.add({ ...values, categoryType, parentId });
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? '编辑分类' : '添加分类'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={450} destroyOnClose>
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="分类名称" name="categoryName" rules={[{ required: true, message: '请输入分类名称' }]}>
          <Input placeholder="请输入分类名称" maxLength={50} />
        </Form.Item>
        <Form.Item label="排序" name="sort">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CategoryFormModal;
