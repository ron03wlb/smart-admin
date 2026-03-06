/**
 * 權限 Fragment 組件（無額外 DOM 節點）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import React from 'react';
import { usePrivilege } from '@/hooks/usePrivilege';

interface PrivilegeFragmentProps {
  /**
   * 權限編碼
   *
   * @example 'system:user:add'
   */
  permissionCode: string;

  /**
   * 子元素
   */
  children: React.ReactNode;
}

/**
 * 無額外 DOM 節點的權限包裝器
 *
 * 對應 Vue 的 `<template v-privilege="'system:user:add'">`
 * 使用 React.Fragment，不會在 DOM 中添加額外節點
 *
 * @example
 * <PrivilegeFragment permissionCode="system:user:query">
 *   <span>內容1</span>
 *   <span>內容2</span>
 * </PrivilegeFragment>
 */
export const PrivilegeFragment: React.FC<PrivilegeFragmentProps> = ({
  permissionCode,
  children,
}) => {
  const hasPermission = usePrivilege(permissionCode);

  if (!hasPermission) {
    return null;
  }

  return <>{children}</>;
};
