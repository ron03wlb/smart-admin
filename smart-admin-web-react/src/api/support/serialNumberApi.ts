/**
 * Serial Number API
 * 單號生成器 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  SerialNumberVO,
  SerialNumberGenerateForm,
  SerialNumberRecordQueryForm,
  SerialNumberRecordVO,
} from '@/views/support/serial-number/types';

/**
 * 單號生成器 API
 */
export const serialNumberApi = {
  /**
   * 獲取所有單號定義
   *
   * @returns 所有單號定義列表
   */
  getAll: (): Promise<ResponseDTO<SerialNumberVO[]>> => {
    return request.get('/support/serialNumber/all');
  },

  /**
   * 生成單號
   *
   * @param form 生成表單
   * @returns 生成的單號列表
   */
  generate: (form: SerialNumberGenerateForm): Promise<ResponseDTO<string[]>> => {
    return request.post('/support/serialNumber/generate', form);
  },

  /**
   * 查詢生成記錄
   *
   * @param form 查詢表單
   * @returns 分頁結果
   */
  queryRecord: (
    form: SerialNumberRecordQueryForm
  ): Promise<ResponseDTO<PageResult<SerialNumberRecordVO>>> => {
    return request.post('/support/serialNumber/queryRecord', form);
  },
};
