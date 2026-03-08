/**
 * Operate Log API
 *
 * Corresponds to Vue's api/support/operate-log-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface OperateLogVO {
  operateLogId: number;
  operateUserId: number;
  operateUserName: string;
  module: string;
  content: string;
  url: string;
  method: string;
  param?: string;
  result?: string;
  ip: string;
  ipRegion?: string;
  userAgent: string;
  successFlag: boolean;
  failReason?: string;
  createTime: string;
}

export interface OperateLogQueryForm {
  pageNum: number;
  pageSize: number;
  keywords?: string;
  operateUserName?: string;
  startDate?: string;
  endDate?: string;
  successFlag?: boolean;
}

export const operateLogApi = {
  /** Query all operate logs (admin) */
  queryList: (data: OperateLogQueryForm) => postRequest<PageResult<OperateLogVO>>('/support/operateLog/page/query', data),

  /** Get operate log detail */
  detail: (id: number) => getRequest<OperateLogVO>(`/support/operateLog/detail/${id}`),

  /** Query current user's operate logs */
  queryListLogin: (data: OperateLogQueryForm) => postRequest<PageResult<OperateLogVO>>('/support/operateLog/page/query/login', data),
};
