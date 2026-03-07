import { getRequest, postRequest } from '@/api/base/request';

export interface ReloadVO {
  tag: string;
  identification: string;
  args: string;
  updateTime?: string;
  createTime: string;
}

export interface ReloadResultVO {
  tag: string;
  args: string;
  result: boolean;
  exception?: string;
  createTime: string;
}

export const reloadApi = {
  queryList: () => getRequest<ReloadVO[]>('/support/reload/query'),
  queryReloadResult: (tag: string) => getRequest<ReloadResultVO[]>(`/support/reload/result/${tag}`),
  reload: (data: { tag: string; identification: string; args: string }) =>
    postRequest<void>('/support/reload/update', data),
};
