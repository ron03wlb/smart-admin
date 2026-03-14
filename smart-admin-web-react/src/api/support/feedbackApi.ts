/**
 * Feedback API
 * 意見反饋 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import request from '@/utils/request';
import { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  FeedbackVO,
  FeedbackQueryForm,
  FeedbackAddForm,
} from '@/views/support/feedback/types';

/**
 * 意見反饋 API
 */
export const feedbackApi = {
  /**
   * 分頁查詢意見反饋
   *
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPage: (form: FeedbackQueryForm): Promise<ResponseDTO<PageResult<FeedbackVO>>> => {
    return request.post('/support/feedback/query', form);
  },

  /**
   * 新增意見反饋
   *
   * @param form 新增表單
   * @returns 成功響應
   */
  addFeedback: (form: FeedbackAddForm): Promise<ResponseDTO<string>> => {
    return request.post('/support/feedback/add', form);
  },
};
