/**
 * Config Constants
 * 配置常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 配置權限點
 */
export const CONFIG_PERMISSION = {
  /**
   * 查詢權限
   */
  QUERY: 'support:config:query',
  /**
   * 新增權限
   */
  ADD: 'support:config:add',
  /**
   * 更新權限
   */
  UPDATE: 'support:config:update',
} as const;

/**
 * 配置驗證規則
 */
export const CONFIG_VALIDATION = {
  /**
   * 參數 Key 最大長度
   */
  KEY_MAX_LENGTH: 50,
  /**
   * 參數名稱最大長度
   */
  NAME_MAX_LENGTH: 100,
  /**
   * 參數值最大長度
   */
  VALUE_MAX_LENGTH: 500,
  /**
   * 備註最大長度
   */
  REMARK_MAX_LENGTH: 500,
} as const;

/**
 * 配置表格列寬
 */
export const CONFIG_TABLE_COLUMNS_WIDTH = {
  /**
   * ID 列寬
   */
  configId: 80,
  /**
   * 參數 Key 列寬
   */
  configKey: 200,
  /**
   * 參數名稱列寬
   */
  configName: 200,
  /**
   * 參數值列寬
   */
  configValue: 250,
  /**
   * 備註列寬
   */
  remark: 200,
  /**
   * 創建時間列寬
   */
  createTime: 180,
  /**
   * 更新時間列寬
   */
  updateTime: 180,
  /**
   * 操作列寬
   */
  operate: 100,
} as const;
