/**
 * File Types
 */

export interface FileVO {
  fileId: number;
  fileKey: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
  creatorId: number;
  creatorName: string;
  createTime: string;
}

export interface FileQueryForm {
  pageNum: number;
  pageSize: number;
  fileKey?: string;
  fileName?: string;
  fileType?: string;
  startDate?: string;
  endDate?: string;
  createTimeBegin?: string;
  createTimeEnd?: string;
}

/** File folder type enum */
export const FILE_FOLDER_TYPE = {
  COMMON: 1,
  NOTICE: 2,
  HELP_DOC: 3,
  FEEDBACK: 4,
} as const;
