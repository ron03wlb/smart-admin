import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface JobVO {
  jobId: number;
  jobName: string;
  jobClass: string;
  triggerType: string;
  triggerValue: string;
  param?: string;
  enabledFlag: boolean;
  remark?: string;
  sort: number;
  updateName?: string;
  updateTime?: string;
  deletedFlag?: boolean;
  triggerTypeDesc?: string;
  lastJobLog?: { successFlag: boolean; executeResult?: string; executeStartTime?: string };
  nextJobExecuteTimeList?: string[];
}

export interface JobLogVO {
  jobLogId: number;
  jobId: number;
  param?: string;
  successFlag: boolean;
  executeStartTime: string;
  executeEndTime: string;
  executeTimeMillis: number;
  executeResult?: string;
  createName?: string;
  ip?: string;
  processId?: string;
  programPath?: string;
}

export interface JobQueryForm {
  keywords?: string;
  triggerType?: string;
  enabledFlag?: boolean;
  deletedFlag?: boolean;
  pageNum: number;
  pageSize: number;
}

export interface JobAddForm {
  jobName: string;
  jobClass: string;
  triggerType: string;
  triggerValue: string;
  param?: string;
  enabledFlag: boolean;
  remark?: string;
  sort?: number;
}

export interface JobUpdateForm extends JobAddForm {
  jobId: number;
}

export const jobApi = {
  query: (data: JobQueryForm) => postRequest<PageResult<JobVO>>('/support/job/query', data),
  getDetail: (jobId: number) => getRequest<JobVO>(`/support/job/${jobId}`),
  add: (data: JobAddForm) => postRequest<void>('/support/job/add', data),
  update: (data: JobUpdateForm) => postRequest<void>('/support/job/update', data),
  updateEnabled: (data: { jobId: number; enabledFlag: boolean }) =>
    postRequest<void>('/support/job/update/enabled', data),
  execute: (data: { jobId: number; param?: string }) => postRequest<void>('/support/job/execute', data),
  delete: (jobId: number) => getRequest<void>(`/support/job/delete?jobId=${jobId}`),
  queryLog: (data: { jobId: number; keywords?: string; successFlag?: boolean; startDate?: string; endDate?: string; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<JobLogVO>>('/support/job/log/query', data),
};
