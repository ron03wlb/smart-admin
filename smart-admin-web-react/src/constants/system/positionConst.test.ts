/**
 * Position Constants Unit Tests
 * 職位管理常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect } from 'vitest';
import {
  POSITION_PERMISSION,
  POSITION_VALIDATION,
  POSITION_TABLE_COLUMNS_WIDTH,
} from './positionConst';

describe('positionConst', () => {
  // ==================== 權限點測試 ====================

  describe('POSITION_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(POSITION_PERMISSION).toHaveProperty('QUERY');
      expect(POSITION_PERMISSION).toHaveProperty('ADD');
      expect(POSITION_PERMISSION).toHaveProperty('UPDATE');
      expect(POSITION_PERMISSION).toHaveProperty('DELETE');
      expect(POSITION_PERMISSION).toHaveProperty('BATCH_DELETE');
    });

    it('should have correct permission values', () => {
      expect(POSITION_PERMISSION.QUERY).toBe('system:position:query');
      expect(POSITION_PERMISSION.ADD).toBe('system:position:add');
      expect(POSITION_PERMISSION.UPDATE).toBe('system:position:update');
      expect(POSITION_PERMISSION.DELETE).toBe('system:position:delete');
      expect(POSITION_PERMISSION.BATCH_DELETE).toBe('system:position:batchDelete');
    });
  });

  // ==================== 驗證規則測試 ====================

  describe('POSITION_VALIDATION', () => {
    it('should have correct name max length', () => {
      expect(POSITION_VALIDATION.NAME_MAX_LENGTH).toBe(30);
    });

    it('should have correct level max length', () => {
      expect(POSITION_VALIDATION.LEVEL_MAX_LENGTH).toBe(30);
    });

    it('should have correct remark max length', () => {
      expect(POSITION_VALIDATION.REMARK_MAX_LENGTH).toBe(200);
    });

    it('should be positive numbers', () => {
      expect(POSITION_VALIDATION.NAME_MAX_LENGTH).toBeGreaterThan(0);
      expect(POSITION_VALIDATION.LEVEL_MAX_LENGTH).toBeGreaterThan(0);
      expect(POSITION_VALIDATION.REMARK_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('POSITION_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'positionName',
        'positionLevel',
        'sort',
        'remark',
        'createTime',
        'updateTime',
        'operate',
      ];

      requiredColumns.forEach((column) => {
        expect(POSITION_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof POSITION_TABLE_COLUMNS_WIDTH[
            column as keyof typeof POSITION_TABLE_COLUMNS_WIDTH
          ]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(POSITION_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(500);
      });
    });

    it('should have specific width values', () => {
      expect(POSITION_TABLE_COLUMNS_WIDTH.positionName).toBe(200);
      expect(POSITION_TABLE_COLUMNS_WIDTH.positionLevel).toBe(150);
      expect(POSITION_TABLE_COLUMNS_WIDTH.sort).toBe(100);
      expect(POSITION_TABLE_COLUMNS_WIDTH.remark).toBe(300);
      expect(POSITION_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
      expect(POSITION_TABLE_COLUMNS_WIDTH.updateTime).toBe(180);
      expect(POSITION_TABLE_COLUMNS_WIDTH.operate).toBe(200);
    });
  });

  // ==================== 類型安全測試 ====================

  describe('Type Safety', () => {
    it('should have immutable permission constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(POSITION_PERMISSION.QUERY).toBe('system:position:query');
      expect(POSITION_PERMISSION.ADD).toBe('system:position:add');
      expect(POSITION_PERMISSION.UPDATE).toBe('system:position:update');
      expect(POSITION_PERMISSION.DELETE).toBe('system:position:delete');
      expect(POSITION_PERMISSION.BATCH_DELETE).toBe('system:position:batchDelete');
    });

    it('should have immutable validation constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(POSITION_VALIDATION.NAME_MAX_LENGTH).toBe(30);
      expect(POSITION_VALIDATION.LEVEL_MAX_LENGTH).toBe(30);
      expect(POSITION_VALIDATION.REMARK_MAX_LENGTH).toBe(200);
    });

    it('should have immutable table columns width constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(POSITION_TABLE_COLUMNS_WIDTH.positionName).toBe(200);
      expect(POSITION_TABLE_COLUMNS_WIDTH.operate).toBe(200);
    });
  });
});
