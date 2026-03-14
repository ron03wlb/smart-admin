/**
 * Config Constants Unit Tests
 * 配置管理常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect } from 'vitest';
import {
  CONFIG_PERMISSION,
  CONFIG_VALIDATION,
  CONFIG_TABLE_COLUMNS_WIDTH,
} from './configConst';

describe('configConst', () => {
  // ==================== 權限點測試 ====================

  describe('CONFIG_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(CONFIG_PERMISSION).toHaveProperty('QUERY');
      expect(CONFIG_PERMISSION).toHaveProperty('ADD');
      expect(CONFIG_PERMISSION).toHaveProperty('UPDATE');
    });

    it('should have correct permission values', () => {
      expect(CONFIG_PERMISSION.QUERY).toBe('support:config:query');
      expect(CONFIG_PERMISSION.ADD).toBe('support:config:add');
      expect(CONFIG_PERMISSION.UPDATE).toBe('support:config:update');
    });
  });

  // ==================== 驗證規則測試 ====================

  describe('CONFIG_VALIDATION', () => {
    it('should have correct key max length', () => {
      expect(CONFIG_VALIDATION.KEY_MAX_LENGTH).toBe(50);
    });

    it('should have correct name max length', () => {
      expect(CONFIG_VALIDATION.NAME_MAX_LENGTH).toBe(100);
    });

    it('should have correct value max length', () => {
      expect(CONFIG_VALIDATION.VALUE_MAX_LENGTH).toBe(500);
    });

    it('should have correct remark max length', () => {
      expect(CONFIG_VALIDATION.REMARK_MAX_LENGTH).toBe(500);
    });

    it('should be positive numbers', () => {
      expect(CONFIG_VALIDATION.KEY_MAX_LENGTH).toBeGreaterThan(0);
      expect(CONFIG_VALIDATION.NAME_MAX_LENGTH).toBeGreaterThan(0);
      expect(CONFIG_VALIDATION.VALUE_MAX_LENGTH).toBeGreaterThan(0);
      expect(CONFIG_VALIDATION.REMARK_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('CONFIG_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'configId',
        'configKey',
        'configName',
        'configValue',
        'remark',
        'createTime',
        'updateTime',
        'operate',
      ];

      requiredColumns.forEach((column) => {
        expect(CONFIG_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof CONFIG_TABLE_COLUMNS_WIDTH[
            column as keyof typeof CONFIG_TABLE_COLUMNS_WIDTH
          ]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(CONFIG_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(500);
      });
    });

    it('should have specific width values', () => {
      expect(CONFIG_TABLE_COLUMNS_WIDTH.configId).toBe(80);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.configKey).toBe(200);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.configName).toBe(200);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.configValue).toBe(250);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.remark).toBe(200);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.updateTime).toBe(180);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.operate).toBe(100);
    });
  });

  // ==================== 類型安全測試 ====================

  describe('Type Safety', () => {
    it('should have immutable permission constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(CONFIG_PERMISSION.QUERY).toBe('support:config:query');
      expect(CONFIG_PERMISSION.ADD).toBe('support:config:add');
      expect(CONFIG_PERMISSION.UPDATE).toBe('support:config:update');
    });

    it('should have immutable validation constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(CONFIG_VALIDATION.KEY_MAX_LENGTH).toBe(50);
      expect(CONFIG_VALIDATION.NAME_MAX_LENGTH).toBe(100);
      expect(CONFIG_VALIDATION.VALUE_MAX_LENGTH).toBe(500);
      expect(CONFIG_VALIDATION.REMARK_MAX_LENGTH).toBe(500);
    });

    it('should have immutable table columns width constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(CONFIG_TABLE_COLUMNS_WIDTH.configId).toBe(80);
      expect(CONFIG_TABLE_COLUMNS_WIDTH.operate).toBe(100);
    });
  });
});
