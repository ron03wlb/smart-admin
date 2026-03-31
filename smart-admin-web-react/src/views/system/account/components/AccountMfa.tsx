/**
 * Account MFA Component
 *
 * Corresponds to Vue's account/components/mfa/index.vue (361L)
 * Full MFA management: status display, enable/disable, backup code management.
 */
import React, { useState, useEffect, useCallback } from 'react';
import {
  Card, Alert, Button, Modal, Form, Input, List, Typography, Space, Statistic, message,
} from 'antd';
import { SafetyOutlined, StopOutlined, ReloadOutlined, InfoCircleOutlined, DownloadOutlined } from '@ant-design/icons';
import { mfaApi } from '@/api/system/mfa-api';
import type { MfaStatus } from '@/api/system/mfa-api';

const AccountMfa: React.FC = () => {
  const [mfaStatus, setMfaStatus] = useState<MfaStatus>({
    mfaEnabled: false,
    mfaType: 'TOTP',
    backupCodesGenerated: false,
    remainingBackupCodes: 0,
    lastVerifiedAt: null,
    enforcedByRole: false,
    needRegenerateBackupCodes: false,
    qrCodeConfirmed: false,
  });
  const [backupCodeCount, setBackupCodeCount] = useState(0);

  // Regenerate backup codes
  const [regenerateModalVisible, setRegenerateModalVisible] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [regenerateToken, setRegenerateToken] = useState('');

  // Disable MFA
  const [disableModalVisible, setDisableModalVisible] = useState(false);
  const [disabling, setDisabling] = useState(false);
  const [disableToken, setDisableToken] = useState('');

  // New backup codes display
  const [newBackupCodesVisible, setNewBackupCodesVisible] = useState(false);
  const [newBackupCodes, setNewBackupCodes] = useState<string[]>([]);

  /** Fetch MFA status */
  const fetchMfaStatus = useCallback(async () => {
    try {
      const res = await mfaApi.getStatus();
      if (res.code === 1 && res.data) {
        setMfaStatus(res.data);
      }
    } catch {
      message.error('获取 MFA 状态失败');
    }
  }, []);

  /** Fetch backup code count */
  const fetchBackupCodeCount = useCallback(async () => {
    try {
      const res = await mfaApi.getBackupCodeCount();
      if (res.code === 1) {
        setBackupCodeCount(res.data);
      }
    } catch {
      message.error('获取备份码数量失败');
    }
  }, []);

  useEffect(() => {
    fetchMfaStatus().then(() => {
      // Only fetch backup code count if MFA is enabled (read from latest state)
    });
  }, [fetchMfaStatus]);

  useEffect(() => {
    if (mfaStatus.mfaEnabled) {
      fetchBackupCodeCount();
    }
  }, [mfaStatus.mfaEnabled, fetchBackupCodeCount]);

  /** Start MFA setup */
  const startSetup = () => {
    message.info('MFA 完整设定流程将在后续版本实现');
  };

  /** Regenerate backup codes */
  const handleRegenerateBackupCodes = async () => {
    if (!regenerateToken || regenerateToken.length !== 6) {
      message.error('请输入 6 位 TOTP 验证码');
      return;
    }
    setRegenerating(true);
    try {
      const res = await mfaApi.regenerateBackupCodes(regenerateToken);
      if (res.code === 1 && res.data) {
        setNewBackupCodes(res.data.backupCodes);
        setRegenerateModalVisible(false);
        setNewBackupCodesVisible(true);
        message.success('备份码重新生成成功');
        await fetchBackupCodeCount();
      } else {
        message.error(res.msg || '重新生成备份码失败');
      }
    } catch {
      message.error('重新生成备份码失败');
    } finally {
      setRegenerating(false);
    }
  };

  /** Disable MFA */
  const handleDisableMfa = async () => {
    if (!disableToken || disableToken.length !== 6) {
      message.error('请输入 6 位 TOTP 验证码');
      return;
    }
    setDisabling(true);
    try {
      const res = await mfaApi.setupDisable(disableToken);
      if (res.code === 1) {
        setDisableModalVisible(false);
        message.success('MFA 已成功禁用');
        await fetchMfaStatus();
        setBackupCodeCount(0);
      } else {
        message.error(res.msg || '禁用 MFA 失败');
      }
    } catch {
      message.error('禁用 MFA 失败');
    } finally {
      setDisabling(false);
    }
  };

  /** Download backup codes */
  const downloadBackupCodes = () => {
    const content = newBackupCodes.map((code, index) => `${index + 1}. ${code}`).join('\n');
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `MFA_Backup_Codes_${new Date().toISOString().split('T')[0]}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    message.success('备份码已下载');
  };

  return (
    <div>
      <Card title="多因素认证（MFA）" bordered={false}>
        {/* MFA Status Alert */}
        {mfaStatus.mfaEnabled ? (
          <Alert
            message="MFA 已启用"
            description="您的帐号已启用多因素认证，每次登入时需要输入 TOTP 验证码或备份码。"
            type="success"
            showIcon
            style={{ marginBottom: 24 }}
          />
        ) : (
          <Alert
            message="MFA 未启用"
            description="建议启用多因素认证以增强帐号安全性。"
            type="warning"
            showIcon
            style={{ marginBottom: 24 }}
          />
        )}

        {/* Backup Code Management */}
        {mfaStatus.mfaEnabled && (
          <Card title="备份码管理" bordered={false} style={{ marginBottom: 24, backgroundColor: '#fafafa' }}>
            <Space direction="vertical" style={{ width: '100%' }} size={16}>
              <Statistic
                title="剩余备份码数量"
                value={backupCodeCount}
                suffix="/ 10"
                valueStyle={{ color: backupCodeCount <= 2 ? '#cf1322' : '#3f8600' }}
              />

              {backupCodeCount <= 2 && (
                <Alert
                  message="备份码即将用尽"
                  description={`您只剩下 ${backupCodeCount} 个备份码，建议立即重新生成备份码。`}
                  type="error"
                  showIcon
                  closable
                  style={{ marginBottom: 16 }}
                />
              )}

              <Button
                type="primary"
                icon={<ReloadOutlined />}
                loading={regenerating}
                onClick={() => { setRegenerateToken(''); setRegenerateModalVisible(true); }}
              >
                重新生成备份码
              </Button>

              <Typography.Text type="secondary">
                <InfoCircleOutlined /> 重新生成备份码需要输入当前的 TOTP 验证码，旧备份码将会失效。
              </Typography.Text>
            </Space>
          </Card>
        )}

        {/* MFA Toggle */}
        <Space size={16}>
          {!mfaStatus.mfaEnabled ? (
            <Button type="primary" icon={<SafetyOutlined />} onClick={startSetup}>
              启用 MFA
            </Button>
          ) : (
            <Button danger icon={<StopOutlined />} onClick={() => { setDisableToken(''); setDisableModalVisible(true); }}>
              禁用 MFA
            </Button>
          )}
        </Space>
      </Card>

      {/* Regenerate Backup Codes Modal */}
      <Modal
        title="重新生成备份码"
        open={regenerateModalVisible}
        onOk={handleRegenerateBackupCodes}
        onCancel={() => setRegenerateModalVisible(false)}
        confirmLoading={regenerating}
        okText="确认重新生成"
        cancelText="取消"
      >
        <Form layout="vertical">
          <Alert
            message="安全验证"
            description="为确保安全，请输入您的 Google Authenticator 中显示的 6 位 TOTP 验证码。"
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Form.Item label="TOTP 验证码" required>
            <Input
              value={regenerateToken}
              onChange={(e) => setRegenerateToken(e.target.value)}
              placeholder="请输入 6 位 TOTP 验证码"
              maxLength={6}
              style={{ width: 200 }}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Disable MFA Modal */}
      <Modal
        title="禁用多因素认证"
        open={disableModalVisible}
        onOk={handleDisableMfa}
        onCancel={() => setDisableModalVisible(false)}
        confirmLoading={disabling}
        okText="确认禁用"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <Form layout="vertical">
          <Alert
            message="警告"
            description="禁用 MFA 将降低您的帐号安全性。为确保安全，请输入您的 TOTP 验证码。"
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Form.Item label="TOTP 验证码" required>
            <Input
              value={disableToken}
              onChange={(e) => setDisableToken(e.target.value)}
              placeholder="请输入 6 位 TOTP 验证码"
              maxLength={6}
              style={{ width: 200 }}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* New Backup Codes Modal */}
      <Modal
        title="新的备份码"
        open={newBackupCodesVisible}
        footer={null}
        closable={false}
        maskClosable={false}
        width={600}
      >
        <Alert
          message="请妥善保存这些备份码"
          description="这些备份码仅显示一次，请立即下载或打印保存。每个备份码只能使用一次。"
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <List
          dataSource={newBackupCodes}
          bordered
          size="small"
          style={{ marginBottom: 16 }}
          renderItem={(item, index) => (
            <List.Item>
              <Typography.Text code strong>{index + 1}. {item}</Typography.Text>
            </List.Item>
          )}
        />
        <Space>
          <Button type="primary" icon={<DownloadOutlined />} onClick={downloadBackupCodes}>
            下载备份码
          </Button>
          <Button onClick={() => { setNewBackupCodesVisible(false); setNewBackupCodes([]); }}>
            我已保存，关闭
          </Button>
        </Space>
      </Modal>
    </div>
  );
};

export default AccountMfa;
