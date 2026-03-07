/**
 * App Configuration Redux Slice
 *
 * Manages layout, theme, language, and UI preferences.
 * Corresponds to Vue's store/modules/system/app-config.ts
 */
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '@/store';
import type { AppConfigState } from '@/types/app-config.types';

export const APP_CONFIG_DEFAULTS: AppConfigState = {
  language: 'zh_CN',
  layout: 'side',
  sideMenuWidth: 200,
  pageTagLocation: 'center',
  darkModeFlag: false,
  sideMenuTheme: 'dark',
  colorIndex: 0,
  pageWidth: '99%',
  borderRadius: 6,
  menuSingleExpandFlag: true,
  pageTagFlag: true,
  pageTagStyle: 'chrome',
  breadCrumbFlag: true,
  footerFlag: true,
  helpDocFlag: true,
  helpDocExpandFlag: false,
  watermarkFlag: true,
  websiteName: 'SmartAdmin 3.X',
  primaryColor: '#1677ff',
  compactFlag: false,
  fullScreenFlag: false,
};

const appConfigSlice = createSlice({
  name: 'appConfig',
  initialState: { ...APP_CONFIG_DEFAULTS },
  reducers: {
    /** Update one or more config fields */
    updateAppConfig(state, action: PayloadAction<Partial<AppConfigState>>) {
      Object.assign(state, action.payload);
    },
    /** Reset all config to defaults */
    resetAppConfig() {
      return { ...APP_CONFIG_DEFAULTS };
    },
    /** Toggle dark mode */
    toggleDarkMode(state) {
      state.darkModeFlag = !state.darkModeFlag;
    },
    /** Set layout mode */
    setLayout(state, action: PayloadAction<AppConfigState['layout']>) {
      state.layout = action.payload;
    },
    /** Set language */
    setLanguage(state, action: PayloadAction<AppConfigState['language']>) {
      state.language = action.payload;
    },
    /** Toggle full screen */
    toggleFullScreen(state) {
      state.fullScreenFlag = !state.fullScreenFlag;
    },
    /** Show help doc */
    showHelpDoc(state) {
      state.helpDocExpandFlag = true;
    },
    /** Hide help doc */
    hideHelpDoc(state) {
      state.helpDocExpandFlag = false;
    },
  },
});

export const {
  updateAppConfig,
  resetAppConfig,
  toggleDarkMode,
  setLayout,
  setLanguage,
  toggleFullScreen,
  showHelpDoc,
  hideHelpDoc,
} = appConfigSlice.actions;

// Selectors
export const selectAppConfig = (state: RootState) => state.appConfig;
export const selectLanguage = (state: RootState) => state.appConfig.language;
export const selectLayout = (state: RootState) => state.appConfig.layout;
export const selectDarkMode = (state: RootState) => state.appConfig.darkModeFlag;
export const selectPageTagFlag = (state: RootState) => state.appConfig.pageTagFlag;
export const selectBreadCrumbFlag = (state: RootState) => state.appConfig.breadCrumbFlag;
export const selectFooterFlag = (state: RootState) => state.appConfig.footerFlag;
export const selectSideMenuTheme = (state: RootState) => state.appConfig.sideMenuTheme;
export const selectSideMenuWidth = (state: RootState) => state.appConfig.sideMenuWidth;
export const selectPrimaryColor = (state: RootState) => state.appConfig.primaryColor;
export const selectColorIndex = (state: RootState) => state.appConfig.colorIndex;
export const selectCompactFlag = (state: RootState) => state.appConfig.compactFlag;
export const selectWatermarkFlag = (state: RootState) => state.appConfig.watermarkFlag;
export const selectPageTagStyle = (state: RootState) => state.appConfig.pageTagStyle;

export type { AppConfigState };
export default appConfigSlice.reducer;
