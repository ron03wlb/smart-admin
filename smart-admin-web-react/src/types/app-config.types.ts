/**
 * App Configuration Types
 *
 * Corresponds to Vue's config/app-config.ts + types/config.ts
 */

export interface AppConfigState {
  /** i18n language */
  language: 'zh_CN' | 'en_US';
  /** Layout mode */
  layout: 'side' | 'side-expand' | 'top' | 'top-expand';
  /** Side menu width (px) */
  sideMenuWidth: number;
  /** Page tag location */
  pageTagLocation: 'top' | 'center';
  /** Dark mode */
  darkModeFlag: boolean;
  /** Side menu theme */
  sideMenuTheme: 'dark' | 'light';
  /** Theme color index */
  colorIndex: number;
  /** Top menu page width (px or %) */
  pageWidth: string;
  /** Border radius */
  borderRadius: number;
  /** Single menu expand mode */
  menuSingleExpandFlag: boolean;
  /** Show page tags */
  pageTagFlag: boolean;
  /** Page tag style */
  pageTagStyle: 'default' | 'antd' | 'chrome';
  /** Show breadcrumb */
  breadCrumbFlag: boolean;
  /** Show footer */
  footerFlag: boolean;
  /** Show help doc */
  helpDocFlag: boolean;
  /** Help doc default expanded */
  helpDocExpandFlag: boolean;
  /** Show watermark */
  watermarkFlag: boolean;
  /** Website name */
  websiteName: string;
  /** Primary theme color */
  primaryColor: string;
  /** Compact mode */
  compactFlag: boolean;
  /** Full screen mode (runtime only) */
  fullScreenFlag: boolean;
}
