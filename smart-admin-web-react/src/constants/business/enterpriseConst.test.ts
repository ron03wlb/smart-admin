/**
 * Enterprise Constants Unit Tests
 * 企業管理常量單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect } from 'vitest';
import {
  ENTERPRISE_PERMISSION,
  ENTERPRISE_VALIDATION,
  ENTERPRISE_TYPE_LABELS,
  ENTERPRISE_TYPE_COLORS,
  ENTERPRISE_TABLE_COLUMNS_WIDTH,
  DISABLED_FLAG_LABELS,
  DISABLED_FLAG_COLORS,
} from './enterpriseConst';
import { EnterpriseTypeEnum } from '@/views/business/enterprise/types';

describe('enterpriseConst', () => {
  // ==================== ENTERPRISE_PERMISSION Tests ====================

  describe('ENTERPRISE_PERMISSION', () => {
    it('should have correct permission strings', () => {
      expect(ENTERPRISE_PERMISSION.QUERY).toBe('oa:enterprise:query');
      expect(ENTERPRISE_PERMISSION.ADD).toBe('oa:enterprise:add');
      expect(ENTERPRISE_PERMISSION.UPDATE).toBe('oa:enterprise:update');
      expect(ENTERPRISE_PERMISSION.DELETE).toBe('oa:enterprise:delete');
      expect(ENTERPRISE_PERMISSION.DETAIL).toBe('oa:enterprise:detail');
      expect(ENTERPRISE_PERMISSION.EXPORT_EXCEL).toBe('oa:enterprise:exportExcel');
    });
  });

  // ==================== ENTERPRISE_VALIDATION Tests ====================

  describe('ENTERPRISE_VALIDATION', () => {
    it('should have correct validation limits', () => {
      expect(ENTERPRISE_VALIDATION.NAME_MAX_LENGTH).toBe(100);
      expect(ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH).toBe(18);
      expect(ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH).toBe(50);
      expect(ENTERPRISE_VALIDATION.PHONE_MAX_LENGTH).toBe(20);
      expect(ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH).toBe(100);
      expect(ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH).toBe(200);
    });

    it('should have positive numbers for all limits', () => {
      expect(ENTERPRISE_VALIDATION.NAME_MAX_LENGTH).toBeGreaterThan(0);
      expect(ENTERPRISE_VALIDATION.CREDIT_CODE_LENGTH).toBeGreaterThan(0);
      expect(ENTERPRISE_VALIDATION.CONTACT_MAX_LENGTH).toBeGreaterThan(0);
      expect(ENTERPRISE_VALIDATION.PHONE_MAX_LENGTH).toBeGreaterThan(0);
      expect(ENTERPRISE_VALIDATION.EMAIL_MAX_LENGTH).toBeGreaterThan(0);
      expect(ENTERPRISE_VALIDATION.ADDRESS_MAX_LENGTH).toBeGreaterThan(0);
    });
  });

  // ==================== ENTERPRISE_TYPE_LABELS Tests ====================

  describe('ENTERPRISE_TYPE_LABELS', () => {
    it('should have correct labels for enterprise types', () => {
      expect(ENTERPRISE_TYPE_LABELS[EnterpriseTypeEnum.LIMITED_LIABILITY]).toBe('有限責任公司');
      expect(ENTERPRISE_TYPE_LABELS[EnterpriseTypeEnum.JOINT_STOCK]).toBe('股份有限公司');
      expect(ENTERPRISE_TYPE_LABELS[EnterpriseTypeEnum.SOLE_PROPRIETORSHIP]).toBe('個人獨資企業');
      expect(ENTERPRISE_TYPE_LABELS[EnterpriseTypeEnum.PARTNERSHIP]).toBe('合夥企業');
      expect(ENTERPRISE_TYPE_LABELS[EnterpriseTypeEnum.OTHER]).toBe('其他');
    });

    it('should have labels for all enterprise types', () => {
      expect(Object.keys(ENTERPRISE_TYPE_LABELS)).toHaveLength(5);
    });
  });

  // ==================== ENTERPRISE_TYPE_COLORS Tests ====================

  describe('ENTERPRISE_TYPE_COLORS', () => {
    it('should have correct colors for enterprise types', () => {
      expect(ENTERPRISE_TYPE_COLORS[EnterpriseTypeEnum.LIMITED_LIABILITY]).toBe('blue');
      expect(ENTERPRISE_TYPE_COLORS[EnterpriseTypeEnum.JOINT_STOCK]).toBe('green');
      expect(ENTERPRISE_TYPE_COLORS[EnterpriseTypeEnum.SOLE_PROPRIETORSHIP]).toBe('orange');
      expect(ENTERPRISE_TYPE_COLORS[EnterpriseTypeEnum.PARTNERSHIP]).toBe('purple');
      expect(ENTERPRISE_TYPE_COLORS[EnterpriseTypeEnum.OTHER]).toBe('default');
    });

    it('should have colors for all enterprise types', () => {
      expect(Object.keys(ENTERPRISE_TYPE_COLORS)).toHaveLength(5);
    });
  });

  // ==================== ENTERPRISE_TABLE_COLUMNS_WIDTH Tests ====================

  describe('ENTERPRISE_TABLE_COLUMNS_WIDTH', () => {
    it('should have correct column widths', () => {
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.enterpriseName).toBe(180);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.unifiedSocialCreditCode).toBe(170);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.type).toBe(120);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.contact).toBe(100);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.contactPhone).toBe(130);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.email).toBe(180);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.address).toBe(250);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.disabledFlag).toBe(80);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.createTime).toBe(180);
      expect(ENTERPRISE_TABLE_COLUMNS_WIDTH.action).toBe(150);
    });

    it('should have positive numbers for all widths', () => {
      Object.values(ENTERPRISE_TABLE_COLUMNS_WIDTH).forEach((width) => {
        expect(width).toBeGreaterThan(0);
      });
    });
  });

  // ==================== DISABLED_FLAG_LABELS Tests ====================

  describe('DISABLED_FLAG_LABELS', () => {
    it('should have correct labels for disabled flag', () => {
      expect(DISABLED_FLAG_LABELS.true).toBe('禁用');
      expect(DISABLED_FLAG_LABELS.false).toBe('啟用');
    });

    it('should have labels for both true and false', () => {
      expect(DISABLED_FLAG_LABELS).toHaveProperty('true');
      expect(DISABLED_FLAG_LABELS).toHaveProperty('false');
    });
  });

  // ==================== DISABLED_FLAG_COLORS Tests ====================

  describe('DISABLED_FLAG_COLORS', () => {
    it('should have correct colors for disabled flag', () => {
      expect(DISABLED_FLAG_COLORS.true).toBe('red');
      expect(DISABLED_FLAG_COLORS.false).toBe('green');
    });

    it('should have colors for both true and false', () => {
      expect(DISABLED_FLAG_COLORS).toHaveProperty('true');
      expect(DISABLED_FLAG_COLORS).toHaveProperty('false');
    });
  });
});
