import { postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface HeartBeatVO {
  heartBeatId: number;
  projectPath: string;
  serverIp: string;
  processNo: string;
  processStartTime: string;
  heartBeatTime: string;
  createTime: string;
}

export interface HeartBeatQueryForm {
  keywords?: string;
  startDate?: string;
  endDate?: string;
  pageNum: number;
  pageSize: number;
}

export const heartBeatApi = {
  queryList: (data: HeartBeatQueryForm) => postRequest<PageResult<HeartBeatVO>>('/support/heartBeat/query', data),
};
