/**
 * API Encrypt API Tests
 * 接口加密 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { apiEncryptApi } from './apiEncryptApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('apiEncryptApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const mockFormData = {
    name: '測試',
    age: 25,
  };

  describe('testRequestEncrypt', () => {
    it('應該調用 POST /support/apiEncrypt/testRequestEncrypt', async () => {
      const mockResponse = {
        code: 200,
        data: mockFormData,
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await apiEncryptApi.testRequestEncrypt(mockFormData);

      expect(request.post).toHaveBeenCalledWith('/support/apiEncrypt/testRequestEncrypt', mockFormData);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('testResponseEncrypt', () => {
    it('應該調用 POST /support/apiEncrypt/testResponseEncrypt', async () => {
      const mockResponse = {
        code: 200,
        data: mockFormData,
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await apiEncryptApi.testResponseEncrypt(mockFormData);

      expect(request.post).toHaveBeenCalledWith('/support/apiEncrypt/testResponseEncrypt', mockFormData);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('testDecryptAndEncrypt', () => {
    it('應該調用 POST /support/apiEncrypt/testDecryptAndEncrypt', async () => {
      const mockResponse = {
        code: 200,
        data: mockFormData,
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await apiEncryptApi.testDecryptAndEncrypt(mockFormData);

      expect(request.post).toHaveBeenCalledWith('/support/apiEncrypt/testDecryptAndEncrypt', mockFormData);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('testArray', () => {
    it('應該調用 POST /support/apiEncrypt/testArray 並傳遞數組', async () => {
      const mockArrayData = [
        { name: '測試1', age: 1 },
        { name: '測試2', age: 2 },
      ];

      const mockResponse = {
        code: 200,
        data: mockArrayData,
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await apiEncryptApi.testArray(mockArrayData);

      expect(request.post).toHaveBeenCalledWith('/support/apiEncrypt/testArray', mockArrayData);
      expect(result).toEqual(mockResponse);
    });
  });
});
