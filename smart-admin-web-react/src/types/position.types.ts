/**
 * Position Types
 */

export interface PositionVO {
  positionId: number;
  positionName: string;
  level: string;
  sortValue: number;
  remark: string;
}

export interface PositionQueryForm {
  pageNum: number;
  pageSize: number;
  positionName?: string;
}

export interface PositionAddForm {
  positionName: string;
  level?: string;
  sortValue?: number;
  remark?: string;
}

export interface PositionUpdateForm extends PositionAddForm {
  positionId: number;
}
