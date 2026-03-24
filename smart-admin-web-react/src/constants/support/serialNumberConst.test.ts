/**
 * Serial Number Constants Tests
 * 單號生成器常量測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect } from 'vitest';
import {
  SERIAL_NUMBER_PERMISSION,
  SERIAL_NUMBER_TABLE_COLUMNS_WIDTH,
  SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH,
} from './serialNumberConst';

describe('serialNumberConst', () => {
  describe('SERIAL_NUMBER_PERMISSION', () => {
    it('應該定義正確的權限點', () => {
      expect(SERIAL_NUMBER_PERMISSION.GENERATE).toBe('support:serialNumber:generate');
      expect(SERIAL_NUMBER_PERMISSION.RECORD).toBe('support:serialNumber:record');
    });

    it('權限點應該是 as const 類型', () => {
      const permissions = Object.values(SERIAL_NUMBER_PERMISSION);
      expect(permissions).toHaveLength(2);
      expect(permissions.every(p => typeof p === 'string')).toBe(true);
    });
  });

  describe('SERIAL_NUMBER_TABLE_COLUMNS_WIDTH', () => {
    it('應該定義所有列的寬度', () => {
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.serialNumberId).toBe(80);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.businessName).toBe(150);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.format).toBe(200);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.ruleType).toBe(120);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.initNumber).toBe(100);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.stepRandomRange).toBe(120);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.remark).toBe(200);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.lastNumber).toBe(200);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.lastTime).toBe(180);
      expect(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.action).toBe(180);
    });

    it('所有列寬度應該是正數', () => {
      const widths = Object.values(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH);
      expect(widths.every(w => w > 0)).toBe(true);
    });

    it('應該有10個列定義', () => {
      const columns = Object.keys(SERIAL_NUMBER_TABLE_COLUMNS_WIDTH);
      expect(columns).toHaveLength(10);
    });
  });

  describe('SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH', () => {
    it('應該定義所有記錄列的寬度', () => {
      expect(SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.serialNumberId).toBe(100);
      expect(SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.recordDate).toBe(150);
      expect(SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.count).toBe(120);
      expect(SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.lastNumber).toBe(150);
      expect(SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH.lastTime).toBe(180);
    });

    it('所有記錄列寬度應該是正數', () => {
      const widths = Object.values(SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH);
      expect(widths.every(w => w > 0)).toBe(true);
    });

    it('應該有5個記錄列定義', () => {
      const columns = Object.keys(SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH);
      expect(columns).toHaveLength(5);
    });
  });
});
