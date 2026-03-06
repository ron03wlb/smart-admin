/**
 * 權限組件單元測試
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { PrivilegeButton, PrivilegeDiv, PrivilegeFragment } from '../index';
import userReducer from '@/store/slices/userSlice';
import type { MenuPoint } from '@/types/user.types';

// ================================ Mock Store Factory ================================

function createTestStore(overrides?: {
  administratorFlag?: boolean;
  pointsList?: MenuPoint[];
}) {
  return configureStore({
    reducer: {
      user: userReducer,
    },
    preloadedState: {
      user: {
        token: 'test-token',
        employeeId: 'test-employee-id',
        employeeName: 'Test User',
        administratorFlag: overrides?.administratorFlag ?? false,
        pointsList: overrides?.pointsList ?? [],
        menuTree: [],
      },
    },
  });
}

const mockPermissions: MenuPoint[] = [
  {
    menuId: '1',
    menuName: '用戶新增',
    webPerms: 'system:user:add',
    visibleFlag: true,
    disabledFlag: false,
  },
  {
    menuId: '2',
    menuName: '用戶編輯',
    webPerms: 'system:user:edit',
    visibleFlag: true,
    disabledFlag: false,
  },
];

// ================================ PrivilegeButton Tests ================================

describe('PrivilegeButton', () => {
  test('有權限時應該顯示按鈕', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeButton permissionCode="system:user:add" type="primary">
          新增
        </PrivilegeButton>
      </Provider>
    );

    // Ant Design adds spaces in Chinese text, use flexible matcher
    expect(screen.getByRole('button', { name: /新\s*增/ })).toBeInTheDocument();
  });

  test('無權限且 hide 模式時不應該渲染', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeButton permissionCode="system:user:delete" mode="hide">
          刪除
        </PrivilegeButton>
      </Provider>
    );

    expect(screen.queryByRole('button', { name: '刪除' })).not.toBeInTheDocument();
  });

  test('無權限且 disable 模式時應該禁用按鈕', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeButton permissionCode="system:user:delete" mode="disable">
          刪除
        </PrivilegeButton>
      </Provider>
    );

    expect(screen.getByRole('button', { name: /刪\s*除/ })).toBeDisabled();
  });

  test('超級管理員應該看到所有按鈕', () => {
    const store = createTestStore({ administratorFlag: true, pointsList: [] });

    render(
      <Provider store={store}>
        <PrivilegeButton permissionCode="system:user:delete" type="primary">
          刪除
        </PrivilegeButton>
      </Provider>
    );

    const button = screen.getByRole('button', { name: /刪\s*除/ });
    expect(button).toBeInTheDocument();
    expect(button).not.toBeDisabled();
  });

  test('有權限時應該保留原始 disabled 狀態', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeButton permissionCode="system:user:add" disabled>
          新增
        </PrivilegeButton>
      </Provider>
    );

    expect(screen.getByRole('button', { name: /新\s*增/ })).toBeDisabled();
  });

  test('應該正確傳遞 Ant Design Button props', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeButton
          permissionCode="system:user:add"
          type="primary"
          size="large"
          danger
          data-testid="privilege-button"
        >
          新增
        </PrivilegeButton>
      </Provider>
    );

    const button = screen.getByTestId('privilege-button');
    expect(button).toBeInTheDocument();
    expect(button).toHaveClass('ant-btn-primary');
    expect(button).toHaveClass('ant-btn-lg');
    expect(button).toHaveClass('ant-btn-dangerous');
  });

  test('默認 mode 應該是 hide', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: [] });

    render(
      <Provider store={store}>
        <PrivilegeButton permissionCode="system:user:delete">刪除</PrivilegeButton>
      </Provider>
    );

    expect(screen.queryByRole('button', { name: '刪除' })).not.toBeInTheDocument();
  });
});

// ================================ PrivilegeDiv Tests ================================

describe('PrivilegeDiv', () => {
  test('有權限時應該渲染 div', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeDiv permissionCode="system:user:add" data-testid="privilege-div">
          <span>內容</span>
        </PrivilegeDiv>
      </Provider>
    );

    expect(screen.getByTestId('privilege-div')).toBeInTheDocument();
    expect(screen.getByText('內容')).toBeInTheDocument();
  });

  test('無權限時不應該渲染', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeDiv permissionCode="system:user:delete" data-testid="privilege-div">
          <span>內容</span>
        </PrivilegeDiv>
      </Provider>
    );

    expect(screen.queryByTestId('privilege-div')).not.toBeInTheDocument();
    expect(screen.queryByText('內容')).not.toBeInTheDocument();
  });

  test('超級管理員應該看到所有內容', () => {
    const store = createTestStore({ administratorFlag: true, pointsList: [] });

    render(
      <Provider store={store}>
        <PrivilegeDiv permissionCode="system:user:delete" data-testid="privilege-div">
          <span>管理員內容</span>
        </PrivilegeDiv>
      </Provider>
    );

    expect(screen.getByTestId('privilege-div')).toBeInTheDocument();
    expect(screen.getByText('管理員內容')).toBeInTheDocument();
  });

  test('應該正確傳遞 div props', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeDiv
          permissionCode="system:user:add"
          className="custom-class"
          style={{ color: 'red' }}
          data-testid="privilege-div"
        >
          <span>內容</span>
        </PrivilegeDiv>
      </Provider>
    );

    const div = screen.getByTestId('privilege-div');
    expect(div).toHaveClass('custom-class');
    expect(div).toHaveStyle({ color: 'rgb(255, 0, 0)' });
  });
});

// ================================ PrivilegeFragment Tests ================================

describe('PrivilegeFragment', () => {
  test('有權限時應該渲染子元素', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeFragment permissionCode="system:user:add">
          <span>內容1</span>
          <span>內容2</span>
        </PrivilegeFragment>
      </Provider>
    );

    expect(screen.getByText('內容1')).toBeInTheDocument();
    expect(screen.getByText('內容2')).toBeInTheDocument();
  });

  test('無權限時不應該渲染', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeFragment permissionCode="system:user:delete">
          <span>內容</span>
        </PrivilegeFragment>
      </Provider>
    );

    expect(screen.queryByText('內容')).not.toBeInTheDocument();
  });

  test('超級管理員應該看到所有內容', () => {
    const store = createTestStore({ administratorFlag: true, pointsList: [] });

    render(
      <Provider store={store}>
        <PrivilegeFragment permissionCode="system:user:delete">
          <span>管理員內容</span>
        </PrivilegeFragment>
      </Provider>
    );

    expect(screen.getByText('管理員內容')).toBeInTheDocument();
  });

  test('不應該添加額外的 DOM 節點（使用 Fragment）', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    const { container } = render(
      <Provider store={store}>
        <div data-testid="parent">
          <PrivilegeFragment permissionCode="system:user:add">
            <span>內容1</span>
            <span>內容2</span>
          </PrivilegeFragment>
        </div>
      </Provider>
    );

    const parent = screen.getByTestId('parent');
    // Fragment 不應該創建額外 wrapper，直接渲染 children
    expect(parent.children.length).toBe(2);
    expect(parent.children[0].tagName).toBe('SPAN');
    expect(parent.children[1].tagName).toBe('SPAN');
  });

  test('應該支持多種類型的子元素', () => {
    const store = createTestStore({ administratorFlag: false, pointsList: mockPermissions });

    render(
      <Provider store={store}>
        <PrivilegeFragment permissionCode="system:user:add">
          <button>按鈕</button>
          <div>Div 內容</div>
          純文本
        </PrivilegeFragment>
      </Provider>
    );

    expect(screen.getByRole('button', { name: '按鈕' })).toBeInTheDocument();
    expect(screen.getByText('Div 內容')).toBeInTheDocument();
    expect(screen.getByText('純文本')).toBeInTheDocument();
  });
});
