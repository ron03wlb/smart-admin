/**
 * Enterprise Form Modal Component
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, message, Select, Switch } from 'antd';
import { enterpriseApi } from '@/api/business/enterpriseApi';
import type {
  EnterpriseVO,
  EnterpriseAddForm,
  EnterpriseUpdateForm,
  EnterpriseFormData,
  EnterpriseTypeEnum,
} from '../types';
import { useModal } from '@/hooks/useModal';
import {
  ENTERPRISE_VALIDATION,
  ENTERPRISE_TYPE_LABELS,
} from '@/constants/business/enterpriseConst';
import { AreaCascader } from '@/components/framework/area-cascader';
import type { AreaOption } from '@/components/framework/area-cascader';
import FileUpload from '@/components/support/file-upload/FileUpload';
import type { UploadFile } from 'antd';

interface EnterpriseFormModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  initialData?: EnterpriseVO;
}

export default function EnterpriseFormModal({
  visible,
  onCancel,
  onSuccess,
  initialData,
}: EnterpriseFormModalProps) {
  const [form] = Form.useForm<EnterpriseFormData>();
  const [loading, setLoading] = React.useState(false);
  const [areaValue, setAreaValue] = useState<(number | string)[]>([]);
  const [logoFileList, setLogoFileList] = useState<UploadFile[]>([]);
  const [licenseFileList, setLicenseFileList] = useState<UploadFile[]>([]);

  const { isEdit } = useModal<EnterpriseVO>({
    defaultFormData: initialData,
  });

  /**
   * Validation rules
   */
  const rules = {
    enterpriseName: [
      { required: true, message: '请输入企业名称' },
      {
        max: ENTERPRISE_VALIDATION.NAME_MAX_LENGTH,
        message: `企业名称最多${ENTERPRISE_VALIDATION.NAME_MAX_LENGTH}个字符`,
      },
    ],
    unifiedSocialCreditCode: [
      { required: true, message: '请输入统一社会信用代码' },
      {
        len: ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH,
        message: `统一社会信用代码必须为${ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH}位`,
      },
    ],
    type: [{ required: true, message: '请选择企业类型' }],
    contact: [
      { required: true, message: '请输入联系人' },
      {
        max: ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH,
        message: `联系人最多${ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH}个字符`,
      },
    ],
    contactPhone: [
      { required: true, message: '请输入联系电话' },
      {
        pattern: /^1[3-9]\d{9}$/,
        message: '请输入正确的手机号码',
      },
    ],
    email: [
      {
        type: 'email' as const,
        message: '请输入正确的邮箱格式',
      },
      {
        max: ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH,
        message: `邮箱最多${ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH}个字符`,
      },
    ],
    address: [
      {
        max: ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH,
        message: `地址最多${ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH}个字符`,
      },
    ],
  };

  /**
   * Handle area cascader change: extract province/city/district values and names
   */
  const handleAreaChange = (values: (number | string)[], selectedOptions: AreaOption[]) => {
    setAreaValue(values || []);
    if (selectedOptions && selectedOptions.length > 0) {
      form.setFieldsValue({
        province: String(selectedOptions[0]?.value ?? ''),
        city: String(selectedOptions[1]?.value ?? ''),
        district: String(selectedOptions[2]?.value ?? ''),
      });
    } else {
      form.setFieldsValue({
        province: undefined,
        city: undefined,
        district: undefined,
      });
    }
  };

  /**
   * Handle logo upload change
   */
  const handleLogoChange = (fileList: UploadFile[]) => {
    setLogoFileList(fileList);
    // Extract the URL from the response of the last successfully uploaded file
    const uploaded = fileList.find((f) => f.status === 'done' && f.response);
    if (uploaded && uploaded.response) {
      const resp = uploaded.response;
      if (resp.ok && resp.data) {
        form.setFieldsValue({ enterpriseLogo: resp.data.fileUrl || resp.data });
      }
    } else if (fileList.length === 0) {
      form.setFieldsValue({ enterpriseLogo: undefined });
    }
  };

  /**
   * Handle business license upload change
   */
  const handleLicenseChange = (fileList: UploadFile[]) => {
    setLicenseFileList(fileList);
    const uploaded = fileList.find((f) => f.status === 'done' && f.response);
    if (uploaded && uploaded.response) {
      const resp = uploaded.response;
      if (resp.ok && resp.data) {
        form.setFieldsValue({ businessLicense: resp.data.fileUrl || resp.data });
      }
    } else if (fileList.length === 0) {
      form.setFieldsValue({ businessLicense: undefined });
    }
  };

  /**
   * Form submit
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit && initialData) {
        const updateForm: EnterpriseUpdateForm = {
          enterpriseId: initialData.enterpriseId,
          enterpriseName: values.enterpriseName!,
          unifiedSocialCreditCode: values.unifiedSocialCreditCode!,
          type: values.type!,
          contact: values.contact!,
          contactPhone: values.contactPhone!,
          email: values.email,
          province: values.province,
          city: values.city,
          district: values.district,
          address: values.address,
          enterpriseLogo: values.enterpriseLogo,
          businessLicense: values.businessLicense,
          disabledFlag: values.disabledFlag || false,
        };

        const res = await enterpriseApi.update(updateForm);
        if (res.ok) {
          message.success('更新成功');
          onSuccess();
        }
      } else {
        const addForm: EnterpriseAddForm = {
          enterpriseName: values.enterpriseName!,
          unifiedSocialCreditCode: values.unifiedSocialCreditCode!,
          type: values.type!,
          contact: values.contact!,
          contactPhone: values.contactPhone!,
          email: values.email,
          province: values.province,
          city: values.city,
          district: values.district,
          address: values.address,
          enterpriseLogo: values.enterpriseLogo,
          businessLicense: values.businessLicense,
          disabledFlag: values.disabledFlag || false,
        };

        const res = await enterpriseApi.create(addForm);
        if (res.ok) {
          message.success('新增成功');
          onSuccess();
        }
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || (isEdit ? '更新失败' : '新增失败'));
      }
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    setAreaValue([]);
    setLogoFileList([]);
    setLicenseFileList([]);
    onCancel();
  };

  /**
   * Initialize form data (edit mode)
   */
  useEffect(() => {
    if (visible && initialData) {
      form.setFieldsValue({
        enterpriseName: initialData.enterpriseName,
        unifiedSocialCreditCode: initialData.unifiedSocialCreditCode,
        type: initialData.type,
        contact: initialData.contact,
        contactPhone: initialData.contactPhone,
        email: initialData.email,
        province: initialData.province,
        city: initialData.city,
        district: initialData.district,
        address: initialData.address,
        enterpriseLogo: initialData.enterpriseLogo,
        businessLicense: initialData.businessLicense,
        disabledFlag: initialData.disabledFlag,
      });

      // Restore area cascader value
      if (initialData.province) {
        const areaVals: (number | string)[] = [];
        if (initialData.province) areaVals.push(Number(initialData.province) || initialData.province);
        if (initialData.city) areaVals.push(Number(initialData.city) || initialData.city);
        if (initialData.district) areaVals.push(Number(initialData.district) || initialData.district);
        setAreaValue(areaVals);
      } else {
        setAreaValue([]);
      }

      // Restore file lists for logo and license
      if (initialData.enterpriseLogo) {
        setLogoFileList([{
          uid: '-1',
          name: 'logo',
          status: 'done',
          url: initialData.enterpriseLogo,
        }]);
      } else {
        setLogoFileList([]);
      }
      if (initialData.businessLicense) {
        setLicenseFileList([{
          uid: '-2',
          name: 'license',
          status: 'done',
          url: initialData.businessLicense,
        }]);
      } else {
        setLicenseFileList([]);
      }
    } else if (visible && !initialData) {
      form.resetFields();
      form.setFieldsValue({ disabledFlag: false });
      setAreaValue([]);
      setLogoFileList([]);
      setLicenseFileList([]);
    }
  }, [visible, initialData, form]);

  return (
    <Modal
      title={isEdit ? '编辑企业' : '新增企业'}
      open={visible}
      onCancel={handleCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={700}
      destroyOnClose
    >
      <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
        <Form.Item label="企业名称" name="enterpriseName" rules={rules.enterpriseName}>
          <Input placeholder="请输入企业名称" maxLength={ENTERPRISE_VALIDATION.NAME_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="企业 Logo" name="enterpriseLogo">
          <FileUpload
            value={logoFileList}
            onChange={handleLogoChange}
            accept=".jpg,.jpeg,.png,.gif"
            maxCount={1}
            maxSize={1}
            listType="picture-card"
          />
        </Form.Item>

        <Form.Item
          label="统一社会信用代码"
          name="unifiedSocialCreditCode"
          rules={rules.unifiedSocialCreditCode}
        >
          <Input
            placeholder="请输入18位统一社会信用代码"
            maxLength={ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH}
          />
        </Form.Item>

        <Form.Item label="企业类型" name="type" rules={rules.type}>
          <Select placeholder="请选择企业类型">
            {Object.entries(ENTERPRISE_TYPE_LABELS).map(([value, label]) => (
              <Select.Option key={value} value={Number(value) as EnterpriseTypeEnum}>
                {label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item label="联系人" name="contact" rules={rules.contact}>
          <Input placeholder="请输入联系人" maxLength={ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="联系电话" name="contactPhone" rules={rules.contactPhone}>
          <Input placeholder="请输入联系电话" maxLength={ENTERPRISE_VALIDATION.PHONE_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="邮箱" name="email" rules={rules.email}>
          <Input placeholder="请输入邮箱（可选）" maxLength={ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="所在城市">
          <AreaCascader
            value={areaValue}
            onChange={handleAreaChange}
            placeholder="请选择所在城市"
            width="100%"
          />
        </Form.Item>
        {/* Hidden fields to store province/city/district codes */}
        <Form.Item name="province" hidden><Input /></Form.Item>
        <Form.Item name="city" hidden><Input /></Form.Item>
        <Form.Item name="district" hidden><Input /></Form.Item>

        <Form.Item label="详细地址" name="address" rules={rules.address}>
          <Input placeholder="请输入详细地址" maxLength={ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="启用状态" name="disabledFlag" valuePropName="checked">
          <Switch checkedChildren="禁用" unCheckedChildren="启用" defaultChecked={false} />
        </Form.Item>

        <Form.Item label="营业执照" name="businessLicense">
          <FileUpload
            value={licenseFileList}
            onChange={handleLicenseChange}
            accept=".jpg,.jpeg,.png,.gif"
            maxCount={1}
            maxSize={1}
            listType="picture-card"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
