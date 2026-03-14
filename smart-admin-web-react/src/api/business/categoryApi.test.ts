/**
 * Category API Test
 * 分類 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { categoryApi } from './categoryApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('categoryApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('queryCategoryTree', () => {
    it('should call POST /category/tree with correct params', async () => {
      const mockParams = { categoryType: 1 };
      const mockResponse = { data: [{ categoryId: 1, categoryName: '測試分類' }] };
      
      vi.mocked(request.post).mockResolvedValueOnce(mockResponse);

      const result = await categoryApi.queryCategoryTree(mockParams);

      expect(request.post).toHaveBeenCalledWith('/category/tree', mockParams);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('addCategory', () => {
    it('should call POST /category/add with correct params', async () => {
      const mockParams = {
        categoryName: '新分類',
        categoryType: 1,
      };
      const mockResponse = { data: null };

      vi.mocked(request.post).mockResolvedValueOnce(mockResponse);

      const result = await categoryApi.addCategory(mockParams);

      expect(request.post).toHaveBeenCalledWith('/category/add', mockParams);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('updateCategory', () => {
    it('should call POST /category/update with correct params', async () => {
      const mockParams = {
        categoryId: 1,
        categoryName: '更新分類',
        categoryType: 1,
      };
      const mockResponse = { data: null };

      vi.mocked(request.post).mockResolvedValueOnce(mockResponse);

      const result = await categoryApi.updateCategory(mockParams);

      expect(request.post).toHaveBeenCalledWith('/category/update', mockParams);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('deleteCategory', () => {
    it('should call GET /category/delete/:id with correct id', async () => {
      const categoryId = 123;
      const mockResponse = { data: null };

      vi.mocked(request.get).mockResolvedValueOnce(mockResponse);

      const result = await categoryApi.deleteCategory(categoryId);

      expect(request.get).toHaveBeenCalledWith(`/category/delete/${categoryId}`);
      expect(result).toEqual(mockResponse);
    });
  });
});
