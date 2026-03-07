/**
 * Message API
 *
 * Corresponds to Vue's api/support/message-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';
import type { MessageVO, MessageQueryForm, MessageSendForm } from '@/types/message.types';

export const messageApi = {
  /** Query my messages */
  queryMessage: (data: MessageQueryForm) => postRequest<PageResult<MessageVO>>('/support/message/queryMyMessage', data),

  /** Get unread count */
  getUnreadCount: () => getRequest<number>('/support/message/getUnreadCount'),

  /** Mark message as read */
  read: (messageId: number) => getRequest<void>(`/support/message/read/${messageId}`),

  /** Query all messages (admin) */
  query: (data: MessageQueryForm) => postRequest<PageResult<MessageVO>>('/message/query', data),

  /** Send messages */
  sendMessages: (data: MessageSendForm) => postRequest<void>('/message/sendMessages', data),

  /** Delete message */
  delete: (messageId: number) => getRequest<void>(`/message/delete/${messageId}`),
};
