/**
 * App Config Slice
 * 應用配置狀態管理
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type {
  AppConfig,
  LanguageType,
  LayoutType,
  ThemeType,
  PageTagLocationType,
  PageTagStyleType,
} from '@/types/appConfig';
import { appDefaultConfig } from '@/config/appDefaultConfig';
import { STORAGE_KEYS } from '@/constants/storageKeys';

/**
 * 從 localStorage 讀取初始狀態
 */
function loadInitialState(): AppConfig {
  try {
    const savedConfig = localStorage.getItem(STORAGE_KEYS.APP_CONFIG);
    if (savedConfig) {
      const parsedConfig = JSON.parse(savedConfig) as AppConfig;
      // 合併默認配置和保存的配置（確保新增的字段有默認值）
      return { ...appDefaultConfig, ...parsedConfig };
    }
  } catch (error) {
    console.error('Failed to load app config from localStorage:', error);
  }
  return appDefaultConfig;
}

/**
 * 獲取初始化語言（用於 i18n 初始化）
 */
export const getInitializedLanguage = (): LanguageType => {
  try {
    const savedConfig = localStorage.getItem(STORAGE_KEYS.APP_CONFIG);
    if (savedConfig) {
      const parsedConfig = JSON.parse(savedConfig) as AppConfig;
      return parsedConfig.language || appDefaultConfig.language;
    }
  } catch (error) {
    console.error('Failed to get initialized language:', error);
  }
  return appDefaultConfig.language;
};

const initialState: AppConfig = loadInitialState();

const appConfigSlice = createSlice({
  name: 'appConfig',
  initialState,
  reducers: {
    /**
     * 重置為默認配置
     */
    reset: () => {
      return appDefaultConfig;
    },

    /**
     * 設置語言
     */
    setLanguage: (state, action: PayloadAction<LanguageType>) => {
      state.language = action.payload;
    },

    /**
     * 設置布局
     */
    setLayout: (state, action: PayloadAction<LayoutType>) => {
      state.layout = action.payload;
    },

    /**
     * 設置側邊菜單主題
     */
    setSideMenuTheme: (state, action: PayloadAction<ThemeType>) => {
      state.sideMenuTheme = action.payload;
    },

    /**
     * 設置標籤頁位置
     */
    setPageTagLocation: (state, action: PayloadAction<PageTagLocationType>) => {
      state.pageTagLocation = action.payload;
    },

    /**
     * 設置側邊菜單寬度
     */
    setSideMenuWidth: (state, action: PayloadAction<number>) => {
      state.sideMenuWidth = action.payload;
    },

    /**
     * 切換夜間模式
     */
    toggleDarkMode: state => {
      state.darkModeFlag = !state.darkModeFlag;
    },

    /**
     * 設置主題顏色索引
     */
    setColorIndex: (state, action: PayloadAction<number>) => {
      state.colorIndex = action.payload;
    },

    /**
     * 設置主題顏色
     */
    setPrimaryColor: (state, action: PayloadAction<string>) => {
      state.primaryColor = action.payload;
    },

    /**
     * 設置頁面寬度
     */
    setPageWidth: (state, action: PayloadAction<string>) => {
      state.pageWidth = action.payload;
    },

    /**
     * 設置圓角
     */
    setBorderRadius: (state, action: PayloadAction<number>) => {
      state.borderRadius = action.payload;
    },

    /**
     * 切換菜單單一展開模式
     */
    toggleMenuSingleExpand: state => {
      state.menuSingleExpandFlag = !state.menuSingleExpandFlag;
    },

    /**
     * 切換標籤頁顯示
     */
    togglePageTag: state => {
      state.pageTagFlag = !state.pageTagFlag;
    },

    /**
     * 設置標籤頁樣式
     */
    setPageTagStyle: (state, action: PayloadAction<PageTagStyleType>) => {
      state.pageTagStyle = action.payload;
    },

    /**
     * 切換面包屑顯示
     */
    toggleBreadCrumb: state => {
      state.breadCrumbFlag = !state.breadCrumbFlag;
    },

    /**
     * 切換頁腳顯示
     */
    toggleFooter: state => {
      state.footerFlag = !state.footerFlag;
    },

    /**
     * 切換幫助文檔顯示
     */
    toggleHelpDoc: state => {
      state.helpDocFlag = !state.helpDocFlag;
    },

    /**
     * 顯示幫助文檔
     */
    showHelpDoc: state => {
      state.helpDocExpandFlag = true;
    },

    /**
     * 隱藏幫助文檔
     */
    hideHelpDoc: state => {
      state.helpDocExpandFlag = false;
    },

    /**
     * 切換水印顯示
     */
    toggleWatermark: state => {
      state.watermarkFlag = !state.watermarkFlag;
    },

    /**
     * 設置網站名稱
     */
    setWebsiteName: (state, action: PayloadAction<string>) => {
      state.websiteName = action.payload;
    },

    /**
     * 切換緊湊模式
     */
    toggleCompact: state => {
      state.compactFlag = !state.compactFlag;
    },

    /**
     * 開始全屏
     */
    startFullScreen: state => {
      state.fullScreenFlag = true;
    },

    /**
     * 退出全屏
     */
    exitFullScreen: state => {
      state.fullScreenFlag = false;
    },

    /**
     * 更新配置（批量更新）
     */
    updateConfig: (state, action: PayloadAction<Partial<AppConfig>>) => {
      return { ...state, ...action.payload };
    },
  },
});

export const {
  reset,
  setLanguage,
  setLayout,
  setSideMenuTheme,
  setPageTagLocation,
  setSideMenuWidth,
  toggleDarkMode,
  setColorIndex,
  setPrimaryColor,
  setPageWidth,
  setBorderRadius,
  toggleMenuSingleExpand,
  togglePageTag,
  setPageTagStyle,
  toggleBreadCrumb,
  toggleFooter,
  toggleHelpDoc,
  showHelpDoc,
  hideHelpDoc,
  toggleWatermark,
  setWebsiteName,
  toggleCompact,
  startFullScreen,
  exitFullScreen,
  updateConfig,
} = appConfigSlice.actions;

export default appConfigSlice.reducer;
