import { describe, it, expect } from 'vitest';
import tenantReducer, { setTenantInfo, clearTenantInfo } from '@/store/slices/tenantSlice';
import type { TenantInfo } from '@/types/tenant';

describe('tenantSlice', () => {
  it('should return initial state', () => {
    const state = tenantReducer(undefined, { type: 'unknown' });
    // Initial state loads from localStorage, defaults are:
    expect(state.tenantId).toBe(null);
    expect(state.timezone).toBeTruthy();
    expect(state.tenantCode).toBeTruthy();
  });

  it('should set tenant info', () => {
    const info: TenantInfo = {
      tenantId: 1001,
      timezone: 'Asia/Taipei',
      tenantCode: 'test-tenant',
    };
    const state = tenantReducer(undefined, setTenantInfo(info));
    expect(state.tenantId).toBe(1001);
    expect(state.timezone).toBe('Asia/Taipei');
    expect(state.tenantCode).toBe('test-tenant');
  });

  it('should clear tenant info', () => {
    const info: TenantInfo = {
      tenantId: 1001,
      timezone: 'Asia/Tokyo',
      tenantCode: 'test-tenant',
    };
    const populated = tenantReducer(undefined, setTenantInfo(info));
    const cleared = tenantReducer(populated, clearTenantInfo());
    expect(cleared.tenantId).toBe(null);
    expect(cleared.timezone).toBe('Asia/Taipei');
    expect(cleared.tenantCode).toBe('default');
  });

  it('should overwrite all fields when setting tenant info', () => {
    const info1: TenantInfo = {
      tenantId: 1001,
      timezone: 'Asia/Taipei',
      tenantCode: 'old-code',
    };
    const state1 = tenantReducer(undefined, setTenantInfo(info1));

    const info2: TenantInfo = {
      tenantId: 1002,
      timezone: 'UTC',
      tenantCode: 'new-code',
    };
    const state2 = tenantReducer(state1, setTenantInfo(info2));
    expect(state2.tenantId).toBe(1002);
    expect(state2.timezone).toBe('UTC');
    expect(state2.tenantCode).toBe('new-code');
  });
});
