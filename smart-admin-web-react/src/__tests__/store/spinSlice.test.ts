import { describe, it, expect } from 'vitest';
import spinReducer, { showLoading, hideLoading } from '@/store/slices/spinSlice';

describe('spinSlice', () => {
  it('should return initial state with loading=false', () => {
    const state = spinReducer(undefined, { type: 'unknown' });
    expect(state.loading).toBe(false);
  });

  it('should set loading=true on showLoading', () => {
    const state = spinReducer(undefined, showLoading());
    expect(state.loading).toBe(true);
  });

  it('should set loading=false on hideLoading', () => {
    const state = spinReducer({ loading: true }, hideLoading());
    expect(state.loading).toBe(false);
  });
});
