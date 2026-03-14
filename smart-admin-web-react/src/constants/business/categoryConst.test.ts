/**
 * Category Constants Test
 * 分類常量測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect } from 'vitest';
import {
  CATEGORY_PERMISSION,
  CATEGORY_VALIDATION,
  CATEGORY_TYPE_LABELS,
  DISABLED_STATUS_LABELS,
  CATEGORY_TABLE_COLUMNS_WIDTH,
} from './categoryConst';

describe('categoryConst', () => {
  describe('CATEGORY_PERMISSION', () => {
    it('should have correct permission codes', () => {
      expect(CATEGORY_PERMISSION.ADD).toBe('business:category:add');
      expect(CATEGORY_PERMISSION.ADD_CHILD).toBe('business:category:addChild');
      expect(CATEGORY_PERMISSION.UPDATE).toBe('business:category:update');
      expect(CATEGORY_PERMISSION.DELETE).toBe('business:category:delete');
    });
  });

  describe('CATEGORY_VALIDATION', () => {
    it('should have correct validation rules', () => {
      expect(CATEGORY_VALIDATION.NAME_MAX_LENGTH).toBe(30);
      expect(CATEGORY_VALIDATION.REMARK_MAX_LENGTH).toBe(200);
    });
  });

  describe('CATEGORY_TYPE_LABELS', () => {
    it('should have correct category type labels', () => {
      expect(CATEGORY_TYPE_LABELS[1]).toBe('商品分類');
      expect(CATEGORY_TYPE_LABELS[2]).toBe('演示分類');
    });
  });

  describe('DISABLED_STATUS_LABELS', () => {
    it('should have correct disabled status labels', () => {
      expect(DISABLED_STATUS_LABELS.false).toBe('啟用');
      expect(DISABLED_STATUS_LABELS.true).toBe('禁用');
    });
  });

  describe('CATEGORY_TABLE_COLUMNS_WIDTH', () => {
    it('should have correct column widths', () => {
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.categoryName).toBe(200);
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.categoryType).toBe(100);
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.sort).toBe(80);
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.disabledFlag).toBe(80);
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.remark).toBe(200);
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.createTime).toBe(160);
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.updateTime).toBe(160);
      expect(CATEGORY_TABLE_COLUMNS_WIDTH.action).toBe(220);
    });
  });
});
