/**
 * Level 3 Protection Config
 *
 * Corresponds to Vue's support/level3protect/level3-protect-config-index.vue (254L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Form, InputNumber, Switch, Button, Space, Alert, Modal, message, Spin } from 'antd';
import { level3ProtectApi } from '@/api/support/level3protect-api';
import type { Level3ProtectConfig } from '@/api/support/level3protect-api';

const defaultConfig: Level3ProtectConfig = {
  twoFactorLoginEnabled: false,
  loginFailMaxTimes: 5,
  loginFailLockMinutes: 30,
  loginActiveTimeoutMinutes: 30,
  passwordComplexityEnabled: true,
  regularChangePasswordMonths: 3,
  regularChangePasswordNotAllowRepeatTimes: 3,
  fileDetectFlag: true,
  maxUploadFileSizeMb: 5,
};

const Level3ProtectConfigIndex: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadConfig = useCallback(async () => {
    setLoading(true);
    try {
      const res = await level3ProtectApi.getConfig();
      if (res.code === 1 && res.data) {
        form.setFieldsValue(res.data);
      }
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    loadConfig();
  }, [loadConfig]);

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await level3ProtectApi.updateConfig(values);
      message.success('保存成功');
    } finally {
      setSaving(false);
    }
  };

  const handleResetDefault = () => {
    Modal.confirm({
      title: '提示',
      content: '确定要恢复为默认配置么？',
      okText: '确定',
      cancelText: '取消',
      onOk() {
        form.setFieldsValue(defaultConfig);
      },
    });
  };

  const handleClearAll = () => {
    Modal.confirm({
      title: '危险操作',
      content: '确定要清除所有防护配置么？这将禁用所有安全策略！',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk() {
        form.setFieldsValue({
          twoFactorLoginEnabled: false,
          loginFailMaxTimes: 0,
          loginFailLockMinutes: 0,
          loginActiveTimeoutMinutes: 0,
          passwordComplexityEnabled: false,
          regularChangePasswordMonths: 0,
          regularChangePasswordNotAllowRepeatTimes: 0,
          fileDetectFlag: false,
          maxUploadFileSizeMb: 0,
        });
      },
    });
  };

  return (
    <Card>
      <Alert type="info" showIcon message="三级等保安全配置：根据等保要求配置安全策略" style={{ marginBottom: 16 }} />
      <Spin spinning={loading}>
        <Form form={form} labelCol={{ span: 8 }} wrapperCol={{ span: 10 }} style={{ maxWidth: 700 }}>
          <Form.Item label="双因子认证" name="twoFactorLoginEnabled" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item label="登录失败最大次数" name="loginFailMaxTimes">
            <InputNumber min={0} max={100} addonAfter="次" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="登录失败锁定时长" name="loginFailLockMinutes">
            <InputNumber min={0} max={1440} addonAfter="分钟" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="登录有效期" name="loginActiveTimeoutMinutes">
            <InputNumber min={0} max={1440} addonAfter="分钟" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="密码复杂度校验" name="passwordComplexityEnabled" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item label="定期修改密码" name="regularChangePasswordMonths">
            <InputNumber min={0} max={12} addonAfter="月" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="密码不允许重复次数" name="regularChangePasswordNotAllowRepeatTimes">
            <InputNumber min={0} max={20} addonAfter="次" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="文件检测" name="fileDetectFlag" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item label="最大上传文件大小" name="maxUploadFileSizeMb">
            <InputNumber min={0} max={100} addonAfter="MB" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item wrapperCol={{ offset: 8 }}>
            <Space>
              <Button type="primary" onClick={handleSave} loading={saving}>保存</Button>
              <Button onClick={handleResetDefault}>恢复默认</Button>
              <Button danger onClick={handleClearAll}>清除全部</Button>
            </Space>
          </Form.Item>
        </Form>
      </Spin>
    </Card>
  );
};

export default Level3ProtectConfigIndex;
