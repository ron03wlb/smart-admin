/**
 * Help Doc Catalog API Test
 * 幫助文檔目錄 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { helpDocCatalogApi } from './helpDocCatalogApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('helpDocCatalogApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('應該調用正確的 API 端點', async () => {
      await helpDocCatalogApi.getAll();

      expect(request.get).toHaveBeenCalledWith('/support/helpDoc/helpDocCatalog/getAll');
    });
  });

  describe('add', () => {
    it('應該調用正確的 API 端點', async () => {
      const mockData = {
        name: 'Test Catalog',
        parentId: 0,
        sort: 0,
      };

      await helpDocCatalogApi.add(mockData);

      expect(request.post).toHaveBeenCalledWith('/support/helpDoc/helpDocCatalog/add', mockData);
    });
  });

  describe('update', () => {
    it('應該調用正確的 API 端點', async () => {
      const mockData = {
        helpDocCatalogId: 123,
        name: 'Updated Catalog',
        parentId: 0,
        sort: 1,
      };

      await helpDocCatalogApi.update(mockData);

      expect(request.post).toHaveBeenCalledWith('/support/helpDoc/helpDocCatalog/update', mockData);
    });
  });

  describe('delete', () => {
    it('應該調用正確的 API 端點', async () => {
      const catalogId = 123;

      await helpDocCatalogApi.delete(catalogId);

      expect(request.get).toHaveBeenCalledWith(
        `/support/helpDoc/helpDocCatalog/delete/${catalogId}`
      );
    });
  });
});
