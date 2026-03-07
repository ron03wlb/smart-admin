import { describe, it, expect } from 'vitest';
import dictReducer, {
  initDictData,
  clearDictData,
  getDictLabel,
} from '@/store/slices/dictSlice';
import type { DictDataItem, DictState } from '@/store/slices/dictSlice';

const SAMPLE_DICT_DATA: DictDataItem[] = [
  { dictCode: 'GENDER', dictName: '性別', dictDisabledFlag: false, dataValue: '1', dataLabel: '男', disabledFlag: false },
  { dictCode: 'GENDER', dictName: '性別', dictDisabledFlag: false, dataValue: '2', dataLabel: '女', disabledFlag: false },
  { dictCode: 'STATUS', dictName: '狀態', dictDisabledFlag: false, dataValue: '1', dataLabel: '啟用', disabledFlag: false },
  { dictCode: 'STATUS', dictName: '狀態', dictDisabledFlag: false, dataValue: '0', dataLabel: '停用', disabledFlag: false },
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
      { dictCode: 'COLOR', dictName: '顏色', dictDisabledFlag: false, dataValue: 'R', dataLabel: '紅', disabledFlag: false },
    ];
    const state2 = dictReducer(state1, initDictData(newData));

    expect(state2.dictList).toHaveLength(1);
    expect(state2.dictList[0].dictCode).toBe('COLOR');
    expect(state2.dictMap['GENDER']).toBeUndefined();
  });
});

describe('getDictLabel', () => {
  const dictMap: DictState['dictMap'] = {};

  beforeAll(() => {
    const state = dictReducer(undefined, initDictData(SAMPLE_DICT_DATA));
    Object.assign(dictMap, state.dictMap);
  });

  it('should get label for string value', () => {
    expect(getDictLabel(dictMap, 'GENDER', '1')).toBe('男');
    expect(getDictLabel(dictMap, 'GENDER', '2')).toBe('女');
  });

  it('should get label for number value', () => {
    expect(getDictLabel(dictMap, 'STATUS', 1)).toBe('啟用');
    expect(getDictLabel(dictMap, 'STATUS', 0)).toBe('停用');
  });

  it('should return empty for null/undefined', () => {
    expect(getDictLabel(dictMap, 'GENDER', null)).toBe('');
    expect(getDictLabel(dictMap, 'GENDER', undefined)).toBe('');
  });

  it('should return empty for unknown code', () => {
    expect(getDictLabel(dictMap, 'UNKNOWN', '1')).toBe('');
  });

  it('should handle comma-separated multi-values', () => {
    expect(getDictLabel(dictMap, 'GENDER', '1,2')).toBe('男,女');
  });
});
