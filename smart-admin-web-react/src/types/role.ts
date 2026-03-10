/**
 * Role Types
 * 角色類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

/**
 * 菜單樹節點
 */
export interface MenuTreeNode {
  /** 菜單 ID */
  menuId: number;
  /** 父菜單 ID */
  parentId?: number | null;
  /** 菜單名稱 */
  menuName: string;
  /** 菜單類型（1=目錄，2=菜單，3=按鈕） */
  menuType: number;
  /** 子菜單 */
  children?: MenuTreeNode[];
  /** 其他屬性... */
  [key: string]: any;
}

/**
 * 角色狀態
 */
export interface RoleState {
  /** 選中的菜單 ID 列表 */
  checkedData: number[];
  /** 菜單樹 Map（key: menuId, value: MenuTreeNode） */
  treeMap: Record<number, MenuTreeNode>;
}
