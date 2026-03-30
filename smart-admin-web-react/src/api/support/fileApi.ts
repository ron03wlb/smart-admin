/**
 * File API
 * 文件上傳 API
 *
 * 參考：Vue 版本 smart-admin-web/src/api/support/file-api.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/api/types/response';

/**
 * 文件上傳響應數據
 */
export interface FileUploadResponse {
  fileId: number;
  fileName: string;
  fileUrl: string;
  fileKey: string;
  fileType: string;
  fileSize: number;
}

/**
 * 文件分頁查詢表單
 */
export interface FileQueryForm {
  pageNum: number;
  pageSize: number;
  fileName?: string;
  fileKey?: string;
  folder?: number;
}

/**
 * 文件 API
 */
export const fileApi = {
  /**
   * 文件上傳 URL
   */
  uploadUrl: '/support/file/upload',

  /**
   * 上傳文件
   * @param formData FormData 對象
   * @param folder 文件夾類型
   */
  uploadFile: (formData: FormData, folder: number): Promise<ResponseDTO<FileUploadResponse>> => {
    return request.post(`/support/file/upload?folder=${folder}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  /**
   * 分頁查詢文件
   * @param params 查詢參數
   */
  queryPage: (params: FileQueryForm): Promise<ResponseDTO<any>> => {
    return request.post('/support/file/queryPage', params);
  },

  /**
   * 獲取文件 URL（根據 fileKey）
   * @param fileKey 文件 Key
   */
  getUrl: (fileKey: string): Promise<ResponseDTO<string>> => {
    return request.get(`/support/file/getFileUrl?fileKey=${fileKey}`);
  },

  /**
   * 下載文件（根據 fileKey）
   * @param fileKey 文件 Key
   */
  downLoadFile: (fileKey: string): Promise<void> => {
    return request
      .get('/support/file/downLoad', {
        params: { fileKey },
        responseType: 'blob',
      })
      .then((response: any) => {
        // 創建 blob URL 並觸發下載
        const blob = new Blob([response]);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileKey;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      });
  },
};
