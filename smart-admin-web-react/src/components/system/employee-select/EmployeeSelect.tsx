/**
 * Employee Select Component
 *
 * Corresponds to Vue's components/system/employee-select/index.vue
 */
import React, { useEffect, useState } from 'react';
import { Select } from 'antd';
import { employeeApi } from '@/api/system/employee-api';
import type { EmployeeVO } from '@/api/system/employee-types';

interface EmployeeSelectProps {
  value?: number | number[];
  onChange?: (value: number | number[] | undefined) => void;
  placeholder?: string;
  width?: string;
  size?: 'large' | 'middle' | 'small';
  roleId?: number;
  disabledFlag?: number;
  style?: React.CSSProperties;
}

const EmployeeSelect: React.FC<EmployeeSelectProps> = ({
  value,
  onChange,
  placeholder = '请选择',
  width = '100%',
  size,
  roleId,
  disabledFlag,
  style,
}) => {
  const [employeeList, setEmployeeList] = useState<EmployeeVO[]>([]);

  useEffect(() => {
    const params: { roleId?: number; disabledFlag?: number } = {};
    if (roleId) params.roleId = roleId;
    if (disabledFlag != null) params.disabledFlag = disabledFlag;

    employeeApi.queryAll(params).then((res) => {
      if (res.code === 1) {
        setEmployeeList(res.data ?? []);
      }
    });
  }, [roleId, disabledFlag]);

  const options = employeeList.map((item) => ({
    label: item.departmentName ? `${item.employeeName} (${item.departmentName})` : item.employeeName,
    value: item.employeeId,
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

export default EmployeeSelect;
