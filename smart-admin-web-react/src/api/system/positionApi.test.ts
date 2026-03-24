/**
 * Position API Unit Tests
 * 職位 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { positionApi } from './positionApi';
import type {
  PositionVO,
  PositionQueryForm,
  PositionFormData,
} from '@/views/system/position/types';
import type { ResponseDTO, PageResult } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('positionApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryPage Tests ====================

  describe('queryPage', () => {
    it('should call POST /position/queryPage with query params', async () => {
      const queryForm: PositionQueryForm = {
        keywords: '經理',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<PositionVO>> = {
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
              positionId: 1,
              positionName: '部門經理',
              positionLevel: 'M1',
              sort: 100,
              remark: '部門負責人',
              createTime: '2026-03-12T10:00:00Z',
            },
          ],
          emptyFlag: false,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await positionApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/position/queryPage', queryForm);
      expect(result).toEqual(mockResponse);
    });

    it('should handle empty keywords', async () => {
      const queryForm: PositionQueryForm = {
        keywords: undefined,
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<PositionVO>> = {
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

      const result = await positionApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/position/queryPage', queryForm);
      expect(result.data.emptyFlag).toBe(true);
    });
  });

  // ==================== queryList Tests ====================

  describe('queryList', () => {
    it('should call GET /position/queryList', async () => {
      const mockResponse: ResponseDTO<PositionVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            positionId: 1,
            positionName: '部門經理',
            positionLevel: 'M1',
            sort: 100,
            remark: '部門負責人',
            createTime: '2026-03-12T10:00:00Z',
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await positionApi.queryList();

      expect(request.get).toHaveBeenCalledWith('/position/queryList');
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== addPosition Tests ====================

  describe('addPosition', () => {
    it('should call POST /position/add with form data', async () => {
      const formData: PositionFormData = {
        positionName: '技術總監',
        positionLevel: 'E5',
        sort: 200,
        remark: '技術部門負責人',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await positionApi.addPosition(formData);

      expect(request.post).toHaveBeenCalledWith('/position/add', formData);
      expect(result).toEqual(mockResponse);
    });

    it('should handle form data without optional fields', async () => {
      const formData: PositionFormData = {
        positionName: '普通員工',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await positionApi.addPosition(formData);

      expect(request.post).toHaveBeenCalledWith('/position/add', formData);
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== updatePosition Tests ====================

  describe('updatePosition', () => {
    it('should call POST /position/update with form data including positionId', async () => {
      const formData: PositionFormData = {
        positionId: 1,
        positionName: '高級經理',
        positionLevel: 'M2',
        sort: 150,
        remark: '更新後的備註',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await positionApi.updatePosition(formData);

      expect(request.post).toHaveBeenCalledWith('/position/update', formData);
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== deletePosition Tests ====================

  describe('deletePosition', () => {
    it('should call GET /position/delete/{positionId}', async () => {
      const positionId = 123;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await positionApi.deletePosition(positionId);

      expect(request.get).toHaveBeenCalledWith(`/position/delete/${positionId}`);
      expect(result).toEqual(mockResponse);
    });
  });

  // ==================== batchDeletePosition Tests ====================

  describe('batchDeletePosition', () => {
    it('should call POST /position/batchDelete with ID array', async () => {
      const idList = [1, 2, 3];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await positionApi.batchDeletePosition(idList);

      expect(request.post).toHaveBeenCalledWith('/position/batchDelete', idList);
      expect(result).toEqual(mockResponse);
    });

    it('should handle empty ID array', async () => {
      const idList: number[] = [];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await positionApi.batchDeletePosition(idList);

      expect(request.post).toHaveBeenCalledWith('/position/batchDelete', idList);
      expect(result).toEqual(mockResponse);
    });
  });
});
