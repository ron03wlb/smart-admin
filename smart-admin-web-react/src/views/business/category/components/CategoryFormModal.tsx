/**
 * Category Form Modal
 * 分類表單模態框
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/catalog/components/category-form-modal.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { categoryApi } from '@/api/business/categoryApi';
import { CATEGORY_VALIDATION } from '@/constants/business/categoryConst';
import type { CategoryVO, CategoryFormData, CategoryTypeEnum } from '../types';

const { TextArea } = Input;

interface CategoryFormModalProps {
  categoryType: CategoryTypeEnum;
  onSuccess: () => void;
}

export interface CategoryFormModalRef {
  show: (parentId?: number, rowData?: CategoryVO) => void;
}

/**
 * 分類表單模態框
 */
const CategoryFormModal = forwardRef<CategoryFormModalRef, CategoryFormModalProps>(
  ({ categoryType, onSuccess }, ref) => {
    const [form] = Form.useForm<CategoryFormData>();
    const [visible, setVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [parentId, setParentId] = useState<number | undefined>(undefined);
    const [isEdit, setIsEdit] = useState(false);

    /**
     * 顯示模態框
     */
    useImperativeHandle(ref, () => ({
      show: (pId?: number, rowData?: CategoryVO) => {
        setParentId(pId);
        setIsEdit(!!rowData);

        if (rowData) {
          // 編輯模式：回填數據
          form.setFieldsValue({
            categoryId: rowData.categoryId,
            categoryName: rowData.categoryName,
            categoryType: rowData.categoryType,
            parentId: rowData.parentId,
            sort: rowData.sort,
            remark: rowData.remark,
          });
        } else {
          // 新增模式：重置表單
          form.resetFields();
          form.setFieldsValue({
            categoryType,
            parentId: pId,
          });
        }

        setVisible(true);
      },
    }));

    /**
     * 關閉模態框
     */
    const handleClose = () => {
      setVisible(false);
      form.resetFields();
      setParentId(undefined);
      setIsEdit(false);
    };

    /**
     * 提交表單
     */
    const handleSubmit = async () => {
      try {
        const values = await form.validateFields();
        setLoading(true);

        if (isEdit && values.categoryId) {
          // 更新
          await categoryApi.updateCategory({
            categoryId: values.categoryId,
            categoryName: values.categoryName!,
            categoryType,
            parentId,
            sort: values.sort,
            remark: values.remark,
          });
          message.success('更新成功');
        } else {
          // 新增
          await categoryApi.addCategory({
            categoryName: values.categoryName!,
            categoryType,
            parentId,
            sort: values.sort,
            remark: values.remark,
          });
          message.success('添加成功');
        }

        handleClose();
        onSuccess();
      } catch (error) {
        // 表單驗證失敗或 API 錯誤
        if (error instanceof Error) {
          message.error(error.message);
        }
      } finally {
        setLoading(false);
      }
    };

    return (
      <Modal
        title={isEdit ? '編輯分類' : parentId ? '增加子分類' : '添加分類'}
        open={visible}
        onOk={handleSubmit}
        onCancel={handleClose}
        confirmLoading={loading}
        okText="確認"
        cancelText="取消"
        width={600}
        destroyOnClose
      >
        <Form
          form={form}
          labelCol={{ span: 6 }}
          wrapperCol={{ span: 16 }}
          style={{ marginTop: 24 }}
        >
          <Form.Item name="categoryId" hidden>
            <Input />
          </Form.Item>

          <Form.Item name="categoryType" hidden>
            <Input />
          </Form.Item>

          <Form.Item name="parentId" hidden>
            <Input />
          </Form.Item>

          <Form.Item
            name="categoryName"
            label="分類名稱"
            rules={[
              { required: true, message: '請輸入分類名稱' },
              {
                max: CATEGORY_VALIDATION.NAME_MAX_LENGTH,
                message: `分類名稱最多 ${CATEGORY_VALIDATION.NAME_MAX_LENGTH} 個字符`,
              },
            ]}
          >
            <Input placeholder="請輸入分類名稱" maxLength={CATEGORY_VALIDATION.NAME_MAX_LENGTH} />
          </Form.Item>

          <Form.Item
            name="sort"
            label="排序"
            rules={[{ type: 'number', min: 0, message: '排序必須大於等於 0' }]}
          >
            <InputNumber placeholder="請輸入排序" min={0} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="remark"
            label="備註"
            rules={[
              {
                max: CATEGORY_VALIDATION.REMARK_MAX_LENGTH,
                message: `備註最多 ${CATEGORY_VALIDATION.REMARK_MAX_LENGTH} 個字符`,
              },
            ]}
          >
            <TextArea
              rows={4}
              placeholder="請輸入備註"
              maxLength={CATEGORY_VALIDATION.REMARK_MAX_LENGTH}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    );
  }
);

CategoryFormModal.displayName = 'CategoryFormModal';

export default CategoryFormModal;
