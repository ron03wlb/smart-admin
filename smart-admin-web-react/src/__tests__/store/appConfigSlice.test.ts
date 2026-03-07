import { describe, it, expect } from 'vitest';
import appConfigReducer, {
  updateAppConfig,
  resetAppConfig,
  toggleDarkMode,
  setLayout,
  setLanguage,
  toggleFullScreen,
  showHelpDoc,
  hideHelpDoc,
  APP_CONFIG_DEFAULTS,
} from '@/store/slices/appConfigSlice';
import type { AppConfigState } from '@/types/app-config.types';

describe('appConfigSlice', () => {
  it('should return default state', () => {
    const state = appConfigReducer(undefined, { type: 'unknown' });
    expect(state).toEqual(APP_CONFIG_DEFAULTS);
    expect(state.language).toBe('zh_CN');
    expect(state.layout).toBe('side');
    expect(state.darkModeFlag).toBe(false);
  });

  it('should toggle dark mode', () => {
    const state = appConfigReducer(APP_CONFIG_DEFAULTS, toggleDarkMode());
    expect(state.darkModeFlag).toBe(true);

    const state2 = appConfigReducer(state, toggleDarkMode());
    expect(state2.darkModeFlag).toBe(false);
  });

  it('should set layout mode', () => {
    const state = appConfigReducer(APP_CONFIG_DEFAULTS, setLayout('top'));
    expect(state.layout).toBe('top');
  });

  it('should set language', () => {
    const state = appConfigReducer(APP_CONFIG_DEFAULTS, setLanguage('en_US'));
    expect(state.language).toBe('en_US');
  });

  it('should update multiple fields', () => {
    const state = appConfigReducer(
      APP_CONFIG_DEFAULTS,
      updateAppConfig({
        darkModeFlag: true,
        sideMenuWidth: 250,
        colorIndex: 3,
      }),
    );
    expect(state.darkModeFlag).toBe(true);
    expect(state.sideMenuWidth).toBe(250);
    expect(state.colorIndex).toBe(3);
    // Unchanged fields preserved
    expect(state.language).toBe('zh_CN');
  });

  it('should reset to defaults', () => {
    const modified: AppConfigState = {
      ...APP_CONFIG_DEFAULTS,
      darkModeFlag: true,
      layout: 'top',
      language: 'en_US',
    };
    const state = appConfigReducer(modified, resetAppConfig());
    expect(state).toEqual(APP_CONFIG_DEFAULTS);
  });

  it('should toggle full screen', () => {
    const state = appConfigReducer(APP_CONFIG_DEFAULTS, toggleFullScreen());
    expect(state.fullScreenFlag).toBe(true);
  });

  it('should show/hide help doc', () => {
    const state = appConfigReducer(APP_CONFIG_DEFAULTS, showHelpDoc());
    expect(state.helpDocExpandFlag).toBe(true);

    const state2 = appConfigReducer(state, hideHelpDoc());
    expect(state2.helpDocExpandFlag).toBe(false);
  });
});
