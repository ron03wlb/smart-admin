/**
 * Message Types
 */

export interface MessageVO {
  messageId: number;
  title: string;
  content: string;
  messageType: number;
  readFlag: boolean;
  dataId?: number;
  receiverUserId: number;
  createTime: string;
}

export interface MessageQueryForm {
  pageNum: number;
  pageSize: number;
  keywords?: string;
  startDate?: string;
  endDate?: string;
  readFlag?: boolean;
  messageType?: number;
}

export interface MessageSendForm {
  title: string;
  content: string;
  messageType?: number;
  receiverUserIdList: number[];
  dataId?: number;
}
