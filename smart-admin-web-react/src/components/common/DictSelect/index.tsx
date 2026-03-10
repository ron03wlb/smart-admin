/**
 * DictSelect Component
 * 字典選擇器組件
 *
 * 參考：Vue 版本 smart-admin-web/src/components/support/dict-select/index.vue
 * 遷移：Vue v-model → React controlled component
 *
 * 功能：
 * - 根據 dictCode 自動加載字典數據
 * - 支持單選和多選模式
 * - 支持禁用和隱藏特定選項
 * - 自動過濾已禁用的字典項
 *
 * 使用示例：
 * ```tsx
 * // 單選模式
 * <DictSelect
 *   dictCode="GOODS_PLACE"
 *   value={goodsPlace}
 *   onChange={(value) => setGoodsPlace(value)}
 * />
 *
 * // 多選模式
 * <DictSelect
 *   dictCode="GOODS_STATUS"
 *   mode="multiple"
 *   value={statusList}
 *   onChange={(values) => setStatusList(values)}
 * />
 *
 * // 禁用和隱藏選項
 * <DictSelect
 *   dictCode="GOODS_PLACE"
 *   disabledOption={['1']} // 禁用 dataValue 為 '1' 的選項
 *   hiddenOption={['2']}   // 隱藏 dataValue 為 '2' 的選項
 *   value={goodsPlace}
 *   onChange={(value) => setGoodsPlace(value)}
 * />
 * ```
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useMemo, useEffect, useState } from 'react';
import { Select, SelectProps } from 'antd';
import { useDict } from '@/hooks/useDict';

/**
 * DictSelect Props
 * 繼承 Ant Design Select 的所有 Props
 */
export interface DictSelectProps<ValueType = any> extends Omit<SelectProps<ValueType>, 'options'> {
  /**
   * 字典代碼（必填）
   * 示例：'GOODS_PLACE', 'GOODS_STATUS'
   */
  dictCode: string;

  /**
   * 需要禁用的選項（字典值編碼）
   * 示例：['1', '2']
   */
  disabledOption?: string[];

  /**
   * 需要隱藏的選項（字典值編碼）
   * 示例：['3', '4']
   */
  hiddenOption?: string[];
}

/**
 * DictSelect 組件
 */
export default function DictSelect<ValueType = any>(props: DictSelectProps<ValueType>) {
  const {
    dictCode,
    disabledOption = [],
    hiddenOption = [],
    value,
    onChange,
    mode,
    ...restProps
  } = props;

  // 獲取字典數據（使用 useDict(dictCode) overload）
  const dictDataList = useDict(dictCode);

  /**
   * 過濾字典數據
   * 1. 過濾隱藏的選項（hiddenOption）
   * 2. 過濾已禁用的字典項（disabledFlag = true）
   */
  const filteredDictData = useMemo(() => {
    return dictDataList.filter(
      item => !hiddenOption.includes(item.dataValue) && !item.dictDisabledFlag
    );
  }, [dictDataList, hiddenOption]);

  /**
   * 轉換為 Ant Design Select 的 options 格式
   */
  const options = useMemo(() => {
    return filteredDictData.map(item => ({
      label: item.dataLabel,
      value: item.dataValue,
      disabled: disabledOption.includes(item.dataValue),
    }));
  }, [filteredDictData, disabledOption]);

  /**
   * 處理值變化
   * 過濾被禁用或隱藏的值
   */
  const [internalValue, setInternalValue] = useState(value);

  useEffect(() => {
    if (value === undefined || value === null) {
      setInternalValue(value);
      return;
    }

    // 多選模式：過濾數組中被禁用或隱藏的值
    if (Array.isArray(value)) {
      const filteredValue = value.filter(
        (item: any) => !disabledOption.includes(item) && !hiddenOption.includes(item)
      );
      setInternalValue(filteredValue as any);
    } else {
      // 單選模式：如果值被禁用或隱藏，清空
      const isHiddenOrDisabled =
        hiddenOption.includes(String(value)) || disabledOption.includes(String(value));
      setInternalValue(isHiddenOrDisabled ? undefined : value);
    }
  }, [value, disabledOption, hiddenOption]);

  /**
   * 處理選擇變化
   */
  const handleChange = (newValue: ValueType) => {
    setInternalValue(newValue);
    onChange?.(newValue, options as any);
  };

  return (
    <Select<ValueType>
      {...restProps}
      mode={mode}
      value={internalValue}
      onChange={handleChange}
      options={options}
      allowClear
      placeholder={props.placeholder || '請選擇'}
    />
  );
}
