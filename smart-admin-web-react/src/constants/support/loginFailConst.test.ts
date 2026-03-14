/**
 * Login Fail Constants Unit Tests
 * 登錄失敗常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect } from 'vitest';
import {
  LOGIN_FAIL_PERMISSION,
  LOCK_FLAG_ENUM,
  LOGIN_FAIL_TABLE_COLUMNS_WIDTH,
} from './loginFailConst';

describe('loginFailConst', () => {
  // ==================== 權限點測試 ====================

  describe('LOGIN_FAIL_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(LOGIN_FAIL_PERMISSION).toHaveProperty('QUERY');
    });

    it('should have correct permission values', () => {
      expect(LOGIN_FAIL_PERMISSION.QUERY).toBe('support:loginFail:query');
    });
  });

  // ==================== 鎖定狀態枚舉測試 ====================

  describe('LOCK_FLAG_ENUM', () => {
    it('should have all lock flag types', () => {
      expect(LOCK_FLAG_ENUM).toHaveProperty('UNLOCKED');
      expect(LOCK_FLAG_ENUM).toHaveProperty('LOCKED');
    });

    it('should have correct UNLOCKED values', () => {
      expect(LOCK_FLAG_ENUM.UNLOCKED.value).toBe(0);
      expect(LOCK_FLAG_ENUM.UNLOCKED.label).toBe('未鎖定');
      expect(LOCK_FLAG_ENUM.UNLOCKED.color).toBe('success');
    });

    it('should have correct LOCKED values', () => {
      expect(LOCK_FLAG_ENUM.LOCKED.value).toBe(1);
      expect(LOCK_FLAG_ENUM.LOCKED.label).toBe('已鎖定');
      expect(LOCK_FLAG_ENUM.LOCKED.color).toBe('error');
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('LOGIN_FAIL_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'loginName',
        'userType',
        'loginFailCount',
        'lockFlag',
        'loginLockBeginTime',
        'createTime',
        'updateTime',
      ];

      requiredColumns.forEach((column) => {
        expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof LOGIN_FAIL_TABLE_COLUMNS_WIDTH[
            column as keyof typeof LOGIN_FAIL_TABLE_COLUMNS_WIDTH
          ]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(LOGIN_FAIL_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(500);
      });
    });

    it('should have specific width values', () => {
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.loginName).toBe(150);
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.userType).toBe(100);
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.loginFailCount).toBe(120);
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.lockFlag).toBe(100);
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.loginLockBeginTime).toBe(180);
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.updateTime).toBe(180);
    });
  });

  // ==================== 類型安全測試 ====================

  describe('Type Safety', () => {
    it('should have immutable permission constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(LOGIN_FAIL_PERMISSION.QUERY).toBe('support:loginFail:query');
    });

    it('should have immutable enum constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(LOCK_FLAG_ENUM.UNLOCKED.value).toBe(0);
      expect(LOCK_FLAG_ENUM.LOCKED.value).toBe(1);
    });

    it('should have immutable table columns width constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.loginName).toBe(150);
      expect(LOGIN_FAIL_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
    });
  });
});
