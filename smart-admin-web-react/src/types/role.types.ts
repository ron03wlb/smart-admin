/**
 * Role Types
 */

export interface RoleVO {
  roleId: number;
  roleName: string;
  remark: string;
}

export interface RoleAddForm {
  roleName: string;
  remark?: string;
}

export interface RoleUpdateForm extends RoleAddForm {
  roleId: number;
}

export interface DataScopeVO {
  dataScopeType: number;
  viewType: number;
  description: string;
}

export interface RoleDataScopeVO {
  dataScopeType: number;
  viewType: number;
}

export interface RoleEmployeeQueryForm {
  pageNum: number;
  pageSize: number;
  roleId: number;
  searchWord?: string;
}

export interface RoleBatchEmployeeForm {
  roleId: number;
  employeeIdList: number[];
}
