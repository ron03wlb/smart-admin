/**
 * DictFormModal Tests
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import DictFormModal from '../DictFormModal';

vi.mock('@/api/support/dict-api', () => ({
  dictApi: {
    add: vi.fn().mockResolvedValue({ code: 1 }),
    update: vi.fn().mockResolvedValue({ code: 1 }),
  },
}));

import { dictApi } from '@/api/support/dict-api';

describe('DictFormModal', () => {
  const onCancel = vi.fn();
  const onSuccess = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  it('should render add mode', () => {
    render(<DictFormModal open={true} onCancel={onCancel} onSuccess={onSuccess} />);
    expect(screen.getByText(/添加字典/)).toBeDefined();
  });

  it('should render edit mode with dict data', () => {
    const dict = { dictId: 1, dictCode: 'STATUS', dictName: '状态字典', remark: '备注' };
    render(<DictFormModal open={true} dict={dict} onCancel={onCancel} onSuccess={onSuccess} />);
    expect(screen.getByDisplayValue('STATUS')).toBeDefined();
    expect(screen.getByDisplayValue('状态字典')).toBeDefined();
  });

  it('should call add API for new dict', async () => {
    render(<DictFormModal open={true} onCancel={onCancel} onSuccess={onSuccess} />);

    fireEvent.change(screen.getByLabelText('字典编码'), { target: { value: 'NEW_CODE' } });
    fireEvent.change(screen.getByLabelText('字典名称'), { target: { value: '新字典' } });

    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(dictApi.add).toHaveBeenCalledWith(
        expect.objectContaining({ dictCode: 'NEW_CODE', dictName: '新字典' }),
      );
    }, { timeout: 5000 });
  });

  it('should call update API for existing dict', async () => {
    const dict = { dictId: 1, dictCode: 'STATUS', dictName: '状态', remark: '' };
    render(<DictFormModal open={true} dict={dict} onCancel={onCancel} onSuccess={onSuccess} />);

    fireEvent.change(screen.getByDisplayValue('状态'), { target: { value: '状态更新' } });
    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(dictApi.update).toHaveBeenCalledWith(
        expect.objectContaining({ dictId: 1, dictName: '状态更新' }),
      );
    }, { timeout: 5000 });
  });
});
