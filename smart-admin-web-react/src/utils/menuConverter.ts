/**
 * Menu Converter
 * 菜單數據轉換器（API VO → 內部類型）
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { MenuItem, PermissionPoint, MenuTypeEnum } from '@/types/menu';
import type { MenuVO, PointVO } from '@/api/system/loginApi';

/**
 * 將 API 返回的 MenuVO 轉換為 MenuItem
 *
 * @param menuVO API 菜單數據
 * @returns 內部菜單類型
 */
export function convertMenuVOToMenuItem(menuVO: MenuVO): MenuItem {
  return {
    menuId: Number(menuVO.menuId), // Convert string to number
    menuName: menuVO.menuName,
    menuType: menuVO.menuType as MenuTypeEnum,
    parentId: Number(menuVO.parentId), // Convert string to number
    sort: menuVO.sort,
    path: menuVO.path || undefined,
    component: undefined, // Not provided by backend
    perms: menuVO.webPerms || undefined,
    icon: menuVO.icon || undefined,
    visibleFlag: true, // Default to visible
    disabledFlag: false, // Default to not disabled
    cacheFlag: false, // Default to not cached
    children: menuVO.children ? menuVO.children.map(convertMenuVOToMenuItem) : undefined,
    webPerms: menuVO.webPerms || undefined,
  };
}

/**
 * 批量轉換 MenuVO 列表
 *
 * @param menuVOList API 菜單列表
 * @returns 內部菜單列表
 */
export function convertMenuVOListToMenuItems(menuVOList: MenuVO[]): MenuItem[] {
  return menuVOList.map(convertMenuVOToMenuItem);
}

/**
 * 將 API 返回的 PointVO 轉換為 PermissionPoint
 *
 * @param pointVO API 權限點數據
 * @param menuId 對應的菜單ID（從菜單列表中提取）
 * @returns 內部權限點類型
 */
export function convertPointVOToPermissionPoint(
  pointVO: PointVO,
  menuId: number = 0
): PermissionPoint {
  return {
    menuId: menuId,
    webPerms: pointVO.webPerms,
    menuName: pointVO.permsName || undefined,
  };
}

/**
 * 批量轉換 PointVO 列表
 *
 * @param pointVOList API 權限點列表
 * @returns 內部權限點列表
 */
export function convertPointVOListToPermissionPoints(pointVOList: PointVO[]): PermissionPoint[] {
  return pointVOList.map(point => convertPointVOToPermissionPoint(point));
}
