/**
 * Dictionary API
 * 字典 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/support/dict-api.ts
 * 遷移：SmartAdmin React API Layer
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type { DictItem, DictDataItem, DictQueryForm, DictForm, DictDataForm } from '@/types/dict';

/**
 * Dictionary API Module
 */
export const dictApi = {
  /**
   * 獲取所有字典 Code
   * @returns 字典 Code 列表
   */
  getAllDict: (): Promise<ResponseDTO<DictItem[]>> => {
    return request.get('/support/dict/getAllDict');
  },

  /**
   * 獲取全部字典數據
   * @returns 字典數據項列表（包含所有字典的所有數據項）
   */
  getAllDictData: (): Promise<ResponseDTO<DictDataItem[]>> => {
    return request.get('/support/dict/getAllDictData');
  },

  /**
   * 分頁查詢字典
   * @param params 查詢參數
   * @returns 分頁結果
   */
  queryDict: (params: DictQueryForm): Promise<ResponseDTO<PageResult<DictItem>>> => {
    return request.post('/support/dict/queryPage', params);
  },

  /**
   * 添加字典
   * @param data 字典表單數據
   * @returns 響應結果
   */
  addDict: (data: DictForm): Promise<ResponseDTO<void>> => {
    return request.post('/support/dict/add', data);
  },

  /**
   * 更新字典
   * @param data 字典表單數據
   * @returns 響應結果
   */
  updateDict: (data: DictForm): Promise<ResponseDTO<void>> => {
    return request.post('/support/dict/update', data);
  },

  /**
   * 批量刪除字典
   * @param dictIdList 字典 ID 列表
   * @returns 響應結果
   */
  batchDeleteDict: (dictIdList: number[]): Promise<ResponseDTO<void>> => {
    return request.post('/support/dict/batchDelete', dictIdList);
  },

  /**
   * 更新字典啟用/禁用狀態
   * @param dictId 字典 ID
   * @returns 響應結果
   */
  updateDisabled: (dictId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/support/dict/updateDisabled/${dictId}`);
  },

  // ==================== 字典數據相關 API ====================

  /**
   * 查詢字典數據
   * @param dictId 字典 ID
   * @returns 字典數據項列表
   */
  queryDictData: (dictId: number): Promise<ResponseDTO<DictDataItem[]>> => {
    return request.get(`/support/dict/dictData/queryDictData/${dictId}`);
  },

  /**
   * 添加字典數據
   * @param data 字典數據表單
   * @returns 響應結果
   */
  addDictData: (data: DictDataForm): Promise<ResponseDTO<void>> => {
    return request.post('/support/dict/dictData/add', data);
  },

  /**
   * 更新字典數據
   * @param data 字典數據表單
   * @returns 響應結果
   */
  updateDictData: (data: DictDataForm): Promise<ResponseDTO<void>> => {
    return request.post('/support/dict/dictData/update', data);
  },

  /**
   * 批量刪除字典數據
   * @param dictDataIdList 字典數據 ID 列表
   * @returns 響應結果
   */
  batchDeleteDictData: (dictDataIdList: number[]): Promise<ResponseDTO<void>> => {
    return request.post('/support/dict/dictData/batchDelete', dictDataIdList);
  },

  /**
   * 更新字典數據啟用/禁用狀態
   * @param dictDataId 字典數據 ID
   * @returns 響應結果
   */
  updateDictDataDisabled: (dictDataId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/support/dict/dictData/updateDisabled/${dictDataId}`);
  },
};
