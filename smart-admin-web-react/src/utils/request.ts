import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { message } from 'antd';
import { LOCAL_STORAGE_KEYS } from '@/constants/storageKeys';

/**
 * 創建 Axios 實例
 * baseURL: http://127.0.0.1:1024 (SmartAdmin 後端 API)
 * timeout: 30秒
 */
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:1024',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 請求攔截器
 * 自動添加 Authorization Header（Bearer Token）
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(LOCAL_STORAGE_KEYS.USER_TOKEN);

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error: AxiosError) => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

/**
 * 響應攔截器
 * 處理 Token 過期（code 30007/30008）
 * 統一錯誤處理
 */
request.interceptors.response.use(
  (response: AxiosResponse) => {
    // 直接返回 response.data（SmartAdmin ResponseDTO）
    return response.data;
  },
  (error: AxiosError<any>) => {
    const { response } = error;

    if (response) {
      const { code, msg } = response.data || {};

      // Token 過期或失效
      if (code === 30007 || code === 30008) {
        message.error('登錄已過期，請重新登錄');
        localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_TOKEN);
        localStorage.removeItem(LOCAL_STORAGE_KEYS.USER_INFO);

        // 跳轉登錄頁
        window.location.href = '/login';
        return Promise.reject(error);
      }

      // 顯示錯誤提示
      message.error(msg || '請求失敗');
    } else {
      // 網絡錯誤
      message.error('網絡連接失敗，請檢查網絡');
    }

    return Promise.reject(error);
  }
);

export default request;
