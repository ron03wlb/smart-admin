/**
 * i18n Configuration
 * 國際化配置
 *
 * 使用 react-i18next 進行國際化管理
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { getInitializedLanguage } from '@/store/slices/appConfigSlice';

// 語言包
import zhCN from './locales/zh-CN';
import enUS from './locales/en-US';

/**
 * 語言選擇列表
 */
export const i18nList = [
  {
    text: '簡體中文',
    value: 'zh_CN',
  },
  {
    text: 'English',
    value: 'en',
  },
];

/**
 * 語言資源
 */
const resources = {
  zh_CN: {
    translation: zhCN,
  },
  en: {
    translation: enUS,
  },
};

/**
 * 初始化 i18next
 */
i18n
  .use(initReactI18next) // 傳遞 i18n 實例給 react-i18next
  .init({
    resources,
    lng: getInitializedLanguage(), // 默認語言
    fallbackLng: 'zh_CN', // 備用語言
    interpolation: {
      escapeValue: false, // React 已經做了 XSS 防護
    },
    react: {
      useSuspense: false, // 禁用 Suspense（避免加載問題）
    },
  });

export default i18n;
