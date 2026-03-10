/**
 * App Config Types
 * 應用配置類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

/**
 * 語言類型
 */
export type LanguageType = 'zh_CN' | 'en' | 'ru' | 'ja' | 'ko';

/**
 * 布局類型
 */
export type LayoutType = 'side' | 'side-expand' | 'top';

/**
 * 主題類型
 */
export type ThemeType = 'light' | 'dark';

/**
 * 標籤頁位置
 */
export type PageTagLocationType = 'left' | 'center' | 'right';

/**
 * 標籤頁樣式
 */
export type PageTagStyleType = 'default' | 'antd' | 'chrome';

/**
 * 應用配置
 */
export interface AppConfig {
  /** i18n 語言選擇 */
  language: LanguageType;
  /** 布局 */
  layout: LayoutType;
  /** 側邊菜單主題 */
  sideMenuTheme: ThemeType;
  /** 標籤頁位置 */
  pageTagLocation: PageTagLocationType;
  /** 側邊菜單寬度 */
  sideMenuWidth: number;
  /** 夜間模式 */
  darkModeFlag: boolean;
  /** 主題顏色索引 */
  colorIndex: number;
  /** 頂部菜單頁面寬度 */
  pageWidth: string;
  /** 圓角 */
  borderRadius: number;
  /** 菜單展開模式（單一展開） */
  menuSingleExpandFlag: boolean;
  /** 標籤頁 */
  pageTagFlag: boolean;
  /** 標籤頁樣式 */
  pageTagStyle: PageTagStyleType;
  /** 面包屑 */
  breadCrumbFlag: boolean;
  /** 頁腳 */
  footerFlag: boolean;
  /** 幫助文檔 */
  helpDocFlag: boolean;
  /** 幫助文檔默認展開 */
  helpDocExpandFlag: boolean;
  /** 水印 */
  watermarkFlag: boolean;
  /** 網站名稱 */
  websiteName: string;
  /** 主題顏色 */
  primaryColor: string;
  /** 緊湊模式 */
  compactFlag: boolean;
  /** 全屏模式 */
  fullScreenFlag: boolean;
}
