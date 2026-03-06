/**
 * 權限檢查 Hooks（對應 Vue v-privilege 指令）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { useMemo } from 'react';
import { useAppSelector } from '@/store/hooks';
import { selectAdministratorFlag, selectPointsList } from '@/store/slices/userSlice';

/**
 * 權限檢查 Hook（對應 Vue v-privilege 指令）
 *
 * @param permissionCode 權限編碼（如 'system:user:add'）
 * @returns boolean 是否有權限
 *
 * @example
 * const canAdd = usePrivilege('system:user:add');
 * if (canAdd) {
 *   // 顯示新增按鈕
 * }
 */
export function usePrivilege(permissionCode: string): boolean {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  return useMemo(() => {
    // 超級管理員直接放行（對應 Vue 的 administratorFlag 檢查）
    if (administratorFlag) {
      return true;
    }

    // 空權限列表則無權限
    if (!pointsList || pointsList.length === 0) {
      return false;
    }

    // 檢查權限點列表（對應 Vue 的 _.some(pointsList, ['webPerms', code])）
    return pointsList.some((point) => point.webPerms === permissionCode);
  }, [administratorFlag, pointsList, permissionCode]);
}

/**
 * 批量權限檢查 Hook
 *
 * @param permissionCodes 權限編碼數組
 * @returns Record<string, boolean> 權限映射表
 *
 * @example
 * const permissions = usePrivileges(['system:user:add', 'system:user:edit', 'system:user:delete']);
 * // { 'system:user:add': true, 'system:user:edit': false, 'system:user:delete': false }
 */
export function usePrivileges(permissionCodes: string[]): Record<string, boolean> {
  const administratorFlag = useAppSelector(selectAdministratorFlag);
  const pointsList = useAppSelector(selectPointsList);

  return useMemo(() => {
    const result: Record<string, boolean> = {};

    if (administratorFlag) {
      // 超管全部返回 true
      permissionCodes.forEach((code) => {
        result[code] = true;
      });
      return result;
    }

    // 批量檢查
    permissionCodes.forEach((code) => {
      result[code] = pointsList.some((point) => point.webPerms === code);
    });

    return result;
  }, [administratorFlag, pointsList, permissionCodes]);
}
