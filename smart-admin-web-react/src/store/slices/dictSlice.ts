/**
 * Dictionary Data Redux Slice
 *
 * Manages system dictionary data cache.
 * Corresponds to Vue's store/modules/system/dict.ts
 *
 * Note: Vue uses Map internally; Redux requires serializable state,
 * so we use Record<string, DictDataItem[]> instead.
 */
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '@/store';

export interface DictItem {
  dictCode: string;
  dictName: string;
  disabledFlag: boolean;
}

export interface DictDataItem {
  dictCode: string;
  dictName: string;
  dictDisabledFlag: boolean;
  dataValue: string;
  dataLabel: string;
  disabledFlag: boolean;
  sortValue?: number;
}

export interface DictState {
  /** All dict code list */
  dictList: DictItem[];
  /** Dict data grouped by code */
  dictMap: Record<string, DictDataItem[]>;
}

const DICT_SPLIT = ',';

const initialState: DictState = {
  dictList: [],
  dictMap: {},
};

const dictSlice = createSlice({
  name: 'dict',
  initialState,
  reducers: {
    /** Initialize all dictionary data from API response */
    initDictData(state, action: PayloadAction<DictDataItem[]>) {
      state.dictList = [];
      state.dictMap = {};

      for (const data of action.payload) {
        // Build dictList (unique by dictCode)
        if (!state.dictList.some((d) => d.dictCode === data.dictCode)) {
          state.dictList.push({
            dictCode: data.dictCode,
            dictName: data.dictName,
            disabledFlag: data.dictDisabledFlag,
          });
        }

        // Build dictMap
        if (!state.dictMap[data.dictCode]) {
          state.dictMap[data.dictCode] = [];
        }
        state.dictMap[data.dictCode].push(data);
      }
    },
    /** Clear all dictionary data */
    clearDictData() {
      return { ...initialState };
    },
  },
});

export const { initDictData, clearDictData } = dictSlice.actions;

// Selectors
export const selectDictList = (state: RootState) => state.dict.dictList;

export const selectDictData =
  (dictCode: string) =>
  (state: RootState): DictDataItem[] =>
    state.dict.dictMap[dictCode] ?? [];

/**
 * Get display label for a dict code + value combination
 * Supports both single values and comma-separated multi-values
 */
export function getDictLabel(
  dictMap: Record<string, DictDataItem[]>,
  dictCode: string,
  dataValue: string | number | null | undefined,
): string {
  if (dataValue === null || dataValue === undefined) return '';

  const dictData = dictMap[dictCode];
  if (!dictData || dictData.length === 0) return '';

  const valueStr = String(dataValue);

  // Handle comma-separated multi-values
  if (typeof dataValue === 'string' && dataValue.includes(DICT_SPLIT)) {
    const values = valueStr.split(DICT_SPLIT);
    const labels = values
      .map((v) => dictData.find((d) => d.dataValue === v)?.dataLabel)
      .filter(Boolean);
    return labels.join(DICT_SPLIT);
  }

  // Single value lookup
  const target = dictData.find((d) => d.dataValue === valueStr);
  return target?.dataLabel ?? '';
}

export default dictSlice.reducer;
