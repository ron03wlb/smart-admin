/**
 * Position Types
 */

export interface PositionVO {
  positionId: number;
  positionName: string;
  positionLevel?: string;
  sort: number;
  remark?: string;
  createTime?: string;
}

export interface PositionQueryForm {
  pageNum: number;
  pageSize: number;
  keywords?: string;
}

export interface PositionAddForm {
  positionName: string;
  positionLevel?: string;
  sort?: number;
  remark?: string;
}

export interface PositionUpdateForm extends PositionAddForm {
  positionId: number;
}
