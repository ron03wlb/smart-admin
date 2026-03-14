/**
 * Employee Management Types
 * 員工管理類型定義
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/employee/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 員工視圖對象（列表展示）
 */
export interface EmployeeVO {
  /** 員工 ID */
  employeeId: number;
  /** 實際姓名 */
  actualName: string;
  /** 性別 (1:男 2:女) */
  gender: number;
  /** 登錄名 */
  loginName: string;
  /** 手機號 */
  phone: string;
  /** 郵箱 */
  email?: string;
  /** 是否為超級管理員 */
  administratorFlag: boolean;
  /** 是否禁用 */
  disabledFlag: boolean;
  /** 是否離職 */
  leaveFlag: boolean;
  /** 職位 ID */
  positionId?: number;
  /** 職位名稱 */
  positionName?: string;
  /** 角色 ID 列表 */
  roleIdList?: number[];
  /** 角色名稱列表（逗號分隔） */
  roleNameList?: string;
  /** 部門 ID */
  departmentId: number;
  /** 部門名稱 */
  departmentName?: string;
  /** 備註 */
  remark?: string;
  /** 創建時間 */
  createTime?: string;
  /** 更新時間 */
  updateTime?: string;
}

/**
 * 員工查詢表單
 */
export interface EmployeeQueryForm {
  /** 部門 ID（可選，用於按部門過濾） */
  departmentId?: number;
  /** 是否禁用（true:禁用 false:啟用 undefined:全部） */
  disabledFlag?: boolean;
  /** 關鍵字（姓名/手機號/登錄賬號） */
  keyword?: string;
  /** 分頁頁碼 */
  pageNum?: number;
  /** 每頁數量 */
  pageSize?: number;
  /** 排序項列表 */
  sortItemList?: Array<{
    column: string;
    order: 'ASC' | 'DESC';
  }>;
}

/**
 * 員工新增表單
 */
export interface EmployeeAddForm {
  /** 實際姓名 */
  actualName: string;
  /** 手機號 */
  phone: string;
  /** 部門 ID */
  departmentId: number;
  /** 登錄名 */
  loginName: string;
  /** 郵箱 */
  email?: string;
  /** 性別 (1:男 2:女) */
  gender: number;
  /** 是否禁用（0:啟用 1:禁用） */
  disabledFlag: number;
  /** 是否離職（0:在職 1:離職） */
  leaveFlag: number;
  /** 職位 ID */
  positionId?: number;
  /** 角色 ID 列表 */
  roleIdList?: number[];
}

/**
 * 員工更新表單
 */
export interface EmployeeUpdateForm {
  /** 員工 ID（必填，用於標識要更新的員工） */
  employeeId: number;
  /** 實際姓名 */
  actualName: string;
  /** 手機號 */
  phone: string;
  /** 部門 ID */
  departmentId: number;
  /** 登錄名 */
  loginName: string;
  /** 郵箱 */
  email?: string;
  /** 性別 (1:男 2:女) */
  gender: number;
  /** 是否禁用（0:啟用 1:禁用） */
  disabledFlag: number;
  /** 是否離職（0:在職 1:離職） */
  leaveFlag: number;
  /** 職位 ID */
  positionId?: number;
  /** 角色 ID 列表 */
  roleIdList?: number[];
}

/**
 * 員工表單（用於 Modal，支持新增和編輯）
 */
export type EmployeeFormData = Partial<EmployeeAddForm> & {
  /** 員工 ID（編輯時有值，新增時為 undefined） */
  employeeId?: number;
};

/**
 * 性別枚舉
 */
export enum GenderEnum {
  /** 男 */
  MALE = 1,
  /** 女 */
  FEMALE = 2,
}

/**
 * 員工狀態枚舉
 */
export enum EmployeeStatusEnum {
  /** 啟用 */
  ENABLED = 0,
  /** 禁用 */
  DISABLED = 1,
}

/**
 * 離職狀態枚舉
 */
export enum LeaveStatusEnum {
  /** 在職 */
  ON_JOB = 0,
  /** 離職 */
  RESIGNED = 1,
}
