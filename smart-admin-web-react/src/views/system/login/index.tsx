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
import { login, getLoginInfo, selectUserLoading, selectUserError } from '@/store/slices/userSlice';
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

  // MFA 雙因子認證 State
  const [showEmailCode, setShowEmailCode] = useState<boolean>(false);
  const [emailCodeCountdown, setEmailCodeCountdown] = useState<number>(0);
  const [emailCodeButtonDisabled, setEmailCodeButtonDisabled] = useState<boolean>(false);

  // 驗證碼自動刷新定時器
  const [captchaRefreshInterval, setCaptchaRefreshInterval] = useState<NodeJS.Timeout | null>(null);

  /**
   * 獲取驗證碼
   */
  const getCaptcha = useCallback(async () => {
    try {
      const response = await loginApi.getCaptcha();
      if (response.ok && response.data) {
        setCaptchaImage(response.data.captchaImage);
        setCaptchaUuid(response.data.captchaUuid);

        // 設置自動刷新定時器（默認 300 秒過期，提前 5 秒刷新）
        const expireSeconds = 300;
        if (captchaRefreshInterval) {
          clearInterval(captchaRefreshInterval);
        }
        const interval = setInterval(() => {
          getCaptcha();
        }, (expireSeconds - 5) * 1000);
        setCaptchaRefreshInterval(interval);
      }
    } catch (err) {
      console.error('獲取驗證碼失敗:', err);
    }
  }, [captchaRefreshInterval]);

  /**
   * 組件掛載時獲取驗證碼並讀取記住的密碼
   */
  useEffect(() => {
    getCaptcha();
    // 檢查是否需要雙因子認證
    checkTwoFactorFlag();

    // 讀取記住的密碼
    const rememberedMe = localStorage.getItem('REMEMBER_ME') === 'true';
    if (rememberedMe) {
      const loginName = localStorage.getItem('LOGIN_NAME') || '';
      const password = localStorage.getItem('PASSWORD') || '';
      form.setFieldsValue({ loginName, password });
      setRememberMe(true);
    }
  }, [getCaptcha, form]);

  /**
   * 檢查是否需要雙因子認證
   */
  const checkTwoFactorFlag = async () => {
    try {
      const response = await loginApi.getTwoFactorLoginFlag();
      if (response.ok && response.data) {
        setShowEmailCode(response.data.twoFactorFlag || false);
      }
    } catch (err) {
      console.error('檢查雙因子認證失敗:', err);
    }
  };

  /**
   * 發送郵箱驗證碼
   */
  const sendEmailCode = async () => {
    try {
      const loginName = form.getFieldValue('loginName');
      if (!loginName) {
        message.warning('請先輸入用戶名');
        return;
      }

      const response = await loginApi.sendLoginEmailCode(loginName);
      if (response.ok) {
        message.success('驗證碼已發送到您的郵箱');
        // 開始倒計時 60 秒
        setEmailCodeCountdown(60);
        setEmailCodeButtonDisabled(true);
      }
    } catch (err) {
      console.error('發送郵箱驗證碼失敗:', err);
      message.error('發送驗證碼失敗');
    }
  };

  /**
   * 郵箱驗證碼倒計時
   */
  useEffect(() => {
    if (emailCodeCountdown > 0) {
      const timer = setTimeout(() => {
        setEmailCodeCountdown(emailCodeCountdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (emailCodeCountdown === 0 && emailCodeButtonDisabled) {
      setEmailCodeButtonDisabled(false);
    }
  }, [emailCodeCountdown, emailCodeButtonDisabled]);

  /**
   * 組件卸載時清除定時器
   */
  useEffect(() => {
    return () => {
      if (captchaRefreshInterval) {
        clearInterval(captchaRefreshInterval);
      }
    };
  }, [captchaRefreshInterval]);

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
        emailCode: values.emailCode, // MFA 郵箱驗證碼
      };

      // 如果勾選了記住密碼，保存到 localStorage
      if (rememberMe) {
        localStorage.setItem('REMEMBER_ME', 'true');
        localStorage.setItem('LOGIN_NAME', values.loginName);
        localStorage.setItem('PASSWORD', values.password);
      } else {
        localStorage.removeItem('REMEMBER_ME');
        localStorage.removeItem('LOGIN_NAME');
        localStorage.removeItem('PASSWORD');
      }

      // 調用 Redux login Thunk
      const result = await dispatch(login(loginForm));

      if (login.fulfilled.match(result)) {
        message.success('登錄成功！');

        // 登錄成功後獲取用戶信息（菜單、權限等）
        const infoResult = await dispatch(getLoginInfo());

        if (getLoginInfo.fulfilled.match(infoResult)) {
          // 用戶信息獲取成功，跳轉首頁
          navigate('/home');
        } else {
          message.error('獲取用戶信息失敗');
        }
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

          {/* 郵箱驗證碼（雙因子認證） */}
          {showEmailCode && (
            <Form.Item
              name="emailCode"
              rules={[{ required: showEmailCode, message: '請輸入郵箱驗證碼' }]}
            >
              <Row gutter={8}>
                <Col span={14}>
                  <Input
                    prefix={<SafetyOutlined />}
                    placeholder="郵箱驗證碼"
                    autoComplete="off"
                  />
                </Col>
                <Col span={10}>
                  <Button
                    onClick={sendEmailCode}
                    disabled={emailCodeButtonDisabled}
                    style={{ width: '100%', height: 40 }}
                  >
                    {emailCodeCountdown > 0
                      ? `${emailCodeCountdown}秒後重試`
                      : '發送驗證碼'}
                  </Button>
                </Col>
              </Row>
            </Form.Item>
          )}

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
