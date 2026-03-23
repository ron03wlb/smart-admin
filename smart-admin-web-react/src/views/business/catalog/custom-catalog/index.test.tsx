/**
 * CustomCatalog Component Unit Tests
 * 自定義分類組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import CustomCatalog from './index';
import userReducer from '@/store/slices/userSlice';
import dictReducer from '@/store/slices/dictSlice';

// Mock CategoryTreeTable component
vi.mock('@/views/business/category', () => ({
  CategoryTreeTable: ({ categoryType, privilegePrefix }: any) => (
    <div data-testid="category-tree-table">
      <div data-testid="category-type">{categoryType}</div>
      <div data-testid="privilege-prefix">{privilegePrefix || 'none'}</div>
    </div>
  ),
}));

// Mock categoryApi
vi.mock('@/api/business/categoryApi', () => ({
  categoryApi: {
    queryCategoryTree: vi.fn(),
    addCategory: vi.fn(),
    updateCategory: vi.fn(),
    deleteCategory: vi.fn(),
  },
}));

// Create mock store
const createMockStore = () => {
  const permissions = [
    'custom:category:add',
    'custom:category:addChild',
    'custom:category:update',
    'custom:category:delete',
  ];
  const pointsList = permissions.map((perm, index) => ({ webPerms: perm, menuId: index + 1 }));

  return configureStore({
    reducer: {
      user: userReducer,
      dict: dictReducer,
    },
    preloadedState: {
      user: {
        token: 'mock-token',
        employeeId: '1',
        employeeName: '管理員',
        loginName: 'admin',
        administratorFlag: false,
        menuTree: [],
        displayMenuTree: [],
        pointsList,
        menuRouterList: [],
        menuParentIdListMap: {},
        unreadMessageCount: 0,
        loading: false,
        error: null,
      },
      dict: {
        dictList: [],
        dictMap: {},
        loading: false,
        error: null,
        lastUpdated: null,
      },
    },
  });
};

describe('CustomCatalog', () => {
  let store: ReturnType<typeof createMockStore>;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createMockStore();
  });

  it('should render CustomCatalog component', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <CustomCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify CategoryTreeTable is rendered
    expect(screen.getByTestId('category-tree-table')).toBeInTheDocument();
  });

  it('should pass categoryType=2 (DEMO) to CategoryTreeTable', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <CustomCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify categoryType is DEMO (2)
    expect(screen.getByTestId('category-type')).toHaveTextContent('2');
  });

  it('should pass privilegePrefix="custom:" to CategoryTreeTable', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <CustomCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify privilegePrefix is "custom:"
    expect(screen.getByTestId('privilege-prefix')).toHaveTextContent('custom:');
  });

  it('should be a simple wrapper component', () => {
    const { container } = render(
      <Provider store={store}>
        <BrowserRouter>
          <CustomCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify it's just a CategoryTreeTable wrapper (no extra DOM elements)
    const categoryTreeTable = container.querySelector('[data-testid="category-tree-table"]');
    expect(categoryTreeTable).toBeTruthy();
    expect(categoryTreeTable?.parentElement?.children.length).toBe(1);
  });

  it('should use custom privilege prefix for permission control', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <CustomCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify privilegePrefix is correctly passed
    const privilegePrefix = screen.getByTestId('privilege-prefix');
    expect(privilegePrefix).toHaveTextContent('custom:');

    // This means permissions like 'custom:category:add' will be used instead of 'category:add'
  });
});
