/**
 * PrivilegeButton Component
 * 權限按鈕組件，根據用戶權限自動顯示/隱藏按鈕
 *
 * 參考：Vue 版本 v-privilege 指令（95+ 處使用）
 * 遷移：v-privilege 指令 → PrivilegeButton 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Button } from 'antd';
import type { ButtonProps } from 'antd';
import { usePrivilege } from '@/hooks/usePrivilege';

/**
 * PrivilegeButton 屬性接口
 * 繼承 Ant Design Button 的所有屬性，並添加 privilege 權限控制
 */
export interface PrivilegeButtonProps extends ButtonProps {
  /**
   * 權限代碼（例如：'goods:add'）
   * 當用戶擁有該權限時，按鈕才會顯示
   */
  privilege: string;

  /**
   * 無權限時是否顯示禁用狀態（默認：false，直接隱藏）
   * 設置為 true 時，無權限時按鈕顯示為禁用狀態
   */
  showDisabled?: boolean;
}

/**
 * 權限按鈕組件
 * 根據用戶權限自動顯示/隱藏按鈕
 *
 * @example
 * ```tsx
 * // 基本用法（無權限時隱藏）
 * <PrivilegeButton privilege="goods:add" type="primary">
 *   新建商品
 * </PrivilegeButton>
 *
 * // 無權限時顯示禁用狀態
 * <PrivilegeButton privilege="goods:edit" type="default" showDisabled>
 *   編輯
 * </PrivilegeButton>
 * ```
 *
 * @param privilege - 權限代碼
 * @param showDisabled - 無權限時是否顯示禁用狀態（默認：false）
 * @param children - 按鈕內容
 * @param props - Ant Design Button 的其他屬性
 */
export default function PrivilegeButton({
  privilege,
  showDisabled = false,
  children,
  ...props
}: PrivilegeButtonProps) {
  const hasPrivilege = usePrivilege(privilege);

  // 無權限且不顯示禁用狀態 → 直接隱藏
  if (!hasPrivilege && !showDisabled) {
    return null;
  }

  // 無權限但顯示禁用狀態 → 顯示禁用按鈕
  if (!hasPrivilege && showDisabled) {
    return (
      <Button {...props} disabled>
        {children}
      </Button>
    );
  }

  // 有權限 → 正常顯示按鈕
  return <Button {...props}>{children}</Button>;
}
