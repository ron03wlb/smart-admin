/**
 * API Encrypt API
 * 接口加密、解密 API
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import request from '@/utils/request';
import type { ResponseDTO } from '@/types/response';

/**
 * 加密測試表單
 */
export interface EncryptTestForm {
  name: string;
  age: number;
}

/**
 * 加密測試 API
 */
export const apiEncryptApi = {
  /**
   * 測試請求加密
   */
  testRequestEncrypt: (params: EncryptTestForm): Promise<ResponseDTO<EncryptTestForm>> => {
    return request.post('/support/apiEncrypt/testRequestEncrypt', params);
  },

  /**
   * 測試返回加密
   */
  testResponseEncrypt: (params: EncryptTestForm): Promise<ResponseDTO<EncryptTestForm>> => {
    return request.post('/support/apiEncrypt/testResponseEncrypt', params);
  },

  /**
   * 測試請求和返回都加密
   */
  testDecryptAndEncrypt: (params: EncryptTestForm): Promise<ResponseDTO<EncryptTestForm>> => {
    return request.post('/support/apiEncrypt/testDecryptAndEncrypt', params);
  },

  /**
   * 測試數組加解密
   */
  testArray: (params: EncryptTestForm[]): Promise<ResponseDTO<EncryptTestForm[]>> => {
    return request.post('/support/apiEncrypt/testArray', params);
  },
};
