/**
 * CodeGenerator API client
 *
 * 6 endpoints for table queries, configuration, code preview, and download.
 */
import { postRequest, getRequest, getDownload } from '@/api/base';
import type { PageResult } from '@/api/base/page.model';
import type {
  TableInfo,
  ColumnInfo,
  CodeGeneratorConfig,
  ConfigUpdateRequest,
  PreviewForm,
  CodeGeneratorQueryForm,
} from '@/types/code-generator.types';

export const codeGeneratorApi = {
  /** Query database tables with pagination */
  queryTableList(param: CodeGeneratorQueryForm) {
    return postRequest<PageResult<TableInfo>>('/support/codeGenerator/table/queryTableList', param);
  },

  /** Get table column definitions */
  getTableColumns(tableName: string) {
    return getRequest<ColumnInfo[]>(`/support/codeGenerator/table/getTableColumns/${tableName}`);
  },

  /** Get table configuration */
  getConfig(tableName: string) {
    return getRequest<CodeGeneratorConfig>(`/support/codeGenerator/table/getConfig/${tableName}`);
  },

  /** Update table configuration */
  updateConfig(param: ConfigUpdateRequest) {
    return postRequest('/support/codeGenerator/table/updateConfig', param);
  },

  /** Preview generated code */
  preview(param: PreviewForm) {
    return postRequest<string>('/support/codeGenerator/code/preview', param);
  },

  /** Download generated code as ZIP */
  downloadCode(tableName: string) {
    getDownload(`/support/codeGenerator/code/download/${tableName}`);
  },
};
