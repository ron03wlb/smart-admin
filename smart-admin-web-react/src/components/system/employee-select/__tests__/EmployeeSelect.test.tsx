import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EmployeeSelect from '../EmployeeSelect';

// Mock the employee API
vi.mock('@/api/system/employee-api', () => ({
  employeeApi: {
    queryAll: vi.fn().mockResolvedValue({
      code: 1,
      success: true,
      data: [
        { employeeId: 1, employeeName: 'Alice', departmentName: '技术部' },
        { employeeId: 2, employeeName: 'Bob', departmentName: '市场部' },
        { employeeId: 3, employeeName: 'Charlie', departmentName: null },
      ],
    }),
  },
}));

describe('EmployeeSelect', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render select component', async () => {
    const { container } = render(<EmployeeSelect />);
    await waitFor(() => {
      expect(container.querySelector('.ant-select')).toBeTruthy();
    });
  });

  it('should load employee data on mount', async () => {
    const { employeeApi } = await import('@/api/system/employee-api');
    render(<EmployeeSelect />);
    await waitFor(() => {
      expect(employeeApi.queryAll).toHaveBeenCalledTimes(1);
    });
  });

  it('should display employees with department name', async () => {
    render(<EmployeeSelect />);

    const select = document.querySelector('.ant-select-selector')!;
    await userEvent.click(select);

    await waitFor(() => {
      expect(screen.getByText('Alice (技术部)')).toBeTruthy();
      expect(screen.getByText('Bob (市场部)')).toBeTruthy();
      expect(screen.getByText('Charlie')).toBeTruthy();
    });
  });

  it('should call onChange when selecting an employee', async () => {
    const onChange = vi.fn();
    render(<EmployeeSelect onChange={onChange} />);

    const select = document.querySelector('.ant-select-selector')!;
    await userEvent.click(select);

    const option = await screen.findByText('Alice (技术部)');
    await userEvent.click(option);

    expect(onChange).toHaveBeenCalledWith(1, expect.anything());
  });
});
