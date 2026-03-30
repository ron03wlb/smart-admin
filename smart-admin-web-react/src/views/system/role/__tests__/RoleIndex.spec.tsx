import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import RoleIndex from '../index';
import { roleApi } from '@/api/system/roleApi';

vi.mock('@/api/system/roleApi');
vi.mock('@/api/system/role-menu-api', () => ({
  roleMenuApi: {
    getRoleSelectedMenu: vi.fn().mockResolvedValue({ ok: true, code: 200, msg: '', data: { menuTreeList: [], selectedMenuId: [] } }),
  },
}));
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    queryEmployee: vi.fn().mockResolvedValue({ ok: true, code: 200, msg: '', data: { list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0, emptyFlag: true } }),
  },
}));

const mockRoles = [
  { roleId: 1, roleName: '管理员', roleCode: 'admin', remark: '', createTime: '', updateTime: '' },
  { roleId: 2, roleName: '普通用户', roleCode: 'user', remark: '', createTime: '', updateTime: '' },
];

describe('RoleIndex', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(roleApi.queryAll).mockResolvedValue({ ok: true, code: 200, msg: '', data: mockRoles });
    vi.mocked(roleApi.getDataScopeByRoleId).mockResolvedValue({ ok: true, code: 200, msg: '', data: [] });
    vi.mocked(roleApi.getDataScopeList).mockResolvedValue({ ok: true, code: 200, msg: '', data: [] });
    vi.mocked(roleApi.queryRoleEmployee).mockResolvedValue({ ok: true, code: 200, msg: '', data: { list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0, emptyFlag: true } });
  });

  it('should display role list on mount', async () => {
    renderWithProviders(<RoleIndex />, { preloadedState: { user: { administratorFlag: true } } });

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument();
      expect(screen.getByText('普通用户')).toBeInTheDocument();
    });

    expect(roleApi.queryAll).toHaveBeenCalled();
  });

  it('should display add button', async () => {
    renderWithProviders(<RoleIndex />, { preloadedState: { user: { administratorFlag: true } } });

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /新增角色/ });
    expect(addButton).toBeInTheDocument();
  });

  it('should open form modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RoleIndex />, { preloadedState: { user: { administratorFlag: true } } });

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /新增角色/ });
    await user.click(addButton);

    await waitFor(() => {
      // Modal title appears in .ant-modal-title, button text in .ant-btn
      expect(document.querySelector('.ant-modal-title')).toBeInTheDocument();
    });
  });

  it('should display table columns', async () => {
    renderWithProviders(<RoleIndex />, { preloadedState: { user: { administratorFlag: true } } });

    await waitFor(() => {
      expect(screen.getByText('管理员')).toBeInTheDocument();
    });

    // Table may have duplicate headers (sticky + body), use getAllByText
    expect(screen.getAllByText('角色名稱').length).toBeGreaterThan(0);
    expect(screen.getAllByText('角色編碼').length).toBeGreaterThan(0);
  });
});
