/**
 * Home Dashboard API
 *
 * Corresponds to Vue's api/system/home-api.ts
 */
import { getRequest } from '@/api/base/request';

export interface HomeAmountStatistics {
  todayOrderCount: number;
  totalOrderCount: number;
  todayAmount: number;
  totalAmount: number;
}

export interface HomeWaitHandle {
  waitMessageCount: number;
  waitNoticeCount: number;
}

export const homeApi = {
  /** Get amount statistics */
  getAmountStatistics: () => getRequest<HomeAmountStatistics>('/home/amount/statistics'),

  /** Get wait handle counts */
  getWaitHandle: () => getRequest<HomeWaitHandle>('/home/wait/handle'),
};
