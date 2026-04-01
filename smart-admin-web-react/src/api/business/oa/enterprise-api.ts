import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';

export interface EnterpriseVO {
  enterpriseId: number;
  enterpriseName: string;
  enterpriseLogo?: string;
  unifiedSocialCreditCode?: string;
  type?: number;
  /** Backend field name is 'contact' (not 'contactName') */
  contact?: string;
  contactPhone?: string;
  email?: string;
  /** Province code (integer) */
  province?: number;
  provinceName?: string;
  /** City code (integer) */
  city?: number;
  cityName?: string;
  /** District code (integer) */
  district?: number;
  districtName?: string;
  address?: string;
  businessLicense?: string;
  disabledFlag: boolean;
  createTime: string;
  updateTime?: string;
}

export interface EnterpriseQueryForm {
  keywords?: string;
  type?: number;
  disabledFlag?: boolean;
  pageNum: number;
  pageSize: number;
}

export interface EnterpriseAddForm {
  enterpriseName: string;
  enterpriseLogo?: string;
  unifiedSocialCreditCode?: string;
  type?: number;
  /** Backend field: contact (required) */
  contact?: string;
  contactPhone?: string;
  email?: string;
  province?: number;
  provinceName?: string;
  city?: number;
  cityName?: string;
  district?: number;
  districtName?: string;
  address?: string;
  businessLicense?: string;
  disabledFlag?: boolean;
}

export interface EnterpriseUpdateForm extends EnterpriseAddForm {
  enterpriseId: number;
}

export interface EnterpriseEmployeeVO {
  employeeId: number;
  actualName: string;
  departmentName?: string;
  phone?: string;
}

export interface BankVO {
  bankId: number;
  enterpriseId: number;
  bankName: string;
  accountName: string;
  accountNumber: string;
  publicFlag?: boolean;
  disabledFlag: boolean;
  remark?: string;
  createTime: string;
}

export interface InvoiceVO {
  invoiceId: number;
  enterpriseId: number;
  invoiceHeads: string;
  taxpayerIdentificationNumber: string;
  accountNumber?: string;
  bankName?: string;
  disabledFlag: boolean;
  remark?: string;
  createTime: string;
}

export const enterpriseApi = {
  pageQuery: (data: EnterpriseQueryForm) =>
    postRequest<PageResult<EnterpriseVO>>('/oa/enterprise/page/query', data),
  detail: (enterpriseId: number) =>
    getRequest<EnterpriseVO>(`/oa/enterprise/get/${enterpriseId}`),
  create: (data: EnterpriseAddForm) => postRequest<void>('/oa/enterprise/create', data),
  update: (data: EnterpriseUpdateForm) => postRequest<void>('/oa/enterprise/update', data),
  delete: (enterpriseId: number) => getRequest<void>(`/oa/enterprise/delete/${enterpriseId}`),
  queryList: (type?: number) =>
    getRequest<EnterpriseVO[]>(`/oa/enterprise/query/list${type ? `?type=${type}` : ''}`),
};

export const enterpriseEmployeeApi = {
  queryPage: (data: { enterpriseId: number; keywords?: string; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<EnterpriseEmployeeVO>>('/oa/enterprise/employee/queryPage', data),
  add: (data: { enterpriseId: number; employeeIdList: number[] }) =>
    postRequest<void>('/oa/enterprise/employee/add', data),
  delete: (data: { enterpriseId: number; employeeId: number }) =>
    postRequest<void>('/oa/enterprise/employee/delete', data),
};

export const bankApi = {
  pageQuery: (data: { enterpriseId: number; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<BankVO>>('/oa/bank/page/query', data),
  create: (data: Omit<BankVO, 'bankId' | 'createTime'>) => postRequest<void>('/oa/bank/create', data),
  update: (data: BankVO) => postRequest<void>('/oa/bank/update', data),
  delete: (bankId: number) => getRequest<void>(`/oa/bank/delete/${bankId}`),
};

export const invoiceApi = {
  pageQuery: (data: { enterpriseId: number; pageNum: number; pageSize: number }) =>
    postRequest<PageResult<InvoiceVO>>('/oa/invoice/page/query', data),
  create: (data: Omit<InvoiceVO, 'invoiceId' | 'createTime'>) => postRequest<void>('/oa/invoice/create', data),
  update: (data: InvoiceVO) => postRequest<void>('/oa/invoice/update', data),
  delete: (invoiceId: number) => getRequest<void>(`/oa/invoice/delete/${invoiceId}`),
};
