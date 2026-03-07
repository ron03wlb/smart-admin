/**
 * Position API
 *
 * Corresponds to Vue's api/system/position-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';
import type { PositionVO, PositionQueryForm, PositionAddForm, PositionUpdateForm } from '@/types/position.types';

export const positionApi = {
  /** Paginated query */
  queryPage: (data: PositionQueryForm) => postRequest<PageResult<PositionVO>>('/position/queryPage', data),

  /** Add position */
  add: (data: PositionAddForm) => postRequest<void>('/position/add', data),

  /** Update position */
  update: (data: PositionUpdateForm) => postRequest<void>('/position/update', data),

  /** Delete position */
  delete: (id: number) => getRequest<void>(`/position/delete/${id}`),

  /** Batch delete */
  batchDelete: (idList: number[]) => postRequest<void>('/position/batchDelete', idList),

  /** Query list (no pagination) */
  queryList: () => getRequest<PositionVO[]>('/position/queryList'),
};
