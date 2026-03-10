/**
 * usePrivilege Hook
 * 權限檢查 Hook，用於判斷用戶是否擁有特定權限
 *
 * 參考：Vue 版本 smart-admin-web/src/directives/privilege.ts (30 行)
 * 遷移：v-privilege 指令 → usePrivilege Hook + PrivilegeButton 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useAppSelector } from '@/store/hooks';
import { selectAdministratorFlag, selectPointsList } from '@/store/slices/userSlice';

/**
 * 權限檢查 Hook
 *
 * @param permission 權限代碼（例如：'goods:add'）
 * @returns 是否有權限
 *
 * @example
 * ```tsx
 * const hasAddPermission = usePrivilege('goods:add');
 *
 * if (hasAddPermission) {
 *   return <Button>新建商品</Button>;
 * }
 * ```
 */
export function usePrivilege(permission: string): boolean {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  // 管理員擁有所有權限
  if (administratorFlag) {
    return true;
  }

  // 檢查權限列表中是否包含該權限
  return pointsList.some(point => point.webPerms === permission);
}

/**
 * 批量權限檢查 Hook
 *
 * @param permissions 權限代碼數組
 * @returns 是否擁有所有權限
 *
 * @example
 * ```tsx
 * const hasAllPermissions = usePrivileges(['goods:add', 'goods:edit']);
 * ```
 */
export function usePrivileges(permissions: string[]): boolean {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  // 管理員擁有所有權限
  if (administratorFlag) {
    return true;
  }

  // 檢查是否擁有所有權限
  return permissions.every(permission => pointsList.some(point => point.webPerms === permission));
}

/**
 * 任一權限檢查 Hook
 *
 * @param permissions 權限代碼數組
 * @returns 是否擁有任一權限
 *
 * @example
 * ```tsx
 * const hasAnyPermission = useAnyPrivilege(['goods:add', 'goods:edit']);
 * ```
 */
export function useAnyPrivilege(permissions: string[]): boolean {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  // 管理員擁有所有權限
  if (administratorFlag) {
    return true;
  }

  // 檢查是否擁有任一權限
  return permissions.some(permission => pointsList.some(point => point.webPerms === permission));
}
