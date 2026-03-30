/**
 * File Constants
 * 文件類型常量
 *
 * 參考：Vue 版本 smart-admin-web/src/constants/support/file-const.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import type { SmartEnum } from '@/types/smart-enum';

/**
 * 文件上傳文件夾類型枚舉
 */
export const FILE_FOLDER_TYPE_ENUM: SmartEnum<number> = {
  COMMON: {
    value: 1,
    desc: '通用',
  },
  NOTICE: {
    value: 2,
    desc: '公告',
  },
  HELP_DOC: {
    value: 3,
    desc: '幫助中心',
  },
  FEEDBACK: {
    value: 4,
    desc: '意見反饋',
  },
};
