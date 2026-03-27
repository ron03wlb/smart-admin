/**
 * Bank API
 * 銀行信息 API 接口
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import type { ResponseDTO, PageResult } from '@/types/response';
import type { BankVO, BankQueryForm, BankCreateForm, BankUpdateForm } from './types';
import { postRequest, getRequest } from '@/utils/request';

export const bankApi = {
  /**
   * 分頁查詢銀行信息
   * @param queryForm 查詢表單
   * @returns 分頁結果
   */
  queryByPage(queryForm: BankQueryForm): Promise<ResponseDTO<PageResult<BankVO>>> {
    return postRequest('/oa/bank/page/query', queryForm);
  },

  /**
   * 根據企業ID查詢銀行信息列表
   * @param enterpriseId 企業ID
   * @returns 銀行信息列表
   */
  queryList(enterpriseId: number): Promise<ResponseDTO<BankVO[]>> {
    return getRequest(`/oa/bank/query/list/${enterpriseId}`);
  },

  /**
   * 查詢銀行信息詳情
   * @param bankId 銀行信息ID
   * @returns 銀行信息詳情
   */
  getDetail(bankId: number): Promise<ResponseDTO<BankVO>> {
    return getRequest(`/oa/bank/get/${bankId}`);
  },

  /**
   * 新建銀行信息
   * @param createForm 新建表單
   * @returns 操作結果
   */
  createBank(createForm: BankCreateForm): Promise<ResponseDTO<string>> {
    return postRequest('/oa/bank/create', createForm);
  },

  /**
   * 更新銀行信息
   * @param updateForm 更新表單
   * @returns 操作結果
   */
  updateBank(updateForm: BankUpdateForm): Promise<ResponseDTO<string>> {
    return postRequest('/oa/bank/update', updateForm);
  },

  /**
   * 刪除銀行信息
   * @param bankId 銀行信息ID
   * @returns 操作結果
   */
  deleteBank(bankId: number): Promise<ResponseDTO<string>> {
    return getRequest(`/oa/bank/delete/${bankId}`);
  },
};
