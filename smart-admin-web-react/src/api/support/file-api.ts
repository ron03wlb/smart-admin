/**
 * File API
 *
 * Corresponds to Vue's api/support/file-api.ts
 */
import { postRequest, getDownload } from '@/api/base/request';
import type { ResponseModel } from '@/api/base/response.model';
import type { PageResult } from '@/api/base/page.model';
import type { FileVO, FileQueryForm } from '@/types/file.types';
import axios from 'axios';
import { LocalStorageKey, localRead } from '@/utils/local-storage';

const BASE_URL = import.meta.env.VITE_APP_API_URL || '/api';

export const fileApi = {
  /** Upload file */
  uploadFile: (formData: FormData, folder: number): Promise<ResponseModel<FileVO>> => {
    return axios.post(`${BASE_URL}/support/file/upload`, formData, {
      params: { folder },
      headers: {
        'Content-Type': 'multipart/form-data',
        Authorization: `Bearer ${localRead(LocalStorageKey.USER_TOKEN) || ''}`,
      },
    }).then((res) => res.data);
  },

  /** Paginated query */
  queryPage: (data: FileQueryForm) => postRequest<PageResult<FileVO>>('/support/file/queryPage', data),

  /** Get file URL */
  getFileUrl: (fileKey: string): string => `${BASE_URL}/support/file/getFileUrl?fileKey=${fileKey}`,

  /** Download file */
  downloadFile: (fileKey: string) => getDownload(`/support/file/downLoad?fileKey=${fileKey}`),
};
