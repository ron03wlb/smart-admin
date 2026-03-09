/**
 * 登錄 API
 * 用戶登錄、退出登錄、驗證碼、雙因子認證等
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';

// ==================== 請求參數類型 ====================

/**
 * 登錄表單
 */
export interface LoginForm {
  /** 用戶名 */
  loginName: string;

  /** 密碼 */
  password: string;

  /** 驗證碼 */
  captchaCode?: string;

  /** 驗證碼UUID */
  captchaUuid?: string;

  /** 郵箱驗證碼（雙因子認證） */
  emailCode?: string;
}

// ==================== 響應數據類型 ====================

/**
 * 登錄結果
 */
export interface LoginResult {
  /** JWT Token */
  token: string;

  /** 員工ID */
  employeeId: string;

  /** 是否需要雙因子認證 */
  twoFactorFlag?: boolean;
}

/**
 * 驗證碼結果
 */
export interface CaptchaResult {
  /** 驗證碼UUID */
  captchaUuid: string;

  /** 驗證碼圖片（Base64） */
  captchaImage: string;
}

/**
 * 登錄信息
 */
export interface LoginInfo {
  /** 員工ID */
  employeeId: string;

  /** 員工姓名 */
  employeeName: string;

  /** 登錄帳號 */
  loginName: string;

  /** 是否管理員 */
  administratorFlag: boolean;

  /** 菜單樹 */
  menuTreeList: MenuVO[];

  /** 權限點列表 */
  pointList: PointVO[];
}

/**
 * 菜單VO
 */
export interface MenuVO {
  /** 菜單ID */
  menuId: string;

  /** 菜單名稱 */
  menuName: string;

  /** 菜單路徑 */
  path: string;

  /** 父菜單ID */
  parentId: string;

  /** 菜單類型（1目錄 2菜單 3功能） */
  menuType: number;

  /** 排序 */
  sort: number;

  /** 圖標 */
  icon?: string;

  /** 子菜單 */
  children?: MenuVO[];

  /** 權限點 */
  webPerms?: string;
}

/**
 * 權限點VO
 */
export interface PointVO {
  /** 權限標識 */
  webPerms: string;

  /** 權限名稱 */
  permsName?: string;
}

/**
 * 雙因子登錄標識
 */
export interface TwoFactorLoginFlag {
  /** 是否啟用雙因子認證 */
  twoFactorFlag: boolean;
}

// ==================== API 方法 ====================

export const loginApi = {
  /**
   * 登錄
   * @param data 登錄表單
   */
  login: (data: LoginForm): Promise<ResponseDTO<LoginResult>> => {
    return request.post('/login', data);
  },

  /**
   * 退出登錄
   */
  logout: (): Promise<ResponseDTO<void>> => {
    return request.get('/login/logout');
  },

  /**
   * 獲取驗證碼
   */
  getCaptcha: (): Promise<ResponseDTO<CaptchaResult>> => {
    return request.get('/login/getCaptcha');
  },

  /**
   * 獲取登錄信息（菜單、權限等）
   */
  getLoginInfo: (): Promise<ResponseDTO<LoginInfo>> => {
    return request.get('/login/getLoginInfo');
  },

  /**
   * 發送郵箱登錄驗證碼（雙因子認證）
   * @param loginName 登錄帳號
   */
  sendLoginEmailCode: (loginName: string): Promise<ResponseDTO<void>> => {
    return request.get(`/login/sendEmailCode/${loginName}`);
  },

  /**
   * 獲取雙因子登錄標識
   */
  getTwoFactorLoginFlag: (): Promise<ResponseDTO<TwoFactorLoginFlag>> => {
    return request.get('/login/getTwoFactorLoginFlag');
  },
};
