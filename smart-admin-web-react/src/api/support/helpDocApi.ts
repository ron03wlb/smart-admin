/**
 * Help Doc API
 * 幫助文檔 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  HelpDocVO,
  HelpDocQueryForm,
  HelpDocFormData,
  HelpDocDetailVO,
} from '@/views/support/help-doc/types';

/**
 * 查看記錄查詢表單
 */
export interface ViewRecordQueryForm {
  pageNum: number;
  pageSize: number;
  searchCount?: boolean;
  helpDocId?: number;
  keywords?: string;
}

/**
 * 查看記錄 VO
 */
export interface ViewRecordVO {
  id: number;
  helpDocId: number;
  helpDocTitle?: string;
  userId: number;
  userName?: string;
  pageViewCount: number;
  firstViewTime: string;
  lastViewTime: string;
}

export const helpDocApi = {
  /**
   * 【管理】分頁查詢幫助文檔
   */
  query: (params: HelpDocQueryForm): Promise<ResponseDTO<PageResult<HelpDocVO>>> => {
    return request.post('/support/helpDoc/query', params);
  },

  /**
   * 【管理】添加幫助文檔
   */
  add: (data: HelpDocFormData): Promise<ResponseDTO<void>> => {
    return request.post('/support/helpDoc/add', data);
  },

  /**
   * 【管理】更新幫助文檔
   */
  update: (data: HelpDocFormData): Promise<ResponseDTO<void>> => {
    return request.post('/support/helpDoc/update', data);
  },

  /**
   * 【管理】刪除幫助文檔
   */
  delete: (helpDocId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/support/helpDoc/delete/${helpDocId}`);
  },

  /**
   * 【管理】獲取幫助文檔詳情
   */
  getDetail: (helpDocId: number): Promise<ResponseDTO<HelpDocDetailVO>> => {
    return request.get(`/support/helpDoc/getDetail/${helpDocId}`);
  },

  /**
   * 【管理】根據關聯ID查詢幫助文檔
   */
  queryHelpDocByRelationId: (relationId: number): Promise<ResponseDTO<HelpDocVO[]>> => {
    return request.get(`/support/helpDoc/queryHelpDocByRelationId/${relationId}`);
  },

  /**
   * 【用戶】查詢全部幫助文檔列表
   */
  getAllHelpDocList: (): Promise<ResponseDTO<HelpDocVO[]>> => {
    return request.get('/support/helpDoc/user/queryAllHelpDocList');
  },

  /**
   * 【用戶】查看幫助文檔
   */
  view: (helpDocId: number): Promise<ResponseDTO<HelpDocDetailVO>> => {
    return request.get(`/support/helpDoc/user/view/${helpDocId}`);
  },

  /**
   * 【用戶】查詢查看記錄
   */
  queryViewRecord: (
    params: ViewRecordQueryForm
  ): Promise<ResponseDTO<PageResult<ViewRecordVO>>> => {
    return request.post('/support/helpDoc/user/queryViewRecord', params);
  },
};
