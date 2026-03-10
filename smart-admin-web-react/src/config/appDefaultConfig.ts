/**
 * App Default Config
 * 應用默認配置
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import type { AppConfig } from '@/types/appConfig';

/**
 * 應用默認配置
 */
export const appDefaultConfig: AppConfig = {
  // i18n 語言選擇
  language: 'zh_CN',
  // 布局: side 或者 side-expand 或者 top
  layout: 'side',
  // 側邊菜單寬度，默認為 200px
  sideMenuWidth: 200,
  // 標籤頁位置
  pageTagLocation: 'center',
  // 夜間模式
  darkModeFlag: false,
  // 菜單主題
  sideMenuTheme: 'dark',
  // 主題顏色索引
  colorIndex: 0,
  // 頂部菜單頁面寬度
  pageWidth: '99%',
  // 圓角
  borderRadius: 6,
  // 菜單展開模式
  menuSingleExpandFlag: true,
  // 標籤頁
  pageTagFlag: true,
  // 標籤頁樣式: default、antd、chrome
  pageTagStyle: 'chrome',
  // 面包屑
  breadCrumbFlag: true,
  // 頁腳
  footerFlag: true,
  // 幫助文檔
  helpDocFlag: true,
  // 幫助文檔默認展開
  helpDocExpandFlag: false,
  // 水印
  watermarkFlag: true,
  // 網站名稱
  websiteName: 'SmartAdmin 3.X',
  // 主題顏色
  primaryColor: '#1677ff',
  // 緊湊
  compactFlag: false,
  // 全屏
  fullScreenFlag: false,
};
