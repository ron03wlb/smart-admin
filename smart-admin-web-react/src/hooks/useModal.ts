/**
 * useModal Hook — Modal open/close with edit state
 *
 * Tracks visibility, edit data, and edit mode for form modals.
 */
import { useState, useCallback } from 'react';

export interface UseModalReturn<T = any> {
  visible: boolean;
  editData: T | null;
  isEdit: boolean;
  open: (data?: T) => void;
  close: () => void;
}

export function useModal<T = any>(): UseModalReturn<T> {
  const [visible, setVisible] = useState(false);
  const [editData, setEditData] = useState<T | null>(null);

  const open = useCallback((data?: T) => {
    setEditData(data ?? null);
    setVisible(true);
  }, []);

  const close = useCallback(() => {
    setVisible(false);
    setEditData(null);
  }, []);

  return {
    visible,
    editData,
    isEdit: editData !== null,
    open,
    close,
  };
}
