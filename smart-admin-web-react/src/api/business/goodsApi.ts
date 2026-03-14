/**
 * Goods Management API
 * 商品管理 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import request from '@/utils/request';
import type { ResponseDTO, PageResult } from '@/api/types/response';
import type { GoodsVO, GoodsQueryForm, GoodsAddForm, GoodsUpdateForm } from '@/views/business/goods/types';

/**
 * 商品管理 API
 */
export const goodsApi = {
  /**
   * 分頁查詢商品列表
   * @param params 查詢條件
   */
  queryGoodsList: (params: GoodsQueryForm): Promise<ResponseDTO<PageResult<GoodsVO>>> => {
    return request.post('/goods/query', params);
  },

  /**
   * 新增商品
   * @param params 新增表單數據
   */
  addGoods: (params: GoodsAddForm): Promise<ResponseDTO<void>> => {
    return request.post('/goods/add', params);
  },

  /**
   * 更新商品
   * @param params 更新表單數據
   */
  updateGoods: (params: GoodsUpdateForm): Promise<ResponseDTO<void>> => {
    return request.post('/goods/update', params);
  },

  /**
   * 刪除商品
   * @param goodsId 商品ID
   */
  deleteGoods: (goodsId: number): Promise<ResponseDTO<void>> => {
    return request.get(`/goods/delete/${goodsId}`);
  },

  /**
   * 批量刪除商品
   * @param goodsIdList 商品ID列表
   */
  batchDelete: (goodsIdList: number[]): Promise<ResponseDTO<void>> => {
    return request.post('/goods/batchDelete', goodsIdList);
  },
};
