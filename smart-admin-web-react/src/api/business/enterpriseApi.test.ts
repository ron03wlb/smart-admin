/**
 * Enterprise API Unit Tests
 * 企業管理 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { enterpriseApi } from './enterpriseApi';
import type {
  EnterpriseVO,
  EnterpriseQueryForm,
  EnterpriseAddForm,
  EnterpriseUpdateForm,
} from '@/views/business/enterprise/types';
import { EnterpriseTypeEnum } from '@/views/business/enterprise/types';
import type { ResponseDTO, PageResult } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('enterpriseApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== pageQuery Tests ====================

  describe('pageQuery', () => {
    it('should call POST /oa/enterprise/page/query with pagination', async () => {
      const queryForm: EnterpriseQueryForm = {
        keywords: '測試企業',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<EnterpriseVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          list: [
            {
              enterpriseId: 1,
              enterpriseName: '測試企業',
              unifiedSocialCreditCode: '123456789012345678',
              type: EnterpriseTypeEnum.LIMITED_LIABILITY,
              contact: '張三',
              contactPhone: '13800138000',
              disabledFlag: false,
              createTime: '2026-03-11 10:00:00',
            },
          ],
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await enterpriseApi.pageQuery(queryForm);

      expect(request.post).toHaveBeenCalledWith('/oa/enterprise/page/query', queryForm);
      expect(result).toEqual(mockResponse);
      expect(result.data?.list).toHaveLength(1);
    });

    it('should handle empty result', async () => {
      const mockResponse: ResponseDTO<PageResult<EnterpriseVO>> = {
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

      const result = await enterpriseApi.pageQuery({});

      expect(result.data?.list).toHaveLength(0);
      expect(result.data?.total).toBe(0);
    });
  });

  // ==================== detail Tests ====================

  describe('detail', () => {
    it('should call GET /oa/enterprise/get/:enterpriseId', async () => {
      const enterpriseId = 10;

      const mockResponse: ResponseDTO<EnterpriseVO> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          enterpriseId: 10,
          enterpriseName: '測試企業',
          unifiedSocialCreditCode: '123456789012345678',
          type: EnterpriseTypeEnum.LIMITED_LIABILITY,
          contact: '張三',
          contactPhone: '13800138000',
          email: 'test@example.com',
          disabledFlag: false,
        },
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await enterpriseApi.detail(enterpriseId);

      expect(request.get).toHaveBeenCalledWith('/oa/enterprise/get/10');
      expect(result.ok).toBe(true);
      expect(result.data?.enterpriseId).toBe(10);
    });
  });

  // ==================== create Tests ====================

  describe('create', () => {
    it('should call POST /oa/enterprise/create', async () => {
      const addForm: EnterpriseAddForm = {
        enterpriseName: '新企業',
        unifiedSocialCreditCode: '123456789012345678',
        type: EnterpriseTypeEnum.LIMITED_LIABILITY,
        contact: '張三',
        contactPhone: '13800138000',
        email: 'test@example.com',
        disabledFlag: false,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await enterpriseApi.create(addForm);

      expect(request.post).toHaveBeenCalledWith('/oa/enterprise/create', addForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== update Tests ====================

  describe('update', () => {
    it('should call POST /oa/enterprise/update', async () => {
      const updateForm: EnterpriseUpdateForm = {
        enterpriseId: 5,
        enterpriseName: '更新後的企業',
        unifiedSocialCreditCode: '123456789012345678',
        type: EnterpriseTypeEnum.JOINT_STOCK,
        contact: '李四',
        contactPhone: '13900139000',
        disabledFlag: true,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await enterpriseApi.update(updateForm);

      expect(request.post).toHaveBeenCalledWith('/oa/enterprise/update', updateForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== delete Tests ====================

  describe('delete', () => {
    it('should call GET /oa/enterprise/delete/:enterpriseId', async () => {
      const enterpriseId = 99;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await enterpriseApi.delete(enterpriseId);

      expect(request.get).toHaveBeenCalledWith('/oa/enterprise/delete/99');
      expect(result.ok).toBe(true);
    });
  });

  // ==================== queryList Tests ====================

  describe('queryList', () => {
    it('should call GET /oa/enterprise/query/list without type', async () => {
      const mockResponse: ResponseDTO<EnterpriseVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            enterpriseId: 1,
            enterpriseName: '測試企業1',
            unifiedSocialCreditCode: '123456789012345678',
            type: EnterpriseTypeEnum.LIMITED_LIABILITY,
            contact: '張三',
            contactPhone: '13800138000',
            disabledFlag: false,
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await enterpriseApi.queryList();

      expect(request.get).toHaveBeenCalledWith('/oa/enterprise/query/list');
      expect(result.data).toHaveLength(1);
    });

    it('should call GET /oa/enterprise/query/list with type', async () => {
      const mockResponse: ResponseDTO<EnterpriseVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await enterpriseApi.queryList(EnterpriseTypeEnum.JOINT_STOCK);

      expect(request.get).toHaveBeenCalledWith('/oa/enterprise/query/list?type=2');
      expect(result.data).toHaveLength(0);
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle API errors in pageQuery', async () => {
      const mockError = new Error('Network Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(enterpriseApi.pageQuery({})).rejects.toThrow('Network Error');
    });

    it('should handle API errors in create', async () => {
      const addForm: EnterpriseAddForm = {
        enterpriseName: '測試',
        unifiedSocialCreditCode: '123456789012345678',
        type: EnterpriseTypeEnum.LIMITED_LIABILITY,
        contact: '張三',
        contactPhone: '13800138000',
        disabledFlag: false,
      };

      const mockError = new Error('Validation Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(enterpriseApi.create(addForm)).rejects.toThrow('Validation Error');
    });

    it('should handle API errors in delete', async () => {
      const mockError = new Error('Enterprise in use');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(enterpriseApi.delete(1)).rejects.toThrow('Enterprise in use');
    });
  });
});
