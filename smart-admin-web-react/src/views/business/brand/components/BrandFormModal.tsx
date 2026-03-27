/**
 * Brand Form Modal
 * 品牌表單模態框
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, InputNumber, Select, message } from 'antd';
import type { BrandVO, BrandFormData, BrandAddForm, BrandUpdateForm } from '../types';
import { BrandStatusEnum } from '../types';
import { brandApi } from '../brandApi';
import { BRAND_FORM_RULES, BRAND_STATUS_OPTIONS } from '../brandConst';

const { TextArea } = Input;

interface BrandFormModalProps {
  onSuccess?: () => void;
}

export interface BrandFormModalRef {
  open: (record?: BrandVO) => void;
  close: () => void;
}

const BrandFormModal = forwardRef<BrandFormModalRef, BrandFormModalProps>(({ onSuccess }, ref) => {
  const [form] = Form.useForm<BrandFormData>();
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [brandId, setBrandId] = useState<number>();

  /**
   * Open Modal
   * 打開模態框
   */
  const open = (record?: BrandVO) => {
    if (record) {
      // Edit mode
      setIsEdit(true);
      setBrandId(record.brandId);
      form.setFieldsValue({
        brandName: record.brandName,
        brandLogo: record.brandLogo,
        description: record.description,
        sort: record.sort,
        status: record.status,
      });
    } else {
      // Add mode
      setIsEdit(false);
      setBrandId(undefined);
      form.setFieldsValue({
        brandName: '',
        brandLogo: '',
        description: '',
        sort: 0,
        status: BrandStatusEnum.ENABLED,
      });
    }
    setVisible(true);
  };

  /**
   * Close Modal
   * 關閉模態框
   */
  const close = () => {
    setVisible(false);
    form.resetFields();
  };

  /**
   * Expose methods to parent
   * 暴露方法給父組件
   */
  useImperativeHandle(ref, () => ({
    open,
    close,
  }));

  /**
   * Handle Submit
   * 處理提交
   */
  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      setLoading(true);

      if (isEdit && brandId) {
        // Update
        const updateForm: BrandUpdateForm = {
          brandId,
          ...values,
        };
        const result = await brandApi.updateBrand(updateForm);
        if (result.ok) {
          message.success('品牌更新成功');
          close();
          onSuccess?.();
        }
      } else {
        // Add
        const addForm: BrandAddForm = values;
        const result = await brandApi.addBrand(addForm);
        if (result.ok) {
          message.success('品牌新增成功');
          close();
          onSuccess?.();
        }
      }
    } catch (error) {
      console.error('Brand form submit error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '編輯品牌' : '新建品牌'}
      open={visible}
      onOk={handleSubmit}
      onCancel={close}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 20 }}>
        <Form.Item label="品牌名稱" name="brandName" rules={BRAND_FORM_RULES.brandName}>
          <Input placeholder="請輸入品牌名稱（最大50字符）" maxLength={50} showCount />
        </Form.Item>

        <Form.Item label="品牌Logo" name="brandLogo" rules={BRAND_FORM_RULES.brandLogo}>
          <Input placeholder="請輸入Logo URL（最大200字符）" maxLength={200} />
        </Form.Item>

        <Form.Item label="品牌描述" name="description" rules={BRAND_FORM_RULES.description}>
          <TextArea
            placeholder="請輸入品牌描述（最大500字符）"
            rows={4}
            maxLength={500}
            showCount
          />
        </Form.Item>

        <Form.Item label="排序" name="sort" rules={BRAND_FORM_RULES.sort}>
          <InputNumber placeholder="請輸入排序值" style={{ width: '100%' }} min={0} />
        </Form.Item>

        <Form.Item label="狀態" name="status" rules={BRAND_FORM_RULES.status}>
          <Select placeholder="請選擇狀態" options={BRAND_STATUS_OPTIONS} />
        </Form.Item>
      </Form>
    </Modal>
  );
});

BrandFormModal.displayName = 'BrandFormModal';

export default BrandFormModal;
