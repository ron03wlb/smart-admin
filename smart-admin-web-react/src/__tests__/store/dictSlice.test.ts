import { describe, it, expect, beforeAll } from 'vitest';
import dictReducer, {
  initDictData,
  clearDictData,
  selectDictLabel,
} from '@/store/slices/dictSlice';
import type { DictState } from '@/store/slices/dictSlice';
import type { DictDataItem } from '@/types/dict';
import type { RootState } from '@/store';

const SAMPLE_DICT_DATA: DictDataItem[] = [
  { dictCode: 'GENDER', dictName: '性別', dictDisabledFlag: false, dataValue: '1', dataLabel: '男', dataSort: 1 },
  { dictCode: 'GENDER', dictName: '性別', dictDisabledFlag: false, dataValue: '2', dataLabel: '女', dataSort: 2 },
  { dictCode: 'STATUS', dictName: '狀態', dictDisabledFlag: false, dataValue: '1', dataLabel: '啟用', dataSort: 1 },
  { dictCode: 'STATUS', dictName: '狀態', dictDisabledFlag: false, dataValue: '0', dataLabel: '停用', dataSort: 2 },
];

describe('dictSlice', () => {
  it('should return initial state', () => {
    const state = dictReducer(undefined, { type: 'unknown' });
    expect(state.dictList).toEqual([]);
    expect(state.dictMap).toEqual({});
  });

  it('should initialize dict data', () => {
    const state = dictReducer(undefined, initDictData(SAMPLE_DICT_DATA));

    expect(state.dictList).toHaveLength(2);
    expect(state.dictList[0].dictCode).toBe('GENDER');
    expect(state.dictList[1].dictCode).toBe('STATUS');

    expect(state.dictMap['GENDER']).toHaveLength(2);
    expect(state.dictMap['STATUS']).toHaveLength(2);
  });

  it('should clear dict data', () => {
    const populated = dictReducer(undefined, initDictData(SAMPLE_DICT_DATA));
    const cleared = dictReducer(populated, clearDictData());

    expect(cleared.dictList).toEqual([]);
    expect(cleared.dictMap).toEqual({});
  });

  it('should replace dict data on re-init', () => {
    const state1 = dictReducer(undefined, initDictData(SAMPLE_DICT_DATA));
    const newData: DictDataItem[] = [
      { dictCode: 'COLOR', dictName: '顏色', dictDisabledFlag: false, dataValue: 'R', dataLabel: '紅', dataSort: 1 },
    ];
    const state2 = dictReducer(state1, initDictData(newData));

    expect(state2.dictList).toHaveLength(1);
    expect(state2.dictList[0].dictCode).toBe('COLOR');
    expect(state2.dictMap['GENDER']).toBeUndefined();
  });
});

describe('selectDictLabel', () => {
  let dictState: DictState;

  beforeAll(() => {
    dictState = dictReducer(undefined, initDictData(SAMPLE_DICT_DATA));
  });

  /**
   * Helper to create a minimal RootState with dict data for testing selectors.
   */
  function mockRootState(): RootState {
    return { dict: dictState } as unknown as RootState;
  }

  it('should get label for string value', () => {
    expect(selectDictLabel(mockRootState(), 'GENDER', '1')).toBe('男');
    expect(selectDictLabel(mockRootState(), 'GENDER', '2')).toBe('女');
  });

  it('should get label for number value', () => {
    expect(selectDictLabel(mockRootState(), 'STATUS', 1)).toBe('啟用');
    expect(selectDictLabel(mockRootState(), 'STATUS', 0)).toBe('停用');
  });

  it('should return empty for null/undefined', () => {
    expect(selectDictLabel(mockRootState(), 'GENDER', null)).toBe('');
    expect(selectDictLabel(mockRootState(), 'GENDER', undefined)).toBe('');
  });

  it('should return empty for unknown code', () => {
    expect(selectDictLabel(mockRootState(), 'UNKNOWN', '1')).toBe('');
  });

  it('should handle comma-separated multi-values', () => {
    expect(selectDictLabel(mockRootState(), 'GENDER', '1,2')).toBe('男,女');
  });
});
