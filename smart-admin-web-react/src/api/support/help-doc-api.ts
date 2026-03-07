import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface HelpDocCatalogVO {
  helpDocCatalogId: number;
  name: string;
  parentId: number;
  sort: number;
  children?: HelpDocCatalogVO[];
}

export interface HelpDocVO {
  helpDocId: number;
  helpDocCatalogId: number;
  helpDocCatalogName?: string;
  title: string;
  author: string;
  sort: number;
  contentHtml?: string;
  contentText?: string;
  pageViewCount: number;
  userViewCount: number;
  createTime: string;
  updateTime?: string;
  attachment?: string;
  relationList?: { relationId: number; relationName: string }[];
}

export interface HelpDocQueryForm {
  helpDocCatalogId?: number;
  keywords?: string;
  startDate?: string;
  endDate?: string;
  pageNum: number;
  pageSize: number;
}

export interface HelpDocAddForm {
  helpDocCatalogId: number;
  title: string;
  author: string;
  sort?: number;
  contentHtml: string;
  contentText?: string;
  attachment?: string;
  relationList?: { relationId: number; relationName: string }[];
}

export interface HelpDocUpdateForm extends HelpDocAddForm {
  helpDocId: number;
}

export const helpDocCatalogApi = {
  getAll: () => getRequest<HelpDocCatalogVO[]>('/support/helpDocCatalog/getAll'),
  add: (data: { name: string; parentId: number; sort?: number }) =>
    postRequest<void>('/support/helpDocCatalog/add', data),
  update: (data: { helpDocCatalogId: number; name: string; parentId: number; sort?: number }) =>
    postRequest<void>('/support/helpDocCatalog/update', data),
  delete: (helpDocCatalogId: number) =>
    getRequest<void>(`/support/helpDocCatalog/delete/${helpDocCatalogId}`),
};

export const helpDocApi = {
  query: (data: HelpDocQueryForm) =>
    postRequest<PageResult<HelpDocVO>>('/support/helpDoc/query', data),
  add: (data: HelpDocAddForm) => postRequest<void>('/support/helpDoc/add', data),
  update: (data: HelpDocUpdateForm) => postRequest<void>('/support/helpDoc/update', data),
  delete: (helpDocId: number) => getRequest<void>(`/support/helpDoc/delete/${helpDocId}`),
  getDetail: (helpDocId: number) => getRequest<HelpDocVO>(`/support/helpDoc/getDetail/${helpDocId}`),
  queryAllForUser: () => getRequest<HelpDocVO[]>('/support/helpDoc/user/queryAllHelpDocList'),
  userView: (helpDocId: number) => getRequest<HelpDocVO>(`/support/helpDoc/user/view/${helpDocId}`),
  queryViewRecord: (data: { helpDocId: number; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<any>>('/support/helpDoc/user/queryViewRecord', data),
};
