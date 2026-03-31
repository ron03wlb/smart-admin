/**
 * Menu Constants Unit Tests
 * 菜單管理常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect } from 'vitest';
import {
  MENU_PERMISSION,
  MENU_VALIDATION,
  MENU_TYPE_LABELS,
  MENU_TYPE_COLORS,
  PERMS_TYPE_LABELS,
  MENU_TABLE_COLUMNS_WIDTH,
  MENU_CONSTANTS,
  FLAG_LABELS,
} from './menuConst';
import { MenuTypeEnum, PermsTypeEnum } from '@/views/system/menu/types';

describe('menuConst', () => {
  // ==================== 權限點測試 ====================

  describe('MENU_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(MENU_PERMISSION).toHaveProperty('ADD');
      expect(MENU_PERMISSION).toHaveProperty('UPDATE');
      expect(MENU_PERMISSION).toHaveProperty('BATCH_DELETE');
    });

    it('should have correct permission values', () => {
      expect(MENU_PERMISSION.ADD).toBe('system:menu:add');
      expect(MENU_PERMISSION.UPDATE).toBe('system:menu:update');
      expect(MENU_PERMISSION.BATCH_DELETE).toBe('system:menu:batchDelete');
    });
  });

  // ==================== 驗證規則測試 ====================

  describe('MENU_VALIDATION', () => {
    it('should have correct validation max lengths', () => {
      expect(MENU_VALIDATION.NAME_MAX_LENGTH).toBe(20);
      expect(MENU_VALIDATION.PATH_MAX_LENGTH).toBe(100);
      expect(MENU_VALIDATION.FRAME_URL_MAX_LENGTH).toBe(500);
    });

    it('should have positive max lengths', () => {
      expect(MENU_VALIDATION.NAME_MAX_LENGTH).toBeGreaterThan(0);
      expect(MENU_VALIDATION.PATH_MAX_LENGTH).toBeGreaterThan(0);
      expect(MENU_VALIDATION.FRAME_URL_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== 菜單類型標籤測試 ====================

  describe('MENU_TYPE_LABELS', () => {
    it('should have correct menu type labels', () => {
      expect(MENU_TYPE_LABELS[MenuTypeEnum.CATALOG]).toBe('目錄');
      expect(MENU_TYPE_LABELS[MenuTypeEnum.MENU]).toBe('菜單');
      expect(MENU_TYPE_LABELS[MenuTypeEnum.POINTS]).toBe('功能點');
    });

    it('should have all menu types', () => {
      expect(Object.keys(MENU_TYPE_LABELS)).toHaveLength(3);
    });
  });

  // ==================== 菜單類型顏色測試 ====================

  describe('MENU_TYPE_COLORS', () => {
    it('should have correct menu type colors', () => {
      expect(MENU_TYPE_COLORS[MenuTypeEnum.CATALOG]).toBe('red');
      expect(MENU_TYPE_COLORS[MenuTypeEnum.MENU]).toBe('blue');
      expect(MENU_TYPE_COLORS[MenuTypeEnum.POINTS]).toBe('orange');
    });

    it('should have all menu type colors', () => {
      expect(Object.keys(MENU_TYPE_COLORS)).toHaveLength(3);
    });
  });

  // ==================== 權限類型標籤測試 ====================

  describe('PERMS_TYPE_LABELS', () => {
    it('should have correct permission type labels', () => {
      expect(PERMS_TYPE_LABELS[PermsTypeEnum.SA_TOKEN]).toBe('Sa-Token');
      expect(PERMS_TYPE_LABELS[PermsTypeEnum.SPRING_SECURITY]).toBe('Spring Security');
    });

    it('should have all permission types', () => {
      expect(Object.keys(PERMS_TYPE_LABELS)).toHaveLength(2);
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('MENU_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'menuName',
        'menuType',
        'icon',
        'path',
        'component',
        'frameFlag',
        'permsType',
        'webPerms',
        'apiPerms',
        'cacheFlag',
        'visibleFlag',
        'disabledFlag',
        'sort',
        'operate',
      ];

      requiredColumns.forEach(column => {
        expect(MENU_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof MENU_TABLE_COLUMNS_WIDTH[column as keyof typeof MENU_TABLE_COLUMNS_WIDTH]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(MENU_TABLE_COLUMNS_WIDTH).forEach(width => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(300);
      });
    });

    it('should have specific width values', () => {
      expect(MENU_TABLE_COLUMNS_WIDTH.menuName).toBe(200);
      expect(MENU_TABLE_COLUMNS_WIDTH.menuType).toBe(90);
      expect(MENU_TABLE_COLUMNS_WIDTH.icon).toBe(80);
      expect(MENU_TABLE_COLUMNS_WIDTH.path).toBe(150);
      expect(MENU_TABLE_COLUMNS_WIDTH.component).toBe(250);
      expect(MENU_TABLE_COLUMNS_WIDTH.operate).toBe(200);
    });
  });

  // ==================== 菜單常量測試 ====================

  describe('MENU_CONSTANTS', () => {
    it('should have TOP_PARENT_ID', () => {
      expect(MENU_CONSTANTS).toHaveProperty('TOP_PARENT_ID');
    });

    it('should have TOP_PARENT_ID equal to 0', () => {
      expect(MENU_CONSTANTS.TOP_PARENT_ID).toBe(0);
    });

    it('should be a non-negative number', () => {
      expect(MENU_CONSTANTS.TOP_PARENT_ID).toBeGreaterThanOrEqual(0);
    });
  });

  // ==================== Flag 標籤測試 ====================

  describe('FLAG_LABELS', () => {
    it('should have correct flag labels', () => {
      expect(FLAG_LABELS['true']).toBe('是');
      expect(FLAG_LABELS['false']).toBe('否');
    });

    it('should only have two flag options', () => {
      expect(Object.keys(FLAG_LABELS)).toHaveLength(2);
    });
  });

  // ==================== 類型枚舉值測試 ====================

  describe('Enum Values', () => {
    it('should have correct MenuTypeEnum values', () => {
      expect(MenuTypeEnum.CATALOG).toBe(1);
      expect(MenuTypeEnum.MENU).toBe(2);
      expect(MenuTypeEnum.POINTS).toBe(3);
    });

    it('should have correct PermsTypeEnum values', () => {
      expect(PermsTypeEnum.SA_TOKEN).toBe(1);
      expect(PermsTypeEnum.SPRING_SECURITY).toBe(2);
    });
  });
});
