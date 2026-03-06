/**
 * Redux 自定義 Hooks（類型安全）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { useDispatch, useSelector } from 'react-redux';
import type { RootState, AppDispatch } from './index';

/**
 * 類型安全的 useDispatch
 *
 * @usage const dispatch = useAppDispatch();
 */
export const useAppDispatch = useDispatch.withTypes<AppDispatch>();

/**
 * 類型安全的 useSelector
 *
 * @usage const token = useAppSelector((state) => state.user.token);
 */
export const useAppSelector = useSelector.withTypes<RootState>();
