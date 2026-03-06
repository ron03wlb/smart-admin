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
import { PersistGate } from 'redux-persist/integration/react';
import { store, persistor } from '@/store';
import { setUserLoginInfo } from '@/store/slices/userSlice';
import {
  PrivilegeButton,
  PrivilegeDiv,
  PrivilegeFragment,
} from '@/components/framework/privilege';
import { usePrivilege } from '@/hooks/usePrivilege';
import type { LoginResult } from '@/types/user.types';

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

// ================================ Integration Tests ================================

describe('權限系統集成測試', () => {
  beforeEach(() => {
    // 清空 Redux Store
    store.dispatch({ type: 'user/logout' });
  });

  test('完整流程：登錄 → 設置權限 → 組件渲染', () => {
    // Step 1: 模擬用戶登錄（普通用戶，有部分權限）
    const loginData: LoginResult = {
      token: 'test-token-123',
      employeeId: 'emp001',
      employeeName: '測試用戶',
      administratorFlag: false,
      menuList: [
        {
          menuId: '1',
          menuName: '用戶查詢',
          menuType: 'POINTS',
          webPerms: 'system:user:query',
          visibleFlag: true,
          disabledFlag: false,
        },
        {
          menuId: '2',
          menuName: '用戶新增',
          menuType: 'POINTS',
          webPerms: 'system:user:add',
          visibleFlag: true,
          disabledFlag: false,
        },
        {
          menuId: '3',
          menuName: '用戶編輯',
          menuType: 'POINTS',
          webPerms: 'system:user:edit',
          visibleFlag: true,
          disabledFlag: false,
        },
        // 沒有 system:user:delete、system:user:import、system:user:export、system:user:statistics
      ],
    };

    store.dispatch(setUserLoginInfo(loginData));

    // Step 2: 渲染測試應用
    render(
      <Provider store={store}>
        <PersistGate loading={null} persistor={persistor}>
          <TestUserListPage />
        </PersistGate>
      </Provider>
    );

    // Step 3: 驗證有權限的內容顯示
    expect(screen.getByTestId('query-form')).toBeInTheDocument(); // canQuery = true
    expect(screen.getByRole('button', { name: /新\s*增\s*用\s*戶/ })).toBeInTheDocument(); // system:user:add
    expect(screen.getByText('編輯')).toBeInTheDocument(); // system:user:edit

    // Step 4: 驗證無權限的內容隱藏
    expect(screen.queryByTestId('export-button')).not.toBeInTheDocument(); // canExport = false
    expect(screen.queryByRole('button', { name: /批\s*量\s*導\s*入/ })).not.toBeInTheDocument(); // system:user:import
    expect(screen.queryByRole('button', { name: /刪\s*除/ })).not.toBeInTheDocument(); // system:user:delete
    expect(screen.queryByTestId('statistics-panel')).not.toBeInTheDocument(); // system:user:statistics
  });

  test('超級管理員應該看到所有內容', () => {
    // 模擬超級管理員登錄（空權限列表，但 administratorFlag = true）
    const adminLoginData: LoginResult = {
      token: 'admin-token-456',
      employeeId: 'admin001',
      employeeName: '超級管理員',
      administratorFlag: true,
      menuList: [], // 超級管理員無需配置權限列表
    };

    store.dispatch(setUserLoginInfo(adminLoginData));

    render(
      <Provider store={store}>
        <PersistGate loading={null} persistor={persistor}>
          <TestUserListPage />
        </PersistGate>
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
    // 模擬無權限用戶登錄
    const noPermissionLoginData: LoginResult = {
      token: 'guest-token-789',
      employeeId: 'guest001',
      employeeName: '訪客用戶',
      administratorFlag: false,
      menuList: [], // 無任何功能點權限
    };

    store.dispatch(setUserLoginInfo(noPermissionLoginData));

    render(
      <Provider store={store}>
        <PersistGate loading={null} persistor={persistor}>
          <TestUserListPage />
        </PersistGate>
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

  test('權限點過濾邏輯（僅保留 menuType=POINTS 且 visibleFlag=true 且 disabledFlag=false）', () => {
    const loginData: LoginResult = {
      token: 'test-token',
      employeeId: 'emp001',
      employeeName: '測試用戶',
      administratorFlag: false,
      menuList: [
        {
          menuId: '1',
          menuName: '有效權限點',
          menuType: 'POINTS',
          webPerms: 'system:user:add',
          visibleFlag: true,
          disabledFlag: false,
        },
        {
          menuId: '2',
          menuName: '菜單（非功能點）',
          menuType: 'MENU',
          webPerms: 'system:user:menu',
          visibleFlag: true,
          disabledFlag: false,
        },
        {
          menuId: '3',
          menuName: '不可見權限點',
          menuType: 'POINTS',
          webPerms: 'system:user:hidden',
          visibleFlag: false,
          disabledFlag: false,
        },
        {
          menuId: '4',
          menuName: '禁用權限點',
          menuType: 'POINTS',
          webPerms: 'system:user:disabled',
          visibleFlag: true,
          disabledFlag: true,
        },
      ],
    };

    store.dispatch(setUserLoginInfo(loginData));

    // 驗證 Redux Store 中的 pointsList 只包含有效權限點
    const state = store.getState();
    expect(state.user.pointsList).toHaveLength(1);
    expect(state.user.pointsList[0].webPerms).toBe('system:user:add');
  });

  test('Redux Store 應該正確保存權限數據', () => {
    const loginData: LoginResult = {
      token: 'test-token',
      employeeId: 'emp001',
      employeeName: '測試用戶',
      administratorFlag: false,
      menuList: [
        {
          menuId: '1',
          menuName: '用戶查詢',
          menuType: 'POINTS',
          webPerms: 'system:user:query',
          visibleFlag: true,
          disabledFlag: false,
        },
      ],
    };

    store.dispatch(setUserLoginInfo(loginData));

    // 驗證 Redux Store 中保存了完整的用戶資訊
    const state = store.getState();
    expect(state.user.token).toBe('test-token');
    expect(state.user.employeeId).toBe('emp001');
    expect(state.user.employeeName).toBe('測試用戶');
    expect(state.user.administratorFlag).toBe(false);
    expect(state.user.pointsList).toHaveLength(1);
    expect(state.user.pointsList[0].webPerms).toBe('system:user:query');

    // 註：Redux Persist 異步持久化到 localStorage，無法在測試中立即驗證
    // 實際應用中，Redux Persist 會在後台自動將 state.user 持久化
  });
});
