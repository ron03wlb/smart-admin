/**
 * Account Password Component
 * 個人中心 - 修改密碼
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/account/components/password/index.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useEffect } from 'react';
import { Form, Input, Button, message } from 'antd';
import { employeeApi } from '@/api/system/employeeApi';

/**
 * 密碼複雜度正則
 */
const PASSWORD_COMPLEX_REGEX =
  /^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\W_!@#$%^&*`~()-+=]+$)(?![a-z0-9]+$)(?![a-z\W_!@#$%^&*`~()-+=]+$)(?![0-9\W_!@#$%^&*`~()-+=]+$)[a-zA-Z0-9\W_!@#$%^&*`~()-+=]{8,20}$/;

const Password: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [passwordComplexityEnabled, setPasswordComplexityEnabled] = useState(false);

  /**
   * 獲取密碼複雜度配置
   */
  const getPasswordComplexityEnabled = async () => {
    try {
      const result = await employeeApi.getPasswordComplexityEnabled();
      setPasswordComplexityEnabled(result.data);
    } catch (error) {
      console.error('獲取密碼複雜度配置失敗:', error);
    }
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    getPasswordComplexityEnabled();
  }, []);

  /**
   * 密碼提示文本
   */
  const passwordTips = passwordComplexityEnabled
    ? '密碼長度8-20位，必須包含字母、數字、特殊符號（如：@#$%^&*()_+-=）等三種字符'
    : '密碼長度至少8位';

  /**
   * 密碼驗證規則
   */
  const passwordRules = passwordComplexityEnabled
    ? [
        { required: true, message: '請輸入新密碼' },
        { pattern: PASSWORD_COMPLEX_REGEX, message: '密碼格式錯誤' },
      ]
    : [
        { required: true, message: '請輸入新密碼' },
        { min: 8, message: '密碼長度至少8位' },
      ];

  /**
   * 提交修改
   */
  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      // 檢查新密碼與確認密碼是否一致
      if (values.newPassword !== values.confirmPwd) {
        message.error('新密碼與確認密碼不一致');
        return;
      }

      setLoading(true);
      await employeeApi.updateEmployeePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      message.success('修改成功');

      // 重置表單
      form.resetFields();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('請檢查表單必填項');
      } else {
        console.error('修改密碼失敗:', error);
        message.error('修改失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ maxWidth: 500 }}>
        <Form form={form} layout="vertical">
          <Form.Item
            label="原密碼"
            name="oldPassword"
            rules={[{ required: true, message: '請輸入原密碼' }]}
          >
            <Input.Password placeholder="請輸入原密碼" autoComplete="off" />
          </Form.Item>

          <Form.Item label="新密碼" name="newPassword" rules={passwordRules} help={passwordTips}>
            <Input.Password placeholder="請輸入新密碼" autoComplete="off" />
          </Form.Item>

          <Form.Item label="確認密碼" name="confirmPwd" rules={passwordRules} help={passwordTips}>
            <Input.Password placeholder="請輸入確認密碼" autoComplete="off" />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              onClick={handleSubmit}
              loading={loading}
              style={{ marginTop: 20 }}
            >
              修改密碼
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
};

export default Password;
