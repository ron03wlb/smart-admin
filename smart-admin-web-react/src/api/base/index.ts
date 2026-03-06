/**
 * API 層基礎模塊導出
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */

// 導出類型定義
export * from './response.model';
export * from './page.model';

// 導出請求方法
export {
  request,
  getRequest,
  postRequest,
  putRequest,
  deleteRequest,
  postDownload,
  getDownload,
} from './request';
