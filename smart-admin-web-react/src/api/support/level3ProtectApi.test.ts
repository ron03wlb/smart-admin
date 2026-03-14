/**
 * Level 3 Protect API Tests
 * 三級等保 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { level3ProtectApi } from './level3ProtectApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('level3ProtectApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const mockConfig = {
    twoFactorLoginEnabled: true,
    loginFailMaxTimes: 5,
    loginFailLockMinutes: 30,
    loginActiveTimeoutMinutes: 30,
    passwordComplexityEnabled: true,
    regularChangePasswordMonths: 3,
    regularChangePasswordNotAllowRepeatTimes: 3,
    fileDetectFlag: true,
    maxUploadFileSizeMb: 50,
  };

  describe('getConfig', () => {
    it('應該調用 GET /support/protect/level3protect/getConfig', async () => {
      const mockResponse = {
        code: 200,
        data: JSON.stringify(mockConfig),
        ok: true,
        msg: '',
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await level3ProtectApi.getConfig();

      expect(request.get).toHaveBeenCalledWith('/support/protect/level3protect/getConfig');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('updateConfig', () => {
    it('應該調用 POST /support/protect/level3protect/updateConfig 並傳遞配置', async () => {
      const mockResponse = {
        code: 200,
        data: null,
        ok: true,
        msg: '配置更新成功',
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await level3ProtectApi.updateConfig(mockConfig);

      expect(request.post).toHaveBeenCalledWith(
        '/support/protect/level3protect/updateConfig',
        mockConfig
      );
      expect(result).toEqual(mockResponse);
    });
  });
});
