/**
 * Feedback API Unit Tests
 * 意見反饋 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { feedbackApi } from './feedbackApi';
import type {
  FeedbackVO,
  FeedbackQueryForm,
  FeedbackAddForm,
} from '@/views/support/feedback/types';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('feedbackApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryPage 測試 ====================

  describe('queryPage', () => {
    it('should call POST /support/feedback/query with correct parameters', async () => {
      const queryForm: FeedbackQueryForm = {
        searchWord: 'test',
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: [],
          total: 0,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await feedbackApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/feedback/query', queryForm);
      expect(result).toEqual(mockResponse);
    });

    it('should handle empty query form', async () => {
      const queryForm: FeedbackQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: [],
          total: 0,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await feedbackApi.queryPage(queryForm);

      expect(request.post).toHaveBeenCalledWith('/support/feedback/query', queryForm);
      expect(result.code).toBe(200);
    });

    it('should return feedback list data correctly', async () => {
      const queryForm: FeedbackQueryForm = {
        pageNum: 1,
        pageSize: 10,
      };

      const mockFeedbackList: FeedbackVO[] = [
        {
          feedbackId: 1,
          feedbackContent: '測試反饋內容',
          feedbackAttachment: '[]',
          userId: 100,
          userName: '張三',
          userType: 1,
          createTime: '2024-03-12T10:00:00',
          updateTime: '2024-03-12T10:00:00',
        },
      ];

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: {
          list: mockFeedbackList,
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await feedbackApi.queryPage(queryForm);

      expect(result.data.list).toHaveLength(1);
      expect(result.data.list[0].feedbackContent).toBe('測試反饋內容');
      expect(result.data.total).toBe(1);
    });
  });

  // ==================== addFeedback 測試 ====================

  describe('addFeedback', () => {
    it('should call POST /support/feedback/add with correct parameters', async () => {
      const addForm: FeedbackAddForm = {
        feedbackContent: '新的反饋內容',
        feedbackAttachment: '[]',
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: 'Feedback added successfully',
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await feedbackApi.addFeedback(addForm);

      expect(request.post).toHaveBeenCalledWith('/support/feedback/add', addForm);
      expect(result).toEqual(mockResponse);
    });

    it('should handle feedback without attachment', async () => {
      const addForm: FeedbackAddForm = {
        feedbackContent: '沒有附件的反饋',
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: 'Feedback added successfully',
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await feedbackApi.addFeedback(addForm);

      expect(request.post).toHaveBeenCalledWith('/support/feedback/add', addForm);
      expect(result.code).toBe(200);
    });

    it('should return success message on successful add', async () => {
      const addForm: FeedbackAddForm = {
        feedbackContent: '測試反饋',
        feedbackAttachment: '[{"url":"test.jpg"}]',
      };

      const mockResponse = {
        code: 200,
        ok: true,
        msg: 'Success',
        data: 'Feedback added successfully',
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await feedbackApi.addFeedback(addForm);

      expect(result.ok).toBe(true);
      expect(result.data).toBe('Feedback added successfully');
    });
  });
});
