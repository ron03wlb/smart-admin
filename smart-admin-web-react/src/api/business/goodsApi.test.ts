/**
 * Goods API Unit Tests
 * 商品 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { goodsApi } from './goodsApi';
import type { GoodsVO, GoodsQueryForm, GoodsAddForm, GoodsUpdateForm } from '@/views/business/goods/types';
import { GoodsStatusEnum } from '@/views/business/goods/types';
import type { ResponseDTO, PageResult } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('goodsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryGoodsList Tests ====================

  describe('queryGoodsList', () => {
    it('should call POST /goods/query with pagination', async () => {
      const queryForm: GoodsQueryForm = {
        searchWord: '測試商品',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<GoodsVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          list: [
            {
              goodsId: 1,
              categoryId: 10,
              categoryName: '電子產品',
              goodsName: '測試商品',
              goodsStatus: GoodsStatusEnum.SELLING,
              place: '中國',
              price: 99.99,
              shelvesFlag: true,
              createTime: '2026-03-11 10:00:00',
            },
          ],
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await goodsApi.queryGoodsList(queryForm);

      expect(request.post).toHaveBeenCalledWith('/goods/query', queryForm);
      expect(result).toEqual(mockResponse);
      expect(result.data?.list).toHaveLength(1);
    });

    it('should handle empty result', async () => {
      const mockResponse: ResponseDTO<PageResult<GoodsVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          list: [],
          total: 0,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await goodsApi.queryGoodsList({});

      expect(result.data?.list).toHaveLength(0);
      expect(result.data?.total).toBe(0);
    });
  });

  // ==================== addGoods Tests ====================

  describe('addGoods', () => {
    it('should call POST /goods/add', async () => {
      const addForm: GoodsAddForm = {
        categoryId: 10,
        goodsName: '新商品',
        goodsStatus: GoodsStatusEnum.APPOINTMENT,
        place: ['中國', '日本'],
        price: 199.99,
        shelvesFlag: true,
        remark: '測試備註',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await goodsApi.addGoods(addForm);

      expect(request.post).toHaveBeenCalledWith('/goods/add', addForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== updateGoods Tests ====================

  describe('updateGoods', () => {
    it('should call POST /goods/update', async () => {
      const updateForm: GoodsUpdateForm = {
        goodsId: 5,
        categoryId: 10,
        goodsName: '更新後的商品',
        goodsStatus: GoodsStatusEnum.SELLING,
        place: ['美國'],
        price: 299.99,
        shelvesFlag: false,
        remark: '更新後的備註',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await goodsApi.updateGoods(updateForm);

      expect(request.post).toHaveBeenCalledWith('/goods/update', updateForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== deleteGoods Tests ====================

  describe('deleteGoods', () => {
    it('should call GET /goods/delete/:goodsId', async () => {
      const goodsId = 99;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await goodsApi.deleteGoods(goodsId);

      expect(request.get).toHaveBeenCalledWith('/goods/delete/99');
      expect(result.ok).toBe(true);
    });
  });

  // ==================== batchDelete Tests ====================

  describe('batchDelete', () => {
    it('should call POST /goods/batchDelete with ID list', async () => {
      const goodsIdList = [10, 11, 12];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await goodsApi.batchDelete(goodsIdList);

      expect(request.post).toHaveBeenCalledWith('/goods/batchDelete', goodsIdList);
      expect(result.ok).toBe(true);
    });

    it('should handle single goods delete', async () => {
      const goodsIdList = [99];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await goodsApi.batchDelete(goodsIdList);

      expect(request.post).toHaveBeenCalledWith('/goods/batchDelete', goodsIdList);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle API errors in queryGoodsList', async () => {
      const mockError = new Error('Network Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(goodsApi.queryGoodsList({})).rejects.toThrow('Network Error');
    });

    it('should handle API errors in addGoods', async () => {
      const addForm: GoodsAddForm = {
        categoryId: 10,
        goodsName: '測試',
        goodsStatus: GoodsStatusEnum.APPOINTMENT,
        place: ['中國'],
        price: 100,
        shelvesFlag: true,
      };

      const mockError = new Error('Validation Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(goodsApi.addGoods(addForm)).rejects.toThrow('Validation Error');
    });

    it('should handle API errors in batchDelete', async () => {
      const mockError = new Error('Goods in use');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(goodsApi.batchDelete([1, 2])).rejects.toThrow('Goods in use');
    });
  });
});
