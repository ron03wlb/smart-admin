/**
 * Role Menu API
 *
 * Corresponds to Vue's api/system/role-menu-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { RoleMenuData } from '@/types/role.types';

export const roleMenuApi = {
  /** Get role selected menu tree with checked IDs */
  getRoleSelectedMenu: (roleId: number) =>
    getRequest<RoleMenuData>(`/role/menu/getRoleSelectedMenu/${roleId}`),

  /** Update role menu permissions */
  updateRoleMenu: (data: { roleId: number; menuIdList: string[] }) =>
    postRequest<void>('/role/menu/updateRoleMenu', data),
};
