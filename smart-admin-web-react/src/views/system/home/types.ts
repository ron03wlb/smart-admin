/**
 * Home Module Type Definitions
 * 首頁模塊類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-17
 */

/**
 * 待辦事項 VO
 */
export interface ToBeDoneItem {
  /** 待辦標題 */
  title: string;
  /** 是否完成 */
  doneFlag: boolean;
  /** 是否星標（優先級） */
  starFlag: boolean;
  /** 創建時間 */
  createTime?: string;
}

/**
 * LocalStorage 鍵名常量
 */
export const LOCAL_STORAGE_KEYS = {
  /** 待辦事項列表 */
  TO_BE_DONE_LIST: 'smartadmin_to_be_done_list',
} as const;
