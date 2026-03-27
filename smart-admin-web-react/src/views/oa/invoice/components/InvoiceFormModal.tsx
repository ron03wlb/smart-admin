/**
 * Invoice Form Modal
 * 發票信息表單模態框
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import type { InvoiceVO, InvoiceFormData, InvoiceAddForm, InvoiceUpdateForm } from '../types';
import { invoiceApi } from '../invoiceApi';
import { INVOICE_FORM_RULES, DISABLED_FLAG_OPTIONS } from '../invoiceConst';

const { TextArea } = Input;

interface InvoiceFormModalProps {
  onSuccess?: () => void;
}

export interface InvoiceFormModalRef {
  open: (record?: InvoiceVO) => void;
  close: () => void;
}

const InvoiceFormModal = forwardRef<InvoiceFormModalRef, InvoiceFormModalProps>(({ onSuccess }, ref) => {
  const [form] = Form.useForm<InvoiceFormData>();
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [invoiceId, setInvoiceId] = useState<number>();

  /**
   * Open Modal
   * 打開模態框
   */
  const open = (record?: InvoiceVO) => {
    if (record) {
      // Edit mode
      setIsEdit(true);
      setInvoiceId(record.invoiceId);
      form.setFieldsValue({
        invoiceHeads: record.invoiceHeads,
        taxpayerIdentificationNumber: record.taxpayerIdentificationNumber,
        accountNumber: record.accountNumber,
        bankName: record.bankName,
        disabledFlag: record.disabledFlag,
        remark: record.remark,
        enterpriseId: record.enterpriseId,
      });
    } else {
      // Add mode
      setIsEdit(false);
      setInvoiceId(undefined);
      form.setFieldsValue({
        invoiceHeads: '',
        taxpayerIdentificationNumber: '',
        accountNumber: '',
        bankName: '',
        disabledFlag: false,
        remark: '',
        enterpriseId: undefined,
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

      if (isEdit && invoiceId) {
        // Update
        const updateForm: InvoiceUpdateForm = {
          invoiceId,
          ...values,
        };
        const result = await invoiceApi.updateInvoice(updateForm);
        if (result.ok) {
          message.success('發票信息更新成功');
          close();
          onSuccess?.();
        }
      } else {
        // Add
        const addForm: InvoiceAddForm = values;
        const result = await invoiceApi.createInvoice(addForm);
        if (result.ok) {
          message.success('發票信息新增成功');
          close();
          onSuccess?.();
        }
      }
    } catch (error) {
      console.error('Invoice form submit error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '編輯發票信息' : '新建發票信息'}
      open={visible}
      onOk={handleSubmit}
      onCancel={close}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 20 }}>
        <Form.Item label="開票抬頭" name="invoiceHeads" rules={INVOICE_FORM_RULES.invoiceHeads}>
          <Input placeholder="請輸入開票抬頭（最大200字符）" maxLength={200} showCount />
        </Form.Item>

        <Form.Item
          label="納稅人識別號"
          name="taxpayerIdentificationNumber"
          rules={INVOICE_FORM_RULES.taxpayerIdentificationNumber}
        >
          <Input placeholder="請輸入納稅人識別號（最大200字符）" maxLength={200} showCount />
        </Form.Item>

        <Form.Item label="銀行賬戶" name="accountNumber" rules={INVOICE_FORM_RULES.accountNumber}>
          <Input placeholder="請輸入銀行賬戶（最大200字符）" maxLength={200} showCount />
        </Form.Item>

        <Form.Item label="開戶行" name="bankName" rules={INVOICE_FORM_RULES.bankName}>
          <Input placeholder="請輸入開戶行（最大200字符）" maxLength={200} showCount />
        </Form.Item>

        <Form.Item label="備註" name="remark" rules={INVOICE_FORM_RULES.remark}>
          <TextArea placeholder="請輸入備註（最大500字符）" rows={4} maxLength={500} showCount />
        </Form.Item>

        <Form.Item label="企業ID" name="enterpriseId" rules={INVOICE_FORM_RULES.enterpriseId}>
          <Input placeholder="請輸入企業ID" type="number" />
        </Form.Item>

        <Form.Item label="禁用狀態" name="disabledFlag" rules={INVOICE_FORM_RULES.disabledFlag}>
          <Select placeholder="請選擇禁用狀態">
            {DISABLED_FLAG_OPTIONS.map((option) => (
              <Select.Option key={String(option.value)} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
});

InvoiceFormModal.displayName = 'InvoiceFormModal';

export default InvoiceFormModal;
