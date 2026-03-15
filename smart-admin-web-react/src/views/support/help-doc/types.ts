/**
 * Help Doc Types
 * 幫助文檔類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 幫助文檔目錄 VO
 */
export interface HelpDocCatalogVO {
  helpDocCatalogId: number;
  name: string;
  parentId: number;
  sort: number;
  children?: HelpDocCatalogVO[];
  // 用於樹排序的前後節點ID
  preId?: number;
  nextId?: number;
}

/**
 * 幫助文檔目錄表單數據
 */
export interface HelpDocCatalogFormData {
  helpDocCatalogId?: number;
  name: string;
  parentId: number;
  sort: number;
}

/**
 * 幫助文檔 VO
 */
export interface HelpDocVO {
  helpDocId: number;
  helpDocCatalogId: number;
  helpDocCatalogName: string;
  title: string;
  author: string;
  sort: number;
  pageViewCount: number;
  userViewCount: number;
  attachment?: FileInfo[];
  relationList?: HelpDocRelation[];
  contentHtml: string;
  contentText: string;
  createTime: string;
  updateTime?: string;
}

/**
 * 幫助文檔查詢表單
 */
export interface HelpDocQueryForm {
  helpDocCatalogId?: number | null;
  keywords?: string;
  createTimeBegin?: string | null;
  createTimeEnd?: string | null;
  pageNum: number;
  pageSize: number;
}

/**
 * 幫助文檔表單數據
 */
export interface HelpDocFormData {
  helpDocId?: number;
  helpDocCatalogId?: number;
  title: string;
  author: string;
  sort: number;
  attachment: FileInfo[];
  relationList: HelpDocRelation[];
  contentHtml: string;
  contentText: string;
}

/**
 * 幫助文檔關聯（菜單或首頁）
 */
export interface HelpDocRelation {
  relationId: number;
  relationName: string;
}

/**
 * 文件信息
 */
export interface FileInfo {
  uid: string;
  name: string;
  url: string;
  size?: number;
}

/**
 * 幫助文檔詳情
 */
export interface HelpDocDetailVO extends HelpDocVO {
  // 可能包含更多詳情字段
}
