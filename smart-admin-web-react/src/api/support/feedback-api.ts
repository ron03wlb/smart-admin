import { postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface FeedbackVO {
  feedbackId: number;
  feedbackContent: string;
  feedbackAttachment?: string;
  userName: string;
  userType: number;
  createTime: string;
}

export interface FeedbackQueryForm {
  searchWord?: string;
  startDate?: string;
  endDate?: string;
  pageNum: number;
  pageSize: number;
}

export const feedbackApi = {
  queryFeedback: (data: FeedbackQueryForm) => postRequest<PageResult<FeedbackVO>>('/support/feedback/query', data),
  addFeedback: (data: { feedbackContent: string; feedbackAttachment?: string }) =>
    postRequest<void>('/support/feedback/add', data),
};
