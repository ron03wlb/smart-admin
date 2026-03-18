/**
 * Enterprise Form Modal Component
 * 企業表單 Modal 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect } from 'react';
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

  const { isEdit } = useModal<EnterpriseVO>({
    defaultFormData: initialData,
  });

  /**
   * 表單驗證規則
   */
  const rules = {
    enterpriseName: [
      { required: true, message: '請輸入企業名稱' },
      {
        max: ENTERPRISE_VALIDATION.NAME_MAX_LENGTH,
        message: `企業名稱最多${ENTERPRISE_VALIDATION.NAME_MAX_LENGTH}個字符`,
      },
    ],
    unifiedSocialCreditCode: [
      { required: true, message: '請輸入統一社會信用代碼' },
      {
        len: ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH,
        message: `統一社會信用代碼必須為${ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH}位`,
      },
    ],
    type: [{ required: true, message: '請選擇企業類型' }],
    contact: [
      { required: true, message: '請輸入聯系人' },
      {
        max: ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH,
        message: `聯系人最多${ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH}個字符`,
      },
    ],
    contactPhone: [
      { required: true, message: '請輸入聯系電話' },
      {
        pattern: /^1[3-9]\d{9}$/,
        message: '請輸入正確的手機號碼',
      },
    ],
    email: [
      {
        type: 'email' as const,
        message: '請輸入正確的郵箱格式',
      },
      {
        max: ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH,
        message: `郵箱最多${ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH}個字符`,
      },
    ],
    address: [
      {
        max: ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH,
        message: `地址最多${ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH}個字符`,
      },
    ],
  };

  /**
   * 表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit && initialData) {
        // 編輯模式
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
        // 新增模式
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
        message.error(error.message || (isEdit ? '更新失敗' : '新增失敗'));
      }
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Modal 關閉處理
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  /**
   * 初始化表單數據（編輯模式）
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
    } else if (visible && !initialData) {
      form.resetFields();
      // 設置默認值
      form.setFieldsValue({
        disabledFlag: false, // 默認啟用
      });
    }
  }, [visible, initialData, form]);

  return (
    <Modal
      title={isEdit ? '編輯企業' : '新增企業'}
      open={visible}
      onCancel={handleCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={700}
      destroyOnClose
    >
      <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
        <Form.Item label="企業名稱" name="enterpriseName" rules={rules.enterpriseName}>
          <Input
            placeholder="請輸入企業名稱"
            maxLength={ENTERPRISE_VALIDATION.NAME_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item
          label="統一社會信用代碼"
          name="unifiedSocialCreditCode"
          rules={rules.unifiedSocialCreditCode}
        >
          <Input
            placeholder="請輸入18位統一社會信用代碼"
            maxLength={ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH}
          />
        </Form.Item>

        <Form.Item label="企業類型" name="type" rules={rules.type}>
          <Select placeholder="請選擇企業類型">
            {Object.entries(ENTERPRISE_TYPE_LABELS).map(([value, label]) => (
              <Select.Option key={value} value={Number(value) as EnterpriseTypeEnum}>
                {label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item label="聯系人" name="contact" rules={rules.contact}>
          <Input
            placeholder="請輸入聯系人"
            maxLength={ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="聯系電話" name="contactPhone" rules={rules.contactPhone}>
          <Input
            placeholder="請輸入聯系電話"
            maxLength={ENTERPRISE_VALIDATION.PHONE_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="郵箱" name="email" rules={rules.email}>
          <Input
            placeholder="請輸入郵箱（可選）"
            maxLength={ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item
          label="詳細地址"
          name="address"
          rules={rules.address}
          tooltip="TODO: 應使用省市區選擇器組件"
        >
          <Input.TextArea
            placeholder="請輸入詳細地址（暫用純文本，後續應使用省市區選擇器）"
            rows={3}
            maxLength={ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item
          label="企業 Logo"
          name="enterpriseLogo"
          tooltip="TODO: 應使用文件上傳組件"
        >
          <Input placeholder="Logo URL（暫用文本，後續應使用文件上傳組件）" />
        </Form.Item>

        <Form.Item
          label="營業執照"
          name="businessLicense"
          tooltip="TODO: 應使用文件上傳組件"
        >
          <Input placeholder="營業執照 URL（暫用文本，後續應使用文件上傳組件）" />
        </Form.Item>

        <Form.Item label="啟用狀態" name="disabledFlag" valuePropName="checked">
          <Switch
            checkedChildren="禁用"
            unCheckedChildren="啟用"
            defaultChecked={false}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
