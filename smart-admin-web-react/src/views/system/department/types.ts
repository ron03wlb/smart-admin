/**
 * Department Management Type Definitions
 * 部門管理類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

/**
 * 部門 VO（View Object）
 * 用於列表展示和樹形結構
 */
export interface DepartmentVO {
  /** 部門ID */
  departmentId: number;

  /** 部門名稱 */
  departmentName: string;

  /** 上級部門ID（0 表示頂級部門） */
  parentId: number;

  /** 部門負責人ID */
  managerId?: number;

  /** 部門負責人姓名 */
  managerName?: string;

  /** 排序值（值越大越靠前） */
  sort: number;

  /** 創建時間 */
  createTime?: string;

  /** 更新時間 */
  updateTime?: string;

  /** 子部門列表（樹形結構） */
  children?: DepartmentVO[];
}

/**
 * 部門查詢表單
 */
export interface DepartmentQueryForm {
  /** 搜索關鍵字（部門名稱） */
  keyword?: string;
}

/**
 * 部門新增表單
 */
export interface DepartmentAddForm {
  /** 部門名稱 */
  departmentName: string;

  /** 上級部門ID */
  parentId: number;

  /** 部門負責人ID */
  managerId?: number;

  /** 排序值（默認 0） */
  sort?: number;
}

/**
 * 部門更新表單
 */
export interface DepartmentUpdateForm {
  /** 部門ID */
  departmentId: number;

  /** 部門名稱 */
  departmentName: string;

  /** 上級部門ID */
  parentId: number;

  /** 部門負責人ID */
  managerId?: number;

  /** 排序值 */
  sort?: number;
}

/**
 * 部門表單數據（用於 Modal）
 */
export interface DepartmentFormData {
  /** 部門ID（編輯時存在） */
  departmentId?: number;

  /** 部門名稱 */
  departmentName?: string;

  /** 上級部門ID */
  parentId?: number;

  /** 部門負責人ID */
  managerId?: number;

  /** 排序值 */
  sort?: number;
}

/**
 * 部門樹節點（用於 TreeSelect）
 */
export interface DepartmentTreeNode {
  /** 節點值（部門ID） */
  value: number;

  /** 節點標題（部門名稱） */
  title: string;

  /** 子節點 */
  children?: DepartmentTreeNode[];
}
