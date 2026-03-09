/**
 * Login Page
 * 登錄頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/login/login.vue (314 行)
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useState, useEffect, useCallback } from 'react';
import { Form, Input, Button, Checkbox, Card, Typography, message, Row, Col } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { login, selectUserLoading, selectUserError } from '@/store/slices/userSlice';
import { loginApi } from '@/api/system/loginApi';
import type { LoginForm as LoginFormType } from '@/api/system/loginApi';

const { Title, Text } = Typography;

/**
 * 登錄頁面
 * 實現用戶登錄功能（用戶名、密碼、驗證碼）
 */
export default function LoginPage() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  // Redux State
  const loading = useAppSelector(selectUserLoading);
  const error = useAppSelector(selectUserError);

  // Local State
  const [captchaImage, setCaptchaImage] = useState<string>('');
  const [captchaUuid, setCaptchaUuid] = useState<string>('');
  const [rememberMe, setRememberMe] = useState<boolean>(false);

  /**
   * 獲取驗證碼
   */
  const getCaptcha = useCallback(async () => {
    try {
      const response = await loginApi.getCaptcha();
      if (response.ok && response.data) {
        setCaptchaImage(response.data.captchaImage);
        setCaptchaUuid(response.data.captchaUuid);
      }
    } catch (err) {
      console.error('獲取驗證碼失敗:', err);
    }
  }, []);

  /**
   * 組件掛載時獲取驗證碼
   */
  useEffect(() => {
    getCaptcha();
  }, [getCaptcha]);

  /**
   * 監聽錯誤並顯示提示
   */
  useEffect(() => {
    if (error) {
      message.error(error);
      // 登錄失敗後刷新驗證碼
      getCaptcha();
      form.setFieldsValue({ captchaCode: '' });
    }
  }, [error, getCaptcha, form]);

  /**
   * 處理表單提交
   */
  const handleSubmit = async (values: any) => {
    try {
      const loginForm: LoginFormType = {
        loginName: values.loginName,
        password: values.password,
        captchaCode: values.captchaCode,
        captchaUuid: captchaUuid,
      };

      // 調用 Redux login Thunk
      const result = await dispatch(login(loginForm));

      if (login.fulfilled.match(result)) {
        message.success('登錄成功！');
        // 登錄成功後跳轉首頁
        navigate('/home');
      }
    } catch (err) {
      console.error('登錄失敗:', err);
    }
  };

  /**
   * Enter 鍵提交
   */
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      form.submit();
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '20px',
      }}
      onKeyPress={handleKeyPress}
    >
      <Card
        style={{
          width: '100%',
          maxWidth: 450,
          boxShadow: '0 8px 24px rgba(0, 0, 0, 0.15)',
          borderRadius: 12,
        }}
      >
        {/* 標題 */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={2} style={{ margin: 0 }}>
            SmartAdmin React
          </Title>
          <Text type="secondary">歡迎登錄</Text>
        </div>

        {/* 登錄表單 */}
        <Form
          form={form}
          name="login"
          initialValues={{
            loginName: 'admin',
            password: '',
            captchaCode: '',
            remember: false,
          }}
          onFinish={handleSubmit}
          size="large"
        >
          {/* 用戶名 */}
          <Form.Item
            name="loginName"
            rules={[{ required: true, message: '請輸入用戶名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用戶名"
              autoComplete="username"
            />
          </Form.Item>

          {/* 密碼 */}
          <Form.Item
            name="password"
            rules={[{ required: true, message: '請輸入密碼' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密碼"
              autoComplete="current-password"
            />
          </Form.Item>

          {/* 驗證碼 */}
          <Form.Item
            name="captchaCode"
            rules={[{ required: true, message: '請輸入驗證碼' }]}
          >
            <Row gutter={8}>
              <Col span={14}>
                <Input
                  prefix={<SafetyOutlined />}
                  placeholder="驗證碼"
                  autoComplete="off"
                />
              </Col>
              <Col span={10}>
                {captchaImage ? (
                  <img
                    src={captchaImage}
                    alt="驗證碼"
                    onClick={getCaptcha}
                    style={{
                      width: '100%',
                      height: 40,
                      cursor: 'pointer',
                      borderRadius: 4,
                      border: '1px solid #d9d9d9',
                    }}
                  />
                ) : (
                  <Button
                    onClick={getCaptcha}
                    style={{ width: '100%', height: 40 }}
                  >
                    載入驗證碼
                  </Button>
                )}
              </Col>
            </Row>
          </Form.Item>

          {/* 記住密碼 */}
          <Form.Item>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Checkbox
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
              >
                記住密碼
              </Checkbox>
              <Text type="secondary" style={{ fontSize: 13 }}>
                (賬號：admin, 密碼：123456)
              </Text>
            </div>
          </Form.Item>

          {/* 登錄按鈕 */}
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              style={{ height: 40 }}
            >
              登錄
            </Button>
          </Form.Item>
        </Form>

        {/* 提示信息 */}
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            SmartAdmin React - Phase 1 POC
          </Text>
        </div>
      </Card>
    </div>
  );
}
