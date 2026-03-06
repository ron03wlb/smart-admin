/**
 * 響應模型（對應後端 ResponseDTO）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
export interface ResponseModel<T = any> {
  /**
   * 狀態碼（1: 成功，其他: 失敗）
   */
  code: number;

  /**
   * 響應數據
   */
  data: T;

  /**
   * 響應消息
   */
  msg?: string;

  /**
   * 是否成功
   */
  success: boolean;
}

/**
 * 錯誤碼常量
 */
export const ErrorCode = {
  /** 成功 */
  SUCCESS: 1,

  /** Token 過期 */
  TOKEN_EXPIRED: 30007,

  /** 賬號已在別處登錄 */
  ACCOUNT_LOGGED_ELSEWHERE: 30008,

  /** 等保安全登錄提醒 */
  SECURITY_LOGIN_REMINDER_1: 30010,
  SECURITY_LOGIN_REMINDER_2: 30011,

  /** 長時間未操作系統 */
  LONG_TIME_NO_OPERATION: 30012,
} as const;
