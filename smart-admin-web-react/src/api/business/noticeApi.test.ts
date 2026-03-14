/**
 * Notice API Unit Tests
 * 通知公告 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { noticeApi } from './noticeApi';
import type {
  NoticeVO,
  NoticeQueryForm,
  NoticeAddForm,
  NoticeUpdateForm,
  NoticeTypeVO,
} from '@/views/business/notice/types';
import type { ResponseDTO, PageResult } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('noticeApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== getAllNoticeTypeList Tests ====================

  describe('getAllNoticeTypeList', () => {
    it('should call GET /oa/noticeType/getAll', async () => {
      const mockResponse: ResponseDTO<NoticeTypeVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            noticeTypeId: 1,
            noticeTypeName: '通知',
          },
          {
            noticeTypeId: 2,
            noticeTypeName: '公告',
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await noticeApi.getAllNoticeTypeList();

      expect(request.get).toHaveBeenCalledWith('/oa/noticeType/getAll');
      expect(result).toEqual(mockResponse);
      expect(result.data).toHaveLength(2);
    });

    it('should handle empty notice type list', async () => {
      const mockResponse: ResponseDTO<NoticeTypeVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await noticeApi.getAllNoticeTypeList();

      expect(result.data).toHaveLength(0);
    });
  });

  // ==================== queryNotice Tests ====================

  describe('queryNotice', () => {
    it('should call POST /oa/notice/query with pagination', async () => {
      const queryForm: NoticeQueryForm = {
        noticeTypeId: 1,
        keywords: '測試公告',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse: ResponseDTO<PageResult<NoticeVO>> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          list: [
            {
              noticeId: 1,
              title: '測試公告',
              noticeTypeId: 1,
              noticeTypeName: '通知',
              author: '張三',
              source: '管理部',
              allVisibleFlag: true,
              createTime: '2026-03-11 10:00:00',
            },
          ],
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await noticeApi.queryNotice(queryForm);

      expect(request.post).toHaveBeenCalledWith('/oa/notice/query', queryForm);
      expect(result).toEqual(mockResponse);
      expect(result.data?.list).toHaveLength(1);
    });

    it('should handle empty result', async () => {
      const mockResponse: ResponseDTO<PageResult<NoticeVO>> = {
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

      const result = await noticeApi.queryNotice({});

      expect(result.data?.list).toHaveLength(0);
      expect(result.data?.total).toBe(0);
    });
  });

  // ==================== addNotice Tests ====================

  describe('addNotice', () => {
    it('should call POST /oa/notice/add', async () => {
      const addForm: NoticeAddForm = {
        title: '新公告',
        noticeTypeId: 1,
        documentNumber: '1024創新實驗室發〔2026〕字第1號',
        author: '張三',
        source: '管理部',
        allVisibleFlag: 1,
        publishTime: '2026-03-11 10:00:00',
        contentHtml: '<p>測試內容</p>',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await noticeApi.addNotice(addForm);

      expect(request.post).toHaveBeenCalledWith('/oa/notice/add', addForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== updateNotice Tests ====================

  describe('updateNotice', () => {
    it('should call POST /oa/notice/update', async () => {
      const updateForm: NoticeUpdateForm = {
        noticeId: 5,
        title: '更新後的公告',
        noticeTypeId: 2,
        documentNumber: '1024創新實驗室發〔2026〕字第2號',
        author: '李四',
        source: '技術部',
        allVisibleFlag: 0,
        publishTime: '2026-03-11 14:00:00',
        contentHtml: '<p>更新後的內容</p>',
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await noticeApi.updateNotice(updateForm);

      expect(request.post).toHaveBeenCalledWith('/oa/notice/update', updateForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== deleteNotice Tests ====================

  describe('deleteNotice', () => {
    it('should call GET /oa/notice/delete/:noticeId', async () => {
      const noticeId = 99;

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await noticeApi.deleteNotice(noticeId);

      expect(request.get).toHaveBeenCalledWith('/oa/notice/delete/99');
      expect(result.ok).toBe(true);
    });
  });

  // ==================== getUpdateNoticeInfo Tests ====================

  describe('getUpdateNoticeInfo', () => {
    it('should call GET /oa/notice/getUpdateVO/:noticeId', async () => {
      const noticeId = 10;

      const mockResponse: ResponseDTO<NoticeVO> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: {
          noticeId: 10,
          title: '測試公告',
          noticeTypeId: 1,
          noticeTypeName: '通知',
          documentNumber: '1024創新實驗室發〔2026〕字第1號',
          author: '張三',
          source: '管理部',
          allVisibleFlag: true,
          publishTime: '2026-03-11 10:00:00',
          createUserName: '管理員',
          createTime: '2026-03-11 09:00:00',
        },
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await noticeApi.getUpdateNoticeInfo(noticeId);

      expect(request.get).toHaveBeenCalledWith('/oa/notice/getUpdateVO/10');
      expect(result.ok).toBe(true);
      expect(result.data?.noticeId).toBe(10);
      expect(result.data?.title).toBe('測試公告');
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle API errors in getAllNoticeTypeList', async () => {
      const mockError = new Error('Network Error');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(noticeApi.getAllNoticeTypeList()).rejects.toThrow('Network Error');
    });

    it('should handle API errors in queryNotice', async () => {
      const mockError = new Error('Network Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(noticeApi.queryNotice({})).rejects.toThrow('Network Error');
    });

    it('should handle API errors in addNotice', async () => {
      const addForm: NoticeAddForm = {
        title: '測試',
        noticeTypeId: 1,
        author: '張三',
        source: '管理部',
        allVisibleFlag: 1,
        contentHtml: '<p>測試</p>',
      };

      const mockError = new Error('Validation Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(noticeApi.addNotice(addForm)).rejects.toThrow('Validation Error');
    });

    it('should handle API errors in deleteNotice', async () => {
      const mockError = new Error('Notice in use');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(noticeApi.deleteNotice(1)).rejects.toThrow('Notice in use');
    });
  });
});
