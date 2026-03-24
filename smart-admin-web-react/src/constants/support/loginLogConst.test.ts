/**
 * Login Log Constants Unit Tests
 * 登錄日誌常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect } from 'vitest';
import {
  LOGIN_LOG_PERMISSION,
  LOGIN_RESULT_ENUM,
  LOGIN_LOG_TABLE_COLUMNS_WIDTH,
} from './loginLogConst';

describe('loginLogConst', () => {
  // ==================== 權限點測試 ====================

  describe('LOGIN_LOG_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(LOGIN_LOG_PERMISSION).toHaveProperty('QUERY');
    });

    it('should have correct permission values', () => {
      expect(LOGIN_LOG_PERMISSION.QUERY).toBe('support:loginLog:query');
    });
  });

  // ==================== 登錄結果枚舉測試 ====================

  describe('LOGIN_RESULT_ENUM', () => {
    it('should have all login result types', () => {
      expect(LOGIN_RESULT_ENUM).toHaveProperty('LOGIN_SUCCESS');
      expect(LOGIN_RESULT_ENUM).toHaveProperty('LOGIN_FAIL');
      expect(LOGIN_RESULT_ENUM).toHaveProperty('LOGIN_OUT');
    });

    it('should have correct LOGIN_SUCCESS values', () => {
      expect(LOGIN_RESULT_ENUM.LOGIN_SUCCESS.value).toBe(0);
      expect(LOGIN_RESULT_ENUM.LOGIN_SUCCESS.label).toBe('登錄成功');
      expect(LOGIN_RESULT_ENUM.LOGIN_SUCCESS.color).toBe('success');
    });

    it('should have correct LOGIN_FAIL values', () => {
      expect(LOGIN_RESULT_ENUM.LOGIN_FAIL.value).toBe(1);
      expect(LOGIN_RESULT_ENUM.LOGIN_FAIL.label).toBe('登錄失敗');
      expect(LOGIN_RESULT_ENUM.LOGIN_FAIL.color).toBe('error');
    });

    it('should have correct LOGIN_OUT values', () => {
      expect(LOGIN_RESULT_ENUM.LOGIN_OUT.value).toBe(2);
      expect(LOGIN_RESULT_ENUM.LOGIN_OUT.label).toBe('退出登錄');
      expect(LOGIN_RESULT_ENUM.LOGIN_OUT.color).toBe('processing');
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('LOGIN_LOG_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'userId',
        'userName',
        'userType',
        'loginIp',
        'loginIpRegion',
        'userAgent',
        'loginResult',
        'remark',
        'createTime',
      ];

      requiredColumns.forEach(column => {
        expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof LOGIN_LOG_TABLE_COLUMNS_WIDTH[column as keyof typeof LOGIN_LOG_TABLE_COLUMNS_WIDTH]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(LOGIN_LOG_TABLE_COLUMNS_WIDTH).forEach(width => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(500);
      });
    });

    it('should have specific width values', () => {
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.userId).toBe(70);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.userName).toBe(120);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.userType).toBe(80);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.loginIp).toBe(120);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.loginIpRegion).toBe(150);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.userAgent).toBe(200);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.loginResult).toBe(100);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.remark).toBe(150);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
    });
  });

  // ==================== 類型安全測試 ====================

  describe('Type Safety', () => {
    it('should have immutable permission constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(LOGIN_LOG_PERMISSION.QUERY).toBe('support:loginLog:query');
    });

    it('should have immutable enum constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(LOGIN_RESULT_ENUM.LOGIN_SUCCESS.value).toBe(0);
      expect(LOGIN_RESULT_ENUM.LOGIN_FAIL.value).toBe(1);
      expect(LOGIN_RESULT_ENUM.LOGIN_OUT.value).toBe(2);
    });

    it('should have immutable table columns width constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.userId).toBe(70);
      expect(LOGIN_LOG_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
    });
  });
});
