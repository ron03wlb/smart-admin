import type { Dayjs } from 'dayjs';

/**
 * Ant Design RangePicker compatible date range type.
 * Matches the onChange callback signature: [Dayjs | null, Dayjs | null] | null
 */
export type DateRangeValue = [Dayjs | null, Dayjs | null] | null;
