/**
 * Notice Management API
 * 通知公告管理 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type {
  NoticeVO,
  NoticeQueryForm,
  NoticeAddForm,
  NoticeUpdateForm,
  NoticeTypeVO,
} from '@/views/business/notice/types';

/**
 * 通知公告管理 API
 */
export const noticeApi = {
  // ---------------- 通知公告類型 -----------------------

  /**
   * 獲取所有通知公告類型
   */
  getAllNoticeTypeList: (): Promise<ResponseDTO<NoticeTypeVO[]>> => {
    return request.get('/oa/noticeType/getAll');
  },

  // ---------------- 通知公告管理 -----------------------

  /**
   * 分頁查詢通知公告列表
   * @param params 查詢條件
   */
  queryNotice: (params: NoticeQueryForm): Promise<ResponseDTO<PageResult<NoticeVO>>> => {
    return request.post('/oa/notice/query', params);
  },

  /**
   * 新增通知公告
   * @param params 新增表單數據
   */
  addNotice: (params: NoticeAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/oa/notice/add', params);
  },

  /**
   * 更新通知公告
   * @param params 更新表單數據
   */
  updateNotice: (params: NoticeUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/oa/notice/update', params);
  },

  /**
   * 刪除通知公告
   * @param noticeId 公告ID
   */
  deleteNotice: (noticeId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/oa/notice/delete/${noticeId}`);
  },

  /**
   * 獲取更新公告信息
   * @param noticeId 公告ID
   */
  getUpdateNoticeInfo: (noticeId: number): Promise<ResponseDTO<NoticeVO>> => {
    return request.get(`/oa/notice/getUpdateVO/${noticeId}`);
  },

  // ---------------- 員工查看通知公告 -----------------------

  /**
   * 員工查詢通知公告列表
   * @param params 查詢條件
   */
  queryEmployeeNotice: (params: NoticeQueryForm): Promise<ResponseDTO<PageResult<NoticeVO>>> => {
    return request.post('/oa/notice/employee/query', params);
  },

  /**
   * 員工查看通知公告詳情
   * @param noticeId 公告ID
   */
  viewEmployeeNotice: (noticeId: number): Promise<ResponseDTO<NoticeVO>> => {
    return request.get(`/oa/notice/employee/view/${noticeId}`);
  },
};
