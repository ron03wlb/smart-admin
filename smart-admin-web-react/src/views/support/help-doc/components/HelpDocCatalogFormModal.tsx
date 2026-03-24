/**
 * Help Doc Catalog Form Modal
 * 幫助文檔目錄表單 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, forwardRef, useImperativeHandle } from 'react';
import { Modal, Form, Input, InputNumber, message } from 'antd';
import { helpDocCatalogApi } from '@/api/support/helpDocCatalogApi';
import type { HelpDocCatalogFormData } from '../types';
import HelpDocCatalogTreeSelect from './HelpDocCatalogTreeSelect';

export interface HelpDocCatalogFormModalProps {
  onRefresh: () => void;
}

export interface HelpDocCatalogFormModalRef {
  showModal: (data: Partial<HelpDocCatalogFormData>) => void;
}

const HelpDocCatalogFormModal = forwardRef<
  HelpDocCatalogFormModalRef,
  HelpDocCatalogFormModalProps
>(({ onRefresh }, ref) => {
  const [form] = Form.useForm<HelpDocCatalogFormData>();
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  useImperativeHandle(ref, () => ({
    showModal: (data: Partial<HelpDocCatalogFormData>) => {
      form.resetFields();
      if (data) {
        form.setFieldsValue({
          helpDocCatalogId: data.helpDocCatalogId,
          name: data.name || '',
          parentId: data.parentId || 0,
          sort: data.sort || 0,
        });
      }
      setVisible(true);
    },
  }));

  const handleOk = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      // 檢查上級目錄不能為自己
      if (values.helpDocCatalogId && values.parentId === values.helpDocCatalogId) {
        message.warning('上級目錄不能為自己');
        return;
      }

      setLoading(true);
      if (values.helpDocCatalogId) {
        await helpDocCatalogApi.update(values);
      } else {
        await helpDocCatalogApi.add(values);
      }

      message.success('保存成功');
      setVisible(false);
      form.resetFields();
      onRefresh();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據!');
      } else {
        console.error('Failed to save catalog:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setVisible(false);
    form.resetFields();
  };

  return (
    <Modal
      open={visible}
      title={form.getFieldValue('helpDocCatalogId') ? '編輯目錄' : '添加目錄'}
      okText="確認"
      cancelText="取消"
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 20 }}>
        <Form.Item name="helpDocCatalogId" hidden>
          <Input />
        </Form.Item>

        {form.getFieldValue('parentId') !== 0 && (
          <Form.Item
            label="上級目錄"
            name="parentId"
            rules={[{ required: true, message: '上級目錄不能為空' }]}
          >
            <HelpDocCatalogTreeSelect />
          </Form.Item>
        )}

        <Form.Item
          label="目錄名稱"
          name="name"
          rules={[
            { required: true, message: '目錄名稱不能為空' },
            { max: 50, message: '目錄名稱不能大於50個字符' },
          ]}
        >
          <Input placeholder="請輸入目錄名稱" />
        </Form.Item>

        <Form.Item label="目錄排序（值越小越靠前）" name="sort">
          <InputNumber min={0} placeholder="請輸入排序" style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
});

HelpDocCatalogFormModal.displayName = 'HelpDocCatalogFormModal';

export default HelpDocCatalogFormModal;
