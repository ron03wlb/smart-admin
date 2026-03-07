/**
 * Menu Constants
 *
 * Corresponds to Vue's constants/system/menu-const.ts
 */
import type { SmartEnum } from '@/types/smart-enum.types';

export const MENU_TYPE_ENUM: SmartEnum<number> = {
  CATALOG: {
    value: 1,
    desc: '目录',
  },
  MENU: {
    value: 2,
    desc: '菜单',
  },
  POINTS: {
    value: 3,
    desc: '功能点',
  },
};

export const MENU_PERMS_TYPE_ENUM: SmartEnum<number> = {
  SA_TOKEN: {
    value: 1,
    desc: 'Sa-Token模式',
  },
};

export const MENU_DEFAULT_PARENT_ID = 0;

export default {
  MENU_TYPE_ENUM,
  MENU_PERMS_TYPE_ENUM,
};
