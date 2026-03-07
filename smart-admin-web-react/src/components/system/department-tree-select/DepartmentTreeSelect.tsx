/**
 * Department Tree Select Component
 *
 * Corresponds to Vue's components/system/department-tree-select/index.vue
 */
import React, { useEffect, useState } from 'react';
import { TreeSelect } from 'antd';
import { departmentApi } from '@/api/system/department-api';
import type { DepartmentVO } from '@/types/department.types';

interface DepartmentTreeSelectProps {
  value?: number | number[];
  onChange?: (value: number | number[] | undefined) => void;
  multiple?: boolean;
  placeholder?: string;
  style?: React.CSSProperties;
}

const DepartmentTreeSelect: React.FC<DepartmentTreeSelectProps> = ({
  value,
  onChange,
  multiple = false,
  placeholder = '请选择部门',
  style,
}) => {
  const [treeData, setTreeData] = useState<DepartmentVO[]>([]);

  useEffect(() => {
    departmentApi.treeList().then((res) => {
      if (res.code === 1) {
        setTreeData(res.data ?? []);
      }
    });
  }, []);

  return (
    <TreeSelect
      value={value}
      onChange={onChange}
      treeData={treeData}
      fieldNames={{ label: 'departmentName', value: 'departmentId', children: 'children' }}
      showSearch
      allowClear
      treeDefaultExpandAll
      multiple={multiple}
      placeholder={placeholder}
      style={{ width: '100%', ...style }}
      popupMatchSelectWidth={false}
      styles={{ popup: { root: { maxHeight: 400, overflow: 'auto' } } }}
      treeNodeFilterProp="departmentName"
    />
  );
};

export default DepartmentTreeSelect;
