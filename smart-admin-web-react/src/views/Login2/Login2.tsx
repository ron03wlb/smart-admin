/**
 * Login2 - Light theme login page
 *
 * White background with left-bg1.png welcome panel.
 * Uses shared useLoginForm hook.
 */
import { Form, Input, Button, Checkbox } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined, MailOutlined } from '@ant-design/icons';
import { useLoginForm } from '@/hooks/useLoginForm';
import loginQR from '@/assets/images/login/login-qr.png';
import styles from './Login2.module.css';

export default function Login2() {
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
    <div className={styles.container}>
      <div className={styles.descPanel}>
        <div className={styles.welcome}>
          <p className={styles.welcomeTitle}>欢迎登录 SmartAdmin</p>
          <p className={styles.welcomeSub}>「高质量代码、简洁、安全」的开发平台</p>
        </div>
      </div>

      <div className={styles.loginPanel}>
        <img src={loginQR} alt="QR" className={styles.loginQr} />
        <h2 className={styles.loginTitle}>账号登录</h2>

        <Form form={form} onFinish={onLogin} autoComplete="off" size="large">
          <Form.Item name="loginName" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" />
          </Form.Item>

          {twoFactorEnabled && (
            <Form.Item name="emailCode" rules={[{ required: true, message: '请输入邮箱验证码' }]}>
              <div className={styles.captchaRow}>
                <Input prefix={<MailOutlined />} placeholder="请输入邮箱验证码" style={{ flex: 1 }} />
                <Button type="primary" onClick={sendEmailCode} disabled={emailCodeDisabled} className={styles.codeBtn}>
                  {emailCodeTips}
                </Button>
              </div>
            </Form.Item>
          )}

          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
          </Form.Item>

          <Form.Item name="captchaCode" rules={[{ required: true, message: '请输入验证码' }]}>
            <div className={styles.captchaRow}>
              <Input prefix={<SafetyOutlined />} placeholder="请输入验证码" className={styles.captchaInput} />
              {captchaImage && (
                <img src={captchaImage} alt="captcha" className={styles.captchaImg} onClick={refreshCaptcha} />
              )}
            </div>
          </Form.Item>

          <Form.Item>
            <Checkbox checked={rememberPwd} onChange={(e) => setRememberPwd(e.target.checked)}>
              记住密码
            </Checkbox>
            <span style={{ color: '#999' }}> ( 账号：admin, 密码：123456)</span>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block className={styles.loginBtn}>
              登录
            </Button>
          </Form.Item>
        </Form>

        <div className={styles.moreLogin}>
          <div className={styles.moreTitleBox}>
            <span className={styles.moreLine} />
            <span className={styles.moreText}>其他方式登录</span>
            <span className={styles.moreLine} />
          </div>
        </div>
      </div>
    </div>
  );
}
