/*
 * ISO-8601 時間處理工具
 *
 * 後端 API 回傳 ISO-8601 with offset（如 "2026-02-14T18:30:00+08:00"），
 * 已自動轉為租戶時區，前端只需解析並格式化顯示。
 *
 * @since G2.3
 */
import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import customParseFormat from 'dayjs/plugin/customParseFormat';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(customParseFormat);

const DISPLAY_DATETIME = 'YYYY-MM-DD HH:mm:ss';
const DISPLAY_DATE = 'YYYY-MM-DD';
const LEGACY_FORMAT = 'YYYY-MM-DD HH:mm:ss';

/**
 * 解析 API 回傳的時間字串（ISO-8601 或舊格式）
 */
export function parseApiDateTime(value: string | null | undefined): dayjs.Dayjs | null {
  if (!value) return null;
  const parsed = dayjs(value);
  if (parsed.isValid()) return parsed;
  const legacy = dayjs(value, LEGACY_FORMAT);
  return legacy.isValid() ? legacy : null;
}

/**
 * 格式化 API 時間為顯示字串
 *
 * 後端已將時間轉為租戶時區，前端直接 format 即可。
 */
export function formatDateTime(value: string | null | undefined, format: string = DISPLAY_DATETIME): string {
  const parsed = parseApiDateTime(value);
  return parsed ? parsed.format(format) : '';
}

/**
 * 格式化為僅日期
 */
export function formatDate(value: string | null | undefined): string {
  return formatDateTime(value, DISPLAY_DATE);
}

/**
 * 將 DatePicker 值轉為 ISO-8601 字串（提交 API 用）
 */
export function toApiDateTime(value: dayjs.Dayjs | null | undefined): string | null {
  if (!value) return null;
  return value.format();
}
