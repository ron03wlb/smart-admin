/**
 * Enterprise Bank Operate Modal
 *
 * Corresponds to Vue's business/oa/enterprise/components/enterprise-bank-operate-modal.vue (131L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Radio, Switch, message } from 'antd';
import { bankApi } from '@/api/business/oa/enterprise-api';
import type { BankVO } from '@/api/business/oa/enterprise-api';

interface Props {
  open: boolean;
  bank?: BankVO;
  enterpriseId: number;
  onCancel: () => void;
  onSuccess: () => void;
}

const EnterpriseBankOperateModal: React.FC<Props> = ({ open, bank, enterpriseId, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!bank;

  useEffect(() => {
    if (open) {
      if (bank) {
        form.setFieldsValue(bank);
      } else {
        form.resetFields();
        form.setFieldsValue({ publicFlag: true, disabledFlag: false });
      }
    }
  }, [open, bank, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await bankApi.update({ ...values, bankId: bank!.bankId, enterpriseId });
      } else {
        await bankApi.create({ ...values, enterpriseId });
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? '编辑银行信息' : '新建银行信息'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={600} destroyOnClose>
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="开户行" name="bankName" rules={[{ required: true, message: '请输入开户行' }]}>
          <Input placeholder="请输入开户行名称" maxLength={100} />
        </Form.Item>
        <Form.Item label="户名" name="accountName" rules={[{ required: true, message: '请输入户名' }]}>
          <Input placeholder="请输入户名" maxLength={100} />
        </Form.Item>
        <Form.Item label="账号" name="accountNumber" rules={[{ required: true, message: '请输入账号' }]}>
          <Input placeholder="请输入银行账号" maxLength={50} />
        </Form.Item>
        <Form.Item label="账户类型" name="publicFlag">
          <Radio.Group>
            <Radio value={true}>对公</Radio>
            <Radio value={false}>对私</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="是否禁用" name="disabledFlag" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={2} maxLength={200} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default EnterpriseBankOperateModal;
