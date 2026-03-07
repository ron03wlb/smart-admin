/**
 * Position Select Component
 *
 * Corresponds to Vue's components/system/position-select/index.vue
 */
import React, { useEffect, useState } from 'react';
import { Select } from 'antd';
import { positionApi } from '@/api/system/position-api';
import type { PositionVO } from '@/types/position.types';

interface PositionSelectProps {
  value?: number | number[];
  onChange?: (value: number | number[] | undefined) => void;
  placeholder?: string;
  width?: string;
  size?: 'large' | 'middle' | 'small';
  style?: React.CSSProperties;
}

const PositionSelect: React.FC<PositionSelectProps> = ({
  value,
  onChange,
  placeholder = '请选择',
  width = '100%',
  size,
  style,
}) => {
  const [positionList, setPositionList] = useState<PositionVO[]>([]);

  useEffect(() => {
    positionApi.queryList().then((res) => {
      if (res.code === 1) {
        setPositionList(res.data ?? []);
      }
    });
  }, []);

  const options = positionList.map((item) => ({
    label: item.positionName,
    value: item.positionId,
  }));

  return (
    <Select
      value={value}
      onChange={onChange}
      style={{ width, ...style }}
      placeholder={placeholder}
      showSearch
      allowClear
      size={size}
      options={options}
      optionFilterProp="label"
    />
  );
};

export default PositionSelect;
