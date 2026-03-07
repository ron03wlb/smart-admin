/**
 * Common Constants
 *
 * Corresponds to Vue's constants/common-const.ts
 */
import type { SmartEnum } from '@/types/smart-enum.types';

export const PAGE_SIZE = 10;

export const PAGE_SIZE_OPTIONS = ['5', '10', '15', '20', '30', '40', '50', '75', '100', '150', '200', '300', '500'];

export const PAGE_PATH_LOGIN = '/login';
export const HOME_PAGE_PATH = '/home';
export const PAGE_PATH_404 = '/404';

export const showTableTotal = (total: number | string) => `共${total}条`;

export const FLAG_NUMBER_ENUM: SmartEnum<number> = {
  TRUE: {
    value: 1,
    desc: '是',
  },
  FALSE: {
    value: 0,
    desc: '否',
  },
};

export const GENDER_ENUM: SmartEnum<number> = {
  UNKNOWN: {
    value: 0,
    desc: '未知',
  },
  MAN: {
    value: 1,
    desc: '男',
  },
  WOMAN: {
    value: 2,
    desc: '女',
  },
};

export const USER_TYPE_ENUM: SmartEnum<number> = {
  ADMIN_EMPLOYEE: {
    value: 1,
    desc: '员工',
  },
};

export const DATA_TYPE_ENUM: SmartEnum<number> = {
  NORMAL: {
    value: 1,
    desc: '普通',
  },
  ENCRYPT: {
    value: 10,
    desc: '加密',
  },
};
