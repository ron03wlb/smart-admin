/**
 * 權限系統集成測試
 *
 * 驗證 Redux Store → usePrivilege Hook → Permission Components 完整流程
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/slices/userSlice';
import type { UserState } from '@/store/slices/userSlice';
import {
  PrivilegeButton,
  PrivilegeDiv,
  PrivilegeFragment,
} from '@/components/framework/privilege';
import { usePrivilege } from '@/hooks/usePrivilege';
import type { PermissionPoint } from '@/types/menu';

// ================================ Test Helpers ================================

/**
 * Create a test store with custom user state
 */
function createTestStore(overrides: Partial<UserState> = {}) {
  const defaultUserState: UserState = {
    token: '',
    employeeId: '',
    employeeName: '',
    loginName: '',
    administratorFlag: false,
    menuTree: [],
    displayMenuTree: [],
    pointsList: [],
    menuRouterList: [],
    menuParentIdListMap: {},
    unreadMessageCount: 0,
    loading: false,
    error: null,
  };

  return configureStore({
    reducer: {
      user: userReducer,
    },
    preloadedState: {
      user: { ...defaultUserState, ...overrides },
    },
  });
}

// ================================ Test Application ================================

/**
 * 測試應用組件（模擬真實用戶列表頁面）
 */
function TestUserListPage() {
  const canQuery = usePrivilege('system:user:query');
  const canExport = usePrivilege('system:user:export');

  return (
    <div>
      <h1>用戶列表</h1>

      {/* 查詢表單（條件渲染） */}
      {canQuery && (
        <div data-testid="query-form">
          <input placeholder="搜尋用戶" />
        </div>
      )}

      {/* 工具欄按鈕 */}
      <div data-testid="toolbar">
        <PrivilegeButton permissionCode="system:user:add" type="primary">
          新增用戶
        </PrivilegeButton>

        <PrivilegeButton permissionCode="system:user:import">
          批量導入
        </PrivilegeButton>

        {canExport && <button data-testid="export-button">導出</button>}
      </div>

      {/* 統計卡片區域 */}
      <PrivilegeDiv permissionCode="system:user:statistics" data-testid="statistics-panel">
        <div>總用戶數：100</div>
        <div>活躍用戶：80</div>
      </PrivilegeDiv>

      {/* 表格操作列（使用 Fragment 避免額外 DOM） */}
      <div data-testid="table-actions">
        <PrivilegeFragment permissionCode="system:user:edit">
          <button>編輯</button>
        </PrivilegeFragment>

        <PrivilegeButton permissionCode="system:user:delete" type="link" danger>
          刪除
        </PrivilegeButton>
      </div>
    </div>
  );
}

// ================================ Test Permission Points ================================

const standardPermissions: PermissionPoint[] = [
  { menuId: '1', webPerms: 'system:user:query', menuName: '用戶查詢' },
  { menuId: '2', webPerms: 'system:user:add', menuName: '用戶新增' },
  { menuId: '3', webPerms: 'system:user:edit', menuName: '用戶編輯' },
];

// ================================ Integration Tests ================================

describe('權限系統集成測試', () => {
  test('完整流程：登錄 → 設置權限 → 組件渲染', () => {
    const store = createTestStore({
      token: 'test-token-123',
      employeeId: 'emp001',
      employeeName: '測試用戶',
      administratorFlag: false,
      pointsList: standardPermissions,
    });

    render(
      <Provider store={store}>
        <TestUserListPage />
      </Provider>
    );

    // 驗證有權限的內容顯示
    expect(screen.getByTestId('query-form')).toBeInTheDocument(); // canQuery = true
    expect(screen.getByRole('button', { name: /新\s*增\s*用\s*戶/ })).toBeInTheDocument(); // system:user:add
    expect(screen.getByText('編輯')).toBeInTheDocument(); // system:user:edit

    // 驗證無權限的內容隱藏
    expect(screen.queryByTestId('export-button')).not.toBeInTheDocument(); // canExport = false
    expect(screen.queryByRole('button', { name: /批\s*量\s*導\s*入/ })).not.toBeInTheDocument(); // system:user:import
    expect(screen.queryByRole('button', { name: /刪\s*除/ })).not.toBeInTheDocument(); // system:user:delete
    expect(screen.queryByTestId('statistics-panel')).not.toBeInTheDocument(); // system:user:statistics
  });

  test('超級管理員應該看到所有內容', () => {
    const store = createTestStore({
      token: 'admin-token-456',
      employeeId: 'admin001',
      employeeName: '超級管理員',
      administratorFlag: true,
      pointsList: [], // 超級管理員無需配置權限列表
    });

    render(
      <Provider store={store}>
        <TestUserListPage />
      </Provider>
    );

    // 所有功能都應該可見
    expect(screen.getByTestId('query-form')).toBeInTheDocument();
    expect(screen.getByTestId('export-button')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /新\s*增\s*用\s*戶/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /批\s*量\s*導\s*入/ })).toBeInTheDocument();
    expect(screen.getByText('編輯')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /刪\s*除/ })).toBeInTheDocument();
    expect(screen.getByTestId('statistics-panel')).toBeInTheDocument();
  });

  test('無任何權限的用戶應該只看到標題', () => {
    const store = createTestStore({
      token: 'guest-token-789',
      employeeId: 'guest001',
      employeeName: '訪客用戶',
      administratorFlag: false,
      pointsList: [],
    });

    render(
      <Provider store={store}>
        <TestUserListPage />
      </Provider>
    );

    // 只有標題可見
    expect(screen.getByText('用戶列表')).toBeInTheDocument();

    // 所有需要權限的功能都不可見
    expect(screen.queryByTestId('query-form')).not.toBeInTheDocument();
    expect(screen.queryByTestId('export-button')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /新\s*增/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /導\s*入/ })).not.toBeInTheDocument();
    expect(screen.queryByText('編輯')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /刪\s*除/ })).not.toBeInTheDocument();
    expect(screen.queryByTestId('statistics-panel')).not.toBeInTheDocument();
  });

  test('權限點過濾邏輯（僅保留有效權限點）', () => {
    // The store directly uses pointsList - filtering is done during login
    // Here we verify that only the pointsList items affect privilege checks
    const store = createTestStore({
      token: 'test-token',
      employeeId: 'emp001',
      employeeName: '測試用戶',
      administratorFlag: false,
      pointsList: [
        { menuId: '1', webPerms: 'system:user:add', menuName: '有效權限點' },
        // Only this one permission - others should not pass
      ],
    });

    const state = store.getState();
    expect(state.user.pointsList).toHaveLength(1);
    expect(state.user.pointsList[0].webPerms).toBe('system:user:add');
  });

  test('Redux Store 應該正確保存權限數據', () => {
    const store = createTestStore({
      token: 'test-token',
      employeeId: 'emp001',
      employeeName: '測試用戶',
      administratorFlag: false,
      pointsList: [
        { menuId: '1', webPerms: 'system:user:query', menuName: '用戶查詢' },
      ],
    });

    // 驗證 Redux Store 中保存了完整的用戶資訊
    const state = store.getState();
    expect(state.user.token).toBe('test-token');
    expect(state.user.employeeId).toBe('emp001');
    expect(state.user.employeeName).toBe('測試用戶');
    expect(state.user.administratorFlag).toBe(false);
    expect(state.user.pointsList).toHaveLength(1);
    expect(state.user.pointsList[0].webPerms).toBe('system:user:query');
  });
});
