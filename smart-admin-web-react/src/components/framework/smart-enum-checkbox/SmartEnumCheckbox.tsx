/**
 * Smart Enum Checkbox Component
 *
 * Corresponds to Vue's components/framework/smart-enum-checkbox/index.vue
 */
import React, { useMemo } from 'react';
import { Checkbox } from 'antd';
import { getValueDescList } from '@/lib/smart-enum';
import type { SmartEnumItem } from '@/types/smart-enum.types';

interface SmartEnumCheckboxProps {
  enumName: string;
  value?: (number | string)[];
  onChange?: (value: (string | number | boolean)[]) => void;
  width?: string;
  disabled?: boolean;
  /** Enum values to disable */
  disabledOption?: (number | string)[];
  /** Enum values to hide */
  hiddenOption?: (number | string)[];
}

const SmartEnumCheckbox: React.FC<SmartEnumCheckboxProps> = ({
  enumName,
  value,
  onChange,
  width = '200px',
  disabled = false,
  disabledOption = [],
  hiddenOption = [],
}) => {
  const items = useMemo(() => {
    const list: SmartEnumItem[] = getValueDescList(enumName);
    return list.filter((item) => !hiddenOption.includes(item.value));
  }, [enumName, hiddenOption]);

  // Filter out disabled/hidden values from current selection
  const resolvedValue = useMemo(() => {
    if (!value) return [];
    return value.filter((v) => !hiddenOption.includes(v) && !disabledOption.includes(v));
  }, [value, hiddenOption, disabledOption]);

  return (
    <Checkbox.Group style={{ width }} value={resolvedValue} onChange={onChange} disabled={disabled}>
      {items.map((item) => (
        <Checkbox key={String(item.value)} value={item.value} disabled={disabledOption.includes(item.value)}>
          {item.desc}
        </Checkbox>
      ))}
    </Checkbox.Group>
  );
};

export default SmartEnumCheckbox;
