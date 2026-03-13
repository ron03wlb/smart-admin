/**
 * Job Constants Tests
 * 定時任務常量測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect } from 'vitest';
import {
  JOB_PERMISSION,
  JOB_VALIDATION,
  JOB_TRIGGER_TYPE_LABELS,
  JOB_TRIGGER_TYPE_COLORS,
  JOB_TABLE_COLUMNS_WIDTH,
} from './jobConst';

describe('Job Constants', () => {
  it('should define all job permissions', () => {
    expect(JOB_PERMISSION.QUERY).toBe('support:job:query');
    expect(JOB_PERMISSION.ADD).toBe('support:job:add');
    expect(JOB_PERMISSION.UPDATE).toBe('support:job:update');
    expect(JOB_PERMISSION.DELETE).toBe('support:job:delete');
    expect(JOB_PERMISSION.EXECUTE).toBe('support:job:execute');
    expect(JOB_PERMISSION.UPDATE_ENABLED).toBe('support:job:update:enabled');
    expect(JOB_PERMISSION.LOG_QUERY).toBe('support:job:log:query');
  });

  it('should define validation rules', () => {
    expect(JOB_VALIDATION.NAME_MAX_LENGTH).toBe(100);
    expect(JOB_VALIDATION.CLASS_MAX_LENGTH).toBe(200);
    expect(JOB_VALIDATION.TRIGGER_VALUE_MAX_LENGTH).toBe(200);
    expect(JOB_VALIDATION.PARAM_MAX_LENGTH).toBe(500);
    expect(JOB_VALIDATION.REMARK_MAX_LENGTH).toBe(500);
  });

  it('should define trigger type labels', () => {
    expect(JOB_TRIGGER_TYPE_LABELS.CRON).toBe('CRON 表達式');
    expect(JOB_TRIGGER_TYPE_LABELS.FIXED_DELAY).toBe('固定延遲');
    expect(JOB_TRIGGER_TYPE_LABELS.FIXED_RATE).toBe('固定頻率');
  });

  it('should define trigger type colors', () => {
    expect(JOB_TRIGGER_TYPE_COLORS.CRON).toBe('success');
    expect(JOB_TRIGGER_TYPE_COLORS.FIXED_DELAY).toBe('processing');
    expect(JOB_TRIGGER_TYPE_COLORS.FIXED_RATE).toBe('warning');
  });

  it('should define table column widths', () => {
    expect(JOB_TABLE_COLUMNS_WIDTH.jobId).toBe(80);
    expect(JOB_TABLE_COLUMNS_WIDTH.jobName).toBe(150);
    expect(JOB_TABLE_COLUMNS_WIDTH.jobClass).toBe(180);
    expect(JOB_TABLE_COLUMNS_WIDTH.triggerType).toBe(110);
    expect(JOB_TABLE_COLUMNS_WIDTH.triggerValue).toBe(150);
    expect(JOB_TABLE_COLUMNS_WIDTH.lastJob).toBe(180);
    expect(JOB_TABLE_COLUMNS_WIDTH.nextJob).toBe(150);
    expect(JOB_TABLE_COLUMNS_WIDTH.enabledFlag).toBe(100);
    expect(JOB_TABLE_COLUMNS_WIDTH.param).toBe(150);
    expect(JOB_TABLE_COLUMNS_WIDTH.remark).toBe(200);
    expect(JOB_TABLE_COLUMNS_WIDTH.sort).toBe(80);
    expect(JOB_TABLE_COLUMNS_WIDTH.updateName).toBe(100);
    expect(JOB_TABLE_COLUMNS_WIDTH.updateTime).toBe(160);
    expect(JOB_TABLE_COLUMNS_WIDTH.action).toBe(200);
  });
});
