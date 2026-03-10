/**
 * App Root Component
 * 應用根組件（集成路由系統 + 國際化）
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useEffect, useMemo } from 'react';
import { ConfigProvider } from 'antd';
import { RouterProvider } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { Locale } from 'antd/es/locale';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';
import i18n from 'i18next';
import type { RootState } from './store';
import type { LanguageType } from './types/appConfig';
import { router } from './router';

/**
 * 語言類型到 Ant Design Locale 的映射
 * 注意：目前僅支持中文和英文
 */
const languageToAntdLocale: Record<LanguageType, Locale> = {
  zh_CN: zhCN,
  en: enUS,
  ru: zhCN, // 待後續擴展，暫用中文
  ja: zhCN, // 待後續擴展，暫用中文
  ko: zhCN, // 待後續擴展，暫用中文
};

/**
 * 語言類型到 i18next 語言代碼的映射
 */
const languageToI18nCode: Record<LanguageType, string> = {
  zh_CN: 'zh_CN',
  en: 'en',
  ru: 'zh_CN', // 待後續擴展，暫用中文
  ja: 'zh_CN', // 待後續擴展，暫用中文
  ko: 'zh_CN', // 待後續擴展，暫用中文
};

/**
 * 應用根組件
 * 提供全局配置（Ant Design 動態語言包 + React Router + i18n 同步）
 */
function App() {
  const language = useSelector((state: RootState) => state.appConfig.language);

  // 根據 Redux 語言狀態動態獲取 Ant Design Locale
  const antdLocale = useMemo(() => languageToAntdLocale[language], [language]);

  // 同步 Redux 語言狀態到 i18next
  useEffect(() => {
    const i18nLang = languageToI18nCode[language];
    if (i18n.language !== i18nLang) {
      i18n.changeLanguage(i18nLang);
    }
  }, [language]);

  return (
    <ConfigProvider locale={antdLocale}>
      <RouterProvider router={router} />
    </ConfigProvider>
  );
}

export default App;
