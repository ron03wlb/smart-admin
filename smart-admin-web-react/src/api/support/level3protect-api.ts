import { getRequest, postRequest } from '@/api/base/request';

export interface Level3ProtectConfig {
  twoFactorLoginEnabled: boolean;
  loginFailMaxTimes: number;
  loginFailLockMinutes: number;
  loginActiveTimeoutMinutes: number;
  passwordComplexityEnabled: boolean;
  regularChangePasswordMonths: number;
  regularChangePasswordNotAllowRepeatTimes: number;
  fileDetectFlag: boolean;
  maxUploadFileSizeMb: number;
}

export interface DataMaskingDemoVO {
  userId: number;
  other: string;
  phone: string;
  idCard: string;
  password: string;
  email: string;
  carLicense: string;
  bankCard: string;
  address: string;
}

export const level3ProtectApi = {
  getConfig: () => getRequest<Level3ProtectConfig>('/support/protect/level3protect/getConfig'),
  updateConfig: (data: Level3ProtectConfig) =>
    postRequest<void>('/support/protect/level3protect/updateConfig', data),
};

export const dataMaskingApi = {
  query: () => getRequest<DataMaskingDemoVO[]>('/support/dataMasking/demo/query'),
};
