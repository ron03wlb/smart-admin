/**
 * Dict Select Component
 *
 * Corresponds to Vue's components/support/dict-select/index.vue
 * Reads dictionary data from Redux store and renders a Select dropdown.
 */
import React, { useMemo } from 'react';
import { Select } from 'antd';
import { useAppSelector } from '@/store/hooks';

interface DictSelectProps {
  dictCode: string;
  value?: string | number | string[];
  onChange?: (value: string | number | string[] | undefined) => void;
  mode?: 'multiple' | 'tags';
  width?: string;
  placeholder?: string;
  size?: 'large' | 'middle' | 'small';
  disabled?: boolean;
  /** Dict data values to disable */
  disabledOption?: string[];
  /** Dict data values to hide */
  hiddenOption?: string[];
  allowClear?: boolean;
  style?: React.CSSProperties;
}

const DictSelect: React.FC<DictSelectProps> = ({
  dictCode,
  value,
  onChange,
  mode,
  width = '200px',
  placeholder = '请选择',
  size,
  disabled = false,
  disabledOption = [],
  hiddenOption = [],
  allowClear = true,
  style,
}) => {
  const dictMap = useAppSelector((state) => state.dict.dictMap);

  const options = useMemo(() => {
    const dictData = dictMap[dictCode] ?? [];
    return dictData
      .filter((item) => !item.disabledFlag && !hiddenOption.includes(item.dataValue))
      .map((item) => ({
        label: item.dataLabel,
        value: item.dataValue,
        disabled: disabledOption.includes(item.dataValue),
      }));
  }, [dictCode, dictMap, hiddenOption, disabledOption]);

  // Resolve value: filter out hidden/disabled values
  const resolvedValue = useMemo(() => {
    if (value === undefined || value === null) return undefined;
    if (Array.isArray(value)) {
      return value.filter((v) => !hiddenOption.includes(v) && !disabledOption.includes(v));
    }
    const strVal = String(value);
    if (hiddenOption.includes(strVal) || disabledOption.includes(strVal)) return undefined;
    return value;
  }, [value, hiddenOption, disabledOption]);

  return (
    <Select
      value={resolvedValue}
      onChange={onChange}
      style={{ width, ...style }}
      placeholder={placeholder}
      allowClear={allowClear}
      size={size}
      mode={mode}
      disabled={disabled}
      options={options}
      optionFilterProp="label"
    />
  );
};

export default DictSelect;
