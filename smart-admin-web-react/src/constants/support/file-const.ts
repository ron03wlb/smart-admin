/**
 * File Constants
 *
 * Corresponds to Vue's constants/support/file-const.ts
 */
import type { SmartEnum } from '@/types/smart-enum.types';

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
    desc: '帮助中心',
  },
  FEEDBACK: {
    value: 4,
    desc: '意见反馈',
  },
};

export default {
  FILE_FOLDER_TYPE_ENUM,
};
