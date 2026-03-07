/**
 * Global Loading (Spin) Redux Slice
 *
 * Controls the global loading overlay.
 * Corresponds to Vue's SmartLoading.show() / SmartLoading.hide()
 */
import { createSlice } from '@reduxjs/toolkit';
import type { RootState } from '@/store';

export interface SpinState {
  loading: boolean;
}

const initialState: SpinState = {
  loading: false,
};

const spinSlice = createSlice({
  name: 'spin',
  initialState,
  reducers: {
    showLoading(state) {
      state.loading = true;
    },
    hideLoading(state) {
      state.loading = false;
    },
  },
});

export const { showLoading, hideLoading } = spinSlice.actions;

export const selectLoading = (state: RootState) => state.spin.loading;

export default spinSlice.reducer;
