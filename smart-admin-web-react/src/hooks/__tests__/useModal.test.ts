import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useModal } from '../useModal';

interface MockData {
  id: number;
  name: string;
}

describe('useModal', () => {
  it('should have correct initial state', () => {
    const { result } = renderHook(() => useModal<MockData>());

    expect(result.current.visible).toBe(false);
    expect(result.current.formData).toEqual({});
    expect(result.current.isEdit).toBe(false);
  });

  it('should open modal without data (add mode)', () => {
    const { result } = renderHook(() => useModal<MockData>());

    act(() => {
      result.current.open();
    });

    expect(result.current.visible).toBe(true);
    expect(result.current.isEdit).toBe(false);
  });

  it('should open modal with data (edit mode)', () => {
    const { result } = renderHook(() => useModal<MockData>());
    const data = { id: 1, name: 'Alice' };

    act(() => {
      result.current.open(data);
    });

    expect(result.current.visible).toBe(true);
    expect(result.current.formData).toEqual(expect.objectContaining(data));
    expect(result.current.isEdit).toBe(true);
  });

  it('should close modal and reset formData', () => {
    const { result } = renderHook(() => useModal<MockData>());

    act(() => {
      result.current.open({ id: 1, name: 'Alice' });
    });

    expect(result.current.visible).toBe(true);

    act(() => {
      result.current.close();
    });

    expect(result.current.visible).toBe(false);
    // formData is reset to defaultFormData (empty object)
    expect(result.current.formData).toEqual({});
    expect(result.current.isEdit).toBe(false);
  });

  it('should switch from edit to add mode', () => {
    const { result } = renderHook(() => useModal<MockData>());

    // Open in edit mode
    act(() => {
      result.current.open({ id: 1, name: 'Alice' });
    });
    expect(result.current.isEdit).toBe(true);

    // Close
    act(() => {
      result.current.close();
    });

    // Open in add mode
    act(() => {
      result.current.open();
    });
    expect(result.current.isEdit).toBe(false);
    expect(result.current.formData).toEqual({});
  });
});
