import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import RoleIndex from '../index';
import { roleApi } from '@/api/system/role-api';
import { roleMenuApi } from '@/api/system/role-menu-api';

vi.mock('@/api/system/role-api');
vi.mock('@/api/system/role-menu-api');
vi.mock('@/api/system/employee-api', () => ({
  employeeApi: { query: vi.fn().mockResolvedValue({ code: 1, data: { list: [], total: 0 } }), queryAll: vi.fn().mockResolvedValue({ code: 1, data: [] }) },
}));

const mockRoles = [
  { roleId: 1, roleName: '管理员', roleCode: 'admin', remark: '' },
  { roleId: 2, roleName: '普通用户', roleCode: 'user', remark: '' },
];

const mockMenuTree = {
  menuTreeList: [
    { menuId: '100', menuName: '系统管理', menuType: 1, children: [] },
  ],
  selectedMenuId: ['100'],
};

describe('RoleIndex', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(roleApi.getAll).mockResolvedValue({
      code: 1,
      data: mockRoles,
      success: true,
    });
    vi.mocked(roleMenuApi.getRoleSelectedMenu).mockResolvedValue({
      code: 1,
      data: mockMenuTree,
      success: true,
    });
    vi.mocked(roleApi.getDataScopeList).mockResolvedValue({
      code: 1,
      data: [],
      success: true,
    });
    vi.mocked(roleApi.queryEmployee).mockResolvedValue({
      code: 1,
      data: { list: [], total: 0 },
      success: true,
    });
  });

  it('should display role list on mount', async () => {
    renderWithProviders(<RoleIndex />);

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument();
      expect(screen.getByText('普通用户')).toBeInTheDocument();
    });

    expect(roleApi.getAll).toHaveBeenCalled();
  });

  it('should display role list card title', async () => {
    renderWithProviders(<RoleIndex />);

    await waitFor(() => {
      expect(screen.getByText('角色列表')).toBeInTheDocument();
    });

    // Ant Design button renders CJK with spaces, use role query
    const addButton = screen.getByRole('button', { name: /添\s*加/ });
    expect(addButton).toBeInTheDocument();
  });

  it('should open form modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RoleIndex />);

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /添\s*加/ });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('添加角色')).toBeInTheDocument();
    });
  });

  it('should show setting tabs', async () => {
    renderWithProviders(<RoleIndex />);

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument();
    });

    expect(screen.getByText('角色-功能权限')).toBeInTheDocument();
    expect(screen.getByText('角色-数据范围')).toBeInTheDocument();
    expect(screen.getByText('角色-员工列表')).toBeInTheDocument();
  });
});
