/**
 * Enterprise Invoice Operate Modal
 *
 * Corresponds to Vue's business/oa/enterprise/components/enterprise-invoice-operate-modal.vue (127L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Switch, message } from 'antd';
import { invoiceApi } from '@/api/business/oa/enterprise-api';
import type { InvoiceVO } from '@/api/business/oa/enterprise-api';

interface Props {
  open: boolean;
  invoice?: InvoiceVO;
  enterpriseId: number;
  onCancel: () => void;
  onSuccess: () => void;
}

const EnterpriseInvoiceOperateModal: React.FC<Props> = ({ open, invoice, enterpriseId, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!invoice;

  useEffect(() => {
    if (open) {
      if (invoice) {
        form.setFieldsValue(invoice);
      } else {
        form.resetFields();
        form.setFieldsValue({ disabledFlag: false });
      }
    }
  }, [open, invoice, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await invoiceApi.update({ ...values, invoiceId: invoice!.invoiceId, enterpriseId });
      } else {
        await invoiceApi.create({ ...values, enterpriseId });
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? '编辑发票信息' : '新建发票信息'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={600} destroyOnClose>
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="发票抬头" name="invoiceHeads" rules={[{ required: true, message: '请输入发票抬头' }]}>
          <Input placeholder="请输入发票抬头" maxLength={200} />
        </Form.Item>
        <Form.Item label="纳税人识别号" name="taxpayerIdentificationNumber" rules={[{ required: true, message: '请输入纳税人识别号' }]}>
          <Input placeholder="请输入纳税人识别号" maxLength={50} />
        </Form.Item>
        <Form.Item label="银行账号" name="accountNumber">
          <Input placeholder="请输入银行账号" maxLength={50} />
        </Form.Item>
        <Form.Item label="开户行" name="bankName">
          <Input placeholder="请输入开户行" maxLength={100} />
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

export default EnterpriseInvoiceOperateModal;
