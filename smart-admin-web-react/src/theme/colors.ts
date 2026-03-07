/**
 * Theme Color Configuration
 *
 * Predefined theme colors matching Vue's header-setting.vue
 */

export const THEME_COLORS = [
  '#1677ff', // Ant Design Blue (default)
  '#1890ff', // Daybreak Blue
  '#722ed1', // Purple
  '#13c2c2', // Cyan
  '#52c41a', // Green
  '#eb2f96', // Magenta
  '#fa8c16', // Orange
  '#f5222d', // Red
];

export const AVATAR_BACKGROUND_COLORS = ['#87d068', '#00B853', '#f56a00', '#1890ff'];

/**
 * Compute a simple hash for a string (used for avatar color selection).
 */
export function stringHashCode(str: string): number {
  let hash = 1;
  if (str.length === 0) return hash;
  for (let i = 0; i < str.length; i++) {
    const chr = str.charCodeAt(i);
    hash = (hash << 5) - hash + chr;
    hash |= 0;
  }
  return Math.abs(hash);
}
