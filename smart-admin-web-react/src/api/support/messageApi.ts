/**
 * Message API
 * 消息管理 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/support/message-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';

/**
 * 消息查詢表單
 */
export interface MessageQueryForm {
  searchWord?: string; // 關鍵詞
  messageType?: number; // 消息類型
  readFlag?: boolean; // 是否已讀
  startDate?: string; // 開始日期
  endDate?: string; // 結束日期
  pageNum: number;
  pageSize: number;
}

/**
 * 消息 VO
 */
export interface MessageVO {
  messageId: number;
  messageType: number;
  title: string;
  content: string;
  receiverUserId: number;
  receiverUserType: number;
  readFlag: boolean;
  readTime?: string;
  createTime: string;
  updateTime: string;
}

/**
 * 發送消息表單
 */
export interface MessageSendForm {
  title: string;
  receiverUserId: number;
  content: string;
  messageType: number;
  receiverUserType: number;
}

/**
 * 消息 API
 */
export const messageApi = {
  /**
   * 管理端 - 分頁查詢消息
   */
  queryAdminMessage: (params: MessageQueryForm): Promise<ResponseDTO<PageResult<MessageVO>>> => {
    return request.post('/message/query', params);
  },

  /**
   * 批量發送消息
   */
  sendMessages: (messages: MessageSendForm[]): Promise<ResponseDTO<void>> => {
    return request.post('/message/sendMessages', messages);
  },

  /**
   * 刪除消息
   */
  deleteMessage: (messageId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/message/delete/${messageId}`);
  },

  /**
   * 用戶端 - 查詢我的消息
   */
  queryMyMessage: (params: MessageQueryForm): Promise<ResponseDTO<PageResult<MessageVO>>> => {
    return request.post('/support/message/queryMyMessage', params);
  },

  /**
   * 用戶端 - 查詢未讀消息數
   */
  queryUnreadCount: (): Promise<ResponseDTO<number>> => {
    return request.get('/support/message/getUnreadCount');
  },

  /**
   * 用戶端 - 標記已讀
   */
  updateReadFlag: (messageId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/support/message/read/${messageId}`);
  },
};
