/**
 * Common Constants
 * 通用常量
 *
 * 參考：Vue 版本 smart-admin-web/src/constants/common-const.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { SmartEnum } from '@/types/smart-enum';

// ==================== 分頁相關 ====================

export const PAGE_SIZE = 10;

export const PAGE_SIZE_OPTIONS = [
  '5',
  '10',
  '15',
  '20',
  '30',
  '40',
  '50',
  '75',
  '100',
  '150',
  '200',
  '300',
  '500',
];

export const showTableTotal = (total: number | string) => {
  return `共${total}條`;
};

// ==================== 路由路徑 ====================

/** 登錄頁路徑 */
export const PAGE_PATH_LOGIN = '/login';

/** 首頁路徑 */
export const HOME_PAGE_PATH = '/home';

/** 404頁路徑 */
export const PAGE_PATH_404 = '/404';

// ==================== 枚舉常量 ====================

/**
 * 是否枚舉（數字類型）
 */
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

/**
 * 性別枚舉
 */
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

/**
 * 用戶類型枚舉
 */
export const USER_TYPE_ENUM: SmartEnum<number> = {
  ADMIN_EMPLOYEE: {
    value: 1,
    desc: '員工',
  },
};

/**
 * 數據類型枚舉
 */
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

/**
 * 用戶狀態枚舉
 */
export const USER_STATUS_ENUM: SmartEnum<number> = {
  ENABLED: {
    value: 1,
    desc: '啟用',
  },
  DISABLED: {
    value: 0,
    desc: '禁用',
  },
};

/**
 * 刪除標記枚舉
 */
export const DELETED_FLAG_ENUM: SmartEnum<number> = {
  NOT_DELETED: {
    value: 0,
    desc: '未刪除',
  },
  DELETED: {
    value: 1,
    desc: '已刪除',
  },
};
