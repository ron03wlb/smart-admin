/**
 * Brand API Tests
 * 品牌 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { brandApi } from './brandApi';
import { BrandStatusEnum } from './types';

// Mock request utilities
vi.mock('@/utils/request', () => ({
  postRequest: vi.fn(),
  getRequest: vi.fn(),
}));

import { postRequest, getRequest } from '@/utils/request';

describe('brandApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should call queryBrand with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: { list: [], total: 0 } };
    (postRequest as any).mockResolvedValue(mockResponse);

    const queryForm = {
      pageNum: 1,
      pageSize: 10,
      keyword: 'Test Brand',
      status: BrandStatusEnum.ENABLED,
    };

    const result = await brandApi.queryBrand(queryForm);

    expect(postRequest).toHaveBeenCalledWith('/brand/query', queryForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call addBrand with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Brand added' };
    (postRequest as any).mockResolvedValue(mockResponse);

    const addForm = {
      brandName: 'New Brand',
      brandLogo: 'https://example.com/logo.png',
      description: 'Brand description',
      sort: 1,
      status: BrandStatusEnum.ENABLED,
    };

    const result = await brandApi.addBrand(addForm);

    expect(postRequest).toHaveBeenCalledWith('/brand/add', addForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call updateBrand with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Brand updated' };
    (postRequest as any).mockResolvedValue(mockResponse);

    const updateForm = {
      brandId: 1,
      brandName: 'Updated Brand',
      brandLogo: 'https://example.com/logo.png',
      description: 'Updated description',
      sort: 2,
      status: BrandStatusEnum.DISABLED,
    };

    const result = await brandApi.updateBrand(updateForm);

    expect(postRequest).toHaveBeenCalledWith('/brand/update', updateForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call batchDelete with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Brands deleted' };
    (postRequest as any).mockResolvedValue(mockResponse);

    const brandIdList = [1, 2, 3];

    const result = await brandApi.batchDelete(brandIdList);

    expect(postRequest).toHaveBeenCalledWith('/brand/batchDelete', brandIdList);
    expect(result).toEqual(mockResponse);
  });

  it('should call getById with correct parameters', async () => {
    const mockResponse = {
      ok: true,
      code: 1,
      msg: 'Success',
      data: {
        brandId: 1,
        brandName: 'Test Brand',
        brandLogo: 'https://example.com/logo.png',
        description: 'Test description',
        sort: 1,
        status: BrandStatusEnum.ENABLED,
        updateTime: '2026-03-26T10:00:00Z',
        createTime: '2026-03-26T10:00:00Z',
      },
    };
    (getRequest as any).mockResolvedValue(mockResponse);

    const result = await brandApi.getById(1);

    expect(getRequest).toHaveBeenCalledWith('/brand/get/1');
    expect(result).toEqual(mockResponse);
  });
});
