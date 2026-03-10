/**
 * Redux Hooks
 * 類型化的 Redux hooks
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { useDispatch, useSelector } from 'react-redux';
import type { TypedUseSelectorHook } from 'react-redux';
import type { RootState, AppDispatch } from '@/store';

/**
 * 類型化的 useDispatch hook
 */
export const useAppDispatch: () => AppDispatch = useDispatch;

/**
 * 類型化的 useSelector hook
 */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
