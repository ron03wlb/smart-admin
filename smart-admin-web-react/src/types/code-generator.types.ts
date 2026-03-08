/**
 * CodeGenerator TypeScript type definitions
 *
 * Mirrors the Vue CodeGenerator types for full feature parity.
 */

// ==================== Table & Column ====================

export interface TableInfo {
  configId?: string;
  tableName: string;
  tableComment: string;
  configTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ColumnInfo {
  columnName: string;
  columnComment: string;
  dataType: string;
  nullableFlag: boolean;
  primaryKeyFlag: boolean;
  autoIncreaseFlag: boolean;
}

// ==================== Basic Config ====================

export interface BasicConfig {
  moduleName: string;
  javaPackageName: string;
  description: string;
  frontAuthor: string;
  frontDate: string;
  backendAuthor: string;
  backendDate: string;
  copyright: string;
}

// ==================== Field Config ====================

export interface FieldConfig {
  columnName: string;
  columnComment: string;
  label: string;
  fieldName: string;
  javaType: string;
  jsType: string;
  dict?: string;
  enumName?: string;
  primaryKeyFlag: boolean;
  autoIncreaseFlag: boolean;
}

// ==================== Insert & Update ====================

export interface InsertAndUpdateConfig {
  isSupportInsertAndUpdate: boolean;
  pageType: string;
  width?: string;
  countPerLine: number;
}

export interface InsertAndUpdateField extends FieldConfig {
  requiredFlag: boolean;
  insertFlag: boolean;
  updateFlag: boolean;
  frontComponent: string;
}

// ==================== Delete ====================

export interface DeleteConfig {
  deleteType: string;
  deleteName?: string;
}

// ==================== Query Fields ====================

export interface QueryFieldConfig extends FieldConfig {
  queryType: string;
  visible: boolean;
}

// ==================== Table Fields ====================

export interface TableFieldConfig extends FieldConfig {
  visible: boolean;
  width?: number;
}

// ==================== Full Config ====================

export interface CodeGeneratorConfig {
  tableName: string;
  basic: BasicConfig;
  fields: FieldConfig[];
  insertAndUpdate: InsertAndUpdateConfig;
  insertAndUpdateFields: InsertAndUpdateField[];
  deleteInfo: DeleteConfig;
  queryFields: QueryFieldConfig[];
  tableFields: TableFieldConfig[];
}

// ==================== Config Update Request ====================

export interface ConfigUpdateRequest {
  tableName: string;
  basic: BasicConfig;
  fields: FieldConfig[];
  insertAndUpdate: InsertAndUpdateConfig;
  insertAndUpdateFields: InsertAndUpdateField[];
  deleteInfo: DeleteConfig;
  queryFields: QueryFieldConfig[];
  tableFields: TableFieldConfig[];
}

// ==================== Preview ====================

export interface PreviewForm {
  tableName: string;
  language: string;
  fileKey: string;
}

// ==================== Query Form ====================

export interface CodeGeneratorQueryForm {
  pageNum: number;
  pageSize: number;
  tableNameKeywords?: string;
}
