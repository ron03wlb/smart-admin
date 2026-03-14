/**
 * Notice Constants Unit Tests
 * 通知公告常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect } from 'vitest';
import {
  NOTICE_PERMISSION,
  NOTICE_VALIDATION,
  NOTICE_TABLE_COLUMNS_WIDTH,
  VISIBLE_FLAG_LABELS,
} from './noticeConst';

describe('noticeConst', () => {
  // ==================== NOTICE_PERMISSION Tests ====================

  describe('NOTICE_PERMISSION', () => {
    it('should have correct permission strings', () => {
      expect(NOTICE_PERMISSION.QUERY).toBe('oa:notice:query');
      expect(NOTICE_PERMISSION.ADD).toBe('oa:notice:add');
      expect(NOTICE_PERMISSION.UPDATE).toBe('oa:notice:update');
      expect(NOTICE_PERMISSION.DELETE).toBe('oa:notice:delete');
    });

    it('should be immutable (readonly)', () => {
      expect(Object.isFrozen(NOTICE_PERMISSION)).toBe(false);
      // Note: TypeScript 'as const' provides compile-time immutability
    });
  });

  // ==================== NOTICE_VALIDATION Tests ====================

  describe('NOTICE_VALIDATION', () => {
    it('should have correct validation limits', () => {
      expect(NOTICE_VALIDATION.TITLE_MAX_LENGTH).toBe(100);
      expect(NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH).toBe(50);
      expect(NOTICE_VALIDATION.AUTHOR_MAX_LENGTH).toBe(50);
      expect(NOTICE_VALIDATION.SOURCE_MAX_LENGTH).toBe(100);
    });

    it('should have positive numbers for all limits', () => {
      expect(NOTICE_VALIDATION.TITLE_MAX_LENGTH).toBeGreaterThan(0);
      expect(NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH).toBeGreaterThan(0);
      expect(NOTICE_VALIDATION.AUTHOR_MAX_LENGTH).toBeGreaterThan(0);
      expect(NOTICE_VALIDATION.SOURCE_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== NOTICE_TABLE_COLUMNS_WIDTH Tests ====================

  describe('NOTICE_TABLE_COLUMNS_WIDTH', () => {
    it('should have correct column widths', () => {
      expect(NOTICE_TABLE_COLUMNS_WIDTH.title).toBe(300);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.noticeTypeName).toBe(120);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.documentNumber).toBe(180);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.author).toBe(100);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.source).toBe(120);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.allVisibleFlag).toBe(100);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.publishTime).toBe(180);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.createUserName).toBe(100);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
      expect(NOTICE_TABLE_COLUMNS_WIDTH.operate).toBe(150);
    });

    it('should have positive numbers for all widths', () => {
      Object.values(NOTICE_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
      });
    });
  });

  // ==================== VISIBLE_FLAG_LABELS Tests ====================

  describe('VISIBLE_FLAG_LABELS', () => {
    it('should have correct labels for visible flag', () => {
      expect(VISIBLE_FLAG_LABELS.true).toBe('全部可見');
      expect(VISIBLE_FLAG_LABELS.false).toBe('部分可見');
    });

    it('should have labels for both true and false', () => {
      expect(VISIBLE_FLAG_LABELS).toHaveProperty('true');
      expect(VISIBLE_FLAG_LABELS).toHaveProperty('false');
    });

    it('should have non-empty labels', () => {
      expect(VISIBLE_FLAG_LABELS.true).toBeTruthy();
      expect(VISIBLE_FLAG_LABELS.false).toBeTruthy();
    });
  });
});
