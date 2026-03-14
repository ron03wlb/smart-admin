/**
 * Config API Unit Tests
 * 配置 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { configApi } from './configApi';
import type { ConfigVO, ConfigQueryForm, ConfigFormData } from '@/views/support/config/types';
import type { ResponseDTO, PageResult } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('configApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryPage Tests ====================

  describe('queryPage', () => {
    it('should call POST /support/config/query with query params', async () => {
      const queryForm: ConfigQueryForm = {
        configKey: 'system',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<ConfigVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          pageNum: 1,
          pageSize: 10,
          total: 1,
          pages: 1,
          list: [
            {
              configId: 1,
              configKey: 'system.title',
              configName: '系統標題',
              configValue: 'SmartAdmin',
              remark: '系統主標題',
              createTime: '2026-03-12T10:00:00Z',
            },
          ],
          emptyFlag: false,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await configApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/config/query', queryForm);
      expect(result).toEqual(mockResponse);
    });

    it('should handle empty configKey', async () => {
      const queryForm: ConfigQueryForm = {
        configKey: undefined,
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<ConfigVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          pageNum: 1,
          pageSize: 10,
          total: 0,
          pages: 0,
          list: [],
          emptyFlag: true,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await configApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/config/query', queryForm);
      expect(result.data.emptyFlag).toBe(true);
    });
  });

  // ==================== addConfig Tests ====================

  describe('addConfig', () => {
    it('should call POST /support/config/add with form data', async () => {
      const formData: ConfigFormData = {
        configKey: 'system.version',
        configName: '系統版本',
        configValue: '1.0.0',
        remark: '當前系統版本號',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await configApi.addConfig(formData);

      expect(request.post).toHaveBeenCalledWith('/support/config/add', formData);
      expect(result).toEqual(mockResponse);
    });

    it('should handle form data without optional fields', async () => {
      const formData: ConfigFormData = {
        configKey: 'app.name',
        configName: '應用名稱',
        configValue: 'MyApp',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await configApi.addConfig(formData);

      expect(request.post).toHaveBeenCalledWith('/support/config/add', formData);
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== updateConfig Tests ====================

  describe('updateConfig', () => {
    it('should call POST /support/config/update with form data including configId', async () => {
      const formData: ConfigFormData = {
        configId: 1,
        configKey: 'system.title',
        configName: '系統標題（更新）',
        configValue: 'SmartAdmin Pro',
        remark: '更新後的系統標題',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await configApi.updateConfig(formData);

      expect(request.post).toHaveBeenCalledWith('/support/config/update', formData);
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== queryByKey Tests ====================

  describe('queryByKey', () => {
    it('should call GET /support/config/queryByKey with configKey param', async () => {
      const configKey = 'system.title';

      const mockResponse: ResponseDTO<ConfigVO> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          configId: 1,
          configKey: 'system.title',
          configName: '系統標題',
          configValue: 'SmartAdmin',
          remark: '系統主標題',
          createTime: '2026-03-12T10:00:00Z',
          updateTime: '2026-03-12T10:00:00Z',
        },
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await configApi.queryByKey(configKey);

      expect(request.get).toHaveBeenCalledWith(`/support/config/queryByKey?configKey=${configKey}`);
      expect(result).toEqual(mockResponse);
    });
  });
});
