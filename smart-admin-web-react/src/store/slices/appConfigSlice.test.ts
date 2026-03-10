/**
 * appConfigSlice Tests
 * appConfigSlice 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import appConfigReducer, {
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
  getInitializedLanguage,
} from './appConfigSlice';
import { appDefaultConfig } from '@/config/appDefaultConfig';
import type { AppConfig } from '@/types/appConfig';
import { STORAGE_KEYS } from '@/constants/storageKeys';

describe('appConfigSlice', () => {
  // 保存原始的 localStorage
  let originalLocalStorage: Storage;

  beforeEach(() => {
    originalLocalStorage = global.localStorage;
    // Mock localStorage
    const localStorageMock = (() => {
      let store: Record<string, string> = {};
      return {
        getItem: vi.fn((key: string) => store[key] || null),
        setItem: vi.fn((key: string, value: string) => {
          store[key] = value;
        }),
        removeItem: vi.fn((key: string) => {
          delete store[key];
        }),
        clear: vi.fn(() => {
          store = {};
        }),
        get length() {
          return Object.keys(store).length;
        },
        key: vi.fn((index: number) => Object.keys(store)[index] || null),
      };
    })();
    global.localStorage = localStorageMock as Storage;
  });

  afterEach(() => {
    global.localStorage = originalLocalStorage;
    vi.clearAllMocks();
  });

  describe('初始狀態', () => {
    it('應該返回默認配置', () => {
      const state = appConfigReducer(undefined, { type: '' });
      expect(state).toEqual(appDefaultConfig);
    });

    it('應該從 localStorage 加載配置', () => {
      const savedConfig: AppConfig = {
        ...appDefaultConfig,
        language: 'en',
        darkModeFlag: true,
        primaryColor: '#ff0000',
      };
      global.localStorage.setItem(STORAGE_KEYS.APP_CONFIG, JSON.stringify(savedConfig));

      // 重新導入以觸發初始化
      vi.resetModules();
    });
  });

  describe('reset action', () => {
    it('應該重置為默認配置', () => {
      const customState: AppConfig = {
        ...appDefaultConfig,
        language: 'en',
        darkModeFlag: true,
      };
      const state = appConfigReducer(customState, reset());
      expect(state).toEqual(appDefaultConfig);
    });
  });

  describe('語言設置', () => {
    it('應該設置語言', () => {
      const state = appConfigReducer(appDefaultConfig, setLanguage('en'));
      expect(state.language).toBe('en');
    });

    it('應該支持多種語言', () => {
      const languages = ['zh_CN', 'en', 'ru', 'ja', 'ko'] as const;
      languages.forEach(lang => {
        const state = appConfigReducer(appDefaultConfig, setLanguage(lang));
        expect(state.language).toBe(lang);
      });
    });
  });

  describe('布局設置', () => {
    it('應該設置布局', () => {
      const state = appConfigReducer(appDefaultConfig, setLayout('side-expand'));
      expect(state.layout).toBe('side-expand');
    });

    it('應該設置側邊菜單主題', () => {
      const state = appConfigReducer(appDefaultConfig, setSideMenuTheme('light'));
      expect(state.sideMenuTheme).toBe('light');
    });

    it('應該設置側邊菜單寬度', () => {
      const state = appConfigReducer(appDefaultConfig, setSideMenuWidth(250));
      expect(state.sideMenuWidth).toBe(250);
    });
  });

  describe('標籤頁設置', () => {
    it('應該設置標籤頁位置', () => {
      const state = appConfigReducer(appDefaultConfig, setPageTagLocation('left'));
      expect(state.pageTagLocation).toBe('left');
    });

    it('應該切換標籤頁顯示', () => {
      const initialState = { ...appDefaultConfig, pageTagFlag: true };
      const state = appConfigReducer(initialState, togglePageTag());
      expect(state.pageTagFlag).toBe(false);
    });

    it('應該設置標籤頁樣式', () => {
      const state = appConfigReducer(appDefaultConfig, setPageTagStyle('antd'));
      expect(state.pageTagStyle).toBe('antd');
    });
  });

  describe('主題設置', () => {
    it('應該切換夜間模式', () => {
      const initialState = { ...appDefaultConfig, darkModeFlag: false };
      const state = appConfigReducer(initialState, toggleDarkMode());
      expect(state.darkModeFlag).toBe(true);
    });

    it('應該設置主題顏色索引', () => {
      const state = appConfigReducer(appDefaultConfig, setColorIndex(5));
      expect(state.colorIndex).toBe(5);
    });

    it('應該設置主題顏色', () => {
      const state = appConfigReducer(appDefaultConfig, setPrimaryColor('#ff5722'));
      expect(state.primaryColor).toBe('#ff5722');
    });
  });

  describe('頁面設置', () => {
    it('應該設置頁面寬度', () => {
      const state = appConfigReducer(appDefaultConfig, setPageWidth('1200px'));
      expect(state.pageWidth).toBe('1200px');
    });

    it('應該設置圓角', () => {
      const state = appConfigReducer(appDefaultConfig, setBorderRadius(10));
      expect(state.borderRadius).toBe(10);
    });
  });

  describe('菜單設置', () => {
    it('應該切換菜單單一展開模式', () => {
      const initialState = { ...appDefaultConfig, menuSingleExpandFlag: true };
      const state = appConfigReducer(initialState, toggleMenuSingleExpand());
      expect(state.menuSingleExpandFlag).toBe(false);
    });
  });

  describe('UI 元素切換', () => {
    it('應該切換面包屑顯示', () => {
      const initialState = { ...appDefaultConfig, breadCrumbFlag: true };
      const state = appConfigReducer(initialState, toggleBreadCrumb());
      expect(state.breadCrumbFlag).toBe(false);
    });

    it('應該切換頁腳顯示', () => {
      const initialState = { ...appDefaultConfig, footerFlag: true };
      const state = appConfigReducer(initialState, toggleFooter());
      expect(state.footerFlag).toBe(false);
    });

    it('應該切換幫助文檔顯示', () => {
      const initialState = { ...appDefaultConfig, helpDocFlag: true };
      const state = appConfigReducer(initialState, toggleHelpDoc());
      expect(state.helpDocFlag).toBe(false);
    });

    it('應該切換水印顯示', () => {
      const initialState = { ...appDefaultConfig, watermarkFlag: true };
      const state = appConfigReducer(initialState, toggleWatermark());
      expect(state.watermarkFlag).toBe(false);
    });

    it('應該切換緊湊模式', () => {
      const initialState = { ...appDefaultConfig, compactFlag: false };
      const state = appConfigReducer(initialState, toggleCompact());
      expect(state.compactFlag).toBe(true);
    });
  });

  describe('幫助文檔操作', () => {
    it('應該顯示幫助文檔', () => {
      const initialState = { ...appDefaultConfig, helpDocExpandFlag: false };
      const state = appConfigReducer(initialState, showHelpDoc());
      expect(state.helpDocExpandFlag).toBe(true);
    });

    it('應該隱藏幫助文檔', () => {
      const initialState = { ...appDefaultConfig, helpDocExpandFlag: true };
      const state = appConfigReducer(initialState, hideHelpDoc());
      expect(state.helpDocExpandFlag).toBe(false);
    });
  });

  describe('全屏操作', () => {
    it('應該開始全屏', () => {
      const state = appConfigReducer(appDefaultConfig, startFullScreen());
      expect(state.fullScreenFlag).toBe(true);
    });

    it('應該退出全屏', () => {
      const initialState = { ...appDefaultConfig, fullScreenFlag: true };
      const state = appConfigReducer(initialState, exitFullScreen());
      expect(state.fullScreenFlag).toBe(false);
    });
  });

  describe('網站設置', () => {
    it('應該設置網站名稱', () => {
      const state = appConfigReducer(appDefaultConfig, setWebsiteName('My Admin'));
      expect(state.websiteName).toBe('My Admin');
    });
  });

  describe('批量更新', () => {
    it('應該批量更新配置', () => {
      const updates: Partial<AppConfig> = {
        language: 'en',
        darkModeFlag: true,
        primaryColor: '#ff0000',
        pageTagFlag: false,
      };
      const state = appConfigReducer(appDefaultConfig, updateConfig(updates));
      expect(state.language).toBe('en');
      expect(state.darkModeFlag).toBe(true);
      expect(state.primaryColor).toBe('#ff0000');
      expect(state.pageTagFlag).toBe(false);
      // 其他字段保持不變
      expect(state.layout).toBe(appDefaultConfig.layout);
    });
  });

  describe('getInitializedLanguage 函數', () => {
    it('應該返回默認語言（無保存配置）', () => {
      const language = getInitializedLanguage();
      expect(language).toBe(appDefaultConfig.language);
    });

    it('應該返回保存的語言', () => {
      const savedConfig: AppConfig = {
        ...appDefaultConfig,
        language: 'ja',
      };
      global.localStorage.setItem(STORAGE_KEYS.APP_CONFIG, JSON.stringify(savedConfig));
      const language = getInitializedLanguage();
      expect(language).toBe('ja');
    });

    it('應該處理無效的 JSON', () => {
      global.localStorage.setItem(STORAGE_KEYS.APP_CONFIG, 'invalid json');
      const language = getInitializedLanguage();
      expect(language).toBe(appDefaultConfig.language);
    });
  });

  describe('狀態不可變性', () => {
    it('應該不修改原始狀態', () => {
      const originalState = { ...appDefaultConfig };
      const state = appConfigReducer(originalState, setLanguage('en'));
      expect(originalState.language).toBe(appDefaultConfig.language);
      expect(state.language).toBe('en');
    });
  });

  describe('邊界情況', () => {
    it('應該處理連續的切換操作', () => {
      let state = appDefaultConfig;
      state = appConfigReducer(state, toggleDarkMode());
      expect(state.darkModeFlag).toBe(true);
      state = appConfigReducer(state, toggleDarkMode());
      expect(state.darkModeFlag).toBe(false);
      state = appConfigReducer(state, toggleDarkMode());
      expect(state.darkModeFlag).toBe(true);
    });

    it('應該處理空的批量更新', () => {
      const state = appConfigReducer(appDefaultConfig, updateConfig({}));
      expect(state).toEqual(appDefaultConfig);
    });
  });
});
