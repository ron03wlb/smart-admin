/**
 * AreaCascader - Province/City/District cascader component
 *
 * Corresponds to Vue's components/framework/area-cascader/index.vue
 * Uses static province-city-district data with Ant Design Cascader.
 */
import React from 'react';
import { Cascader } from 'antd';
import { PROVINCE_CITY_DISTRICT } from './province-city-district';

interface CascaderOption {
  value: number;
  label: string;
  children?: CascaderOption[];
}

export interface AreaOption {
  value: number;
  label: string;
}

export interface AreaCascaderProps {
  /** Selected area values: [province, city, district] */
  value?: (number | string)[];
  /** Change callback returning selected values and option objects */
  onChange?: (values: (number | string)[], selectedOptions: AreaOption[]) => void;
  /** Placeholder text */
  placeholder?: string;
  /** Width style */
  width?: string | number;
  /** Disabled state */
  disabled?: boolean;
  /** Allow clear */
  allowClear?: boolean;
  /** Additional CSS style */
  style?: React.CSSProperties;
}

const AreaCascader: React.FC<AreaCascaderProps> = ({
  value,
  onChange,
  placeholder = '请选择地区',
  width = '100%',
  disabled,
  allowClear = true,
  style,
}) => {
  const handleChange = (selectedValues: (string | number)[], selectedOptions: CascaderOption[]) => {
    if (onChange) {
      const options = (selectedOptions || []).map((opt) => ({
        value: opt.value,
        label: opt.label,
      }));
      onChange(selectedValues, options);
    }
  };

  const filter = (inputValue: string, path: CascaderOption[]) => {
    return path.some((option) => option.label.toLowerCase().includes(inputValue.toLowerCase()));
  };

  // Cast value to number[] since the province-city-district data uses number values
  const cascaderValue = value?.map((v) => (typeof v === 'string' ? Number(v) : v));

  return (
    <Cascader<CascaderOption>
      style={{ width, ...style }}
      options={PROVINCE_CITY_DISTRICT as CascaderOption[]}
      value={cascaderValue}
      onChange={handleChange}
      showSearch={{ filter }}
      placeholder={placeholder}
      disabled={disabled}
      allowClear={allowClear}
    />
  );
};

export default AreaCascader;
