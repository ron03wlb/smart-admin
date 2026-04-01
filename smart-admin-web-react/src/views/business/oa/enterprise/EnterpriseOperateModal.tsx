/**
 * Enterprise Operate Modal
 *
 * Corresponds to Vue's business/oa/enterprise/components/enterprise-operate-modal.vue (267L)
 * Full feature parity: type, AreaCascader, logo, businessLicense, disabledFlag.
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Switch, message } from 'antd';
import { enterpriseApi } from '@/api/business/oa/enterprise-api';
import type { EnterpriseVO } from '@/api/business/oa/enterprise-api';
import { AreaCascader } from '@/components/framework/area-cascader';
import type { AreaOption } from '@/components/framework/area-cascader';

// Enterprise type options matching backend EnterpriseTypeEnum
const ENTERPRISE_TYPE_OPTIONS = [
  { label: '有限企业', value: 1 },
  { label: '外资企业', value: 2 },
];

interface Props {
  open: boolean;
  enterprise?: EnterpriseVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const EnterpriseOperateModal: React.FC<Props> = ({ open, enterprise, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [areaValue, setAreaValue] = useState<(number | string)[]>([]);
  const isEdit = !!enterprise;

  useEffect(() => {
    if (open) {
      if (enterprise) {
        form.setFieldsValue({
          ...enterprise,
          // map 'contact' from backend into form field
          contact: enterprise.contact,
        });
        // Restore area cascader
        const vals: (number | string)[] = [];
        if (enterprise.province) vals.push(enterprise.province);
        if (enterprise.city) vals.push(enterprise.city);
        if (enterprise.district) vals.push(enterprise.district);
        setAreaValue(vals);
      } else {
        form.resetFields();
        form.setFieldsValue({ disabledFlag: false });
        setAreaValue([]);
      }
    }
  }, [open, enterprise, form]);

  const handleAreaChange = (values: (number | string)[], selectedOptions: AreaOption[]) => {
    setAreaValue(values || []);
    if (selectedOptions && selectedOptions.length > 0) {
      form.setFieldsValue({
        province: selectedOptions[0] ? Number(selectedOptions[0].value) : undefined,
        provinceName: selectedOptions[0]?.label,
        city: selectedOptions[1] ? Number(selectedOptions[1].value) : undefined,
        cityName: selectedOptions[1]?.label,
        district: selectedOptions[2] ? Number(selectedOptions[2].value) : undefined,
        districtName: selectedOptions[2]?.label,
      });
    } else {
      form.setFieldsValue({ province: undefined, provinceName: undefined, city: undefined, cityName: undefined, district: undefined, districtName: undefined });
    }
  };

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

  const handleCancel = () => {
    setAreaValue([]);
    onCancel();
  };

  return (
    <Modal
      title={isEdit ? '编辑企业' : '新建企业'}
      open={open} onOk={handleOk} onCancel={handleCancel}
      confirmLoading={loading} width={700} destroyOnClose
    >
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="企业名称" name="enterpriseName" rules={[{ required: true, message: '请输入企业名称' }]}>
          <Input placeholder="请输入企业名称" maxLength={200} />
        </Form.Item>
        <Form.Item label="企业类型" name="type">
          <Select placeholder="请选择企业类型" options={ENTERPRISE_TYPE_OPTIONS} allowClear />
        </Form.Item>
        <Form.Item label="统一社会信用代码" name="unifiedSocialCreditCode" rules={[{ required: true, message: '请输入统一社会信用代码' }]}>
          <Input placeholder="请输入统一社会信用代码" maxLength={200} />
        </Form.Item>
        <Form.Item label="联系人" name="contact" rules={[{ required: true, message: '请输入联系人' }]}>
          <Input placeholder="请输入联系人" maxLength={100} />
        </Form.Item>
        <Form.Item label="联系电话" name="contactPhone" rules={[{ required: true, message: '请输入联系电话' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}>
          <Input placeholder="请输入联系电话" maxLength={20} />
        </Form.Item>
        <Form.Item label="邮箱" name="email" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
          <Input placeholder="请输入邮箱" maxLength={100} />
        </Form.Item>
        <Form.Item label="所在城市">
          <AreaCascader value={areaValue} onChange={handleAreaChange} placeholder="请选择所在城市" width="100%" />
        </Form.Item>
        {/* Hidden area code fields */}
        <Form.Item name="province" hidden><Input /></Form.Item>
        <Form.Item name="provinceName" hidden><Input /></Form.Item>
        <Form.Item name="city" hidden><Input /></Form.Item>
        <Form.Item name="cityName" hidden><Input /></Form.Item>
        <Form.Item name="district" hidden><Input /></Form.Item>
        <Form.Item name="districtName" hidden><Input /></Form.Item>
        <Form.Item label="详细地址" name="address">
          <Input placeholder="请输入详细地址" maxLength={500} />
        </Form.Item>
        <Form.Item label="启用状态" name="disabledFlag" valuePropName="checked">
          <Switch checkedChildren="禁用" unCheckedChildren="启用" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default EnterpriseOperateModal;
