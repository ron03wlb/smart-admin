import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface GoodsVO {
  goodsId: number;
  categoryId: number;
  categoryName?: string;
  goodsName: string;
  goodsStatus: number;
  place: string;
  price: number;
  shelvesFlag: boolean;
  remark?: string;
  createTime: string;
  updateTime?: string;
}

export interface GoodsQueryForm {
  categoryId?: number;
  goodsName?: string;
  goodsStatus?: number;
  place?: string;
  shelvesFlag?: boolean;
  pageNum: number;
  pageSize: number;
}

export interface GoodsAddForm {
  categoryId: number;
  goodsName: string;
  goodsStatus?: number;
  place?: string;
  price?: number;
  shelvesFlag?: boolean;
  remark?: string;
}

export interface GoodsUpdateForm extends GoodsAddForm {
  goodsId: number;
}

export const goodsApi = {
  query: (data: GoodsQueryForm) => postRequest<PageResult<GoodsVO>>('/goods/query', data),
  add: (data: GoodsAddForm) => postRequest<void>('/goods/add', data),
  update: (data: GoodsUpdateForm) => postRequest<void>('/goods/update', data),
  delete: (goodsId: number) => getRequest<void>(`/goods/delete/${goodsId}`),
  batchDelete: (idList: number[]) => postRequest<void>('/goods/batchDelete', idList),
  exportGoods: () => getRequest<void>('/goods/exportGoods'),
};
