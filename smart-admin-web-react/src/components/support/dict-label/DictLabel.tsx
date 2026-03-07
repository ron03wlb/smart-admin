/**
 * Dict Label Component
 *
 * Corresponds to Vue's components/support/dict-label/index.vue
 * Displays the label for a dictionary code + value from the Redux store.
 */
import React from 'react';
import { useAppSelector } from '@/store/hooks';
import { getDictLabel } from '@/store/slices/dictSlice';

interface DictLabelProps {
  dictCode: string;
  dataValue: string | number | null | undefined;
}

const DictLabel: React.FC<DictLabelProps> = ({ dictCode, dataValue }) => {
  const dictMap = useAppSelector((state) => state.dict.dictMap);
  const label = getDictLabel(dictMap, dictCode, dataValue);
  return <span>{label}</span>;
};

export default DictLabel;
