/**
 * Smart Loading Component + Imperative API
 *
 * Corresponds to Vue's components/framework/smart-loading/index.ts
 *
 * Declarative: Place <SmartLoading /> in App.tsx, shows fullscreen spinner when spin.loading is true.
 * Imperative: Call SmartLoading.show() / SmartLoading.hide() from anywhere (e.g., API interceptors).
 */
import React from 'react';
import { Spin } from 'antd';
import { useAppSelector } from '@/store/hooks';
import { store } from '@/store';
import { showLoading, hideLoading } from '@/store/slices/spinSlice';

const SmartLoadingOverlay: React.FC = () => {
  const loading = useAppSelector((state) => state.spin.loading);

  if (!loading) return null;

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.65)',
        zIndex: 9999,
      }}
    >
      <Spin size="large" />
    </div>
  );
};

/**
 * Imperative API — can be called outside of React components
 */
export const SmartLoading = {
  show: () => {
    store.dispatch(showLoading());
  },
  hide: () => {
    store.dispatch(hideLoading());
  },
};

export default SmartLoadingOverlay;
