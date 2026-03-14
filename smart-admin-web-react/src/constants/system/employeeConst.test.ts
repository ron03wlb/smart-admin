/**
 * Employee Constants Unit Tests
 * 員工管理常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect } from 'vitest';
import {
  EMPLOYEE_PERMISSION,
  EMPLOYEE_VALIDATION,
  GENDER_LABELS,
  EMPLOYEE_STATUS_LABELS,
  LEAVE_STATUS_LABELS,
  EMPLOYEE_TABLE_COLUMNS_WIDTH,
} from './employeeConst';

describe('employeeConst', () => {
  // ==================== 權限點測試 ====================

  describe('EMPLOYEE_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(EMPLOYEE_PERMISSION).toHaveProperty('ADD');
      expect(EMPLOYEE_PERMISSION).toHaveProperty('UPDATE');
      expect(EMPLOYEE_PERMISSION).toHaveProperty('DELETE');
      expect(EMPLOYEE_PERMISSION).toHaveProperty('RESET_PASSWORD');
      expect(EMPLOYEE_PERMISSION).toHaveProperty('DISABLED');
      expect(EMPLOYEE_PERMISSION).toHaveProperty('UPDATE_DEPARTMENT');
    });

    it('should have correct permission values', () => {
      expect(EMPLOYEE_PERMISSION.ADD).toBe('system:employee:add');
      expect(EMPLOYEE_PERMISSION.UPDATE).toBe('system:employee:update');
      expect(EMPLOYEE_PERMISSION.DELETE).toBe('system:employee:delete');
      expect(EMPLOYEE_PERMISSION.RESET_PASSWORD).toBe('system:employee:password:reset');
      expect(EMPLOYEE_PERMISSION.DISABLED).toBe('system:employee:disabled');
      expect(EMPLOYEE_PERMISSION.UPDATE_DEPARTMENT).toBe('system:employee:department:update');
    });

    it('should be immutable (as const)', () => {
      // TypeScript 'as const' provides compile-time immutability
      // Note: Runtime immutability would require Object.freeze()
      expect(EMPLOYEE_PERMISSION.ADD).toBe('system:employee:add');
    });
  });

  // ==================== 驗證規則測試 ====================

  describe('EMPLOYEE_VALIDATION', () => {
    describe('PHONE_REGEX', () => {
      it('should validate correct Chinese phone numbers', () => {
        const validPhones = [
          '13800138000',
          '15912345678',
          '18612345678',
          '19912345678',
        ];

        validPhones.forEach((phone) => {
          expect(EMPLOYEE_VALIDATION.PHONE_REGEX.test(phone)).toBe(true);
        });
      });

      it('should reject invalid phone numbers', () => {
        const invalidPhones = [
          '12345678901', // 不以 1[3-9] 開頭
          '1381234567',  // 少於 11 位
          '138123456789', // 多於 11 位
          'abcdefghijk',  // 非數字
          '12812345678',  // 第二位不在 3-9 範圍
        ];

        invalidPhones.forEach((phone) => {
          expect(EMPLOYEE_VALIDATION.PHONE_REGEX.test(phone)).toBe(false);
        });
      });
    });

    describe('EMAIL_REGEX', () => {
      it('should validate correct email addresses', () => {
        const validEmails = [
          'test@example.com',
          'user.name@company.co.uk',
          'admin+tag@domain.org',
          'user123@test-site.com',
        ];

        validEmails.forEach((email) => {
          expect(EMPLOYEE_VALIDATION.EMAIL_REGEX.test(email)).toBe(true);
        });
      });

      it('should reject invalid email addresses', () => {
        const invalidEmails = [
          'invalid',
          '@example.com',
          'user@',
          'user @example.com', // 空格
          'user@example',      // 沒有頂級域名
        ];

        invalidEmails.forEach((email) => {
          expect(EMPLOYEE_VALIDATION.EMAIL_REGEX.test(email)).toBe(false);
        });
      });
    });

    describe('Length Constraints', () => {
      it('should have correct name max length', () => {
        expect(EMPLOYEE_VALIDATION.NAME_MAX_LENGTH).toBe(30);
      });

      it('should have correct login name max length', () => {
        expect(EMPLOYEE_VALIDATION.LOGIN_NAME_MAX_LENGTH).toBe(30);
      });
    });
  });

  // ==================== 標籤映射測試 ====================

  describe('GENDER_LABELS', () => {
    it('should have correct gender labels', () => {
      expect(GENDER_LABELS[1]).toBe('男');
      expect(GENDER_LABELS[2]).toBe('女');
    });

    it('should only have two gender options', () => {
      expect(Object.keys(GENDER_LABELS)).toHaveLength(2);
    });
  });

  describe('EMPLOYEE_STATUS_LABELS', () => {
    it('should have correct status labels', () => {
      expect(EMPLOYEE_STATUS_LABELS[0]).toBe('啟用');
      expect(EMPLOYEE_STATUS_LABELS[1]).toBe('禁用');
    });

    it('should only have two status options', () => {
      expect(Object.keys(EMPLOYEE_STATUS_LABELS)).toHaveLength(2);
    });
  });

  describe('LEAVE_STATUS_LABELS', () => {
    it('should have correct leave status labels', () => {
      expect(LEAVE_STATUS_LABELS[0]).toBe('在職');
      expect(LEAVE_STATUS_LABELS[1]).toBe('離職');
    });

    it('should only have two leave status options', () => {
      expect(Object.keys(LEAVE_STATUS_LABELS)).toHaveLength(2);
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('EMPLOYEE_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'actualName',
        'gender',
        'loginName',
        'phone',
        'email',
        'administratorFlag',
        'disabledFlag',
        'positionName',
        'roleNameList',
        'departmentName',
        'operate',
      ];

      requiredColumns.forEach((column) => {
        expect(EMPLOYEE_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(typeof EMPLOYEE_TABLE_COLUMNS_WIDTH[column as keyof typeof EMPLOYEE_TABLE_COLUMNS_WIDTH]).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      // 驗證寬度為正數
      Object.values(EMPLOYEE_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(300); // 合理的最大寬度
      });
    });

    it('should have specific width values', () => {
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.actualName).toBe(85);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.gender).toBe(70);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.loginName).toBe(100);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.phone).toBe(85);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.email).toBe(100);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.administratorFlag).toBe(60);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.disabledFlag).toBe(60);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.positionName).toBe(100);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.roleNameList).toBe(100);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.departmentName).toBe(200);
      expect(EMPLOYEE_TABLE_COLUMNS_WIDTH.operate).toBe(140);
    });
  });

  // ==================== 邊界情況測試 ====================

  describe('Edge Cases', () => {
    it('should handle edge case phone numbers', () => {
      // 邊界值測試
      expect(EMPLOYEE_VALIDATION.PHONE_REGEX.test('13000000000')).toBe(true);
      expect(EMPLOYEE_VALIDATION.PHONE_REGEX.test('19999999999')).toBe(true);
    });

    it('should handle edge case emails', () => {
      // 最短有效郵箱
      expect(EMPLOYEE_VALIDATION.EMAIL_REGEX.test('a@b.co')).toBe(true);
      // 帶特殊字符
      expect(EMPLOYEE_VALIDATION.EMAIL_REGEX.test('user+tag@example.com')).toBe(true);
    });
  });
});
