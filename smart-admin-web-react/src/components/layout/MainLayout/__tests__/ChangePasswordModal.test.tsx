/**
 * ChangePasswordModal Tests
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import ChangePasswordModal from '../ChangePasswordModal';

vi.mock('@/api/system/employee-api', () => ({
  employeeApi: {
    update: vi.fn().mockResolvedValue({ code: 1 }),
  },
}));

vi.mock('@/components/framework/smart-loading', () => ({
  SmartLoading: { show: vi.fn(), hide: vi.fn() },
}));

import { employeeApi } from '@/api/system/employee-api';

describe('ChangePasswordModal', () => {
  const onClose = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  it('should render modal with form fields when visible', () => {
    render(<ChangePasswordModal visible={true} onClose={onClose} />);
    expect(screen.getByText('修改密码')).toBeDefined();
    expect(screen.getByLabelText('原密码')).toBeDefined();
    expect(screen.getByLabelText('新密码')).toBeDefined();
    expect(screen.getByLabelText('确认密码')).toBeDefined();
  });

  it('should not render content when not visible', () => {
    render(<ChangePasswordModal visible={false} onClose={onClose} />);
    expect(screen.queryByLabelText('原密码')).toBeNull();
  });

  it('should call update API with password data', async () => {
    render(<ChangePasswordModal visible={true} onClose={onClose} />);

    fireEvent.change(screen.getByLabelText('原密码'), { target: { value: 'OldPass1' } });
    fireEvent.change(screen.getByLabelText('新密码'), { target: { value: 'NewPass1a' } });
    fireEvent.change(screen.getByLabelText('确认密码'), { target: { value: 'NewPass1a' } });

    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(employeeApi.update).toHaveBeenCalledWith(
        expect.objectContaining({ oldPassword: 'OldPass1', newPassword: 'NewPass1a' }),
      );
    }, { timeout: 5000 });
  });

  it('should show placeholders', () => {
    render(<ChangePasswordModal visible={true} onClose={onClose} />);
    expect(screen.getByPlaceholderText('请输入原密码')).toBeDefined();
    expect(screen.getByPlaceholderText(/包含大小写/)).toBeDefined();
    expect(screen.getByPlaceholderText('请再次输入新密码')).toBeDefined();
  });
});
