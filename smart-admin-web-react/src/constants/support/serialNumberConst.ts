/**
 * Serial Number Constants
 * 單號生成器常量定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

/**
 * 單號生成器權限點
 */
export const SERIAL_NUMBER_PERMISSION = {
  GENERATE: 'support:serialNumber:generate',
  RECORD: 'support:serialNumber:record',
} as const;

/**
 * 單號生成器表格列寬度
 */
export const SERIAL_NUMBER_TABLE_COLUMNS_WIDTH = {
  serialNumberId: 80,
  businessName: 150,
  format: 200,
  ruleType: 120,
  initNumber: 100,
  stepRandomRange: 120,
  remark: 200,
  lastNumber: 200,
  lastTime: 180,
  action: 180,
} as const;

/**
 * 單號生成器記錄表格列寬度
 */
export const SERIAL_NUMBER_RECORD_TABLE_COLUMNS_WIDTH = {
  serialNumberId: 100,
  recordDate: 150,
  count: 120,
  lastNumber: 150,
  lastTime: 180,
} as const;
