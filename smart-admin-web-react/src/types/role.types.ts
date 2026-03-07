/**
 * Role Types
 */

export interface RoleVO {
  roleId: number;
  roleName: string;
  roleCode?: string;
  remark: string;
}

export interface RoleAddForm {
  roleName: string;
  roleCode?: string;
  remark?: string;
}

export interface RoleUpdateForm extends RoleAddForm {
  roleId: number;
}

// --- Data Scope ---

export interface DataScopeViewType {
  viewType: number;
  viewTypeName: string;
}

export interface DataScopeDefinition {
  dataScopeType: number;
  dataScopeTypeName: string;
  dataScopeTypeDesc: string;
  viewTypeList: DataScopeViewType[];
}

export interface RoleDataScopeVO {
  dataScopeType: number;
  viewType: number;
}

// --- Role Menu Tree ---

export interface MenuTreeNode {
  menuId: string;
  menuName: string;
  menuType: number;
  parentId?: string;
  contextMenuId?: string;
  children?: MenuTreeNode[];
}

export interface RoleMenuData {
  menuTreeList: MenuTreeNode[];
  selectedMenuId: string[];
}

// --- Role Employee ---

export interface RoleEmployeeQueryForm {
  pageNum: number;
  pageSize: number;
  roleId: number;
  keywords?: string;
}

export interface RoleBatchEmployeeForm {
  roleId: number;
  employeeIdList: number[];
}

export interface RoleEmployeeVO {
  employeeId: number;
  actualName: string;
  phone?: string;
  loginName?: string;
  departmentName?: string;
  disabledFlag: boolean;
  gender?: number;
}
