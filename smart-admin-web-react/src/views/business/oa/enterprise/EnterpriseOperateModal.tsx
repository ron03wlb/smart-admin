/**
 * Enterprise Operate Modal
 *
 * Corresponds to Vue's business/oa/enterprise/components/enterprise-operate-modal.vue (267L)
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, message } from 'antd';
import { enterpriseApi } from '@/api/business/oa/enterprise-api';
import type { EnterpriseVO } from '@/api/business/oa/enterprise-api';

interface Props {
  open: boolean;
  enterprise?: EnterpriseVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const EnterpriseOperateModal: React.FC<Props> = ({ open, enterprise, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!enterprise;

  useEffect(() => {
    if (open) {
      if (enterprise) {
        form.setFieldsValue(enterprise);
      } else {
        form.resetFields();
      }
    }
  }, [open, enterprise, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await enterpriseApi.update({ ...values, enterpriseId: enterprise!.enterpriseId });
      } else {
        await enterpriseApi.create(values);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? '编辑企业' : '新建企业'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={700} destroyOnClose>
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="企业名称" name="enterpriseName" rules={[{ required: true, message: '请输入企业名称' }]}>
          <Input placeholder="请输入企业名称" maxLength={200} />
        </Form.Item>
        <Form.Item label="统一社会信用代码" name="unifiedSocialCreditCode">
          <Input placeholder="请输入统一社会信用代码" maxLength={50} />
        </Form.Item>
        <Form.Item label="联系人" name="contactName">
          <Input placeholder="请输入联系人" maxLength={50} />
        </Form.Item>
        <Form.Item label="联系电话" name="contactPhone">
          <Input placeholder="请输入联系电话" maxLength={20} />
        </Form.Item>
        <Form.Item label="邮箱" name="email">
          <Input placeholder="请输入邮箱" maxLength={100} />
        </Form.Item>
        <Form.Item label="地址" name="address">
          <Input placeholder="请输入地址" maxLength={300} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default EnterpriseOperateModal;
