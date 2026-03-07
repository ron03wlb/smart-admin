/**
 * Boolean Select Component
 *
 * Corresponds to Vue's components/framework/boolean-select/index.vue
 * Renders FLAG_NUMBER_ENUM (1=是, 0=否) and converts between Boolean and Number.
 */
import React, { useMemo } from 'react';
import { Select } from 'antd';
import { getValueDescList } from '@/lib/smart-enum';

interface BooleanSelectProps {
  value?: boolean | null;
  onChange?: (value: boolean | null) => void;
  width?: number;
  placeholder?: string;
  size?: 'large' | 'middle' | 'small';
  disabled?: boolean;
}

function convertBoolean2Number(value: boolean | null | undefined): number | null {
  if (value === null || value === undefined) return null;
  return value ? 1 : 0;
}

const BooleanSelect: React.FC<BooleanSelectProps> = ({
  value,
  onChange,
  width = 100,
  placeholder = '请选择',
  size,
  disabled = false,
}) => {
  const options = useMemo(() => {
    return getValueDescList<number>('FLAG_NUMBER_ENUM').map((item) => ({
      label: item.desc,
      value: item.value,
    }));
  }, []);

  const selectValue = useMemo(() => convertBoolean2Number(value), [value]);

  const handleChange = (val: number | undefined) => {
    if (val === undefined || val === null) {
      onChange?.(null);
    } else {
      onChange?.(val === 1);
    }
  };

  return (
    <Select
      value={selectValue ?? undefined}
      onChange={handleChange}
      style={{ width }}
      placeholder={placeholder}
      showSearch
      allowClear
      size={size}
      disabled={disabled}
      options={options}
    />
  );
};

export default BooleanSelect;
