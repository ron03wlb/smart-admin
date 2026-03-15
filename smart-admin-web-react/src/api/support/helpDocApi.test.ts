/**
 * Help Doc API Test
 * 幫助文檔 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { helpDocApi } from './helpDocApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('helpDocApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('query', () => {
    it('應該調用正確的 API 端點', async () => {
      const mockParams = {
        helpDocCatalogId: 1,
        keywords: 'test',
        pageNum: 1,
        pageSize: 10,
      };

      await helpDocApi.query(mockParams);

      expect(request.post).toHaveBeenCalledWith('/support/helpDoc/query', mockParams);
    });
  });

  describe('add', () => {
    it('應該調用正確的 API 端點', async () => {
      const mockData = {
        title: 'Test Title',
        author: 'Test Author',
        sort: 0,
        attachment: [],
        relationList: [],
        contentHtml: '<p>Test</p>',
        contentText: 'Test',
      };

      await helpDocApi.add(mockData);

      expect(request.post).toHaveBeenCalledWith('/support/helpDoc/add', mockData);
    });
  });

  describe('delete', () => {
    it('應該調用正確的 API 端點', async () => {
      const helpDocId = 123;

      await helpDocApi.delete(helpDocId);

      expect(request.get).toHaveBeenCalledWith(`/support/helpDoc/delete/${helpDocId}`);
    });
  });

  describe('getDetail', () => {
    it('應該調用正確的 API 端點', async () => {
      const helpDocId = 123;

      await helpDocApi.getDetail(helpDocId);

      expect(request.get).toHaveBeenCalledWith(`/support/helpDoc/getDetail/${helpDocId}`);
    });
  });
});
