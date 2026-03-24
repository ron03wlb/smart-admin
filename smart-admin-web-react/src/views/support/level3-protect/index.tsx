/**
 * Level 3 Protect Config Page
 * 三級等保配置頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/level3protect/level3-protect-config-index.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useEffect } from 'react';
import { Card, Alert, Form, Switch, InputNumber, Button, Space, message, Modal } from 'antd';
import { level3ProtectApi, type Level3ProtectConfig } from '@/api/support/level3ProtectApi';

/**
 * 三級等保默認配置（開啟保護）
 */
const PROTECT_DEFAULT_VALUES: Level3ProtectConfig = {
  loginFailMaxTimes: 5,
  loginFailLockMinutes: 30,
  loginActiveTimeoutMinutes: 30,
  passwordComplexityEnabled: true,
  regularChangePasswordMonths: 3,
  regularChangePasswordNotAllowRepeatTimes: 3,
  twoFactorLoginEnabled: true,
  fileDetectFlag: true,
  maxUploadFileSizeMb: 50,
};

/**
 * 無保護默認配置（關閉保護）
 */
const NO_PROTECT_DEFAULT_VALUES: Level3ProtectConfig = {
  loginFailMaxTimes: 0,
  loginFailLockMinutes: 0,
  loginActiveTimeoutMinutes: 0,
  passwordComplexityEnabled: false,
  regularChangePasswordMonths: 0,
  regularChangePasswordNotAllowRepeatTimes: 0,
  twoFactorLoginEnabled: false,
  fileDetectFlag: false,
  maxUploadFileSizeMb: 0,
};

const Level3ProtectPage: React.FC = () => {
  const [form] = Form.useForm<Level3ProtectConfig>();
  const [loading, setLoading] = useState(false);

  /**
   * 獲取配置
   */
  const getConfig = async () => {
    setLoading(true);
    try {
      const result = await level3ProtectApi.getConfig();
      if (!result.data) {
        message.warning('當前未配置三級等保');
        return;
      }
      const config: Level3ProtectConfig = JSON.parse(result.data);
      form.setFieldsValue(config);
    } catch (error) {
      console.error('獲取配置失敗:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    getConfig();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * 保存配置
   */
  const handleSave = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();
      setLoading(true);
      await level3ProtectApi.updateConfig(values);
      message.success('配置更新成功');
      getConfig();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據！');
      } else {
        console.error('保存配置失敗:', error);
        message.error('配置更新失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 恢復默認配置
   */
  const handleReset = async () => {
    form.setFieldsValue(PROTECT_DEFAULT_VALUES);
    await handleSave();
  };

  /**
   * 清除所有配置
   */
  const handleClear = () => {
    Modal.confirm({
      title: '提示',
      content: '確定要清除三級等保配置嗎？這樣系統不安全哦',
      okText: '清除三級等保配置',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        form.setFieldsValue(NO_PROTECT_DEFAULT_VALUES);
        await handleSave();
      },
    });
  };

  return (
    <div style={{ padding: '20px' }}>
      {/* 說明 Alert */}
      <Alert
        closable
        message={<h4>三級等保：</h4>}
        description={
          <pre style={{ whiteSpace: 'pre-wrap' }}>
            {`1.三級等保是中國國家等級保護認證中的最高級別認證，該認證包含了五個等級保護安全技術要求和五個安全管理要求，共涉及測評分類73類，要求非常嚴格。
2.三級等保是地市級以上國家機關、重要企事業單位需要達成的認證，在金融行業中，可以看作是除了銀行機構以外最高級別的信息安全等級保護。
3.具體三級等保要求，請查看"1024創新實驗室"寫的相關文檔 `}
            <a
              href="https://smartadmin.vip/views/level3protect/basic.html"
              target="_blank"
              rel="noreferrer"
            >
              三級等保文檔
            </a>
          </pre>
        }
      />

      <br />

      {/* 配置表單 */}
      <Card title="三級等保配置" loading={loading}>
        <Form
          form={form}
          labelCol={{ span: 6 }}
          wrapperCol={{ span: 18 }}
          initialValues={PROTECT_DEFAULT_VALUES}
          autoComplete="off"
        >
          <Form.Item
            label="配置雙因子登錄模式"
            name="twoFactorLoginEnabled"
            valuePropName="checked"
            extra="在用戶登錄時，需要同時提供用戶名和密碼以及其他形式的身份驗證信息，例如短信驗證碼等"
          >
            <Switch checkedChildren="開啟" unCheckedChildren="關閉" />
          </Form.Item>

          <Form.Item
            label="最大連續登錄失敗次數"
            name="loginFailMaxTimes"
            extra="連續登錄失敗超過一定次數，則需要鎖定；默認5次；0則不鎖定；"
            rules={[{ required: true, message: '請輸入最大連續登錄失敗次數' }]}
          >
            <InputNumber
              min={0}
              max={10}
              placeholder="最大連續登錄失敗次數"
              addonAfter="次"
              style={{ width: 200 }}
            />
          </Form.Item>

          <Form.Item
            label="連續登錄失敗鎖定分鐘"
            name="loginFailLockMinutes"
            extra="連續登錄失敗鎖定的時間；默認30分鐘，0則不鎖定"
            rules={[{ required: true, message: '請輸入連續登錄失敗鎖定分鐘' }]}
          >
            <InputNumber
              min={0}
              placeholder="連續登錄失敗鎖定分鐘"
              addonAfter="分鐘"
              style={{ width: 200 }}
            />
          </Form.Item>

          <Form.Item
            label="登錄後無操作自動退出的分鐘"
            name="loginActiveTimeoutMinutes"
            extra="如：登錄1小時沒操作自動退出當前登錄狀態；默認30分鐘"
            rules={[{ required: true, message: '請輸入登錄後無操作自動退出的分鐘' }]}
          >
            <InputNumber
              min={-1}
              placeholder="登錄後無操作自動退出的分鐘"
              addonAfter="分鐘"
              style={{ width: 200 }}
            />
          </Form.Item>

          <Form.Item
            label="開啟密碼複雜度"
            name="passwordComplexityEnabled"
            valuePropName="checked"
            extra="密碼長度為8-20位且必須包含字母、數字、特殊符號（如：@#$%^&*()_+-=）等三種字符"
          >
            <Switch checkedChildren="開啟" unCheckedChildren="關閉" />
          </Form.Item>

          <Form.Item
            label="定期修改密碼時間間隔"
            name="regularChangePasswordMonths"
            extra="定期修改密碼時間間隔，默認3個月"
            rules={[{ required: true, message: '請輸入定期修改密碼時間間隔' }]}
          >
            <InputNumber
              min={-1}
              max={6}
              placeholder="定期修改密碼時間間隔"
              addonAfter="月"
              style={{ width: 200 }}
            />
          </Form.Item>

          <Form.Item
            label="定期修改密碼不允許重複次數"
            name="regularChangePasswordNotAllowRepeatTimes"
            extra="定期修改密碼不允許重複次數，默認：3次以內密碼不能相同"
            rules={[{ required: true, message: '請輸入定期修改密碼不允許重複次數' }]}
          >
            <InputNumber
              min={-1}
              max={6}
              placeholder="相同密碼不允許重複次數"
              addonAfter="次"
              style={{ width: 200 }}
            />
          </Form.Item>

          <Form.Item
            label="文件安全檢測"
            name="fileDetectFlag"
            valuePropName="checked"
            extra="對文件類型、惡意文件進行檢測；（具體請看後端： SecurityFileService 類 checkFile 方法）"
          >
            <Switch checkedChildren="開啟" unCheckedChildren="關閉" />
          </Form.Item>

          <Form.Item
            label="上傳文件大小限制"
            name="maxUploadFileSizeMb"
            extra="上傳文件大小限制，默認 50 mb ( 0 表示不限制)"
            rules={[{ required: true, message: '請輸入上傳文件大小限制' }]}
          >
            <InputNumber
              min={0}
              placeholder="上傳文件大小限制"
              addonAfter="mb(兆)"
              style={{ width: 200 }}
            />
          </Form.Item>

          <br />

          <Form.Item wrapperCol={{ span: 14, offset: 6 }}>
            <Space>
              <Button type="primary" onClick={handleSave} loading={loading}>
                保存配置
              </Button>
              <Button onClick={handleReset} loading={loading}>
                恢復三級等保默認配置
              </Button>
              <Button danger onClick={handleClear} loading={loading}>
                清除所有配置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Level3ProtectPage;
