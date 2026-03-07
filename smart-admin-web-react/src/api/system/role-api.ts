/**
 * Role API
 *
 * Corresponds to Vue's api/system/role-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { PageResult } from '@/api/base/page.model';
import type {
  RoleVO,
  RoleAddForm,
  RoleUpdateForm,
  DataScopeVO,
  RoleDataScopeVO,
  RoleEmployeeQueryForm,
  RoleBatchEmployeeForm,
} from '@/types/role.types';

export const roleApi = {
  /** Get all roles */
  getAll: () => getRequest<RoleVO[]>('/role/getAll'),

  /** Add role */
  add: (data: RoleAddForm) => postRequest<void>('/role/add', data),

  /** Update role */
  update: (data: RoleUpdateForm) => postRequest<void>('/role/update', data),

  /** Delete role */
  delete: (roleId: number) => getRequest<void>(`/role/delete/${roleId}`),

  // --- Data Scope ---

  /** Get data scope list */
  getDataScopeList: () => getRequest<DataScopeVO[]>('/dataScope/list'),

  /** Get role data scope */
  getRoleDataScopeList: (roleId: number) => getRequest<RoleDataScopeVO[]>(`/role/dataScope/getRoleDataScopeList/${roleId}`),

  /** Update role data scope */
  updateRoleDataScope: (data: { roleId: number; dataScopeList: RoleDataScopeVO[] }) =>
    postRequest<void>('/role/dataScope/updateRoleDataScopeList', data),

  // --- Role Employees ---

  /** Query role employees */
  queryEmployee: (data: RoleEmployeeQueryForm) => postRequest<PageResult<any>>('/role/employee/queryEmployee', data),

  /** Remove employee from role */
  removeEmployee: (employeeId: number, roleId: number) =>
    getRequest<void>(`/role/employee/removeEmployee?employeeId=${employeeId}&roleId=${roleId}`),

  /** Batch remove employees from role */
  batchRemoveEmployee: (data: RoleBatchEmployeeForm) =>
    postRequest<void>('/role/employee/batchRemoveRoleEmployee', data),

  /** Get all employees by role ID */
  getAllEmployeeByRoleId: (roleId: number) => getRequest<any[]>(`/role/employee/getAllEmployeeByRoleId/${roleId}`),

  /** Batch add employees to role */
  batchAddEmployee: (data: RoleBatchEmployeeForm) =>
    postRequest<void>('/role/employee/batchAddRoleEmployee', data),
};
