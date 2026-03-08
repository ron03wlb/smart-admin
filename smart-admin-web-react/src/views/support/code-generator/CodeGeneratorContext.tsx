/**
 * CodeGenerator React Context
 *
 * Replaces Vue provide/inject for sharing table info, columns, and config
 * across all 6 tab components in the config drawer.
 */
import { createContext, useContext } from 'react';
import type { ColumnInfo, CodeGeneratorConfig } from '@/types/code-generator.types';

export interface TableInfo {
  tableName: string;
  tableComment: string;
  createTime?: string;
  updateTime?: string;
}

export interface CodeGeneratorContextType {
  tableInfo: TableInfo;
  tableColumns: ColumnInfo[];
  tableConfig: CodeGeneratorConfig | null;
}

export const CodeGeneratorContext = createContext<CodeGeneratorContextType>({
  tableInfo: { tableName: '', tableComment: '' },
  tableColumns: [],
  tableConfig: null,
});

export function useCodeGeneratorContext() {
  return useContext(CodeGeneratorContext);
}
