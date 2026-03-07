/**
 * Department API
 *
 * Corresponds to Vue's api/system/department-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';
import type { DepartmentVO, DepartmentAddForm, DepartmentUpdateForm } from '@/types/department.types';

export const departmentApi = {
  /** List all departments (flat) */
  listAll: () => getRequest<DepartmentVO[]>('/department/listAll'),

  /** Get department tree */
  treeList: () => getRequest<DepartmentVO[]>('/department/treeList'),

  /** Add department */
  add: (data: DepartmentAddForm) => postRequest<void>('/department/add', data),

  /** Update department */
  update: (data: DepartmentUpdateForm) => postRequest<void>('/department/update', data),

  /** Delete department */
  delete: (departmentId: number) => getRequest<void>(`/department/delete/${departmentId}`),
};
