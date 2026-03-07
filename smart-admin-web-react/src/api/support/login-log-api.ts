/**
 * Login Log API
 *
 * Corresponds to Vue's api/support/login-log-api.ts
 */
import { postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface LoginLogVO {
  loginLogId: number;
  userId: number;
  userName: string;
  userAgent: string;
  loginIp: string;
  loginIpRegion?: string;
  loginType: number;
  loginResult: number;
  createTime: string;
  remark?: string;
}

export interface LoginLogQueryForm {
  pageNum: number;
  pageSize: number;
  startDate?: string;
  endDate?: string;
}

export const loginLogApi = {
  /** Query all login logs (admin) */
  queryList: (data: LoginLogQueryForm) => postRequest<PageResult<LoginLogVO>>('/support/loginLog/page/query', data),

  /** Query current user's login logs */
  queryListLogin: (data: LoginLogQueryForm) => postRequest<PageResult<LoginLogVO>>('/support/loginLog/page/query/login', data),
};
