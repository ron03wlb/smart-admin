/**
 * Table Column API
 *
 * Manages user's table column preferences.
 * Corresponds to Vue's api/support/table-column-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';

export interface TableColumnItem {
  columnKey: string;
  columnName: string;
  sortValue: number;
  showFlag: boolean;
}

export const tableColumnApi = {
  /** Get columns config for a table */
  getColumns: (tableId: number) => getRequest<TableColumnItem[]>(`/support/tableColumn/getColumns/${tableId}`),

  /** Update columns config */
  update: (data: { tableId: number; columnList: TableColumnItem[] }) =>
    postRequest<void>('/support/tableColumn/update', data),

  /** Delete columns config */
  delete: (tableId: number) => getRequest<void>(`/support/tableColumn/delete/${tableId}`),
};
