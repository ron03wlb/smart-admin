/**
 * Code Generator API
 * 代碼生成器 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import type { PageResult, ResponseDTO } from '@/api/types/response';
import request from '@/utils/request';

/**
 * 代碼生成器 - 表信息
 */
export interface TableInfo {
  tableName: string;
  tableComment: string;
  configTime?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 代碼生成器 - 表查詢表單
 */
export interface TableQueryForm {
  tableNameKeywords?: string;
  pageNum: number;
  pageSize: number;
  searchCount?: boolean;
}

/**
 * 代碼生成器 - 表列信息
 */
export interface TableColumn {
  columnName: string;
  columnType: string;
  columnComment: string;
  nullable: string;
  columnKey: string;
  extra: string;
}

/**
 * 代碼生成器 - 基礎配置
 */
export interface BasicConfig {
  moduleName?: string;
  javaPackageName?: string;
  description?: string;
  frontAuthor?: string;
  frontDate?: string;
  backendAuthor?: string;
  backendDate?: string;
  copyright?: string;
}

/**
 * 代碼生成器 - 配置信息
 */
export interface CodeGeneratorConfig {
  basic?: BasicConfig;
  fields?: any[];
  insertAndUpdate?: any;
  deleteInfo?: any;
  queryFields?: any[];
  tableFields?: any[];
}

/**
 * 代碼生成器 - 更新配置請求
 */
export interface UpdateConfigRequest {
  tableName: string;
  basic?: BasicConfig;
  fields?: any[];
  insertAndUpdate?: any;
  deleteInfo?: any;
  queryFields?: any[];
  tableFields?: any[];
}

/**
 * 代碼生成器 - 預覽請求
 */
export interface PreviewRequest {
  tableName: string;
  templateFile: string;
}

export const codeGeneratorApi = {
  /**
   * 查詢數據庫的表
   */
  queryTableList: (params: TableQueryForm): Promise<ResponseDTO<PageResult<TableInfo>>> => {
    return request.post('/support/codeGenerator/table/queryTableList', params);
  },

  /**
   * 查詢表的列
   */
  getTableColumns: (tableName: string): Promise<ResponseDTO<TableColumn[]>> => {
    return request.get(`/support/codeGenerator/table/getTableColumns/${tableName}`);
  },

  /**
   * 獲取表的配置信息
   */
  getConfig: (tableName: string): Promise<ResponseDTO<CodeGeneratorConfig>> => {
    return request.get(`/support/codeGenerator/table/getConfig/${tableName}`);
  },

  /**
   * 更新配置信息
   */
  updateConfig: (params: UpdateConfigRequest): Promise<ResponseDTO<void>> => {
    return request.post('/support/codeGenerator/table/updateConfig', params);
  },

  /**
   * 預覽代碼
   */
  preview: (params: PreviewRequest): Promise<ResponseDTO<string>> => {
    return request.post('/support/codeGenerator/code/preview', params);
  },

  /**
   * 下載代碼
   */
  downloadCode: (tableName: string): void => {
    // 使用 window.location 觸發下載
    window.location.href = `/api/support/codeGenerator/code/download/${tableName}`;
  },
};
