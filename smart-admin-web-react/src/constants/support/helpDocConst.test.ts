/**
 * Help Doc Constants Test
 * 幫助文檔常量定義測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect } from 'vitest';
import {
  HELP_DOC_PERMISSION,
  HELP_DOC_CATALOG_PERMISSION,
  HELP_DOC_TABLE_COLUMNS_WIDTH,
} from './helpDocConst';

describe('helpDocConst', () => {
  describe('HELP_DOC_PERMISSION', () => {
    it('應該定義正確的權限點', () => {
      expect(HELP_DOC_PERMISSION.QUERY).toBe('support:helpDoc:query');
      expect(HELP_DOC_PERMISSION.ADD).toBe('support:helpDoc:add');
      expect(HELP_DOC_PERMISSION.UPDATE).toBe('support:helpDoc:update');
      expect(HELP_DOC_PERMISSION.DELETE).toBe('support:helpDoc:delete');
    });
  });

  describe('HELP_DOC_CATALOG_PERMISSION', () => {
    it('應該定義正確的目錄權限點', () => {
      expect(HELP_DOC_CATALOG_PERMISSION.ADD).toBe('support:helpDocCatalog:addCategory');
      expect(HELP_DOC_CATALOG_PERMISSION.EDIT).toBe('support:helpDocCatalog:edit');
      expect(HELP_DOC_CATALOG_PERMISSION.DELETE).toBe('support:helpDocCatalog:delete');
    });
  });

  describe('HELP_DOC_TABLE_COLUMNS_WIDTH', () => {
    it('應該定義表格列寬度', () => {
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.title).toBe(0);
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.helpDocCatalogName).toBe(120);
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.author).toBe(110);
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.sort).toBe(90);
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.pageViewCount).toBe(90);
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.userViewCount).toBe(90);
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.createTime).toBe(150);
      expect(HELP_DOC_TABLE_COLUMNS_WIDTH.action).toBe(90);
    });
  });
});
