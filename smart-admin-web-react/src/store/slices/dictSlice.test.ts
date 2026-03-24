/**
 * dictSlice Tests
 * 字典狀態管理測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import dictReducer, {
  fetchAllDictData,
  clearDictData,
  initDictData,
  selectDictList,
  selectDictLoading,
  selectDictError,
  selectDictLastUpdated,
  selectDictDataByCode,
  selectDictLabel,
} from './dictSlice';
import { dictApi } from '@/api/support/dictApi';
import type { RootState } from '../index';
import type { DictDataItem } from '@/types/dict';

// ==================== Mock 設置 ====================

vi.mock('@/api/support/dictApi', () => ({
  dictApi: {
    getAllDictData: vi.fn(),
  },
}));

// ==================== 測試數據 ====================

const mockDictDataList: DictDataItem[] = [
  {
    dictCode: 'GOODS_PLACE',
    dictName: '商品產地',
    dataValue: '1',
    dataLabel: '中國',
    dictDisabledFlag: false,
    dataSort: 1,
  },
  {
    dictCode: 'GOODS_PLACE',
    dictName: '商品產地',
    dataValue: '2',
    dataLabel: '美國',
    dictDisabledFlag: false,
    dataSort: 1,
  },
  {
    dictCode: 'GOODS_PLACE',
    dictName: '商品產地',
    dataValue: '3',
    dataLabel: '日本',
    dictDisabledFlag: false,
    dataSort: 1,
  },
  {
    dictCode: 'USER_STATUS',
    dictName: '用戶狀態',
    dataValue: '1',
    dataLabel: '啟用',
    dictDisabledFlag: false,
    dataSort: 1,
  },
  {
    dictCode: 'USER_STATUS',
    dictName: '用戶狀態',
    dataValue: '0',
    dataLabel: '禁用',
    dictDisabledFlag: false,
    dataSort: 1,
  },
  {
    dictCode: 'ORDER_STATUS',
    dictName: '訂單狀態',
    dataValue: '10',
    dataLabel: '待支付',
    dictDisabledFlag: false,
    dataSort: 1,
  },
  {
    dictCode: 'ORDER_STATUS',
    dictName: '訂單狀態',
    dataValue: '20',
    dataLabel: '已支付',
    dictDisabledFlag: false,
    dataSort: 1,
  },
];

describe('dictSlice', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  // ==================== 初始狀態測試 ====================

  describe('初始狀態', () => {
    it('應該返回默認狀態', () => {
      const state = dictReducer(undefined, { type: '' });
      expect(state.dictList).toEqual([]);
      expect(state.dictMap).toEqual({});
      expect(state.loading).toBe(false);
      expect(state.error).toBe(null);
      expect(state.lastUpdated).toBe(null);
    });
  });

  // ==================== Reducers 測試 ====================

  describe('Reducers', () => {
    it('clearDictData - 應該清空字典數據', () => {
      // 先初始化一些數據
      let state = dictReducer(undefined, initDictData(mockDictDataList));
      expect(state.dictList.length).toBeGreaterThan(0);

      // 清空數據
      state = dictReducer(state, clearDictData());
      expect(state.dictList).toEqual([]);
      expect(state.dictMap).toEqual({});
      expect(state.lastUpdated).toBe(null);
    });

    it('initDictData - 應該手動初始化字典並去重', () => {
      const state = dictReducer(undefined, initDictData(mockDictDataList));

      // 驗證 dictList（去重後應有 3 個字典）
      expect(state.dictList).toEqual([
        { dictCode: 'GOODS_PLACE', dictName: '商品產地', disabledFlag: false },
        { dictCode: 'USER_STATUS', dictName: '用戶狀態', disabledFlag: false },
        { dictCode: 'ORDER_STATUS', dictName: '訂單狀態', disabledFlag: false },
      ]);

      // 驗證 dictMap
      expect(state.dictMap['GOODS_PLACE']).toHaveLength(3);
      expect(state.dictMap['USER_STATUS']).toHaveLength(2);
      expect(state.dictMap['ORDER_STATUS']).toHaveLength(2);

      // 驗證 lastUpdated
      expect(state.lastUpdated).toBeDefined();
      expect(typeof state.lastUpdated).toBe('number');
    });

    it('initDictData - 應該處理重複的 dictCode（去重）', () => {
      const duplicatedData: DictDataItem[] = [
        {
          dictCode: 'TEST_CODE',
          dictName: '測試字典',
          dataValue: '1',
          dataLabel: '選項1',
          dictDisabledFlag: false,
          dataSort: 1,
        },
        {
          dictCode: 'TEST_CODE',
          dictName: '測試字典',
          dataValue: '2',
          dataLabel: '選項2',
          dictDisabledFlag: false,
          dataSort: 2,
        },
      ];

      const state = dictReducer(undefined, initDictData(duplicatedData));

      // dictList 中只應有 1 個 TEST_CODE
      expect(state.dictList).toHaveLength(1);
      expect(state.dictList[0].dictCode).toBe('TEST_CODE');

      // dictMap 中應有 2 個數據項
      expect(state.dictMap['TEST_CODE']).toHaveLength(2);
    });
  });

  // ==================== fetchAllDictData AsyncThunk 測試 ====================

  describe('fetchAllDictData AsyncThunk', () => {
    it('pending - 應該設置 loading=true, error=null', () => {
      const state = dictReducer(undefined, { type: fetchAllDictData.pending.type });

      expect(state.loading).toBe(true);
      expect(state.error).toBe(null);
    });

    it('fulfilled - 應該初始化 dictList 和 dictMap', async () => {
      (dictApi.getAllDictData as any).mockResolvedValue({
        ok: true,
        data: mockDictDataList,
      });

      const action = await fetchAllDictData()(vi.fn(), vi.fn(), {});
      const state = dictReducer(undefined, action);

      expect(state.loading).toBe(false);
      expect(state.dictList).toHaveLength(3); // 3 個不同的 dictCode
      expect(state.dictMap['GOODS_PLACE']).toHaveLength(3);
      expect(state.dictMap['USER_STATUS']).toHaveLength(2);
      expect(state.dictMap['ORDER_STATUS']).toHaveLength(2);
    });

    it('fulfilled - 應該設置 lastUpdated 時間戳', async () => {
      (dictApi.getAllDictData as any).mockResolvedValue({
        ok: true,
        data: mockDictDataList,
      });

      const beforeTime = Date.now();
      const action = await fetchAllDictData()(vi.fn(), vi.fn(), {});
      const state = dictReducer(undefined, action);
      const afterTime = Date.now();

      expect(state.lastUpdated).toBeDefined();
      expect(state.lastUpdated).toBeGreaterThanOrEqual(beforeTime);
      expect(state.lastUpdated).toBeLessThanOrEqual(afterTime);
    });

    it('rejected - 應該設置 error', async () => {
      (dictApi.getAllDictData as any).mockRejectedValue(new Error('網絡錯誤'));

      const action = await fetchAllDictData()(vi.fn(), vi.fn(), {});
      const state = dictReducer(undefined, action);

      expect(state.loading).toBe(false);
      expect(state.error).toBe('網絡錯誤');
    });

    it('API 返回 ok=false - 應該調用 rejectWithValue', async () => {
      (dictApi.getAllDictData as any).mockResolvedValue({
        ok: false,
        msg: '獲取字典數據失敗',
      });

      const action = await fetchAllDictData()(vi.fn(), vi.fn(), {});

      expect(action.type).toBe(fetchAllDictData.rejected.type);
      expect(action.payload).toBe('獲取字典數據失敗');
    });
  });

  // ==================== Selectors - 基礎 ====================

  describe('Selectors - 基礎', () => {
    const mockState: Partial<RootState> = {
      dict: {
        dictList: [{ dictCode: 'TEST_DICT', dictName: '測試字典', disabledFlag: false }],
        dictMap: {
          TEST_DICT: [
            {
              dictCode: 'TEST_DICT',
              dictName: '測試字典',
              dataValue: '1',
              dataLabel: '選項1',
              dictDisabledFlag: false,
              dataSort: 1,
            },
          ],
        },
        loading: true,
        error: '測試錯誤',
        lastUpdated: 1234567890,
      },
    };

    it('selectDictList', () => {
      expect(selectDictList(mockState as RootState)).toEqual([
        { dictCode: 'TEST_DICT', dictName: '測試字典', disabledFlag: false },
      ]);
    });

    it('selectDictLoading', () => {
      expect(selectDictLoading(mockState as RootState)).toBe(true);
    });

    it('selectDictError', () => {
      expect(selectDictError(mockState as RootState)).toBe('測試錯誤');
    });

    it('selectDictLastUpdated', () => {
      expect(selectDictLastUpdated(mockState as RootState)).toBe(1234567890);
    });
  });

  // ==================== selectDictDataByCode 測試 ====================

  describe('selectDictDataByCode', () => {
    const mockState: Partial<RootState> = {
      dict: {
        dictList: [],
        dictMap: {
          GOODS_PLACE: mockDictDataList.filter(item => item.dictCode === 'GOODS_PLACE'),
          USER_STATUS: mockDictDataList.filter(item => item.dictCode === 'USER_STATUS'),
        },
        loading: false,
        error: null,
        lastUpdated: null,
      },
    };

    it('應該根據 dictCode 返回字典數據', () => {
      const data = selectDictDataByCode(mockState as RootState, 'GOODS_PLACE');
      expect(data).toHaveLength(3);
      expect(data[0].dataLabel).toBe('中國');
      expect(data[1].dataLabel).toBe('美國');
      expect(data[2].dataLabel).toBe('日本');
    });

    it('應該處理不存在的 dictCode', () => {
      const data = selectDictDataByCode(mockState as RootState, 'NON_EXISTENT');
      expect(data).toEqual([]);
    });

    it('應該處理空 dictCode', () => {
      const data = selectDictDataByCode(mockState as RootState, '');
      expect(data).toEqual([]);
    });
  });

  // ==================== selectDictLabel - 核心邏輯 ====================

  describe('selectDictLabel - 核心邏輯', () => {
    const mockState: Partial<RootState> = {
      dict: {
        dictList: [],
        dictMap: {
          GOODS_PLACE: mockDictDataList.filter(item => item.dictCode === 'GOODS_PLACE'),
          USER_STATUS: mockDictDataList.filter(item => item.dictCode === 'USER_STATUS'),
          ORDER_STATUS: mockDictDataList.filter(item => item.dictCode === 'ORDER_STATUS'),
        },
        loading: false,
        error: null,
        lastUpdated: null,
      },
    };

    it('單值查詢 - 應該返回對應 label', () => {
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '1')).toBe('中國');
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '2')).toBe('美國');
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '3')).toBe('日本');
    });

    it('多值查詢 - 應該返回逗號分隔的 labels', () => {
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '1,2')).toBe('中國,美國');
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '1,2,3')).toBe(
        '中國,美國,日本'
      );
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '2,3')).toBe('美國,日本');
    });

    it('數字類型 - 應該自動轉換為字符串查詢', () => {
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', 1)).toBe('中國');
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', 2)).toBe('美國');
      expect(selectDictLabel(mockState as RootState, 'USER_STATUS', 1)).toBe('啟用');
      expect(selectDictLabel(mockState as RootState, 'USER_STATUS', 0)).toBe('禁用');
    });

    it('null - 應該返回空字符串', () => {
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', null)).toBe('');
    });

    it('undefined - 應該返回空字符串', () => {
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', undefined)).toBe('');
    });

    it('NaN - 應該返回空字符串', () => {
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', NaN)).toBe('');
    });

    it('不存在的 value - 應該返回空字符串', () => {
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '999')).toBe('');
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', 999)).toBe('');
    });

    it('空字典 - 應該返回空字符串', () => {
      const emptyState: Partial<RootState> = {
        dict: {
          dictList: [],
          dictMap: {},
          loading: false,
          error: null,
          lastUpdated: null,
        },
      };
      expect(selectDictLabel(emptyState as RootState, 'NON_EXISTENT', '1')).toBe('');
    });

    it('混合場景 - 部分值存在、部分不存在', () => {
      // '1' 存在 → '中國'
      // '999' 不存在 → 跳過
      // '2' 存在 → '美國'
      expect(selectDictLabel(mockState as RootState, 'GOODS_PLACE', '1,999,2')).toBe('中國,美國');
    });

    it('數字字符串混合測試', () => {
      // ORDER_STATUS 有 '10' 和 '20'
      expect(selectDictLabel(mockState as RootState, 'ORDER_STATUS', '10')).toBe('待支付');
      expect(selectDictLabel(mockState as RootState, 'ORDER_STATUS', '20')).toBe('已支付');
      expect(selectDictLabel(mockState as RootState, 'ORDER_STATUS', '10,20')).toBe(
        '待支付,已支付'
      );
    });
  });

  // ==================== 完整工作流測試 ====================

  describe('完整工作流', () => {
    it('應該支持：fetchAllDictData → selectDictLabel → clearDictData', async () => {
      // Step 1: 獲取字典數據
      (dictApi.getAllDictData as any).mockResolvedValue({
        ok: true,
        data: mockDictDataList,
      });

      const fetchAction = await fetchAllDictData()(vi.fn(), vi.fn(), {});
      let state = dictReducer(undefined, fetchAction);

      expect(state.dictList).toHaveLength(3);
      expect(state.dictMap['GOODS_PLACE']).toHaveLength(3);

      // Step 2: 查詢字典標籤
      const mockState: Partial<RootState> = { dict: state };
      const label = selectDictLabel(mockState as RootState, 'GOODS_PLACE', '1,2');
      expect(label).toBe('中國,美國');

      // Step 3: 清除字典數據
      state = dictReducer(state, clearDictData());
      expect(state.dictList).toEqual([]);
      expect(state.dictMap).toEqual({});
      expect(state.lastUpdated).toBe(null);
    });

    it('應該支持手動初始化字典（initDictData）', () => {
      const state = dictReducer(undefined, initDictData(mockDictDataList));

      expect(state.dictList).toHaveLength(3);
      expect(state.dictMap['GOODS_PLACE']).toHaveLength(3);
      expect(state.lastUpdated).toBeDefined();

      // 查詢字典標籤
      const mockState: Partial<RootState> = { dict: state };
      const label = selectDictLabel(mockState as RootState, 'USER_STATUS', '1');
      expect(label).toBe('啟用');
    });
  });

  // ==================== 邊界情況測試 ====================

  describe('邊界情況', () => {
    it('應該保持狀態不可變性', () => {
      const initialState = dictReducer(undefined, { type: '' });
      const newState = dictReducer(initialState, initDictData(mockDictDataList));

      // 原始狀態不應改變
      expect(initialState.dictList).toEqual([]);
      expect(newState.dictList.length).toBeGreaterThan(0);
      expect(initialState).not.toBe(newState);
    });

    it('應該處理超大字典數據', () => {
      // 生成 1000 個字典項
      const largeDictData: DictDataItem[] = [];
      for (let i = 1; i <= 1000; i++) {
        largeDictData.push({
          dictCode: 'LARGE_DICT',
          dictName: '超大字典',
          dataValue: String(i),
          dataLabel: `選項${i}`,
          dictDisabledFlag: false,
          dataSort: i,
        });
      }

      const state = dictReducer(undefined, initDictData(largeDictData));

      expect(state.dictList).toHaveLength(1);
      expect(state.dictMap['LARGE_DICT']).toHaveLength(1000);

      // 性能測試：查詢標籤
      const mockState: Partial<RootState> = { dict: state };
      const label = selectDictLabel(mockState as RootState, 'LARGE_DICT', '500');
      expect(label).toBe('選項500');
    });

    it('應該處理 API 網絡錯誤', async () => {
      (dictApi.getAllDictData as any).mockRejectedValue(new Error('網絡連接失敗'));

      const action = await fetchAllDictData()(vi.fn(), vi.fn(), {});

      expect(action.type).toBe(fetchAllDictData.rejected.type);
      expect(action.payload).toBe('網絡連接失敗');
    });

    it('應該處理空數據響應', async () => {
      (dictApi.getAllDictData as any).mockResolvedValue({
        ok: true,
        data: [],
      });

      const action = await fetchAllDictData()(vi.fn(), vi.fn(), {});
      const state = dictReducer(undefined, action);

      expect(state.dictList).toEqual([]);
      expect(state.dictMap).toEqual({});
      expect(state.loading).toBe(false);
    });
  });
});
