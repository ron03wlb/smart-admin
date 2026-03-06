/**
 * Vitest 測試環境設置
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import '@testing-library/jest-dom/vitest';

// 全局測試配置
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

// ================================= Ant Design Mocks =================================

/**
 * Mock window.matchMedia（Ant Design Grid 系統需要）
 */
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {}, // deprecated
    removeListener: () => {}, // deprecated
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => true,
  }),
});

/**
 * Mock ResizeObserver（Ant Design Table 等組件需要）
 */
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};
