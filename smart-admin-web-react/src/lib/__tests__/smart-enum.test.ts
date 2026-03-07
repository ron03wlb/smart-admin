import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getDescByValue, getValueDescList, getValueDesc } from '../smart-enum';

describe('smart-enum', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  describe('getDescByValue', () => {
    it('should return desc for FLAG_NUMBER_ENUM', () => {
      expect(getDescByValue('FLAG_NUMBER_ENUM', 1)).toBe('是');
      expect(getDescByValue('FLAG_NUMBER_ENUM', 0)).toBe('否');
    });

    it('should handle boolean values for FLAG_NUMBER_ENUM', () => {
      expect(getDescByValue('FLAG_NUMBER_ENUM', true)).toBe('是');
      expect(getDescByValue('FLAG_NUMBER_ENUM', false)).toBe('否');
    });

    it('should return desc for GENDER_ENUM', () => {
      expect(getDescByValue('GENDER_ENUM', 1)).toBe('男');
      expect(getDescByValue('GENDER_ENUM', 2)).toBe('女');
      expect(getDescByValue('GENDER_ENUM', 0)).toBe('未知');
    });

    it('should return desc for string-valued enums', () => {
      expect(getDescByValue('LAYOUT_ENUM', 'side')).toBe('传统');
      expect(getDescByValue('LAYOUT_ENUM', 'top')).toBe('顶部');
    });

    it('should return empty string for unknown value', () => {
      expect(getDescByValue('FLAG_NUMBER_ENUM', 999)).toBe('');
    });

    it('should return empty string and log error for unknown enum name', () => {
      expect(getDescByValue('NONEXISTENT_ENUM', 1)).toBe('');
      expect(console.error).toHaveBeenCalledWith(
        expect.stringContaining('NONEXISTENT_ENUM'),
      );
    });
  });

  describe('getValueDescList', () => {
    it('should return all items for FLAG_NUMBER_ENUM', () => {
      const list = getValueDescList('FLAG_NUMBER_ENUM');
      expect(list).toHaveLength(2);
      expect(list[0]).toEqual({ value: 1, desc: '是' });
      expect(list[1]).toEqual({ value: 0, desc: '否' });
    });

    it('should return all items for MENU_TYPE_ENUM', () => {
      const list = getValueDescList('MENU_TYPE_ENUM');
      expect(list).toHaveLength(3);
      expect(list.map((i) => i.desc)).toEqual(['目录', '菜单', '功能点']);
    });

    it('should return empty array for unknown enum name', () => {
      const list = getValueDescList('NONEXISTENT_ENUM');
      expect(list).toEqual([]);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe('getValueDesc', () => {
    it('should return value-to-desc mapping', () => {
      const result = getValueDesc('FLAG_NUMBER_ENUM');
      expect(result).toEqual({ '1': '是', '0': '否' });
    });

    it('should handle string-valued enums', () => {
      const result = getValueDesc('LAYOUT_ENUM');
      expect(result).toEqual({
        side: '传统',
        'side-expand': '展开',
        top: '顶部',
        'top-expand': '分组',
      });
    });

    it('should return empty object for unknown enum name', () => {
      const result = getValueDesc('NONEXISTENT_ENUM');
      expect(result).toEqual({});
      expect(console.error).toHaveBeenCalled();
    });
  });
});
