import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface ChangeLogVO {
  changeLogId: number;
  updateVersion: string;
  type: number;
  publishAuthor: string;
  publicDate: string;
  content: string;
  link?: string;
  createTime: string;
  updateTime?: string;
}

export interface ChangeLogQueryForm {
  type?: number;
  keywords?: string;
  startDate?: string;
  endDate?: string;
  pageNum: number;
  pageSize: number;
}

export interface ChangeLogAddForm {
  updateVersion: string;
  type: number;
  publishAuthor: string;
  publicDate: string;
  content: string;
  link?: string;
}

export interface ChangeLogUpdateForm extends ChangeLogAddForm {
  changeLogId: number;
}

export const changeLogApi = {
  queryPage: (data: ChangeLogQueryForm) =>
    postRequest<PageResult<ChangeLogVO>>('/support/changeLog/queryPage', data),
  add: (data: ChangeLogAddForm) => postRequest<void>('/support/changeLog/add', data),
  update: (data: ChangeLogUpdateForm) => postRequest<void>('/support/changeLog/update', data),
  delete: (changeLogId: number) => getRequest<void>(`/support/changeLog/delete/${changeLogId}`),
  batchDelete: (idList: number[]) => postRequest<void>('/support/changeLog/batchDelete', idList),
};
