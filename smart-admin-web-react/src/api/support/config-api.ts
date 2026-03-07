import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface ConfigVO {
  configId: number;
  configKey: string;
  configName: string;
  configValue: string;
  remark?: string;
  createTime: string;
  updateTime?: string;
}

export interface ConfigQueryForm {
  configKey?: string;
  pageNum: number;
  pageSize: number;
}

export interface ConfigAddForm {
  configKey: string;
  configName: string;
  configValue: string;
  remark?: string;
}

export interface ConfigUpdateForm extends ConfigAddForm {
  configId: number;
}

export const configApi = {
  queryList: (data: ConfigQueryForm) => postRequest<PageResult<ConfigVO>>('/support/config/query', data),
  addConfig: (data: ConfigAddForm) => postRequest<void>('/support/config/add', data),
  updateConfig: (data: ConfigUpdateForm) => postRequest<void>('/support/config/update', data),
  queryByKey: (configKey: string) => getRequest<ConfigVO>('/support/config/queryByKey', { configKey }),
};
