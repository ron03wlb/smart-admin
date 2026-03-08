/**
 * CodeGenerator utility functions
 *
 * Type mappings (SQL → Java/JS/Component) and enum detection.
 */

// ==================== Java Type Mapping ====================

const JavaTypeMap = new Map<string, string>([
  ['bit', 'Boolean'], ['bool', 'Boolean'],
  ['int', 'Integer'], ['int2', 'Integer'], ['int4', 'Integer'],
  ['tinyint', 'Integer'], ['smallint', 'Integer'], ['integer', 'Integer'], ['year', 'Integer'],
  ['bigint', 'Long'], ['int8', 'Long'],
  ['float', 'BigDecimal'], ['double', 'BigDecimal'], ['decimal', 'BigDecimal'],
  ['char', 'String'], ['varchar', 'String'], ['nvarchar', 'String'],
  ['tinytext', 'String'], ['text', 'String'], ['longtext', 'String'], ['blob', 'String'],
  ['date', 'LocalDate'],
  ['datetime', 'LocalDateTime'], ['datetime2', 'LocalDateTime'],
  ['timestamp', 'LocalDateTime'], ['timestamp without time zone', 'LocalDateTime'],
]);

export const JavaTypeList = ['Boolean', 'Integer', 'Long', 'Double', 'String', 'BigDecimal', 'LocalDate', 'LocalDateTime'];

export function getJavaType(dataType: string): string {
  return JavaTypeMap.get(dataType) ?? 'String';
}

// ==================== JS Type Mapping ====================

const JsTypeMap = new Map<string, string>([
  ['bit', 'Boolean'], ['bool', 'Boolean'],
  ['int', 'Number'], ['int2', 'Number'], ['int4', 'Number'], ['int8', 'Number'],
  ['tinyint', 'Number'], ['smallint', 'Number'], ['integer', 'Number'],
  ['year', 'Number'], ['bigint', 'Number'],
  ['float', 'Number'], ['double', 'Number'], ['decimal', 'Number'],
  ['char', 'String'], ['varchar', 'String'], ['nvarchar', 'String'], ['character', 'String'],
  ['tinytext', 'String'], ['text', 'String'], ['longtext', 'String'], ['blob', 'String'],
  ['date', 'Date'], ['datetime', 'Date'], ['datetime2', 'Date'],
  ['timestamp', 'Date'], ['timestamp without time zone', 'Date'],
]);

export const JsTypeList = ['Boolean', 'Number', 'String', 'Date'];

export function getJsType(dataType: string): string {
  return JsTypeMap.get(dataType) ?? 'String';
}

// ==================== Frontend Component Mapping ====================

const FrontComponentMap = new Map<string, string>([
  ['bit', 'BooleanSelect'], ['bool', 'BooleanSelect'],
  ['int', 'InputNumber'], ['int2', 'InputNumber'], ['int4', 'InputNumber'], ['int8', 'InputNumber'],
  ['tinyint', 'InputNumber'], ['smallint', 'InputNumber'], ['integer', 'InputNumber'],
  ['bigint', 'InputNumber'], ['float', 'InputNumber'], ['double', 'InputNumber'], ['decimal', 'InputNumber'],
  ['year', 'Date'], ['timestamp', 'Date'],
  ['char', 'Input'], ['varchar', 'Input'], ['nvarchar', 'Input'], ['character', 'Input'], ['tinytext', 'Input'],
  ['text', 'Textarea'], ['longtext', 'Textarea'],
  ['blob', 'FileUpload'],
  ['date', 'Date'], ['datetime', 'DateTime'], ['datetime2', 'DateTime'],
  ['timestamp without time zone', 'DateTime'],
]);

export function getFrontComponent(dataType: string): string {
  return FrontComponentMap.get(dataType) ?? 'Input';
}

// ==================== Enum Detection ====================

/** Convert string to PascalCase */
export function convertUpperCamel(str: string): string {
  return str.replace(/(^|[-_])(\w)/g, (_, _sep, char) => char.toUpperCase());
}

/** Convert string to camelCase */
export function convertLowerCamel(str: string): string {
  const pascal = convertUpperCamel(str);
  return pascal.charAt(0).toLowerCase() + pascal.slice(1);
}

/** Convert PascalCase/camelCase to kebab-case */
export function convertLowerHyphen(str: string): string {
  return str.replace(/([a-z0-9])([A-Z])/g, '$1-$2').replace(/_/g, '-').toLowerCase();
}

/** Build Java enum class name from module + column */
export function convertJavaEnumName(moduleName: string, columnName: string): string {
  return moduleName + convertUpperCamel(columnName) + 'Enum';
}

/** Check if column comment contains enum definition ([key:value] or 【key:value】) */
export function checkExistEnum(comment: string): boolean {
  if (!comment) return false;

  let leftIdx = comment.indexOf('[');
  if (leftIdx === -1) leftIdx = comment.indexOf('【');

  let rightIdx = comment.indexOf(']');
  if (rightIdx === -1) rightIdx = comment.indexOf('】');

  if (leftIdx === -1 || rightIdx === -1) return false;
  return comment.indexOf(':') !== -1;
}

// ==================== File Lists ====================

export const LANGUAGE_LIST = ['js', 'ts', 'java'];

export const JS_FILE_LIST = ['js/list.vue', 'js/form.vue', 'js/api.js', 'js/const.js'];
export const TS_FILE_LIST = ['ts/list.vue', 'ts/form.vue', 'ts/api.ts', 'ts/const.ts'];

export const JAVA_DOMAIN_FILE_LIST = ['Entity.java', 'AddForm.java', 'UpdateForm.java', 'QueryForm.java', 'VO.java'];
export const JAVA_FILE_LIST = [
  'Controller.java', 'Service.java', 'Manager.java', 'Dao.java', 'Mapper.xml',
  ...JAVA_DOMAIN_FILE_LIST,
  'Menu.sql',
];

// ==================== Enum Constants ====================

export const CODE_FRONT_COMPONENT_LIST = [
  { value: 'Input', label: '输入框' },
  { value: 'InputNumber', label: '数字输入框' },
  { value: 'Textarea', label: '文本' },
  { value: 'BooleanSelect', label: '布尔下拉框' },
  { value: 'SmartEnumSelect', label: '枚举下拉框' },
  { value: 'DictSelect', label: '字典下拉' },
  { value: 'Date', label: '日期选择' },
  { value: 'DateTime', label: '时间选择' },
  { value: 'FileUpload', label: '文件上传' },
];

export const CODE_PAGE_TYPE_LIST = [
  { value: 'modal', label: '弹窗' },
  { value: 'drawer', label: '抽屉' },
  { value: 'Page', label: '新页面' },
];

export const CODE_DELETE_TYPE_LIST = [
  { value: 'Single', label: '单个删除' },
  { value: 'Batch', label: '批量删除' },
  { value: 'SingleAndBatch', label: '单个删除和批量删除' },
];

export const CODE_QUERY_TYPE_LIST = [
  { value: 'Like', label: '模糊查询' },
  { value: 'Equal', label: '等于查询' },
  { value: 'DateRange', label: '日期范围' },
  { value: 'Date', label: '指定日期' },
  { value: 'Enum', label: '枚举' },
  { value: 'Dict', label: '字典' },
];
