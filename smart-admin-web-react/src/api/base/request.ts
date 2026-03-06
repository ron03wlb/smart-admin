/**
 * Axios 請求配置
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import axios from 'axios';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import { message, Modal } from 'antd';
import { LocalStorageKey, localRead, localRemove } from '@/utils/local-storage';
import type { ResponseModel } from './response.model';
import { ErrorCode } from './response.model';

/**
 * Token 消息頭
 */
const TOKEN_HEADER = 'Authorization';

/**
 * 創建 Axios 實例
 */
const smartAxios = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL || '/api',
  timeout: 30000,
});

/**
 * 退出系統
 */
function logout() {
  // 清除 Token
  localRemove(LocalStorageKey.USER_TOKEN);
  localRemove(LocalStorageKey.USER_POINTS);

  // 跳轉到登錄頁
  window.location.href = '/';
}

// ================================= 請求攔截器 =================================

smartAxios.interceptors.request.use(
  (config) => {
    // 在發送請求之前添加 Token
    const token = localRead<string>(LocalStorageKey.USER_TOKEN);
    if (token) {
      config.headers[TOKEN_HEADER] = `Bearer ${token}`;
    } else {
      delete config.headers[TOKEN_HEADER];
    }

    // 多租戶 headers
    const tenantId = localRead<string>(LocalStorageKey.TENANT_ID);
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId;
    }

    const tenantTimezone = localRead<string>(LocalStorageKey.TENANT_TIMEZONE);
    if (tenantTimezone) {
      config.headers['X-Timezone'] = tenantTimezone;
    }

    return config;
  },
  (error) => {
    // 對請求錯誤做些什麼
    return Promise.reject(error);
  }
);

// ================================= 響應攔截器 =================================

smartAxios.interceptors.response.use(
  (response: AxiosResponse) => {
    // 根據 Content-Type 判斷是否為 JSON 數據
    const contentType =
      response.headers['content-type'] || response.headers['Content-Type'];

    if (contentType && contentType.indexOf('application/json') === -1) {
      // 非 JSON 數據（如文件下載）直接返回
      return response;
    }

    // 如果是 Blob 數據
    if (response.data && response.data instanceof Blob) {
      return Promise.reject(response.data);
    }

    const res = response.data as ResponseModel;

    // 成功響應
    if (res.code && res.code === ErrorCode.SUCCESS) {
      return response;
    }

    // 錯誤響應處理
    if (res.code) {
      // Token 過期或賬號已在別處登錄
      if (
        res.code === ErrorCode.TOKEN_EXPIRED ||
        res.code === ErrorCode.ACCOUNT_LOGGED_ELSEWHERE
      ) {
        message.destroy();
        message.error('您沒有登錄，請重新登錄');
        setTimeout(logout, 300);
        return Promise.reject(response);
      }

      // 等保安全的登錄提醒
      if (
        res.code === ErrorCode.SECURITY_LOGIN_REMINDER_1 ||
        res.code === ErrorCode.SECURITY_LOGIN_REMINDER_2
      ) {
        Modal.error({
          title: '重要提醒',
          content: res.msg,
        });
        return Promise.reject(response);
      }

      // 長時間未操作系統，需要重新登錄
      if (res.code === ErrorCode.LONG_TIME_NO_OPERATION) {
        Modal.error({
          title: '重要提醒',
          content: res.msg,
          onOk: logout,
        });
        setTimeout(logout, 3000);
        return Promise.reject(response);
      }

      // 其他錯誤
      message.destroy();
      message.error(res.msg || '操作失敗');
      return Promise.reject(response);
    }

    return response;
  },
  (error) => {
    // 對響應錯誤做點什麼
    if (error.message && error.message.indexOf('timeout') !== -1) {
      message.destroy();
      message.error('網絡超時');
    } else if (error.message === 'Network Error') {
      message.destroy();
      message.error('網絡連接錯誤');
    } else if (error.message && error.message.indexOf('Request') !== -1) {
      message.destroy();
      message.error('網絡發生錯誤');
    }
    return Promise.reject(error);
  }
);

// ================================= 對外提供請求方法 =================================

/**
 * 通用請求封裝
 */
export const request = <T = any>(config: AxiosRequestConfig): Promise<ResponseModel<T>> => {
  return smartAxios.request(config).then((response) => response.data);
};

/**
 * GET 請求
 */
export const getRequest = <T = any>(
  url: string,
  params?: any
): Promise<ResponseModel<T>> => {
  return request<T>({ url, method: 'get', params });
};

/**
 * POST 請求
 */
export const postRequest = <T = any>(
  url: string,
  data?: any
): Promise<ResponseModel<T>> => {
  return request<T>({ url, method: 'post', data });
};

/**
 * PUT 請求
 */
export const putRequest = <T = any>(
  url: string,
  data?: any
): Promise<ResponseModel<T>> => {
  return request<T>({ url, method: 'put', data });
};

/**
 * DELETE 請求
 */
export const deleteRequest = <T = any>(
  url: string,
  params?: any
): Promise<ResponseModel<T>> => {
  return request<T>({ url, method: 'delete', params });
};

// ================================= 文件下載 =================================

/**
 * POST 下載
 */
export const postDownload = (url: string, data?: any): void => {
  request({
    method: 'post',
    url,
    data,
    responseType: 'blob',
  })
    .then((response: any) => {
      handleDownloadData(response);
    })
    .catch((error) => {
      handleDownloadError(error);
    });
};

/**
 * GET 下載
 */
export const getDownload = (url: string, params?: any): void => {
  request({
    method: 'get',
    url,
    params,
    responseType: 'blob',
  })
    .then((response: any) => {
      handleDownloadData(response);
    })
    .catch((error) => {
      handleDownloadError(error);
    });
};

/**
 * 處理下載錯誤
 */
function handleDownloadError(error: any): void {
  if (error instanceof Blob) {
    const fileReader = new FileReader();
    fileReader.readAsText(error);
    fileReader.onload = () => {
      try {
        const msg = fileReader.result as string;
        const jsonMsg = JSON.parse(msg);
        message.destroy();
        message.error(jsonMsg.msg || '下載失敗');
      } catch {
        message.destroy();
        message.error('下載失敗');
      }
    };
  } else {
    message.destroy();
    message.error('網絡發生錯誤');
  }
}

/**
 * 處理下載數據
 */
function handleDownloadData(response: AxiosResponse): void {
  if (!response) {
    return;
  }

  // 獲取返回類型
  const contentType =
    response.headers['content-type'] || response.headers['Content-Type'];

  // 構建下載數據
  const url = window.URL.createObjectURL(
    new Blob([response.data], { type: contentType })
  );
  const link = document.createElement('a');
  link.style.display = 'none';
  link.href = url;

  // 從消息頭獲取文件名
  const contentDisposition =
    response.headers['content-disposition'] ||
    response.headers['Content-Disposition'];

  let filename = 'download';
  if (contentDisposition) {
    const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
    if (filenameMatch && filenameMatch[1]) {
      filename = filenameMatch[1].replace(/['"]/g, '');
      filename = decodeURIComponent(filename);
    }
  }

  link.setAttribute('download', filename);

  // 觸發點擊下載
  document.body.appendChild(link);
  link.click();

  // 下載完釋放
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

export default smartAxios;
