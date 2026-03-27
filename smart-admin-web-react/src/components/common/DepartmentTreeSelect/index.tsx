/**
 * DepartmentTreeSelect Component
 * 部門樹形選擇器組件
 *
 * 用法：
 * <DepartmentTreeSelect
 *   value={departmentId}
 *   onChange={(value) => setDepartmentId(value)}
 *   multiple          // 可選：多選模式
 *   excludeIds={[1]}  // 可選：排除特定部門ID
 * />
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-25
 */

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { TreeSelect, message } from 'antd';
import type { TreeSelectProps } from 'antd';
import { departmentApi } from '@/api/system/departmentApi';
import type { DepartmentVO } from '@/views/system/department/types';

/**
 * 部門樹節點（TreeSelect 格式）
 */
interface DepartmentTreeNode {
  title: string;
  value: number;
  key: number;
  disabled?: boolean;
  children?: DepartmentTreeNode[];
}

/**
 * 部門樹形選擇器 Props
 */
export interface DepartmentTreeSelectProps extends Omit<TreeSelectProps, 'treeData'> {
  /** 排除的部門 ID 列表（這些部門將被禁用） */
  excludeIds?: number[];
  /** 是否顯示完整路徑（顯示所有父級部門） */
  showFullPath?: boolean;
}

/**
 * 部門樹形選擇器組件
 */
const DepartmentTreeSelect: React.FC<DepartmentTreeSelectProps> = props => {
  const { excludeIds = [], showFullPath = false, ...restProps } = props;

  // 部門樹數據狀態（原始數據）
  const [rawDepartmentData, setRawDepartmentData] = useState<DepartmentVO[]>([]);
  const [loading, setLoading] = useState(false);

  /**
   * 轉換部門 VO 為 TreeSelect 節點格式
   */
  const convertToTreeNode = useCallback(
    (department: DepartmentVO): DepartmentTreeNode => {
      const node: DepartmentTreeNode = {
        title: department.departmentName,
        value: department.departmentId,
        key: department.departmentId,
        disabled: excludeIds.includes(department.departmentId),
      };

      // 遞歸處理子部門
      if (department.children && department.children.length > 0) {
        node.children = department.children.map(child => convertToTreeNode(child));
      }

      return node;
    },
    [excludeIds]
  );

  /**
   * 加載部門樹數據（只在初始化時執行一次）
   */
  useEffect(() => {
    const fetchDepartmentTree = async () => {
      setLoading(true);
      try {
        const response = await departmentApi.queryDepartmentTree();

        if (response.ok && response.data) {
          setRawDepartmentData(response.data);
        } else {
          message.error('查詢部門樹失敗');
        }
      } catch (error) {
        console.error('查詢部門樹失敗:', error);
        message.error('查詢部門樹失敗，請稍後重試');
      } finally {
        setLoading(false);
      }
    };

    fetchDepartmentTree();
  }, []); // 空依賴數組，只在掛載時執行一次

  /**
   * 當原始數據或 excludeIds 變化時，重新計算 treeData
   */
  const departmentTree = useMemo(() => {
    return rawDepartmentData.map(dept => convertToTreeNode(dept));
  }, [rawDepartmentData, convertToTreeNode]);

  /**
   * TreeSelect 配置
   */
  const treeSelectConfig = useMemo(
    () => ({
      showSearch: true,
      treeDefaultExpandAll: true,
      allowClear: true,
      placeholder: '請選擇部門',
      popupStyle: { maxHeight: 400, overflow: 'auto' },
      treeNodeFilterProp: 'title',
      ...restProps,
    }),
    [restProps]
  );

  return (
    <TreeSelect
      {...treeSelectConfig}
      loading={loading}
      treeData={departmentTree}
      style={{ width: '100%', ...restProps.style }}
    />
  );
};

export default DepartmentTreeSelect;
