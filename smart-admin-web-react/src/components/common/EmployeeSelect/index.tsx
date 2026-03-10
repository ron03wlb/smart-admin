/**
 * EmployeeSelect Component
 * 員工選擇器組件
 *
 * 用法：
 * <EmployeeSelect
 *   value={employeeId}
 *   onChange={(value) => setEmployeeId(value)}
 *   departmentId={1}  // 可選：按部門篩選
 *   multiple          // 可選：多選模式
 * />
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Select, message } from 'antd';
import type { SelectProps } from 'antd';
import { employeeApi, EmployeeVO } from '@/api/system/employeeApi';

/**
 * 員工選擇器 Props
 */
export interface EmployeeSelectProps extends Omit<SelectProps, 'options'> {
  /** 部門 ID（可選，用於篩選特定部門的員工） */
  departmentId?: number;
  /** 是否顯示禁用的員工 */
  showDisabled?: boolean;
  /** 是否顯示離職的員工 */
  showLeave?: boolean;
  /** 禁用的員工 ID 列表 */
  disabledEmployeeIds?: number[];
}

/**
 * 員工選擇器組件
 */
const EmployeeSelect: React.FC<EmployeeSelectProps> = props => {
  const {
    departmentId,
    showDisabled = false,
    showLeave = false,
    disabledEmployeeIds = [],
    ...restProps
  } = props;

  // 員工列表狀態
  const [employeeList, setEmployeeList] = useState<EmployeeVO[]>([]);
  const [loading, setLoading] = useState(false);

  /**
   * 加載員工數據
   */
  const fetchEmployees = useCallback(async () => {
    setLoading(true);
    try {
      let response;
      if (departmentId) {
        // 按部門查詢
        response = await employeeApi.queryEmployeeByDeptId(departmentId);
      } else {
        // 查詢所有員工
        response = await employeeApi.queryAll();
      }

      if (response.ok && response.data) {
        setEmployeeList(response.data);
      } else {
        message.error('查詢員工列表失敗');
      }
    } catch (error) {
      console.error('查詢員工列表失敗:', error);
      message.error('查詢員工列表失敗，請稍後重試');
    } finally {
      setLoading(false);
    }
  }, [departmentId]);

  /**
   * 初始化加載員工
   */
  useEffect(() => {
    fetchEmployees();
  }, [fetchEmployees]);

  /**
   * 篩選並構建選項
   */
  const options = useMemo(() => {
    return employeeList
      .filter(employee => {
        // 篩選禁用員工
        if (!showDisabled && employee.isDisabled) {
          return false;
        }
        // 篩選離職員工
        if (!showLeave && employee.isLeave) {
          return false;
        }
        return true;
      })
      .map(employee => ({
        label: `${employee.actualName}${employee.departmentName ? ` (${employee.departmentName})` : ''}`,
        value: employee.employeeId,
        disabled: disabledEmployeeIds.includes(employee.employeeId),
      }));
  }, [employeeList, showDisabled, showLeave, disabledEmployeeIds]);

  return (
    <Select
      {...restProps}
      loading={loading}
      options={options}
      placeholder={restProps.placeholder || '請選擇員工'}
      showSearch
      filterOption={(input, option) =>
        (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
      }
    />
  );
};

export default EmployeeSelect;
