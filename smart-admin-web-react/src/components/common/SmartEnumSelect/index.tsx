/**
 * SmartEnumSelect Component
 * 枚舉選擇器組件
 *
 * 參考：Vue 版本 smart-admin-web/src/components/framework/smart-enum-select/index.vue
 *
 * 功能：
 * - 自動從枚舉常量獲取選項數據
 * - 支持禁用某些選項
 * - 支持隱藏某些選項
 * - 支持 Ant Design Select 所有屬性
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { useMemo } from 'react';
import { Select, SelectProps } from 'antd';
import { getValueDescList } from '@/utils/smart-enum';

export interface SmartEnumSelectProps<T = number | string> extends Omit<SelectProps<T>, 'options'> {
  /**
   * 枚舉名稱
   * 例如：'FLAG_NUMBER_ENUM', 'GENDER_ENUM', 'USER_STATUS_ENUM'
   */
  enumName: string;

  /**
   * 需要禁用的選項枚舉值列表
   * 例如：[0, 1] 表示禁用值為 0 和 1 的選項
   */
  disabledOptions?: T[];

  /**
   * 需要隱藏的選項枚舉值列表
   * 例如：[0] 表示隱藏值為 0 的選項
   */
  hiddenOptions?: T[];

  /**
   * 寬度
   * 例如：'100%', '200px'
   */
  width?: string | number;
}

/**
 * SmartEnumSelect 組件
 */
function SmartEnumSelect<T = number | string>(props: SmartEnumSelectProps<T>) {
  const {
    enumName,
    disabledOptions = [],
    hiddenOptions = [],
    width = '100%',
    placeholder = '請選擇',
    showSearch = true,
    allowClear = true,
    value,
    ...restProps
  } = props;

  /**
   * 獲取枚舉選項列表
   * 自動過濾隱藏的選項
   */
  const options = useMemo(() => {
    const enumItems = getValueDescList<T>(enumName);

    return enumItems
      .filter(item => !hiddenOptions.includes(item.value))
      .map(item => ({
        label: item.desc,
        value: item.value,
        disabled: disabledOptions.includes(item.value),
      }));
  }, [enumName, disabledOptions, hiddenOptions]);

  /**
   * 處理選中值
   * 如果值被禁用或隱藏，則清空
   */
  const safeValue = useMemo(() => {
    if (value !== undefined && value !== null) {
      const isDisabledOrHidden =
        disabledOptions.includes(value as T) || hiddenOptions.includes(value as T);
      return isDisabledOrHidden ? undefined : value;
    }
    return value;
  }, [value, disabledOptions, hiddenOptions]);

  return (
    <Select<T>
      {...restProps}
      value={safeValue}
      style={{ width, ...restProps.style }}
      placeholder={placeholder}
      showSearch={showSearch}
      allowClear={allowClear}
      options={options}
    />
  );
}

export default SmartEnumSelect;
