/**
 * Invoice API Tests
 * 發票信息 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { invoiceApi } from './invoiceApi';

// Mock request utilities
vi.mock('@/utils/request', () => ({
  postRequest: vi.fn(),
  getRequest: vi.fn(),
}));

import { postRequest, getRequest } from '@/utils/request';

describe('invoiceApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should call queryByPage with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: { list: [], total: 0 } };
    (postRequest as any).mockResolvedValue(mockResponse);

    const queryForm = {
      pageNum: 1,
      pageSize: 10,
      keywords: 'Test Invoice',
      enterpriseId: 1,
      disabledFlag: false,
    };

    const result = await invoiceApi.queryByPage(queryForm);

    expect(postRequest).toHaveBeenCalledWith('/oa/invoice/page/query', queryForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call queryList with correct parameters', async () => {
    const mockResponse = {
      ok: true,
      code: 1,
      msg: 'Success',
      data: [
        {
          invoiceId: 1,
          invoiceHeads: 'Test Company',
          taxpayerIdentificationNumber: '123456789',
          accountNumber: '1234567890',
          bankName: 'Test Bank',
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

    const result = await invoiceApi.queryList(1);

    expect(getRequest).toHaveBeenCalledWith('/oa/invoice/query/list/1');
    expect(result).toEqual(mockResponse);
  });

  it('should call getDetail with correct parameters', async () => {
    const mockResponse = {
      ok: true,
      code: 1,
      msg: 'Success',
      data: {
        invoiceId: 1,
        invoiceHeads: 'Test Company',
        taxpayerIdentificationNumber: '123456789',
        accountNumber: '1234567890',
        bankName: 'Test Bank',
        remark: 'Test remark',
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

    const result = await invoiceApi.getDetail(1);

    expect(getRequest).toHaveBeenCalledWith('/oa/invoice/get/1');
    expect(result).toEqual(mockResponse);
  });

  it('should call createInvoice with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Invoice created' };
    (postRequest as any).mockResolvedValue(mockResponse);

    const addForm = {
      invoiceHeads: 'New Company',
      taxpayerIdentificationNumber: '987654321',
      accountNumber: '9876543210',
      bankName: 'New Bank',
      disabledFlag: false,
      remark: 'New invoice remark',
      enterpriseId: 2,
    };

    const result = await invoiceApi.createInvoice(addForm);

    expect(postRequest).toHaveBeenCalledWith('/oa/invoice/create', addForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call updateInvoice with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Invoice updated' };
    (postRequest as any).mockResolvedValue(mockResponse);

    const updateForm = {
      invoiceId: 1,
      invoiceHeads: 'Updated Company',
      taxpayerIdentificationNumber: '111222333',
      accountNumber: '1111222233',
      bankName: 'Updated Bank',
      disabledFlag: false,
      remark: 'Updated remark',
      enterpriseId: 1,
    };

    const result = await invoiceApi.updateInvoice(updateForm);

    expect(postRequest).toHaveBeenCalledWith('/oa/invoice/update', updateForm);
    expect(result).toEqual(mockResponse);
  });

  it('should call deleteInvoice with correct parameters', async () => {
    const mockResponse = { ok: true, code: 1, msg: 'Success', data: 'Invoice deleted' };
    (getRequest as any).mockResolvedValue(mockResponse);

    const result = await invoiceApi.deleteInvoice(1);

    expect(getRequest).toHaveBeenCalledWith('/invoice/delete/1');
    expect(result).toEqual(mockResponse);
  });
});
