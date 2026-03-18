/**
 * Config Form Modal Component
 * 配置表單 Modal 組件（新增/編輯）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/config/config-form-modal.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  message,
} from 'antd';
import { useModal } from '@/hooks/useModal';
import { configApi } from '@/api/support/configApi';
import type { ConfigFormData } from '../types';
import { CONFIG_VALIDATION } from '@/constants/support/configConst';

const { TextArea } = Input;

/**
 * 配置表單 Modal Props
 */
export interface ConfigFormModalProps {
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
  initialData?: ConfigFormData;
}

/**
 * 配置表單 Modal 組件
 */
export const ConfigFormModal: React.FC<ConfigFormModalProps> = ({
  visible,
  onCancel,
  onSuccess,
  initialData,
}) => {
  const [form] = Form.useForm<ConfigFormData>();
  const [loading, setLoading] = useState(false);

  // ==================== Modal Mode Detection ====================

  const { isEdit } = useModal({
    defaultFormData: initialData,
  });

  // ==================== Form Initialization ====================

  useEffect(() => {
    if (visible) {
      if (initialData) {
        // 編輯模式：填充表單數據
        form.setFieldsValue(initialData);
      } else {
        // 新增模式：重置表單
        form.resetFields();
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

      if (isEdit) {
        // 編輯模式
        await configApi.updateConfig({
          ...values,
          configId: initialData!.configId!,
        });
        message.success('更新成功');
        onSuccess();
      } else {
        // 新增模式
        await configApi.addConfig(values);
        message.success('添加成功');
        onSuccess();
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據！');
      } else {
        message.error(isEdit ? '更新失敗' : '添加失敗');
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
      title={isEdit ? '編輯配置' : '添加配置'}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      okText={isEdit ? '確認' : '確認'}
      cancelText="取消"
      destroyOnClose
    >
      <Form
        form={form}
        layout="horizontal"
        labelCol={{ span: 5 }}
      >
        <Form.Item
          label="參數 Key"
          name="configKey"
          rules={[
            { required: true, message: '請輸入參數 Key' },
            { max: CONFIG_VALIDATION.KEY_MAX_LENGTH, message: `參數 Key 不能大於${CONFIG_VALIDATION.KEY_MAX_LENGTH}個字符` },
          ]}
        >
          <Input placeholder="請輸入參數 Key" />
        </Form.Item>

        <Form.Item
          label="參數名稱"
          name="configName"
          rules={[
            { required: true, message: '請輸入參數名稱' },
            { max: CONFIG_VALIDATION.NAME_MAX_LENGTH, message: `參數名稱不能大於${CONFIG_VALIDATION.NAME_MAX_LENGTH}個字符` },
          ]}
        >
          <Input placeholder="請輸入參數名稱" />
        </Form.Item>

        <Form.Item
          label="參數值"
          name="configValue"
          rules={[
            { required: true, message: '請輸入參數值' },
            { max: CONFIG_VALIDATION.VALUE_MAX_LENGTH, message: `參數值不能大於${CONFIG_VALIDATION.VALUE_MAX_LENGTH}個字符` },
          ]}
        >
          <Input placeholder="請輸入參數值" />
        </Form.Item>

        <Form.Item
          label="備註"
          name="remark"
          rules={[
            { max: CONFIG_VALIDATION.REMARK_MAX_LENGTH, message: `備註不能大於${CONFIG_VALIDATION.REMARK_MAX_LENGTH}個字符` },
          ]}
        >
          <TextArea
            placeholder="請輸入備註"
            rows={4}
            maxLength={CONFIG_VALIDATION.REMARK_MAX_LENGTH}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};
