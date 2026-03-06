/**
 * 登錄相關 API
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { postRequest, getRequest } from '@/api/base';
import type { ResponseModel } from '@/api/base/response.model';
import type { LoginResult } from '@/types/user.types';

/**
 * 登錄表單
 */
export interface LoginForm {
  /** 登錄名（用戶名） */
  loginName: string;

  /** 密碼（SM4 加密後的 Base64 字符串） */
  password: string;

  /** 驗證碼 */
  captchaCode?: string;

  /** 驗證碼 UUID */
  captchaUuid?: string;

  /** 登錄設備：1-PC, 2-Android, 3-iOS, 4-H5 */
  loginDevice?: number;

  /** 郵箱驗證碼（雙因子認證） */
  emailCode?: string;
}

/**
 * 驗證碼響應
 */
export interface CaptchaResult {
  /** 驗證碼 UUID */
  captchaUuid: string;

  /** 驗證碼 Base64 圖片 */
  captchaBase64Image: string;

  /** 過期時間（秒） */
  expireSeconds: number;

  /** 驗證碼文本（僅開發環境返回） */
  captchaText?: string;
}

/**
 * 用戶登錄
 */
export function login(data: LoginForm): Promise<ResponseModel<LoginResult>> {
  return postRequest('/login', data);
}

/**
 * 用戶登出
 */
export function logout(): Promise<ResponseModel<void>> {
  return getRequest('/login/logout');
}

/**
 * 獲取驗證碼
 */
export function getCaptcha(): Promise<ResponseModel<CaptchaResult>> {
  return getRequest('/login/getCaptcha');
}

/**
 * 獲取登錄信息
 */
export function getLoginInfo(): Promise<ResponseModel<LoginResult>> {
  return getRequest('/login/getLoginInfo');
}

/**
 * 發送郵箱驗證碼（雙因子認證）
 */
export function sendLoginEmailCode(loginName: string): Promise<ResponseModel<void>> {
  return getRequest(`/login/sendEmailCode/${loginName}`);
}

/**
 * 獲取雙因子登錄標識
 */
export function getTwoFactorLoginFlag(): Promise<ResponseModel<boolean>> {
  return getRequest('/login/getTwoFactorLoginFlag');
}
