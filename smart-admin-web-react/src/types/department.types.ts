/**
 * Department Types
 */

export interface DepartmentVO {
  departmentId: number;
  departmentName: string;
  parentId: number;
  sortValue: number;
  managerEmployeeId?: number;
  managerName?: string;
  children?: DepartmentVO[];
}

export interface DepartmentAddForm {
  departmentName: string;
  parentId: number;
  sortValue?: number;
  managerEmployeeId?: number;
}

export interface DepartmentUpdateForm extends DepartmentAddForm {
  departmentId: number;
}
