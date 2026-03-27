/**
 * Invoice API
 * 發票信息 API 接口
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import type { ResponseDTO, PageResult } from '@/types/response';
import type { InvoiceVO, InvoiceQueryForm, InvoiceAddForm, InvoiceUpdateForm } from './types';
import { postRequest, getRequest } from '@/utils/request';

export const invoiceApi = {
  /**
   * 分頁查詢發票信息
   * @param queryForm 查詢表單
   * @returns 分頁結果
   */
  queryByPage(queryForm: InvoiceQueryForm): Promise<ResponseDTO<PageResult<InvoiceVO>>> {
    return postRequest('/oa/invoice/page/query', queryForm);
  },

  /**
   * 根據企業ID查詢發票信息列表
   * @param enterpriseId 企業ID
   * @returns 發票信息列表
   */
  queryList(enterpriseId: number): Promise<ResponseDTO<InvoiceVO[]>> {
    return getRequest(`/oa/invoice/query/list/${enterpriseId}`);
  },

  /**
   * 查詢發票信息詳情
   * @param invoiceId 發票信息ID
   * @returns 發票信息詳情
   */
  getDetail(invoiceId: number): Promise<ResponseDTO<InvoiceVO>> {
    return getRequest(`/oa/invoice/get/${invoiceId}`);
  },

  /**
   * 新建發票信息
   * @param addForm 新建表單
   * @returns 操作結果
   */
  createInvoice(addForm: InvoiceAddForm): Promise<ResponseDTO<string>> {
    return postRequest('/oa/invoice/create', addForm);
  },

  /**
   * 更新發票信息
   * @param updateForm 更新表單
   * @returns 操作結果
   */
  updateInvoice(updateForm: InvoiceUpdateForm): Promise<ResponseDTO<string>> {
    return postRequest('/oa/invoice/update', updateForm);
  },

  /**
   * 刪除發票信息
   * @param invoiceId 發票信息ID
   * @returns 操作結果
   */
  deleteInvoice(invoiceId: number): Promise<ResponseDTO<string>> {
    return getRequest(`/invoice/delete/${invoiceId}`);
  },
};
