/**
 * 登錄頁面
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Checkbox, message } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons';
import { useAppDispatch } from '@/store/hooks';
import { setUserLoginInfo } from '@/store/slices/userSlice';
import { getCaptcha, login, type LoginForm } from '@/api/system/login.api';
import { encryptData } from '@/lib/encrypt';
import { LocalStorageKey } from '@/constants/local-storage.const';
import { localSave, localRead, localRemove } from '@/utils/local-storage.util';
import styles from './Login.module.css';

/**
 * 登錄表單值
 */
interface LoginFormValues {
  /** 登錄名 */
  loginName: string;

  /** 密碼（明文） */
  password: string;

  /** 驗證碼 */
  captchaCode: string;
}

/**
 * 記住密碼緩存
 */
interface RememberPasswordCache {
  loginName: string;
  password: string; // Encrypted password
}

export default function Login() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  // Captcha state
  const [captchaImage, setCaptchaImage] = useState<string>('');
  const [captchaUuid, setCaptchaUuid] = useState<string>('');
  const refreshIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Form state
  const [loading, setLoading] = useState(false);
  const [rememberPwd, setRememberPwd] = useState(false);

  /**
   * 獲取驗證碼
   */
  const getCaptchaImage = async () => {
    try {
      const res = await getCaptcha();
      setCaptchaImage(res.data.captchaBase64Image);
      setCaptchaUuid(res.data.captchaUuid);

      // 自動刷新計時器（過期前 5 秒刷新）
      startRefreshInterval(res.data.expireSeconds);

      // 開發環境打印驗證碼文本
      if (res.data.captchaText && import.meta.env.DEV) {
        console.log('驗證碼:', res.data.captchaText);
      }
    } catch (error) {
      message.error('獲取驗證碼失敗');
    }
  };

  /**
   * 啟動自動刷新計時器
   */
  const startRefreshInterval = (expireSeconds: number) => {
    if (refreshIntervalRef.current) {
      clearInterval(refreshIntervalRef.current);
    }

    // 過期前 5 秒刷新
    const refreshTime = Math.max((expireSeconds - 5) * 1000, 30000); // 最少 30 秒
    refreshIntervalRef.current = setInterval(getCaptchaImage, refreshTime);
  };

  /**
   * 停止自動刷新計時器
   */
  const stopRefreshInterval = () => {
    if (refreshIntervalRef.current) {
      clearInterval(refreshIntervalRef.current);
      refreshIntervalRef.current = null;
    }
  };

  /**
   * 登錄提交
   */
  const onLogin = async (values: LoginFormValues) => {
    try {
      setLoading(true);

      // 1. 密碼加密（SM4）
      const encryptedPassword = encryptData(values.password);
      if (!encryptedPassword) {
        message.error('密碼加密失敗');
        return;
      }

      // 2. 構建登錄請求
      const loginData: LoginForm = {
        loginName: values.loginName,
        password: encryptedPassword,
        captchaCode: values.captchaCode,
        captchaUuid: captchaUuid,
        loginDevice: 1, // 1-PC, 2-Android, 3-iOS, 4-H5
      };

      // 3. 調用登錄 API
      const res = await login(loginData);

      // 4. 停止驗證碼刷新
      stopRefreshInterval();

      // 5. 保存 Token（Redux Persist 自動持久化）
      localSave(LocalStorageKey.USER_TOKEN, res.data.token);

      // 6. 更新 Redux Store
      dispatch(setUserLoginInfo(res.data));

      // 7. 多租戶設置（如果有）
      if (res.data.tenantId) {
        localSave(LocalStorageKey.TENANT_ID, res.data.tenantId);
        localSave(LocalStorageKey.TENANT_TIMEZONE, res.data.timezone || 'Asia/Taipei');
      }

      // 8. 記住密碼
      if (rememberPwd) {
        localSave<RememberPasswordCache>(LocalStorageKey.REMEMBER_PWD, {
          loginName: values.loginName,
          password: encryptedPassword,
        });
      } else {
        localRemove(LocalStorageKey.REMEMBER_PWD);
      }

      // 9. 跳轉到首頁
      message.success('登錄成功');
      navigate('/home');
    } catch (error) {
      message.error('登錄失敗，請檢查用戶名和密碼');
      getCaptchaImage(); // 刷新驗證碼
    } finally {
      setLoading(false);
    }
  };

  /**
   * 組件初始化
   */
  useEffect(() => {
    // 獲取驗證碼
    getCaptchaImage();

    // 恢復記住的密碼
    const saved = localRead<RememberPasswordCache>(LocalStorageKey.REMEMBER_PWD);
    if (saved) {
      form.setFieldsValue({
        loginName: saved.loginName,
        password: saved.password, // 顯示加密後的密碼
      });
      setRememberPwd(true);
    }

    // 清理計時器
    return () => {
      stopRefreshInterval();
    };
  }, []);

  return (
    <div className={styles.loginContainer}>
      {/* 左側歡迎信息 */}
      <div className={styles.desc}>
        <h1>SmartAdmin</h1>
        <p>React + TypeScript + Redux Toolkit</p>
        <p>企業級中後台管理系統</p>
      </div>

      {/* 右側登錄表單 */}
      <div className={styles.login}>
        <h2 className={styles.title}>用戶登錄</h2>

        <Form
          form={form}
          onFinish={onLogin}
          autoComplete="off"
          size="large"
        >
          {/* 用戶名 */}
          <Form.Item
            name="loginName"
            rules={[
              { required: true, message: '請輸入用戶名' },
              { min: 2, message: '用戶名至少 2 個字符' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="請輸入用戶名"
              autoComplete="username"
            />
          </Form.Item>

          {/* 密碼 */}
          <Form.Item
            name="password"
            rules={[
              { required: true, message: '請輸入密碼' },
              { min: 6, message: '密碼至少 6 個字符' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="請輸入密碼"
              autoComplete="current-password"
            />
          </Form.Item>

          {/* 驗證碼 */}
          <Form.Item
            name="captchaCode"
            rules={[{ required: true, message: '請輸入驗證碼' }]}
          >
            <div className={styles.captchaRow}>
              <Input
                prefix={<SafetyOutlined />}
                placeholder="請輸入驗證碼"
                className={styles.captchaInput}
                autoComplete="off"
              />
              {captchaImage && (
                <img
                  src={captchaImage}
                  alt="驗證碼"
                  className={styles.captchaImage}
                  onClick={getCaptchaImage}
                  title="點擊刷新驗證碼"
                />
              )}
            </div>
          </Form.Item>

          {/* 記住密碼 */}
          <Form.Item>
            <Checkbox
              checked={rememberPwd}
              onChange={(e) => setRememberPwd(e.target.checked)}
            >
              記住密碼
            </Checkbox>
          </Form.Item>

          {/* 登錄按鈕 */}
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
            >
              登錄
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
}
