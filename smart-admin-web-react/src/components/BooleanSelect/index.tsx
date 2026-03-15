/**
 * Boolean Select Component
 * 布爾值選擇器組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React from 'react';
import { Select } from 'antd';

export interface BooleanSelectProps {
  value?: number | null;
  onChange?: (value: number | null) => void;
  style?: React.CSSProperties;
  allowClear?: boolean;
}

/**
 * Boolean Select Component
 * 提供「全部」、「啟用」、「禁用」三個選項
 */
const BooleanSelect: React.FC<BooleanSelectProps> = ({
  value,
  onChange,
  style,
  allowClear = false,
}) => {
  const options = [
    { label: '全部', value: null },
    { label: '啟用', value: 0 },
    { label: '禁用', value: 1 },
  ];

  return (
    <Select
      value={value}
      onChange={onChange}
      options={options}
      style={style}
      allowClear={allowClear}
      placeholder="請選擇狀態"
    />
  );
};

export default BooleanSelect;
