/**
 * Reload Constants Tests
 * 重載常量測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect } from 'vitest';
import { RELOAD_PERMISSION, RELOAD_TABLE_COLUMNS_WIDTH } from './reloadConst';

describe('reloadConst', () => {
  describe('RELOAD_PERMISSION', () => {
    it('應該包含正確的權限點定義', () => {
      expect(RELOAD_PERMISSION.EXECUTE).toBe('support:reload:execute');
      expect(RELOAD_PERMISSION.RESULT).toBe('support:reload:result');
    });
  });

  describe('RELOAD_TABLE_COLUMNS_WIDTH', () => {
    it('應該包含正確的表格列寬度定義', () => {
      expect(RELOAD_TABLE_COLUMNS_WIDTH.tag).toBe(200);
      expect(RELOAD_TABLE_COLUMNS_WIDTH.identification).toBe(0);
      expect(RELOAD_TABLE_COLUMNS_WIDTH.args).toBe(0);
      expect(RELOAD_TABLE_COLUMNS_WIDTH.updateTime).toBe(150);
      expect(RELOAD_TABLE_COLUMNS_WIDTH.createTime).toBe(150);
      expect(RELOAD_TABLE_COLUMNS_WIDTH.action).toBe(150);
    });
  });
});
