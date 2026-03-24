/**
 * Role Constants Unit Tests
 * 角色常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect } from 'vitest';
import { ROLE_PERMISSION, ROLE_VALIDATION, ROLE_TABLE_COLUMNS_WIDTH } from './roleConst';

describe('roleConst', () => {
  // ==================== ROLE_PERMISSION Tests ====================

  describe('ROLE_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(ROLE_PERMISSION).toHaveProperty('ADD');
      expect(ROLE_PERMISSION).toHaveProperty('UPDATE');
      expect(ROLE_PERMISSION).toHaveProperty('DELETE');
      expect(ROLE_PERMISSION).toHaveProperty('QUERY');
    });

    it('should have correct permission values', () => {
      expect(ROLE_PERMISSION.ADD).toBe('system:role:add');
      expect(ROLE_PERMISSION.UPDATE).toBe('system:role:update');
      expect(ROLE_PERMISSION.DELETE).toBe('system:role:delete');
      expect(ROLE_PERMISSION.QUERY).toBe('system:role:query');
    });

    it('should be a const object (readonly)', () => {
      expect(() => {
        (ROLE_PERMISSION as any).ADD = 'modified';
      }).not.toThrow();
    });
  });

  // ==================== ROLE_VALIDATION Tests ====================

  describe('ROLE_VALIDATION', () => {
    it('should have all required validation keys', () => {
      expect(ROLE_VALIDATION).toHaveProperty('NAME_MAX_LENGTH');
      expect(ROLE_VALIDATION).toHaveProperty('CODE_MAX_LENGTH');
      expect(ROLE_VALIDATION).toHaveProperty('REMARK_MAX_LENGTH');
    });

    it('should have correct validation values', () => {
      expect(ROLE_VALIDATION.NAME_MAX_LENGTH).toBe(20);
      expect(ROLE_VALIDATION.CODE_MAX_LENGTH).toBe(50);
      expect(ROLE_VALIDATION.REMARK_MAX_LENGTH).toBe(200);
    });

    it('should have all values as positive numbers', () => {
      expect(ROLE_VALIDATION.NAME_MAX_LENGTH).toBeGreaterThan(0);
      expect(ROLE_VALIDATION.CODE_MAX_LENGTH).toBeGreaterThan(0);
      expect(ROLE_VALIDATION.REMARK_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== ROLE_TABLE_COLUMNS_WIDTH Tests ====================

  describe('ROLE_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width keys', () => {
      expect(ROLE_TABLE_COLUMNS_WIDTH).toHaveProperty('roleName');
      expect(ROLE_TABLE_COLUMNS_WIDTH).toHaveProperty('roleCode');
      expect(ROLE_TABLE_COLUMNS_WIDTH).toHaveProperty('remark');
      expect(ROLE_TABLE_COLUMNS_WIDTH).toHaveProperty('createTime');
      expect(ROLE_TABLE_COLUMNS_WIDTH).toHaveProperty('updateTime');
      expect(ROLE_TABLE_COLUMNS_WIDTH).toHaveProperty('operate');
    });

    it('should have correct column widths', () => {
      expect(ROLE_TABLE_COLUMNS_WIDTH.roleName).toBe(200);
      expect(ROLE_TABLE_COLUMNS_WIDTH.roleCode).toBe(200);
      expect(ROLE_TABLE_COLUMNS_WIDTH.remark).toBe(250);
      expect(ROLE_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
      expect(ROLE_TABLE_COLUMNS_WIDTH.updateTime).toBe(180);
      expect(ROLE_TABLE_COLUMNS_WIDTH.operate).toBe(200);
    });

    it('should have all values as positive numbers', () => {
      expect(ROLE_TABLE_COLUMNS_WIDTH.roleName).toBeGreaterThan(0);
      expect(ROLE_TABLE_COLUMNS_WIDTH.roleCode).toBeGreaterThan(0);
      expect(ROLE_TABLE_COLUMNS_WIDTH.remark).toBeGreaterThan(0);
      expect(ROLE_TABLE_COLUMNS_WIDTH.createTime).toBeGreaterThan(0);
      expect(ROLE_TABLE_COLUMNS_WIDTH.updateTime).toBeGreaterThan(0);
      expect(ROLE_TABLE_COLUMNS_WIDTH.operate).toBeGreaterThan(0);
    });
  });
});
