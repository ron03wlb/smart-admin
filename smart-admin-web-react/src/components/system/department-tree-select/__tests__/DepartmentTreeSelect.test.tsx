import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import DepartmentTreeSelect from '../DepartmentTreeSelect';

// Mock the department API
vi.mock('@/api/system/department-api', () => ({
  departmentApi: {
    treeList: vi.fn().mockResolvedValue({
      code: 1,
      success: true,
      data: [
        {
          departmentId: 1,
          departmentName: '技术部',
          children: [
            { departmentId: 2, departmentName: '前端组', children: [] },
            { departmentId: 3, departmentName: '后端组', children: [] },
          ],
        },
      ],
    }),
  },
}));

describe('DepartmentTreeSelect', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render tree select component', async () => {
    const { container } = render(<DepartmentTreeSelect />);
    await waitFor(() => {
      expect(container.querySelector('.ant-select')).toBeTruthy();
    });
  });

  it('should load department tree data on mount', async () => {
    const { departmentApi } = await import('@/api/system/department-api');
    render(<DepartmentTreeSelect />);
    await waitFor(() => {
      expect(departmentApi.treeList).toHaveBeenCalledTimes(1);
    });
  });

  it('should display placeholder', () => {
    const { container } = render(<DepartmentTreeSelect placeholder="选择部门" />);
    expect(container.querySelector('.ant-select-selection-placeholder')?.textContent).toBe('选择部门');
  });
});
