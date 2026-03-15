/**
 * Reload Types
 * 重載類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * Reload VO
 */
export interface ReloadVO {
  tag: string;
  identification: string;
  args: string;
  createTime: string;
  updateTime: string;
}

/**
 * Reload 表單數據
 */
export interface ReloadFormData {
  tag: string;
  identification: string;
  args: string;
}

/**
 * Reload 結果 VO
 */
export interface ReloadResultVO {
  id?: number;
  tag: string;
  args: string;
  result: boolean;
  exception?: string;
  createTime: string;
}
