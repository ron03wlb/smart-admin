/**
 * Code Generator Utilities
 * 代碼生成器工具函數
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/code-generator-util.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

/**
 * 轉換為大寫駝峰（UpperCamelCase / PascalCase）
 * 例如：user_info → UserInfo
 */
export function convertUpperCamel(str: string): string {
  if (!str) return '';

  // 先轉為小寫，然後按 _ 分割
  return str
    .toLowerCase()
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join('');
}

/**
 * 轉換為小寫連字符（kebab-case）
 * 例如：UserInfo → user-info
 */
export function convertLowerHyphen(str: string): string {
  if (!str) return '';

  // 將大寫字母前插入連字符，然後轉為小寫
  return str
    .replace(/([A-Z])/g, '-$1')
    .toLowerCase()
    .replace(/^-/, ''); // 移除開頭的連字符
}

/**
 * 文件列表常量
 */
export const LANGUAGE_LIST = ['js', 'ts', 'java'];

export const JS_FILE_LIST = ['js/list.vue', 'js/form.vue', 'js/api.js', 'js/const.js'];

export const TS_FILE_LIST = ['ts/list.vue', 'ts/form.vue', 'ts/api.ts', 'ts/const.ts'];

export const JAVA_DOMAIN_FILE_LIST = [
  'Entity.java',
  'AddForm.java',
  'UpdateForm.java',
  'QueryForm.java',
  'VO.java',
];

export const JAVA_FILE_LIST = [
  'Controller.java',
  'Service.java',
  'Manager.java',
  'Dao.java',
  'Mapper.xml',
  ...JAVA_DOMAIN_FILE_LIST,
  'Menu.sql',
];
