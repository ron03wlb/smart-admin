/**
 * Dictionary Types
 *
 * Corresponds to Vue dict API response types
 */

export interface DictVO {
  dictId: number;
  dictCode: string;
  dictName: string;
  remark: string;
  disabledFlag: boolean;
}

export interface DictDataVO {
  dictDataId: number;
  dictId: number;
  dictCode: string;
  dictName: string;
  dictDisabledFlag: boolean;
  dataValue: string;
  dataLabel: string;
  sortOrder: number;
  disabledFlag: boolean;
  remark: string;
}

export interface DictQueryForm {
  pageNum: number;
  pageSize: number;
  searchWord?: string;
  deletedFlag?: boolean;
}

export interface DictAddForm {
  dictCode: string;
  dictName: string;
  remark?: string;
}

export interface DictUpdateForm extends DictAddForm {
  dictId: number;
}

export interface DictDataAddForm {
  dictId: number;
  dataLabel: string;
  dataValue: string;
  sortOrder?: number;
  remark?: string;
}

export interface DictDataUpdateForm extends DictDataAddForm {
  dictDataId: number;
}
