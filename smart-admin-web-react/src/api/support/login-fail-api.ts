import { postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface LoginFailVO {
  loginFailId: number;
  loginName: string;
  userType: number;
  loginFailCount: number;
  lockFlag: boolean;
  loginLockBeginTime?: string;
  createTime: string;
  updateTime?: string;
}

export interface LoginFailQueryForm {
  loginName?: string;
  lockFlag?: boolean;
  loginLockBeginTimeBegin?: string;
  loginLockBeginTimeEnd?: string;
  pageNum: number;
  pageSize: number;
}

export const loginFailApi = {
  queryPage: (data: LoginFailQueryForm) =>
    postRequest<PageResult<LoginFailVO>>('/support/protect/loginFail/queryPage', data),
  batchDelete: (idList: number[]) =>
    postRequest<void>('/support/protect/loginFail/batchDelete', idList),
};
