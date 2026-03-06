/**
 * 權限按鈕組件（對應 Vue <a-button v-privilege>）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import React from 'react';
import { Button } from 'antd';
import type { ButtonProps } from 'antd';
import { usePrivilege } from '@/hooks/usePrivilege';

interface PrivilegeButtonProps extends ButtonProps {
  /**
   * 權限編碼（對應 Vue v-privilege 的值）
   *
   * @example 'system:user:add'
   */
  permissionCode: string;

  /**
   * 無權限時的處理方式
   * - 'hide': 隱藏按鈕（默認，對應 Vue removeChild）
   * - 'disable': 禁用按鈕
   *
   * @default 'hide'
   */
  mode?: 'hide' | 'disable';
}

/**
 * 權限按鈕組件
 *
 * 對應 Vue 的 `<a-button v-privilege="'system:user:add'">` 語法
 *
 * @example
 * // 無權限時隱藏
 * <PrivilegeButton permissionCode="system:user:add" type="primary">
 *   新增用戶
 * </PrivilegeButton>
 *
 * @example
 * // 無權限時禁用
 * <PrivilegeButton
 *   permissionCode="system:user:edit"
 *   mode="disable"
 *   type="link"
 * >
 *   編輯
 * </PrivilegeButton>
 */
export const PrivilegeButton: React.FC<PrivilegeButtonProps> = ({
  permissionCode,
  mode = 'hide',
  children,
  disabled,
  ...buttonProps
}) => {
  const hasPermission = usePrivilege(permissionCode);

  // 無權限且隱藏模式：返回 null（對應 Vue 的 removeChild）
  if (!hasPermission && mode === 'hide') {
    return null;
  }

  // 無權限且禁用模式：禁用按鈕
  if (!hasPermission && mode === 'disable') {
    return (
      <Button {...buttonProps} disabled>
        {children}
      </Button>
    );
  }

  // 有權限：正常渲染（保留原始 disabled 狀態）
  return (
    <Button {...buttonProps} disabled={disabled}>
      {children}
    </Button>
  );
};
