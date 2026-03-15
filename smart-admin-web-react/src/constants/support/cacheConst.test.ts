/**
 * Cache Constants Test
 * 緩存常量定義測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect } from 'vitest';
import { CACHE_PERMISSION, CACHE_TABLE_COLUMNS_WIDTH } from './cacheConst';

describe('cacheConst', () => {
  describe('CACHE_PERMISSION', () => {
    it('應該定義正確的權限點', () => {
      expect(CACHE_PERMISSION.DELETE).toBe('support:cache:delete');
      expect(CACHE_PERMISSION.KEYS).toBe('support:cache:keys');
    });
  });

  describe('CACHE_TABLE_COLUMNS_WIDTH', () => {
    it('應該定義表格列寬度', () => {
      expect(CACHE_TABLE_COLUMNS_WIDTH.key).toBe(0);
      expect(CACHE_TABLE_COLUMNS_WIDTH.action).toBe(160);
    });
  });
});
