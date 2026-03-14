/**
 * Position Form Modal Component
 * 職位表單 Modal 組件（新增/編輯）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/position/position-form.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  InputNumber,
  message,
} from 'antd';
import { useModal } from '@/hooks/useModal';
import { positionApi } from '@/api/system/positionApi';
import type { PositionFormData } from '../types';
import { POSITION_VALIDATION } from '@/constants/system/positionConst';

const { TextArea } = Input;

/**
 * 職位表單 Modal Props
 */
export interface PositionFormModalProps {
  /**
   * Modal 顯示狀態（由父組件控制）
   */
  visible: boolean;
  /**
   * 關閉 Modal 的回調
   */
  onCancel: () => void;
  /**
   * 提交成功後的回調
   */
  onSuccess: () => void;
  /**
   * 初始數據（編輯模式時傳入）
   */
  initialData?: PositionFormData;
}

/**
 * 職位表單 Modal 組件
 */
export const PositionFormModal: React.FC<PositionFormModalProps> = ({
  visible,
  onCancel,
  onSuccess,
  initialData,
}) => {
  const [form] = Form.useForm<PositionFormData>();
  const [loading, setLoading] = useState(false);

  // ==================== Modal Mode Detection ====================

  const { isEditMode } = useModal({
    editIdField: 'positionId',
    recordData: initialData,
  });

  // ==================== Form Initialization ====================

  useEffect(() => {
    if (visible) {
      if (initialData) {
        // 編輯模式：填充表單數據
        form.setFieldsValue(initialData);
      } else {
        // 新增模式：重置表單並設置默認值
        form.resetFields();
        form.setFieldsValue({
          sort: 0, // 默認排序為 0
        });
      }
    }
  }, [visible, initialData, form]);

  // ==================== Form Submission ====================

  /**
   * 處理表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEditMode) {
        // 編輯模式
        await positionApi.updatePosition({
          ...values,
          positionId: initialData!.positionId!,
        });
        message.success('更新成功');
        onSuccess();
      } else {
        // 新增模式
        await positionApi.addPosition(values);
        message.success('添加成功');
        onSuccess();
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據！');
      } else {
        message.error(isEditMode ? '更新失敗' : '添加失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 處理取消操作
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  // ==================== Render ====================

  return (
    <Modal
      title={isEditMode ? '編輯職位' : '添加職位'}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      okText={isEditMode ? '更新' : '保存'}
      cancelText="取消"
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          sort: 0,
        }}
      >
        <Form.Item
          label="職位名稱"
          name="positionName"
          rules={[
            { required: true, message: '職位名稱不能為空' },
            { max: POSITION_VALIDATION.NAME_MAX_LENGTH, message: `職位名稱不能大於${POSITION_VALIDATION.NAME_MAX_LENGTH}個字符` },
          ]}
        >
          <Input placeholder="請輸入職位名稱" />
        </Form.Item>

        <Form.Item
          label="職級"
          name="positionLevel"
          rules={[
            { max: POSITION_VALIDATION.LEVEL_MAX_LENGTH, message: `職級不能大於${POSITION_VALIDATION.LEVEL_MAX_LENGTH}個字符` },
          ]}
        >
          <Input placeholder="請輸入職級" />
        </Form.Item>

        <Form.Item
          label="排序（值越大越靠前）"
          name="sort"
          rules={[
            { required: true, message: '排序不能為空' },
          ]}
        >
          <InputNumber
            placeholder="請輸入排序"
            min={0}
            step={1}
            precision={0}
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Form.Item
          label="備註"
          name="remark"
          rules={[
            { max: POSITION_VALIDATION.REMARK_MAX_LENGTH, message: `備註不能大於${POSITION_VALIDATION.REMARK_MAX_LENGTH}個字符` },
          ]}
        >
          <TextArea
            placeholder="請輸入備註"
            rows={4}
            maxLength={POSITION_VALIDATION.REMARK_MAX_LENGTH}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};
