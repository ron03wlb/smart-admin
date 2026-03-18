/**
 * Date Utility Functions
 * 日期工具函數
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

/**
 * 格式化日期時間
 * @param date 日期字符串或Date對象
 * @returns 格式化後的日期時間字符串
 */
export const formatDateTime = (date: string | Date | undefined | null): string => {
  if (!date) return '-';

  try {
    const d = typeof date === 'string' ? new Date(date) : date;
    if (isNaN(d.getTime())) return '-';

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hour = String(d.getHours()).padStart(2, '0');
    const minute = String(d.getMinutes()).padStart(2, '0');
    const second = String(d.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  } catch {
    return '-';
  }
};

/**
 * 格式化日期
 * @param date 日期字符串或Date對象
 * @returns 格式化後的日期字符串
 */
export const formatDate = (date: string | Date | undefined | null): string => {
  if (!date) return '-';

  try {
    const d = typeof date === 'string' ? new Date(date) : date;
    if (isNaN(d.getTime())) return '-';

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  } catch {
    return '-';
  }
};
