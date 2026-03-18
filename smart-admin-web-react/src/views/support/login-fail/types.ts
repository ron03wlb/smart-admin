/**
 * Login Fail Types
 * 登錄失敗記錄類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 登錄失敗記錄 VO
 */
export interface LoginFailVO {
  loginFailId: number;
  userId?: number;
  userType: number;
  loginName: string;
  loginFailCount: number;
  lockFlag: number; // 0-未鎖定, 1-已鎖定
  loginLockBeginTime?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 登錄失敗記錄查詢表單
 */
export interface LoginFailQueryForm {
  loginName?: string;
  lockFlag?: number; // 0-未鎖定, 1-已鎖定, undefined-全部
  loginLockBeginTimeBegin?: string;
  loginLockBeginTimeEnd?: string;
  pageNum?: number;
  pageSize?: number;
}
