/**
 * All Constants Entry
 *
 * Corresponds to Vue's constants/index.ts
 * Aggregates all SmartEnum constants for the smart-enum library.
 */
import { FLAG_NUMBER_ENUM, GENDER_ENUM, USER_TYPE_ENUM, DATA_TYPE_ENUM } from './common-const';
import { LAYOUT_ENUM, PAGE_TAG_ENUM } from './layout-const';
import menuConst from './system/menu-const';
import fileConst from './support/file-const';
import type { SmartEnumWrapper } from '@/types/smart-enum.types';

const allEnums: SmartEnumWrapper = {
  FLAG_NUMBER_ENUM,
  GENDER_ENUM,
  USER_TYPE_ENUM,
  DATA_TYPE_ENUM,
  LAYOUT_ENUM,
  PAGE_TAG_ENUM,
  ...menuConst,
  ...fileConst,
};

export default allEnums;
