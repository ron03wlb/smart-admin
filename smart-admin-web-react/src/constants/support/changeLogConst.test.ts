/**
 * ChangeLog Constants Tests
 * 系統更新日誌常量測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect } from 'vitest';
import {
  CHANGE_LOG_PERMISSION,
  CHANGE_LOG_VALIDATION,
  CHANGE_LOG_TYPE_LABELS,
  CHANGE_LOG_TYPE_COLORS,
  CHANGE_LOG_TABLE_COLUMNS_WIDTH,
} from './changeLogConst';

describe('changeLogConst', () => {
  describe('CHANGE_LOG_PERMISSION', () => {
    it('should have correct permission keys', () => {
      expect(CHANGE_LOG_PERMISSION.QUERY).toBe('support:changeLog:query');
      expect(CHANGE_LOG_PERMISSION.ADD).toBe('support:changeLog:add');
      expect(CHANGE_LOG_PERMISSION.UPDATE).toBe('support:changeLog:update');
      expect(CHANGE_LOG_PERMISSION.DELETE).toBe('support:changeLog:delete');
      expect(CHANGE_LOG_PERMISSION.BATCH_DELETE).toBe('support:changeLog:batchDelete');
    });
  });

  describe('CHANGE_LOG_VALIDATION', () => {
    it('should have correct validation rules', () => {
      expect(CHANGE_LOG_VALIDATION.VERSION_MAX_LENGTH).toBe(50);
      expect(CHANGE_LOG_VALIDATION.AUTHOR_MAX_LENGTH).toBe(50);
      expect(CHANGE_LOG_VALIDATION.CONTENT_MAX_LENGTH).toBe(2000);
      expect(CHANGE_LOG_VALIDATION.LINK_MAX_LENGTH).toBe(500);
    });
  });

  describe('CHANGE_LOG_TYPE_LABELS', () => {
    it('should have correct type labels', () => {
      expect(CHANGE_LOG_TYPE_LABELS[1]).toBe('重大更新');
      expect(CHANGE_LOG_TYPE_LABELS[2]).toBe('功能更新');
      expect(CHANGE_LOG_TYPE_LABELS[3]).toBe('Bug修復');
    });
  });

  describe('CHANGE_LOG_TYPE_COLORS', () => {
    it('should have correct type colors', () => {
      expect(CHANGE_LOG_TYPE_COLORS[1]).toBe('red');
      expect(CHANGE_LOG_TYPE_COLORS[2]).toBe('blue');
      expect(CHANGE_LOG_TYPE_COLORS[3]).toBe('green');
    });
  });

  describe('CHANGE_LOG_TABLE_COLUMNS_WIDTH', () => {
    it('should have correct column widths', () => {
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.updateVersion).toBe(120);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.type).toBe(100);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.publishAuthor).toBe(100);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.publicDate).toBe(120);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.content).toBe(300);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.link).toBe(200);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.createTime).toBe(160);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.updateTime).toBe(160);
      expect(CHANGE_LOG_TABLE_COLUMNS_WIDTH.action).toBe(140);
    });
  });
});
