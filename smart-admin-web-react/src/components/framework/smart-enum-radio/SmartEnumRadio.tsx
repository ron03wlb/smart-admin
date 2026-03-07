/**
 * Smart Enum Radio Component
 *
 * Corresponds to Vue's components/framework/smart-enum-radio/index.vue
 */
import React, { useMemo } from 'react';
import { Radio } from 'antd';
import type { RadioChangeEvent } from 'antd';
import { getValueDescList } from '@/lib/smart-enum';
import type { SmartEnumItem } from '@/types/smart-enum.types';

interface SmartEnumRadioProps {
  enumName: string;
  value?: number | string;
  onChange?: (value: number | string) => void;
  disabled?: boolean;
  /** Render as button-style radio */
  isButton?: boolean;
  /** Enum values to disable */
  disabledOption?: (number | string)[];
  /** Enum values to hide */
  hiddenOption?: (number | string)[];
}

const SmartEnumRadio: React.FC<SmartEnumRadioProps> = ({
  enumName,
  value,
  onChange,
  disabled = false,
  isButton = false,
  disabledOption = [],
  hiddenOption = [],
}) => {
  const items = useMemo(() => {
    const list: SmartEnumItem[] = getValueDescList(enumName);
    return list.filter((item) => !hiddenOption.includes(item.value));
  }, [enumName, hiddenOption]);

  const resolvedValue = useMemo(() => {
    if (value === undefined || value === null) return undefined;
    if (disabledOption.includes(value) || hiddenOption.includes(value)) return undefined;
    return value;
  }, [value, disabledOption, hiddenOption]);

  const handleChange = (e: RadioChangeEvent) => {
    onChange?.(e.target.value);
  };

  const RadioItem = isButton ? Radio.Button : Radio;

  return (
    <Radio.Group
      value={resolvedValue}
      onChange={handleChange}
      disabled={disabled}
      buttonStyle={isButton ? 'solid' : undefined}
    >
      {items.map((item) => (
        <RadioItem key={String(item.value)} value={item.value} disabled={disabledOption.includes(item.value)}>
          {item.desc}
        </RadioItem>
      ))}
    </Radio.Group>
  );
};

export default SmartEnumRadio;
