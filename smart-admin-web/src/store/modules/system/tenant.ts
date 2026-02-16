/*
 * 租戶上下文管理
 *
 * @since G2.3
 */
import { defineStore } from 'pinia';
import localKey from '/@/constants/local-storage-key-const';
import { localRead, localSave, localRemove } from '/@/utils/local-util';

export const useTenantStore = defineStore({
  id: 'tenantStore',
  state: () => ({
    tenantId: null as number | null,
    timezone: '' as string,
    tenantCode: '' as string,
  }),
  getters: {
    getTenantId(state): number | null {
      if (state.tenantId) return state.tenantId;
      const stored = localRead(localKey.TENANT_ID);
      return stored ? Number(stored) : null;
    },
    getTimezone(state): string {
      if (state.timezone) return state.timezone;
      return localRead(localKey.TENANT_TIMEZONE) || 'Asia/Taipei';
    },
    getTenantCode(state): string {
      if (state.tenantCode) return state.tenantCode;
      return localRead(localKey.TENANT_CODE) || 'default';
    },
  },
  actions: {
    setTenantInfo(data: { tenantId: number; timezone: string; tenantCode: string }) {
      this.tenantId = data.tenantId;
      this.timezone = data.timezone;
      this.tenantCode = data.tenantCode;
      localSave(localKey.TENANT_ID, String(data.tenantId));
      localSave(localKey.TENANT_TIMEZONE, data.timezone);
      localSave(localKey.TENANT_CODE, data.tenantCode);
    },
    clearTenantInfo() {
      this.tenantId = null;
      this.timezone = '';
      this.tenantCode = '';
      localRemove(localKey.TENANT_ID);
      localRemove(localKey.TENANT_TIMEZONE);
      localRemove(localKey.TENANT_CODE);
    },
  },
});
