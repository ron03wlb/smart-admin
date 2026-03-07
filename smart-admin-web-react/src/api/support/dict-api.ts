/**
 * Dictionary API
 *
 * Corresponds to Vue's api/support/dict-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';
import type { DictVO, DictDataVO, DictQueryForm, DictAddForm, DictUpdateForm, DictDataAddForm, DictDataUpdateForm } from '@/types/dict.types';

export const dictApi = {
  /** Get all dict codes */
  getAllDict: () => getRequest<DictVO[]>('/support/dict/getAllDict'),

  /** Get all dict data (for cache) */
  getAllDictData: () => getRequest<DictDataVO[]>('/support/dict/getAllDictData'),

  /** Paginated query */
  queryPage: (data: DictQueryForm) => postRequest<PageResult<DictVO>>('/support/dict/queryPage', data),

  /** Add dict */
  add: (data: DictAddForm) => postRequest<void>('/support/dict/add', data),

  /** Update dict */
  update: (data: DictUpdateForm) => postRequest<void>('/support/dict/update', data),

  /** Batch delete */
  batchDelete: (dictIdList: number[]) => postRequest<void>('/support/dict/batchDelete', dictIdList),

  /** Toggle disabled */
  updateDisabled: (dictId: number) => getRequest<void>(`/support/dict/updateDisabled/${dictId}`),

  // --- Dict Data sub-API ---

  /** Query dict data by dict ID */
  queryDictData: (dictId: number) => getRequest<DictDataVO[]>(`/support/dict/dictData/queryDictData/${dictId}`),

  /** Add dict data */
  addDictData: (data: DictDataAddForm) => postRequest<void>('/support/dict/dictData/add', data),

  /** Update dict data */
  updateDictData: (data: DictDataUpdateForm) => postRequest<void>('/support/dict/dictData/update', data),

  /** Batch delete dict data */
  batchDeleteDictData: (dictDataIdList: number[]) => postRequest<void>('/support/dict/dictData/batchDelete', dictDataIdList),

  /** Toggle dict data disabled */
  updateDictDataDisabled: (dictDataId: number) => getRequest<void>(`/support/dict/dictData/updateDisabled/${dictDataId}`),
};
