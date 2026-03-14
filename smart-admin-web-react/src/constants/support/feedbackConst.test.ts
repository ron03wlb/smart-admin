/**
 * Feedback Constants Unit Tests
 * 意見反饋常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect } from 'vitest';
import {
  FEEDBACK_PERMISSION,
  FEEDBACK_VALIDATION,
  FEEDBACK_TABLE_COLUMNS_WIDTH,
} from './feedbackConst';

describe('feedbackConst', () => {
  // ==================== 權限點測試 ====================

  describe('FEEDBACK_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(FEEDBACK_PERMISSION).toHaveProperty('QUERY');
    });

    it('should have correct permission values', () => {
      expect(FEEDBACK_PERMISSION.QUERY).toBe('support:feedback:query');
    });
  });

  // ==================== 驗證規則測試 ====================

  describe('FEEDBACK_VALIDATION', () => {
    it('should have correct search word max length', () => {
      expect(FEEDBACK_VALIDATION.SEARCH_WORD_MAX_LENGTH).toBe(25);
    });

    it('should have correct content max length', () => {
      expect(FEEDBACK_VALIDATION.CONTENT_MAX_LENGTH).toBe(500);
    });

    it('should be positive numbers', () => {
      expect(FEEDBACK_VALIDATION.SEARCH_WORD_MAX_LENGTH).toBeGreaterThan(0);
      expect(FEEDBACK_VALIDATION.CONTENT_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== 表格列寬度配置測試 ====================

  describe('FEEDBACK_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width definitions', () => {
      const requiredColumns = [
        'feedbackId',
        'feedbackContent',
        'feedbackAttachment',
        'userName',
        'userType',
        'createTime',
      ];

      requiredColumns.forEach((column) => {
        expect(FEEDBACK_TABLE_COLUMNS_WIDTH).toHaveProperty(column);
        expect(
          typeof FEEDBACK_TABLE_COLUMNS_WIDTH[
            column as keyof typeof FEEDBACK_TABLE_COLUMNS_WIDTH
          ]
        ).toBe('number');
      });
    });

    it('should have reasonable width values', () => {
      Object.values(FEEDBACK_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
        expect(width).toBeLessThanOrEqual(500);
      });
    });

    it('should have specific width values', () => {
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackId).toBe(80);
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackContent).toBe(300);
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackAttachment).toBe(150);
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.userName).toBe(100);
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.userType).toBe(100);
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
    });
  });

  // ==================== 類型安全測試 ====================

  describe('Type Safety', () => {
    it('should have immutable permission constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(FEEDBACK_PERMISSION.QUERY).toBe('support:feedback:query');
    });

    it('should have immutable validation constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(FEEDBACK_VALIDATION.SEARCH_WORD_MAX_LENGTH).toBe(25);
      expect(FEEDBACK_VALIDATION.CONTENT_MAX_LENGTH).toBe(500);
    });

    it('should have immutable table columns width constants (compile-time)', () => {
      // TypeScript 'as const' 提供編譯時不可變性
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackId).toBe(80);
      expect(FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackContent).toBe(300);
    });
  });
});
