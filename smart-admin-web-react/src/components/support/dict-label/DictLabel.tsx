/**
 * Dict Label Component
 *
 * Corresponds to Vue's components/support/dict-label/index.vue
 * Displays the label for a dictionary code + value from the Redux store.
 */
import React from 'react';
import { useAppSelector } from '@/store/hooks';
import { selectDictLabel } from '@/store/slices/dictSlice';

interface DictLabelProps {
  dictCode: string;
  dataValue: string | number | null | undefined;
}

const DictLabel: React.FC<DictLabelProps> = ({ dictCode, dataValue }) => {
  const label = useAppSelector((state) => selectDictLabel(state, dictCode, dataValue));
  return <span>{label}</span>;
};

export default DictLabel;
