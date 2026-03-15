/**
 * Heart Beat Constants Test
 * 心跳記錄常量定義測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect } from 'vitest';
import { HEART_BEAT_TABLE_COLUMNS_WIDTH } from './heartBeatConst';

describe('heartBeatConst', () => {
  describe('HEART_BEAT_TABLE_COLUMNS_WIDTH', () => {
    it('應該定義表格列寬度', () => {
      expect(HEART_BEAT_TABLE_COLUMNS_WIDTH.projectPath).toBe(0);
      expect(HEART_BEAT_TABLE_COLUMNS_WIDTH.serverIp).toBe(0);
      expect(HEART_BEAT_TABLE_COLUMNS_WIDTH.processNo).toBe(100);
      expect(HEART_BEAT_TABLE_COLUMNS_WIDTH.processStartTime).toBe(150);
      expect(HEART_BEAT_TABLE_COLUMNS_WIDTH.heartBeatTime).toBe(150);
    });
  });
});
