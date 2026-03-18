/**
 * Language Switcher Component
 * 語言切換器組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { Dropdown, Button } from 'antd';
import type { MenuProps } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { useSelector, useDispatch } from 'react-redux';
import type { RootState } from '@/store';
import { setLanguage } from '@/store/slices/appConfigSlice';
import type { LanguageType } from '@/types/appConfig';

/**
 * 語言選項配置
 * 注意：目前僅支持中文和英文，其他語言待後續擴展
 */
const languageOptions: Array<{ key: LanguageType; label: string }> = [
  { key: 'zh_CN', label: '简体中文' },
  { key: 'en', label: 'English' },
  // { key: 'ru', label: 'Русский' }, // 待後續擴展
  // { key: 'ja', label: '日本語' }, // 待後續擴展
  // { key: 'ko', label: '한국어' }, // 待後續擴展
];

/**
 * 語言切換器組件
 */
export default function LanguageSwitcher() {
  const dispatch = useDispatch();
  const currentLanguage = useSelector((state: RootState) => state.appConfig.language);

  // 處理語言切換
  const handleLanguageChange = (language: LanguageType) => {
    dispatch(setLanguage(language));
  };

  // 下拉菜單項配置
  const menuItems: MenuProps['items'] = languageOptions.map(option => ({
    key: option.key,
    label: option.label,
    onClick: () => handleLanguageChange(option.key),
  }));

  // 獲取當前語言的顯示文本
  const currentLanguageLabel =
    languageOptions.find(opt => opt.key === currentLanguage)?.label || '简体中文';

  return (
    <Dropdown menu={{ items: menuItems }} placement="bottomRight">
      <Button icon={<GlobalOutlined />} type="text">
        {currentLanguageLabel}
      </Button>
    </Dropdown>
  );
}
