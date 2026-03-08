import { screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import MenuList from '../MenuList';
import { menuApi } from '@/api/system/menu-api';

vi.mock('@/api/system/menu-api');

const mockMenus = [
  { menuId: '1', menuName: '系统管理', menuType: 1, parentId: '0', path: '/system', sort: 1 },
  { menuId: '2', menuName: '员工管理', menuType: 2, parentId: '1', path: '/system/employee', component: 'system/employee/employee-list', sort: 1 },
  { menuId: '3', menuName: '查看详情', menuType: 3, parentId: '2', apiPerms: 'system:employee:detail', sort: 1 },
];

describe('MenuList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(menuApi.query).mockResolvedValue({
      code: 1,
      data: mockMenus as any,
      success: true,
    });
  });

  it('should display menu tree table on mount', async () => {
    renderWithProviders(<MenuList />);

    await waitFor(() => {
      expect(screen.getByText('系统管理')).toBeInTheDocument();
    });

    expect(menuApi.query).toHaveBeenCalled();
  });

  it('should display menu type tag for root item', async () => {
    renderWithProviders(<MenuList />);

    await waitFor(() => {
      // Root-level item (type=1 catalog) should show its tag
      expect(screen.getByText('目录')).toBeInTheDocument();
    });
  });

  it('should filter menus by keyword input', async () => {
    const user = userEvent.setup();
    renderWithProviders(<MenuList />);

    await waitFor(() => {
      expect(screen.getByText('系统管理')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('名称/路径/组件/权限');
    await user.type(searchInput, '员工');

    // After filtering, only matching items should remain
    await waitFor(() => {
      expect(screen.getByText('员工管理')).toBeInTheDocument();
    });
  });

  it('should open drawer when add button clicked', async () => {
    renderWithProviders(<MenuList />);

    await waitFor(() => {
      expect(screen.getByText('系统管理')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /新\s*建/ }));

    await waitFor(() => {
      expect(screen.getByText('添加菜单')).toBeInTheDocument();
    });
  });

  it('should reset search on reset button click', async () => {
    renderWithProviders(<MenuList />);

    await waitFor(() => {
      expect(screen.getByText('系统管理')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('名称/路径/组件/权限');
    fireEvent.change(searchInput, { target: { value: '员工' } });

    fireEvent.click(screen.getByRole('button', { name: /重\s*置/ }));

    expect(searchInput).toHaveValue('');
  });
});
