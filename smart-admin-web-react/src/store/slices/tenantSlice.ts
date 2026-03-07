/**
 * Multi-Tenant Redux Slice
 *
 * Manages current tenant context.
 * Corresponds to Vue's store/modules/system/tenant.ts
 *
 * The tenant ID and timezone are also used by request.ts interceptor
 * to set X-Tenant-Id and X-Timezone headers.
 */
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '@/store';

export interface TenantState {
  tenantId: string;
  tenantName: string;
  timezone: string;
}

const initialState: TenantState = {
  tenantId: '',
  tenantName: '',
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
};

const tenantSlice = createSlice({
  name: 'tenant',
  initialState,
  reducers: {
    setTenantInfo(state, action: PayloadAction<Partial<TenantState>>) {
      Object.assign(state, action.payload);
    },
    clearTenantInfo() {
      return {
        tenantId: '',
        tenantName: '',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      };
    },
  },
});

export const { setTenantInfo, clearTenantInfo } = tenantSlice.actions;

export const selectTenantId = (state: RootState) => state.tenant.tenantId;
export const selectTenantName = (state: RootState) => state.tenant.tenantName;
export const selectTimezone = (state: RootState) => state.tenant.timezone;

export default tenantSlice.reducer;
