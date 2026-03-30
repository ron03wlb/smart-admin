/**
 * Position API
 * 職位 API 層
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  PositionVO,
  PositionFormData,
  PositionQueryForm,
} from '@/views/system/position/types';

/**
 * 職位 API
 */
export const positionApi = {
  /**
   * 分頁查詢職位列表
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryPage: (form: PositionQueryForm): Promise<ResponseDTO<PageResult<PositionVO>>> => {
    return request.post('/position/queryPage', form);
  },

  /**
   * 查詢所有職位（不分頁）
   * @returns 職位列表
   */
  queryList: (): Promise<ResponseDTO<PositionVO[]>> => {
    return request.get('/position/queryList');
  },

  /**
   * 新增職位
   * @param form 表單數據
   * @returns 響應結果
   */
  addPosition: (form: PositionFormData): Promise<ResponseDTO<void>> => {
    return request.post('/position/add', form);
  },

  /**
   * 更新職位
   * @param form 表單數據
   * @returns 響應結果
   */
  updatePosition: (form: PositionFormData): Promise<ResponseDTO<void>> => {
    return request.post('/position/update', form);
  },

  /**
   * 刪除職位
   * @param positionId 職位 ID
   * @returns 響應結果
   */
  deletePosition: (positionId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/position/delete/${positionId}`);
  },

  /**
   * 批量刪除職位
   * @param idList ID 列表
   * @returns 響應結果
   */
  batchDeletePosition: (idList: number[]): Promise<ResponseDTO<void>> => {
    return request.post('/position/batchDelete', idList);
  },
};
