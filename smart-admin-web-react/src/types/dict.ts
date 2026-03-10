/**
 * Dictionary Type Definitions
 * 字典類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

/**
 * Dictionary Item
 * 字典項
 */
export interface DictItem {
  dictCode: string;
  dictName: string;
  disabledFlag: boolean;
}

/**
 * Dictionary Data Item
 * 字典數據項
 */
export interface DictDataItem {
  dictCode: string;
  dictName: string;
  dictDisabledFlag: boolean;
  dataValue: string;
  dataLabel: string;
  dataSort: number;
  remark?: string;
}

/**
 * Dictionary Query Form
 * 字典查詢表單
 */
export interface DictQueryForm {
  dictCode?: string;
  dictName?: string;
  pageNum: number;
  pageSize: number;
}

/**
 * Dictionary Add/Update Form
 * 字典添加/更新表單
 */
export interface DictForm {
  dictId?: number;
  dictCode: string;
  dictName: string;
  remark?: string;
}

/**
 * Dictionary Data Add/Update Form
 * 字典數據添加/更新表單
 */
export interface DictDataForm {
  dictDataId?: number;
  dictId: number;
  dataValue: string;
  dataLabel: string;
  dataSort: number;
  remark?: string;
}
