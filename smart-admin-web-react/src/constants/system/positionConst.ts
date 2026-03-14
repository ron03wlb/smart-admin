/**
 * Position Constants
 * 職位常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

/**
 * 職位權限點
 */
export const POSITION_PERMISSION = {
  /**
   * 查詢權限
   */
  QUERY: 'system:position:query',
  /**
   * 新增權限
   */
  ADD: 'system:position:add',
  /**
   * 更新權限
   */
  UPDATE: 'system:position:update',
  /**
   * 刪除權限
   */
  DELETE: 'system:position:delete',
  /**
   * 批量刪除權限
   */
  BATCH_DELETE: 'system:position:batchDelete',
} as const;

/**
 * 職位驗證規則
 */
export const POSITION_VALIDATION = {
  /**
   * 職位名稱最大長度
   */
  NAME_MAX_LENGTH: 30,
  /**
   * 職級最大長度
   */
  LEVEL_MAX_LENGTH: 30,
  /**
   * 備註最大長度
   */
  REMARK_MAX_LENGTH: 200,
} as const;

/**
 * 職位表格列寬
 */
export const POSITION_TABLE_COLUMNS_WIDTH = {
  /**
   * 職位名稱列寬
   */
  positionName: 200,
  /**
   * 職級列寬
   */
  positionLevel: 150,
  /**
   * 排序列寬
   */
  sort: 100,
  /**
   * 備註列寬
   */
  remark: 300,
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
  operate: 200,
} as const;
