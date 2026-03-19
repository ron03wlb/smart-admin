/**
 * GoodsCatalog Component Unit Tests
 * 商品分類組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-19
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import GoodsCatalog from './index';
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
const createMockStore = () =>
  configureStore({
    reducer: {
      user: userReducer,
      dict: dictReducer,
    },
    preloadedState: {
      user: {
        userInfo: {
          employeeId: 1,
          loginName: 'admin',
          actualName: '管理員',
          phone: '13800138000',
        },
        privilegeList: ['category:add', 'category:addChild', 'category:update', 'category:delete'],
        roleList: [],
        isLoggedIn: true,
        loading: false,
        error: null,
      },
      dict: {
        dictData: {},
        loading: false,
        error: null,
      },
    },
  });

describe('GoodsCatalog', () => {
  let store: ReturnType<typeof createMockStore>;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createMockStore();
  });

  it('should render GoodsCatalog component', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <GoodsCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify CategoryTreeTable is rendered
    expect(screen.getByTestId('category-tree-table')).toBeInTheDocument();
  });

  it('should pass categoryType=1 (GOODS) to CategoryTreeTable', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <GoodsCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify categoryType is GOODS (1)
    expect(screen.getByTestId('category-type')).toHaveTextContent('1');
  });

  it('should not pass privilegePrefix to CategoryTreeTable', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <GoodsCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify privilegePrefix is not passed (default empty)
    expect(screen.getByTestId('privilege-prefix')).toHaveTextContent('none');
  });

  it('should be a simple wrapper component', () => {
    const { container } = render(
      <Provider store={store}>
        <BrowserRouter>
          <GoodsCatalog />
        </BrowserRouter>
      </Provider>
    );

    // Verify it's just a CategoryTreeTable wrapper (no extra DOM elements)
    const categoryTreeTable = container.querySelector('[data-testid="category-tree-table"]');
    expect(categoryTreeTable).toBeTruthy();
    expect(categoryTreeTable?.parentElement?.children.length).toBe(1);
  });
});
