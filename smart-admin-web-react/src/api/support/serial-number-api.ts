import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface SerialNumberVO {
  serialNumberId: number;
  businessName: string;
  format: string;
  ruleType: number;
  initNumber: number;
  stepRandomRange: number;
  remark?: string;
  lastNumber?: string;
  lastTime?: string;
}

export interface SerialNumberRecordVO {
  serialNumberId: number;
  recordDate: string;
  count: number;
  lastNumber: string;
  lastTime: string;
}

export interface SerialNumberGenerateForm {
  serialNumberId: number;
  count: number;
}

export const serialNumberApi = {
  getAll: () => getRequest<SerialNumberVO[]>('/support/serialNumber/all'),
  generate: (data: SerialNumberGenerateForm) => postRequest<string[]>('/support/serialNumber/generate', data),
  queryRecord: (data: { serialNumberId: number; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<SerialNumberRecordVO>>('/support/serialNumber/queryRecord', data),
};
