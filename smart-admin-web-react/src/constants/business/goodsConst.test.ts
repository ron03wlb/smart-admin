/**
 * Goods Constants Unit Tests
 * 商品常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect } from 'vitest';
import {
  GOODS_PERMISSION,
  GOODS_VALIDATION,
  GOODS_TABLE_COLUMNS_WIDTH,
  GOODS_STATUS_LABELS,
  GOODS_STATUS_COLORS,
  SHELVES_FLAG_LABELS,
} from './goodsConst';
import { GoodsStatusEnum } from '@/views/business/goods/types';

describe('goodsConst', () => {
  // ==================== GOODS_PERMISSION Tests ====================

  describe('GOODS_PERMISSION', () => {
    it('should have all required permission keys', () => {
      expect(GOODS_PERMISSION).toHaveProperty('QUERY');
      expect(GOODS_PERMISSION).toHaveProperty('ADD');
      expect(GOODS_PERMISSION).toHaveProperty('UPDATE');
      expect(GOODS_PERMISSION).toHaveProperty('DELETE');
      expect(GOODS_PERMISSION).toHaveProperty('BATCH_DELETE');
      expect(GOODS_PERMISSION).toHaveProperty('IMPORT');
      expect(GOODS_PERMISSION).toHaveProperty('EXPORT');
    });

    it('should have correct permission values', () => {
      expect(GOODS_PERMISSION.QUERY).toBe('goods:query');
      expect(GOODS_PERMISSION.ADD).toBe('goods:add');
      expect(GOODS_PERMISSION.UPDATE).toBe('goods:update');
      expect(GOODS_PERMISSION.DELETE).toBe('goods:delete');
      expect(GOODS_PERMISSION.BATCH_DELETE).toBe('goods:batchDelete');
      expect(GOODS_PERMISSION.IMPORT).toBe('goods:importGoods');
      expect(GOODS_PERMISSION.EXPORT).toBe('goods:exportGoods');
    });
  });

  // ==================== GOODS_VALIDATION Tests ====================

  describe('GOODS_VALIDATION', () => {
    it('should have all required validation keys', () => {
      expect(GOODS_VALIDATION).toHaveProperty('NAME_MAX_LENGTH');
      expect(GOODS_VALIDATION).toHaveProperty('REMARK_MAX_LENGTH');
      expect(GOODS_VALIDATION).toHaveProperty('MIN_PRICE');
      expect(GOODS_VALIDATION).toHaveProperty('MAX_PRICE');
    });

    it('should have correct validation values', () => {
      expect(GOODS_VALIDATION.NAME_MAX_LENGTH).toBe(50);
      expect(GOODS_VALIDATION.REMARK_MAX_LENGTH).toBe(200);
      expect(GOODS_VALIDATION.MIN_PRICE).toBe(0);
      expect(GOODS_VALIDATION.MAX_PRICE).toBe(999999);
    });

    it('should have valid price range', () => {
      expect(GOODS_VALIDATION.MIN_PRICE).toBeLessThan(GOODS_VALIDATION.MAX_PRICE);
    });
  });

  // ==================== GOODS_TABLE_COLUMNS_WIDTH Tests ====================

  describe('GOODS_TABLE_COLUMNS_WIDTH', () => {
    it('should have all required column width keys', () => {
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('categoryName');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('goodsName');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('goodsStatus');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('place');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('price');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('shelvesFlag');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('remark');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('createTime');
      expect(GOODS_TABLE_COLUMNS_WIDTH).toHaveProperty('operate');
    });

    it('should have all values as positive numbers', () => {
      expect(GOODS_TABLE_COLUMNS_WIDTH.categoryName).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.goodsName).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.goodsStatus).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.place).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.price).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.shelvesFlag).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.remark).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.createTime).toBeGreaterThan(0);
      expect(GOODS_TABLE_COLUMNS_WIDTH.operate).toBeGreaterThan(0);
    });
  });

  // ==================== GOODS_STATUS_LABELS Tests ====================

  describe('GOODS_STATUS_LABELS', () => {
    it('should have labels for all GoodsStatusEnum values', () => {
      expect(GOODS_STATUS_LABELS[GoodsStatusEnum.APPOINTMENT]).toBe('預約中');
      expect(GOODS_STATUS_LABELS[GoodsStatusEnum.SELLING]).toBe('售賣中');
      expect(GOODS_STATUS_LABELS[GoodsStatusEnum.SOLD_OUT]).toBe('售罄');
    });
  });

  // ==================== GOODS_STATUS_COLORS Tests ====================

  describe('GOODS_STATUS_COLORS', () => {
    it('should have colors for all GoodsStatusEnum values', () => {
      expect(GOODS_STATUS_COLORS[GoodsStatusEnum.APPOINTMENT]).toBe('blue');
      expect(GOODS_STATUS_COLORS[GoodsStatusEnum.SELLING]).toBe('green');
      expect(GOODS_STATUS_COLORS[GoodsStatusEnum.SOLD_OUT]).toBe('red');
    });
  });

  // ==================== SHELVES_FLAG_LABELS Tests ====================

  describe('SHELVES_FLAG_LABELS', () => {
    it('should have labels for shelves flags', () => {
      expect(SHELVES_FLAG_LABELS.true).toBe('上架');
      expect(SHELVES_FLAG_LABELS.false).toBe('下架');
    });
  });
});
