/**
 * Message Constants
 * 消息管理常量
 *
 * 參考：Vue 版本 smart-admin-web/src/constants/business/message/message-const.ts
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 消息類型枚舉
 */
export const MESSAGE_TYPE_ENUM = {
  MAIL: {
    value: 1,
    label: '站內信',
  },
  ORDER: {
    value: 2,
    label: '訂單',
  },
} as const;

/**
 * 接收人類型枚舉
 */
export const MESSAGE_RECEIVE_TYPE_ENUM = {
  EMPLOYEE: {
    value: 1,
    label: '員工',
  },
} as const;

/**
 * 獲取消息類型描述
 */
export function getMessageTypeLabel(value: number): string {
  const entry = Object.values(MESSAGE_TYPE_ENUM).find(item => item.value === value);
  return entry?.label || '未知';
}

/**
 * 獲取接收人類型描述
 */
export function getMessageReceiveTypeLabel(value: number): string {
  const entry = Object.values(MESSAGE_RECEIVE_TYPE_ENUM).find(item => item.value === value);
  return entry?.label || '未知';
}

/**
 * 消息類型選項（用於 Select 組件）
 */
export const MESSAGE_TYPE_OPTIONS = Object.values(MESSAGE_TYPE_ENUM).map(item => ({
  value: item.value,
  label: item.label,
}));

/**
 * 接收人類型選項（用於 Select 組件）
 */
export const MESSAGE_RECEIVE_TYPE_OPTIONS = Object.values(MESSAGE_RECEIVE_TYPE_ENUM).map(item => ({
  value: item.value,
  label: item.label,
}));
