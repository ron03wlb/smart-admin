/**
 * Menu API
 *
 * Corresponds to Vue's api/system/menu-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { MenuItem } from '@/types/user.types';

export interface MenuAddForm {
  menuName: string;
  menuType: number;
  parentId: number;
  path?: string;
  component?: string;
  icon?: string;
  sortValue?: number;
  visibleFlag?: boolean;
  disabledFlag?: boolean;
  cacheFlag?: boolean;
  frameFlag?: boolean;
  frameUrl?: string;
  webPerms?: string;
  apiPerms?: string;
}

export interface MenuUpdateForm extends MenuAddForm {
  menuId: number;
}

export const menuApi = {
  /** Add menu */
  add: (data: MenuAddForm) => postRequest<void>('/menu/add', data),

  /** Update menu */
  update: (data: MenuUpdateForm) => postRequest<void>('/menu/update', data),

  /** Batch delete menus */
  batchDelete: (menuIdList: number[]) => getRequest<void>(`/menu/batchDelete?menuIdList=${menuIdList.join(',')}`),

  /** Query all menus (flat) */
  query: () => getRequest<MenuItem[]>('/menu/query'),

  /** Get menu tree */
  tree: (onlyMenu?: boolean) => getRequest<MenuItem[]>(`/menu/tree?onlyMenu=${onlyMenu ?? false}`),

  /** Get authorized URLs */
  getAuthUrl: () => getRequest<string[]>('/menu/auth/url'),
};
