/**
 * Department Constants Unit Tests
 * 部門管理常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect } from 'vitest';
import {
  DEPARTMENT_PERMISSION,
  DEPARTMENT_VALIDATION,
  DEPARTMENT_TABLE_COLUMNS_WIDTH,
  DEPARTMENT_CONSTANTS,
} from './departmentConst';

describe('departmentConst', () => {
  // ==================== 權限點測試 ====================

  describe('DEPARTMENT_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(DEPARTMENT_PERMISSION).toHaveProperty('QUERY');
      expect(DEPARTMENT_PERMISSION).toHaveProperty('ADD');
      expect(DEPARTMENT_PERMISSION).toHaveProperty('UPDATE');
      expect(DEPARTMENT_PERMISSION).toHaveProperty('DELETE');
    });

    it('should have correct permission values', () => {
      expect(DEPARTMENT_PERMISSION.QUERY).toBe('support:department:query');
      expect(DEPARTMENT_PERMISSION.ADD).toBe('system:department:add');
      expect(DEPARTMENT_PERMISSION.UPDATE).toBe('system:department:update');
      expect(DEPARTMENT_PERMISSION.DELETE).toBe('system:department:delete');
    });
  });

  // ==================== 驗證規則測試 ====================

  describe('DEPARTMENT_VALIDATION', () => {
    it('should have correct name max length', () => {
      expect(DEPARTMENT_VALIDATION.NAME_MAX_LENGTH).toBe(50);
    });

    it('should be a positive number', () => {
      expect(DEPARTMENT_VALIDATION.NAME_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('DEPARTMENT_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'departmentName',
        'managerName',
        'sort',
        'createTime',
        'updateTime',
        'operate',
      ];

      requiredColumns.forEach((column) => {
        expect(DEPARTMENT_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof DEPARTMENT_TABLE_COLUMNS_WIDTH[
            column as keyof typeof DEPARTMENT_TABLE_COLUMNS_WIDTH
          ]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(DEPARTMENT_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(500);
      });
    });

    it('should have specific width values', () => {
      expect(DEPARTMENT_TABLE_COLUMNS_WIDTH.departmentName).toBe(300);
      expect(DEPARTMENT_TABLE_COLUMNS_WIDTH.managerName).toBe(120);
      expect(DEPARTMENT_TABLE_COLUMNS_WIDTH.sort).toBe(100);
      expect(DEPARTMENT_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
      expect(DEPARTMENT_TABLE_COLUMNS_WIDTH.updateTime).toBe(180);
      expect(DEPARTMENT_TABLE_COLUMNS_WIDTH.operate).toBe(240);
    });
  });

  // ==================== 部門常量測試 ====================

  describe('DEPARTMENT_CONSTANTS', () => {
    it('should have TOP_PARENT_ID', () => {
      expect(DEPARTMENT_CONSTANTS).toHaveProperty('TOP_PARENT_ID');
    });

    it('should have TOP_PARENT_ID equal to 0', () => {
      expect(DEPARTMENT_CONSTANTS.TOP_PARENT_ID).toBe(0);
    });

    it('should be a non-negative number', () => {
      expect(DEPARTMENT_CONSTANTS.TOP_PARENT_ID).toBeGreaterThanOrEqual(0);
    });
  });

  // ==================== 類型安全測試 ====================

  describe('Type Safety', () => {
    it('should have immutable permission constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      // 注意：運行時不可變性需要 Object.freeze()
      expect(DEPARTMENT_PERMISSION.ADD).toBe('system:department:add');
      expect(DEPARTMENT_PERMISSION.QUERY).toBe('support:department:query');
      expect(DEPARTMENT_PERMISSION.UPDATE).toBe('system:department:update');
      expect(DEPARTMENT_PERMISSION.DELETE).toBe('system:department:delete');
    });

    it('should have immutable validation constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(DEPARTMENT_VALIDATION.NAME_MAX_LENGTH).toBe(50);
    });
  });
});
