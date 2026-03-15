/**
 * Dict Types
 * 字典類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 字典 VO
 */
export interface DictVO {
  dictId: number;
  dictCode: string; // 編碼
  dictName: string; // 名稱
  remark?: string; // 備註
  disabledFlag: number; // 0-啟用, 1-禁用
  updateTime?: string;
  // 前端使用字段
  enabled?: boolean; // Switch 綁定值（!disabledFlag）
}

/**
 * 字典查詢表單
 */
export interface DictQueryForm {
  keywords?: string; // 編碼/名稱/備註
  disabledFlag?: number | null; // 0-啟用, 1-禁用, null-全部
  pageNum: number;
  pageSize: number;
}

/**
 * 字典表單數據
 */
export interface DictFormData {
  dictId?: number;
  dictCode: string;
  dictName: string;
  remark?: string;
}

/**
 * 字典值 VO
 */
export interface DictDataVO {
  dictDataId: number;
  dictId: number;
  dictCode: string;
  dataValue: string; // 字典項值
  dataLabel: string; // 字典項名稱
  sortOrder: number; // 排序
  remark?: string;
  disabledFlag: number; // 0-啟用, 1-禁用
  updateTime?: string;
  // 前端使用字段
  enabled?: boolean; // Switch 綁定值（!disabledFlag）
}

/**
 * 字典值表單數據
 */
export interface DictDataFormData {
  dictDataId?: number;
  dictId: number;
  dictCode: string;
  dataValue: string;
  dataLabel: string;
  sortOrder: number;
  remark?: string;
}
