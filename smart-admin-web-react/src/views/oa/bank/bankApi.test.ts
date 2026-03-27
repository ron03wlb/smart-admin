/**
 * Bank API Tests
 * 銀行信息 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { bankApi } from './bankApi';

// Mock request utilities
vi.mock('@/utils/request', () => ({
  postRequest: vi.fn(),
  getRequest: vi.fn(),
}));

import { postRequest, getRequest } from '@/utils/request';

describe('bankApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should call queryByPage with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: { list: [], total: 0 } };
    (postRequest as any).mockResolvedValue(mockResponse);

    const queryForm = {
      pageNum: 1,
      pageSize: 10,
      keywords: 'Test Bank',
      enterpriseId: 1,
      disabledFlag: false,
    };

    const result = await bankApi.queryByPage(queryForm);

    expect(postRequest).toHaveBeenCalledWith('/oa/bank/page/query', queryForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call queryList with correct parameters', async () => {
    const mockResponse = {
      ok: true,
      code: 1,
      msg: 'Success',
      data: [
        {
          bankId: 1,
          bankName: 'Test Bank',
          accountName: 'Test Account',
          accountNumber: '1234567890',
          businessFlag: true,
          enterpriseId: 1,
          enterpriseName: 'Test Enterprise',
          disabledFlag: false,
          createUserId: 1,
          createUserName: 'Admin',
          createTime: '2026-03-26T10:00:00Z',
          updateTime: '2026-03-26T10:00:00Z',
        },
      ],
    };
    (getRequest as any).mockResolvedValue(mockResponse);

    const result = await bankApi.queryList(1);

    expect(getRequest).toHaveBeenCalledWith('/oa/bank/query/list/1');
    expect(result).toEqual(mockResponse);
  });

  it('should call getDetail with correct parameters', async () => {
    const mockResponse = {
      ok: true,
      code: 1,
      msg: 'Success',
      data: {
        bankId: 1,
        bankName: 'Test Bank',
        accountName: 'Test Account',
        accountNumber: '1234567890',
        remark: 'Test remark',
        businessFlag: true,
        enterpriseId: 1,
        enterpriseName: 'Test Enterprise',
        disabledFlag: false,
        createUserId: 1,
        createUserName: 'Admin',
        createTime: '2026-03-26T10:00:00Z',
        updateTime: '2026-03-26T10:00:00Z',
      },
    };
    (getRequest as any).mockResolvedValue(mockResponse);

    const result = await bankApi.getDetail(1);

    expect(getRequest).toHaveBeenCalledWith('/oa/bank/get/1');
    expect(result).toEqual(mockResponse);
  });

  it('should call createBank with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Bank created' };
    (postRequest as any).mockResolvedValue(mockResponse);

    const createForm = {
      bankName: 'New Bank',
      accountName: 'New Account',
      accountNumber: '9876543210',
      remark: 'New bank remark',
      businessFlag: false,
      enterpriseId: 2,
      disabledFlag: false,
    };

    const result = await bankApi.createBank(createForm);

    expect(postRequest).toHaveBeenCalledWith('/oa/bank/create', createForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call updateBank with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Bank updated' };
    (postRequest as any).mockResolvedValue(mockResponse);

    const updateForm = {
      bankId: 1,
      bankName: 'Updated Bank',
      accountName: 'Updated Account',
      accountNumber: '1111222233',
      remark: 'Updated remark',
      businessFlag: true,
      enterpriseId: 1,
      disabledFlag: false,
    };

    const result = await bankApi.updateBank(updateForm);

    expect(postRequest).toHaveBeenCalledWith('/oa/bank/update', updateForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call deleteBank with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Bank deleted' };
    (getRequest as any).mockResolvedValue(mockResponse);

    const result = await bankApi.deleteBank(1);

    expect(getRequest).toHaveBeenCalledWith('/oa/bank/delete/1');
    expect(result).toEqual(mockResponse);
  });
});
