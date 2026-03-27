/**
 * Bank Form Modal
 * 銀行信息表單模態框
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import type { BankVO, BankFormData, BankCreateForm, BankUpdateForm } from '../types';
import { bankApi } from '../bankApi';
import { BANK_FORM_RULES, BUSINESS_FLAG_OPTIONS, DISABLED_FLAG_OPTIONS } from '../bankConst';

const { TextArea } = Input;

interface BankFormModalProps {
  onSuccess?: () => void;
}

export interface BankFormModalRef {
  open: (record?: BankVO) => void;
  close: () => void;
}

const BankFormModal = forwardRef<BankFormModalRef, BankFormModalProps>(({ onSuccess }, ref) => {
  const [form] = Form.useForm<BankFormData>();
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [bankId, setBankId] = useState<number>();

  /**
   * Open Modal
   * 打開模態框
   */
  const open = (record?: BankVO) => {
    if (record) {
      // Edit mode
      setIsEdit(true);
      setBankId(record.bankId);
      form.setFieldsValue({
        bankName: record.bankName,
        accountName: record.accountName,
        accountNumber: record.accountNumber,
        remark: record.remark,
        businessFlag: record.businessFlag,
        enterpriseId: record.enterpriseId,
        disabledFlag: record.disabledFlag,
      });
    } else {
      // Add mode
      setIsEdit(false);
      setBankId(undefined);
      form.setFieldsValue({
        bankName: '',
        accountName: '',
        accountNumber: '',
        remark: '',
        businessFlag: false,
        enterpriseId: undefined,
        disabledFlag: false,
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

      if (isEdit && bankId) {
        // Update
        const updateForm: BankUpdateForm = {
          bankId,
          ...values,
        };
        const result = await bankApi.updateBank(updateForm);
        if (result.ok) {
          message.success('銀行信息更新成功');
          close();
          onSuccess?.();
        }
      } else {
        // Add
        const createForm: BankCreateForm = values;
        const result = await bankApi.createBank(createForm);
        if (result.ok) {
          message.success('銀行信息新增成功');
          close();
          onSuccess?.();
        }
      }
    } catch (error) {
      console.error('Bank form submit error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '編輯銀行信息' : '新建銀行信息'}
      open={visible}
      onOk={handleSubmit}
      onCancel={close}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 20 }}>
        <Form.Item label="開戶銀行" name="bankName" rules={BANK_FORM_RULES.bankName}>
          <Input placeholder="請輸入開戶銀行（最大200字符）" maxLength={200} showCount />
        </Form.Item>

        <Form.Item label="賬戶名稱" name="accountName" rules={BANK_FORM_RULES.accountName}>
          <Input placeholder="請輸入賬戶名稱（最大200字符）" maxLength={200} showCount />
        </Form.Item>

        <Form.Item label="賬號" name="accountNumber" rules={BANK_FORM_RULES.accountNumber}>
          <Input placeholder="請輸入賬號（最大200字符）" maxLength={200} showCount />
        </Form.Item>

        <Form.Item label="備註" name="remark" rules={BANK_FORM_RULES.remark}>
          <TextArea placeholder="請輸入備註（最大500字符）" rows={4} maxLength={500} showCount />
        </Form.Item>

        <Form.Item label="是否對公" name="businessFlag" rules={BANK_FORM_RULES.businessFlag}>
          <Select placeholder="請選擇是否對公">
            {BUSINESS_FLAG_OPTIONS.map((option) => (
              <Select.Option key={String(option.value)} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item label="企業ID" name="enterpriseId" rules={BANK_FORM_RULES.enterpriseId}>
          <Input placeholder="請輸入企業ID" type="number" />
        </Form.Item>

        <Form.Item label="禁用狀態" name="disabledFlag" rules={BANK_FORM_RULES.disabledFlag}>
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

BankFormModal.displayName = 'BankFormModal';

export default BankFormModal;
