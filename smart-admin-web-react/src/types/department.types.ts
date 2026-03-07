/**
 * Department Types
 */

export interface DepartmentVO {
  departmentId: number;
  departmentName: string;
  parentId: number;
  sort: number;
  managerId?: number;
  managerName?: string;
  createTime?: string;
  updateTime?: string;
  children?: DepartmentVO[];
}

export interface DepartmentAddForm {
  departmentName: string;
  parentId: number;
  sort?: number;
  managerId?: number;
}

export interface DepartmentUpdateForm extends DepartmentAddForm {
  departmentId: number;
}
