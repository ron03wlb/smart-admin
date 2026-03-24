/**
 * Operate Log Constants Unit Tests
 * 操作日誌常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect } from 'vitest';
import {
  OPERATE_LOG_PERMISSION,
  SUCCESS_FLAG_ENUM,
  OPERATE_LOG_TABLE_COLUMNS_WIDTH,
} from './operateLogConst';

describe('operateLogConst', () => {
  // ==================== 權限點測試 ====================

  describe('OPERATE_LOG_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(OPERATE_LOG_PERMISSION).toHaveProperty('QUERY');
      expect(OPERATE_LOG_PERMISSION).toHaveProperty('DETAIL');
    });

    it('should have correct permission values', () => {
      expect(OPERATE_LOG_PERMISSION.QUERY).toBe('support:operateLog:query');
      expect(OPERATE_LOG_PERMISSION.DETAIL).toBe('support:operateLog:detail');
    });
  });

  // ==================== 請求結果狀態枚舉測試 ====================

  describe('SUCCESS_FLAG_ENUM', () => {
    it('should have all success flag types', () => {
      expect(SUCCESS_FLAG_ENUM).toHaveProperty('FAILURE');
      expect(SUCCESS_FLAG_ENUM).toHaveProperty('SUCCESS');
    });

    it('should have correct FAILURE values', () => {
      expect(SUCCESS_FLAG_ENUM.FAILURE.value).toBe(0);
      expect(SUCCESS_FLAG_ENUM.FAILURE.label).toBe('失敗');
      expect(SUCCESS_FLAG_ENUM.FAILURE.color).toBe('error');
    });

    it('should have correct SUCCESS values', () => {
      expect(SUCCESS_FLAG_ENUM.SUCCESS.value).toBe(1);
      expect(SUCCESS_FLAG_ENUM.SUCCESS.label).toBe('成功');
      expect(SUCCESS_FLAG_ENUM.SUCCESS.color).toBe('success');
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('OPERATE_LOG_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'operateUserName',
        'operateUserType',
        'module',
        'content',
        'url',
        'response',
        'ipRegion',
        'userAgent',
        'createTime',
        'successFlag',
        'action',
      ];

      requiredColumns.forEach(column => {
        expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof OPERATE_LOG_TABLE_COLUMNS_WIDTH[
            column as keyof typeof OPERATE_LOG_TABLE_COLUMNS_WIDTH
          ]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(OPERATE_LOG_TABLE_COLUMNS_WIDTH).forEach(width => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(500);
      });
    });

    it('should have specific width values', () => {
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.operateUserName).toBe(70);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.operateUserType).toBe(50);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.module).toBe(120);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.content).toBe(150);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.url).toBe(200);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.response).toBe(150);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.ipRegion).toBe(150);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.userAgent).toBe(150);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.createTime).toBe(150);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.successFlag).toBe(60);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.action).toBe(60);
    });
  });

  // ==================== 類型安全測試 ====================

  describe('Type Safety', () => {
    it('should have immutable permission constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(OPERATE_LOG_PERMISSION.QUERY).toBe('support:operateLog:query');
      expect(OPERATE_LOG_PERMISSION.DETAIL).toBe('support:operateLog:detail');
    });

    it('should have immutable enum constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(SUCCESS_FLAG_ENUM.FAILURE.value).toBe(0);
      expect(SUCCESS_FLAG_ENUM.SUCCESS.value).toBe(1);
    });

    it('should have immutable table columns width constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.operateUserName).toBe(70);
      expect(OPERATE_LOG_TABLE_COLUMNS_WIDTH.createTime).toBe(150);
    });
  });
});
