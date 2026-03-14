/**
 * Config Types
 * 配置類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 配置 VO（列表顯示）
 */
export interface ConfigVO {
  /**
   * 配置 ID
   */
  configId: number;
  /**
   * 參數 Key
   */
  configKey: string;
  /**
   * 參數名稱
   */
  configName: string;
  /**
   * 參數值
   */
  configValue: string;
  /**
   * 備註
   */
  remark?: string;
  /**
   * 創建時間
   */
  createTime?: string;
  /**
   * 更新時間
   */
  updateTime?: string;
}

/**
 * 配置表單數據（新增/編輯）
 */
export interface ConfigFormData {
  /**
   * 配置 ID（編輯時有值）
   */
  configId?: number;
  /**
   * 參數 Key
   */
  configKey: string;
  /**
   * 參數名稱
   */
  configName: string;
  /**
   * 參數值
   */
  configValue: string;
  /**
   * 備註
   */
  remark?: string;
}

/**
 * 配置查詢表單
 */
export interface ConfigQueryForm {
  /**
   * 參數 Key（關鍵字查詢）
   */
  configKey?: string;
  /**
   * 頁碼
   */
  pageNum: number;
  /**
   * 每頁數量
   */
  pageSize: number;
}
