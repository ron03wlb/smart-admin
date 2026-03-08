/**
 * useLoginForm Hook - Shared login logic for Login, Login2, Login3
 *
 * Encapsulates: captcha lifecycle, 2FA email code, SM4 encryption,
 * remember password, Enter key binding, login submission.
 */
import { useState, useEffect, useRef, useCallback } from 'react';
import { Form, message } from 'antd';
import type { FormInstance } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch } from '@/store/hooks';
import { setUserLoginInfo } from '@/store/slices/userSlice';
import {
  getCaptcha,
  login,
  sendLoginEmailCode,
  getTwoFactorLoginFlag,
  type LoginForm as LoginFormData,
} from '@/api/system/login.api';
import { encryptData } from '@/lib/encrypt';
import { LocalStorageKey } from '@/constants/local-storage.const';
import { localSave, localRead, localRemove } from '@/utils/local-storage.util';

export interface LoginFormValues {
  loginName: string;
  password: string;
  captchaCode: string;
  emailCode?: string;
}

interface RememberPasswordCache {
  loginName: string;
  password: string;
}

export interface UseLoginFormReturn {
  form: FormInstance<LoginFormValues>;
  loading: boolean;
  rememberPwd: boolean;
  setRememberPwd: (v: boolean) => void;
  captchaImage: string;
  captchaUuid: string;
  refreshCaptcha: () => Promise<void>;
  onLogin: (values: LoginFormValues) => Promise<void>;
  twoFactorEnabled: boolean;
  emailCodeTips: string;
  emailCodeDisabled: boolean;
  sendEmailCode: () => Promise<void>;
}

export function useLoginForm(): UseLoginFormReturn {
  const [form] = Form.useForm<LoginFormValues>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  // Captcha state
  const [captchaImage, setCaptchaImage] = useState('');
  const [captchaUuid, setCaptchaUuid] = useState('');
  const refreshIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Form state
  const [loading, setLoading] = useState(false);
  const [rememberPwd, setRememberPwd] = useState(false);

  // 2FA state
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(false);
  const [emailCodeTips, setEmailCodeTips] = useState('获取邮箱验证码');
  const [emailCodeDisabled, setEmailCodeDisabled] = useState(false);
  const countDownTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const stopRefreshInterval = useCallback(() => {
    if (refreshIntervalRef.current) {
      clearInterval(refreshIntervalRef.current);
      refreshIntervalRef.current = null;
    }
  }, []);

  const refreshCaptcha = useCallback(async () => {
    try {
      const res = await getCaptcha();
      setCaptchaImage(res.data.captchaBase64Image);
      setCaptchaUuid(res.data.captchaUuid);

      // Auto-refresh before expiry
      stopRefreshInterval();
      const refreshTime = Math.max((res.data.expireSeconds - 5) * 1000, 30000);
      refreshIntervalRef.current = setInterval(async () => {
        try {
          const r = await getCaptcha();
          setCaptchaImage(r.data.captchaBase64Image);
          setCaptchaUuid(r.data.captchaUuid);
        } catch {
          // silent
        }
      }, refreshTime);

      if (res.data.captchaText && import.meta.env.DEV) {
        console.log('Captcha:', res.data.captchaText);
      }
    } catch {
      message.error('获取验证码失败');
    }
  }, [stopRefreshInterval]);

  // 2FA: check flag
  const checkTwoFactorFlag = useCallback(async () => {
    try {
      const res = await getTwoFactorLoginFlag();
      setTwoFactorEnabled(res.data);
    } catch {
      // silent
    }
  }, []);

  // 2FA: send email code with 60s countdown
  const sendEmailCode = useCallback(async () => {
    const loginName = form.getFieldValue('loginName');
    if (!loginName) {
      message.warning('请先输入用户名');
      return;
    }
    try {
      await sendLoginEmailCode(loginName);
      message.success('验证码发送成功！请查看邮箱');

      // Start 60s countdown
      setEmailCodeDisabled(true);
      let countDown = 60;
      setEmailCodeTips(`${countDown}秒后重新获取`);
      countDownTimerRef.current = setInterval(() => {
        countDown--;
        if (countDown > 0) {
          setEmailCodeTips(`${countDown}秒后重新获取`);
        } else {
          if (countDownTimerRef.current) clearInterval(countDownTimerRef.current);
          setEmailCodeDisabled(false);
          setEmailCodeTips('获取验证码');
        }
      }, 1000);
    } catch {
      message.error('验证码发送失败');
    }
  }, [form]);

  // Login submission
  const onLogin = useCallback(
    async (values: LoginFormValues) => {
      try {
        setLoading(true);

        const encryptedPassword = encryptData(values.password);
        if (!encryptedPassword) {
          message.error('密码加密失败');
          return;
        }

        const loginData: LoginFormData = {
          loginName: values.loginName,
          password: encryptedPassword,
          captchaCode: values.captchaCode,
          captchaUuid: captchaUuid,
          loginDevice: 1,
        };

        if (twoFactorEnabled && values.emailCode) {
          loginData.emailCode = values.emailCode;
        }

        const res = await login(loginData);

        stopRefreshInterval();
        localSave(LocalStorageKey.USER_TOKEN, res.data.token);
        dispatch(setUserLoginInfo(res.data));

        if (res.data.tenantId) {
          localSave(LocalStorageKey.TENANT_ID, res.data.tenantId);
          localSave(LocalStorageKey.TENANT_TIMEZONE, res.data.timezone || 'Asia/Taipei');
        }

        if (rememberPwd) {
          localSave<RememberPasswordCache>(LocalStorageKey.REMEMBER_PWD, {
            loginName: values.loginName,
            password: encryptedPassword,
          });
        } else {
          localRemove(LocalStorageKey.REMEMBER_PWD);
        }

        message.success('登录成功');
        navigate('/home');
      } catch {
        message.error('登录失败，请检查用户名和密码');
        refreshCaptcha();
      } finally {
        setLoading(false);
      }
    },
    [captchaUuid, twoFactorEnabled, rememberPwd, dispatch, navigate, stopRefreshInterval, refreshCaptcha]
  );

  // Init: load captcha, check 2FA flag, restore remembered password, bind Enter key
  useEffect(() => {
    refreshCaptcha();
    checkTwoFactorFlag();

    const saved = localRead<RememberPasswordCache>(LocalStorageKey.REMEMBER_PWD);
    if (saved) {
      form.setFieldsValue({ loginName: saved.loginName, password: saved.password });
      setRememberPwd(true);
    }

    const handleKeyUp = (e: KeyboardEvent) => {
      if (e.key === 'Enter') {
        form.submit();
      }
    };
    document.addEventListener('keyup', handleKeyUp);

    return () => {
      stopRefreshInterval();
      if (countDownTimerRef.current) clearInterval(countDownTimerRef.current);
      document.removeEventListener('keyup', handleKeyUp);
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return {
    form,
    loading,
    rememberPwd,
    setRememberPwd,
    captchaImage,
    captchaUuid,
    refreshCaptcha,
    onLogin,
    twoFactorEnabled,
    emailCodeTips,
    emailCodeDisabled,
    sendEmailCode,
  };
}
