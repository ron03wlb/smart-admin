import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface NoticeTypeVO {
  noticeTypeId: number;
  noticeTypeName: string;
}

export interface NoticeVO {
  noticeId: number;
  noticeTypeId: number;
  noticeTypeName?: string;
  title: string;
  allVisibleFlag: boolean;
  scheduledPublishFlag: boolean;
  publishTime?: string;
  contentHtml?: string;
  contentText?: string;
  author: string;
  source?: string;
  documentNumber?: string;
  pageViewCount?: number;
  userViewCount?: number;
  createTime: string;
  updateTime?: string;
  attachment?: string;
}

export interface NoticeQueryForm {
  noticeTypeId?: number;
  keywords?: string;
  documentNumber?: string;
  author?: string;
  startDate?: string;
  endDate?: string;
  createStartDate?: string;
  createEndDate?: string;
  pageNum: number;
  pageSize: number;
}

export interface NoticeAddForm {
  noticeTypeId: number;
  title: string;
  allVisibleFlag: boolean;
  scheduledPublishFlag: boolean;
  publishTime?: string;
  contentHtml: string;
  contentText?: string;
  author: string;
  source?: string;
  documentNumber?: string;
  attachment?: string;
  visibleRangeList?: { dataType: number; dataId: number }[];
}

export interface NoticeUpdateForm extends NoticeAddForm {
  noticeId: number;
}

export interface NoticeEmployeeVO {
  noticeId: number;
  title: string;
  noticeTypeName?: string;
  publishTime: string;
  author: string;
  readFlag: boolean;
  createTime: string;
}

export interface NoticeViewRecordVO {
  employeeId: number;
  employeeName: string;
  departmentName?: string;
  pageViewCount: number;
  firstIp?: string;
  firstTime?: string;
  lastIp?: string;
  lastTime?: string;
}

export const noticeTypeApi = {
  getAll: () => getRequest<NoticeTypeVO[]>('/oa/noticeType/getAll'),
  add: (name: string) => getRequest<void>(`/oa/noticeType/add/${name}`),
  update: (noticeTypeId: number, name: string) =>
    getRequest<void>(`/oa/noticeType/update/${noticeTypeId}/${name}`),
  delete: (noticeTypeId: number) => getRequest<void>(`/oa/noticeType/delete/${noticeTypeId}`),
};

export const noticeApi = {
  query: (data: NoticeQueryForm) => postRequest<PageResult<NoticeVO>>('/oa/notice/query', data),
  add: (data: NoticeAddForm) => postRequest<void>('/oa/notice/add', data),
  update: (data: NoticeUpdateForm) => postRequest<void>('/oa/notice/update', data),
  delete: (noticeId: number) => getRequest<void>(`/oa/notice/delete/${noticeId}`),
  getUpdateVO: (noticeId: number) => getRequest<NoticeVO>(`/oa/notice/getUpdateVO/${noticeId}`),
  employeeView: (noticeId: number) => getRequest<NoticeVO>(`/oa/notice/employee/view/${noticeId}`),
  employeeQuery: (data: { keywords?: string; noticeTypeId?: number; readFlag?: boolean; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<NoticeEmployeeVO>>('/oa/notice/employee/query', data),
  queryViewRecord: (data: { noticeId: number; departmentId?: number; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<NoticeViewRecordVO>>('/oa/notice/employee/queryViewRecord', data),
};
