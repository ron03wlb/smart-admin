/**
 * Login page
 *
 * Uses shared useLoginForm hook for captcha, 2FA, SM4 encryption,
 * remember password, and Enter key binding.
 */
import { Form, Input, Button, Checkbox } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined, MailOutlined } from '@ant-design/icons';
import { useLoginForm } from '@/hooks/useLoginForm';
import styles from './Login.module.css';

export default function Login() {
  const {
    form,
    loading,
    rememberPwd,
    setRememberPwd,
    captchaImage,
    refreshCaptcha,
    onLogin,
    twoFactorEnabled,
    emailCodeTips,
    emailCodeDisabled,
    sendEmailCode,
  } = useLoginForm();

  return (
    <div className={styles.loginContainer}>
      <div className={styles.desc}>
        <h1>SmartAdmin</h1>
        <p>React + TypeScript + Redux Toolkit</p>
        <p>企业级中后台管理系统</p>
      </div>

      <div className={styles.login}>
        <h2 className={styles.title}>用户登录</h2>

        <Form form={form} onFinish={onLogin} autoComplete="off" size="large">
          <Form.Item
            name="loginName"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 2, message: '用户名至少 2 个字符' },
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" autoComplete="username" />
          </Form.Item>

          {twoFactorEnabled && (
            <Form.Item name="emailCode" rules={[{ required: true, message: '请输入邮箱验证码' }]}>
              <div className={styles.captchaRow}>
                <Input prefix={<MailOutlined />} placeholder="请输入邮箱验证码" className={styles.captchaInput} />
                <Button type="primary" onClick={sendEmailCode} disabled={emailCodeDisabled} style={{ minWidth: 130 }}>
                  {emailCodeTips}
                </Button>
              </div>
            </Form.Item>
          )}

          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, message: '密码至少 6 个字符' },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" autoComplete="current-password" />
          </Form.Item>

          <Form.Item name="captchaCode" rules={[{ required: true, message: '请输入验证码' }]}>
            <div className={styles.captchaRow}>
              <Input prefix={<SafetyOutlined />} placeholder="请输入验证码" className={styles.captchaInput} autoComplete="off" />
              {captchaImage && (
                <img src={captchaImage} alt="验证码" className={styles.captchaImage} onClick={refreshCaptcha} title="点击刷新验证码" />
              )}
            </div>
          </Form.Item>

          <Form.Item>
            <Checkbox checked={rememberPwd} onChange={(e) => setRememberPwd(e.target.checked)}>
              记住密码
            </Checkbox>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
}
