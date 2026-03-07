import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import DepartmentList from '../DepartmentList';
import { departmentApi } from '@/api/system/department-api';

vi.mock('@/api/system/department-api');
vi.mock('@/api/system/employee-api', () => ({
  employeeApi: { queryAll: vi.fn().mockResolvedValue({ code: 1, data: [] }) },
}));

const mockDepartments = [
  { departmentId: 1, departmentName: '总公司', parentId: 0, sort: 1, managerName: '张三', createTime: '2026-01-01', updateTime: '2026-01-01' },
  { departmentId: 2, departmentName: '技术部', parentId: 1, sort: 1, managerName: '李四', createTime: '2026-01-02', updateTime: '2026-01-02' },
  { departmentId: 3, departmentName: '产品部', parentId: 1, sort: 2, managerName: '王五', createTime: '2026-01-03', updateTime: '2026-01-03' },
];

const adminState = { user: { administratorFlag: true } };

describe('DepartmentList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(departmentApi.listAll).mockResolvedValue({
      code: 1,
      data: mockDepartments,
      success: true,
    });
  });

  it('should fetch and display department tree on mount', async () => {
    renderWithProviders(<DepartmentList />, { preloadedState: adminState });

    await waitFor(() => {
      expect(screen.getByText('总公司')).toBeInTheDocument();
    });

    expect(departmentApi.listAll).toHaveBeenCalled();
  });

  it('should handle search input and reset', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DepartmentList />, { preloadedState: adminState });

    await waitFor(() => {
      expect(screen.getByText('总公司')).toBeInTheDocument();
    });

    const input = screen.getByPlaceholderText('请输入部门名称');
    await user.type(input, '技术');
    expect(input).toHaveValue('技术');

    // Reset should clear the input
    const resetButton = screen.getByText('重置');
    await user.click(resetButton);
    expect(input).toHaveValue('');
  });

  it('should open form modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DepartmentList />, { preloadedState: adminState });

    await waitFor(() => {
      expect(screen.getByText('总公司')).toBeInTheDocument();
    });

    const addButton = screen.getByText('新建');
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('添加部门')).toBeInTheDocument();
    });
  });

  it('should render table with correct column headers', async () => {
    renderWithProviders(<DepartmentList />, { preloadedState: adminState });

    await waitFor(() => {
      expect(screen.getByText('总公司')).toBeInTheDocument();
    });

    expect(departmentApi.listAll).toHaveBeenCalled();
    expect(screen.getByText('负责人')).toBeInTheDocument();
    expect(screen.getByText('排序')).toBeInTheDocument();
  });
});
