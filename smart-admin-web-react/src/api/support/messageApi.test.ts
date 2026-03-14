/**
 * Message API Tests
 * 消息管理 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { messageApi } from './messageApi';
import request from '@/utils/request';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('messageApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const mockQueryForm = {
    searchWord: '測試',
    messageType: 1,
    readFlag: false,
    pageNum: 1,
    pageSize: 10,
  };

  const mockMessage = {
    messageId: 1,
    messageType: 1,
    title: '測試消息',
    content: '測試內容',
    receiverUserId: 100,
    receiverUserType: 1,
    readFlag: false,
    createTime: '2026-03-14 10:00:00',
    updateTime: '2026-03-14 10:00:00',
  };

  describe('queryAdminMessage', () => {
    it('應該調用 POST /message/query', async () => {
      const mockResponse = {
        code: 200,
        data: {
          list: [mockMessage],
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await messageApi.queryAdminMessage(mockQueryForm);

      expect(request.post).toHaveBeenCalledWith('/message/query', mockQueryForm);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('sendMessages', () => {
    it('應該調用 POST /message/sendMessages 並傳遞消息數組', async () => {
      const mockMessages = [
        {
          title: '測試消息1',
          receiverUserId: 100,
          content: '測試內容1',
          messageType: 1,
          receiverUserType: 1,
        },
        {
          title: '測試消息2',
          receiverUserId: 101,
          content: '測試內容2',
          messageType: 1,
          receiverUserType: 1,
        },
      ];

      const mockResponse = {
        code: 200,
        data: null,
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await messageApi.sendMessages(mockMessages);

      expect(request.post).toHaveBeenCalledWith('/message/sendMessages', mockMessages);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('deleteMessage', () => {
    it('應該調用 GET /message/delete/:messageId', async () => {
      const messageId = 123;
      const mockResponse = {
        code: 200,
        data: null,
        ok: true,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await messageApi.deleteMessage(messageId);

      expect(request.get).toHaveBeenCalledWith(`/message/delete/${messageId}`);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('queryMyMessage', () => {
    it('應該調用 POST /support/message/queryMyMessage', async () => {
      const mockResponse = {
        code: 200,
        data: {
          list: [mockMessage],
          total: 1,
          pageNum: 1,
          pageSize: 10,
        },
        ok: true,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await messageApi.queryMyMessage(mockQueryForm);

      expect(request.post).toHaveBeenCalledWith('/support/message/queryMyMessage', mockQueryForm);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('queryUnreadCount', () => {
    it('應該調用 GET /support/message/getUnreadCount', async () => {
      const mockResponse = {
        code: 200,
        data: 5,
        ok: true,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await messageApi.queryUnreadCount();

      expect(request.get).toHaveBeenCalledWith('/support/message/getUnreadCount');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('updateReadFlag', () => {
    it('應該調用 GET /support/message/read/:messageId', async () => {
      const messageId = 123;
      const mockResponse = {
        code: 200,
        data: null,
        ok: true,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await messageApi.updateReadFlag(messageId);

      expect(request.get).toHaveBeenCalledWith(`/support/message/read/${messageId}`);
      expect(result).toEqual(mockResponse);
    });
  });
});
