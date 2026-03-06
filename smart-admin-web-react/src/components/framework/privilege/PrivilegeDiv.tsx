/**
 * 權限 Div 組件
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import React from 'react';
import { usePrivilege } from '@/hooks/usePrivilege';

interface PrivilegeDivProps extends React.HTMLAttributes<HTMLDivElement> {
  /**
   * 權限編碼
   *
   * @example 'system:user:add'
   */
  permissionCode: string;
}

/**
 * 權限 Div 組件
 *
 * 對應 Vue 的 `<div v-privilege="'system:user:add'">`
 *
 * @example
 * <PrivilegeDiv permissionCode="system:user:query" className="user-info">
 *   <span>用戶資訊區域</span>
 * </PrivilegeDiv>
 */
export const PrivilegeDiv: React.FC<PrivilegeDivProps> = ({
  permissionCode,
  children,
  ...divProps
}) => {
  const hasPermission = usePrivilege(permissionCode);

  if (!hasPermission) {
    return null;
  }

  return <div {...divProps}>{children}</div>;
};
