/**
 * Message Constants Tests
 * 消息管理常量測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect } from 'vitest';
import {
  MESSAGE_TYPE_ENUM,
  MESSAGE_RECEIVE_TYPE_ENUM,
  getMessageTypeLabel,
  getMessageReceiveTypeLabel,
  MESSAGE_TYPE_OPTIONS,
  MESSAGE_RECEIVE_TYPE_OPTIONS,
} from './messageConst';

describe('messageConst', () => {
  describe('MESSAGE_TYPE_ENUM', () => {
    it('應該包含正確的消息類型值', () => {
      expect(MESSAGE_TYPE_ENUM.MAIL.value).toBe(1);
      expect(MESSAGE_TYPE_ENUM.MAIL.label).toBe('站內信');
      expect(MESSAGE_TYPE_ENUM.ORDER.value).toBe(2);
      expect(MESSAGE_TYPE_ENUM.ORDER.label).toBe('訂單');
    });
  });

  describe('MESSAGE_RECEIVE_TYPE_ENUM', () => {
    it('應該包含正確的接收人類型值', () => {
      expect(MESSAGE_RECEIVE_TYPE_ENUM.EMPLOYEE.value).toBe(1);
      expect(MESSAGE_RECEIVE_TYPE_ENUM.EMPLOYEE.label).toBe('員工');
    });
  });

  describe('getMessageTypeLabel', () => {
    it('應該返回對應的消息類型描述', () => {
      expect(getMessageTypeLabel(1)).toBe('站內信');
      expect(getMessageTypeLabel(2)).toBe('訂單');
    });

    it('應該返回「未知」對於無效值', () => {
      expect(getMessageTypeLabel(999)).toBe('未知');
    });
  });

  describe('getMessageReceiveTypeLabel', () => {
    it('應該返回對應的接收人類型描述', () => {
      expect(getMessageReceiveTypeLabel(1)).toBe('員工');
    });

    it('應該返回「未知」對於無效值', () => {
      expect(getMessageReceiveTypeLabel(999)).toBe('未知');
    });
  });

  describe('MESSAGE_TYPE_OPTIONS', () => {
    it('應該生成正確的選項數組', () => {
      expect(MESSAGE_TYPE_OPTIONS).toEqual([
        { value: 1, label: '站內信' },
        { value: 2, label: '訂單' },
      ]);
    });
  });

  describe('MESSAGE_RECEIVE_TYPE_OPTIONS', () => {
    it('應該生成正確的選項數組', () => {
      expect(MESSAGE_RECEIVE_TYPE_OPTIONS).toEqual([{ value: 1, label: '員工' }]);
    });
  });
});
