/**
 * Smart Enum Select Component
 *
 * Corresponds to Vue's components/framework/smart-enum-select/index.vue
 */
import React, { useMemo } from 'react';
import { Select } from 'antd';
import { getValueDescList } from '@/lib/smart-enum';
import type { SmartEnumItem } from '@/types/smart-enum.types';

interface SmartEnumSelectProps {
  enumName: string;
  value?: number | string;
  onChange?: (value: number | string | undefined) => void;
  width?: string;
  placeholder?: string;
  size?: 'large' | 'middle' | 'small';
  disabled?: boolean;
  /** Enum values to disable */
  disabledOption?: (number | string)[];
  /** Enum values to hide */
  hiddenOption?: (number | string)[];
  allowClear?: boolean;
  style?: React.CSSProperties;
}

const SmartEnumSelect: React.FC<SmartEnumSelectProps> = ({
  enumName,
  value,
  onChange,
  width = '100%',
  placeholder = '请选择',
  size,
  disabled = false,
  disabledOption = [],
  hiddenOption = [],
  allowClear = true,
  style,
}) => {
  const options = useMemo(() => {
    const list: SmartEnumItem[] = getValueDescList(enumName);
    return list
      .filter((item) => !hiddenOption.includes(item.value))
      .map((item) => ({
        label: item.desc,
        value: item.value,
        disabled: disabledOption.includes(item.value),
      }));
  }, [enumName, hiddenOption, disabledOption]);

  // Clear value if it's in disabled or hidden options
  const resolvedValue = useMemo(() => {
    if (value === undefined || value === null) return undefined;
    if (disabledOption.includes(value) || hiddenOption.includes(value)) return undefined;
    return value;
  }, [value, disabledOption, hiddenOption]);

  return (
    <Select
      value={resolvedValue}
      onChange={onChange}
      style={{ width, ...style }}
      placeholder={placeholder}
      showSearch
      allowClear={allowClear}
      size={size}
      disabled={disabled}
      options={options}
      optionFilterProp="label"
    />
  );
};

export default SmartEnumSelect;
