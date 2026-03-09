/**
 * Redux Store Hooks
 * 提供類型安全的 useDispatch 和 useSelector
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { useDispatch, useSelector } from 'react-redux';
import type { TypedUseSelectorHook } from 'react-redux';
import type { RootState, AppDispatch } from './index';

/**
 * 類型安全的 useDispatch Hook
 * 使用方式：const dispatch = useAppDispatch();
 */
export const useAppDispatch: () => AppDispatch = useDispatch;

/**
 * 類型安全的 useSelector Hook
 * 使用方式：const token = useAppSelector(state => state.user.token);
 */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
