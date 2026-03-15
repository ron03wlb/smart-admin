/**
 * Help Doc Constants
 * 幫助文檔常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 幫助文檔權限點
 */
export const HELP_DOC_PERMISSION = {
  QUERY: 'support:helpDoc:query',
  ADD: 'support:helpDoc:add',
  UPDATE: 'support:helpDoc:update',
  DELETE: 'support:helpDoc:delete',
} as const;

/**
 * 幫助文檔目錄權限點
 */
export const HELP_DOC_CATALOG_PERMISSION = {
  ADD: 'support:helpDocCatalog:addCategory',
  EDIT: 'support:helpDocCatalog:edit',
  DELETE: 'support:helpDocCatalog:delete',
} as const;

/**
 * 幫助文檔表格列寬度
 */
export const HELP_DOC_TABLE_COLUMNS_WIDTH = {
  title: 0, // 自適應
  helpDocCatalogName: 120,
  author: 110,
  sort: 90,
  pageViewCount: 90,
  userViewCount: 90,
  createTime: 150,
  action: 90,
} as const;
