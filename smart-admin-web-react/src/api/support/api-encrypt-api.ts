import { postRequest } from '@/api/base/request';

export interface ApiEncryptForm {
  name: string;
  age: number;
}

export const apiEncryptApi = {
  testRequestEncrypt: (data: ApiEncryptForm) =>
    postRequest<any>('/support/apiEncrypt/testRequestEncrypt', data),
  testResponseEncrypt: (data: ApiEncryptForm) =>
    postRequest<any>('/support/apiEncrypt/testResponseEncrypt', data),
  testDecryptAndEncrypt: (data: ApiEncryptForm) =>
    postRequest<any>('/support/apiEncrypt/testDecryptAndEncrypt', data),
  testArray: (data: ApiEncryptForm[]) =>
    postRequest<any>('/support/apiEncrypt/testArray', data),
};
