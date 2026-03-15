/**
 * Heart Beat API
 * 心跳記錄 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/types/response';
import type { HeartBeatVO, HeartBeatQueryForm } from '@/views/support/heart-beat/types';

export const heartBeatApi = {
  /**
   * 分頁查詢心跳記錄
   */
  queryList: (params: HeartBeatQueryForm): Promise<ResponseDTO<PageResult<HeartBeatVO>>> => {
    return request.post('/support/heartBeat/query', params);
  },
};
