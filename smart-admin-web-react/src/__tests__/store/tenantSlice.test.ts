import { describe, it, expect } from 'vitest';
import tenantReducer, { setTenantInfo, clearTenantInfo } from '@/store/slices/tenantSlice';

describe('tenantSlice', () => {
  it('should return initial state', () => {
    const state = tenantReducer(undefined, { type: 'unknown' });
    expect(state.tenantId).toBe('');
    expect(state.tenantName).toBe('');
    expect(state.timezone).toBeTruthy();
  });

  it('should set tenant info', () => {
    const state = tenantReducer(undefined, setTenantInfo({
      tenantId: 'T001',
      tenantName: 'Test Tenant',
    }));
    expect(state.tenantId).toBe('T001');
    expect(state.tenantName).toBe('Test Tenant');
  });

  it('should clear tenant info', () => {
    const populated = tenantReducer(undefined, setTenantInfo({
      tenantId: 'T001',
      tenantName: 'Test Tenant',
      timezone: 'Asia/Taipei',
    }));
    const cleared = tenantReducer(populated, clearTenantInfo());
    expect(cleared.tenantId).toBe('');
    expect(cleared.tenantName).toBe('');
  });

  it('should partially update tenant info', () => {
    const state1 = tenantReducer(undefined, setTenantInfo({
      tenantId: 'T001',
      tenantName: 'Old Name',
    }));
    const state2 = tenantReducer(state1, setTenantInfo({ tenantName: 'New Name' }));
    expect(state2.tenantId).toBe('T001');
    expect(state2.tenantName).toBe('New Name');
  });
});
