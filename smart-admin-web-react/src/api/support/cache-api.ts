import { getRequest } from '@/api/base/request';

export const cacheApi = {
  getAllCacheNames: () => getRequest<string[]>('/support/cache/names'),
  getKeys: (cacheName: string) => getRequest<string[]>(`/support/cache/keys/${cacheName}`),
  remove: (cacheName: string) => getRequest<void>(`/support/cache/remove/${cacheName}`),
};
