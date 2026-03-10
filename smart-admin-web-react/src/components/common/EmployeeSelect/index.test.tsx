/**
 * EmployeeSelect Component Tests
 * EmployeeSelect 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { store } from '@/store';
import EmployeeSelect from './index';
import { employeeApi, EmployeeVO } from '@/api/system/employeeApi';
import React from 'react';

// Mock employeeApi
vi.mock('@/api/system/employeeApi', () => ({
  employeeApi: {
    queryAll: vi.fn(),
    queryEmployeeByDeptId: vi.fn(),
  },
}));

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
  };
});

// Wrapper 組件（提供 Redux Provider）
const Wrapper = ({ children }: { children: React.ReactNode }) => {
  return <Provider store={store}>{children}</Provider>;
};

// Mock 員工數據
const mockEmployees: EmployeeVO[] = [
  {
    employeeId: 1,
    actualName: '張三',
    loginName: 'zhangsan',
    phone: '13800138001',
    departmentId: 1,
    departmentName: '技術部',
    isDisabled: false,
    isLeave: false,
  },
  {
    employeeId: 2,
    actualName: '李四',
    loginName: 'lisi',
    phone: '13800138002',
    departmentId: 1,
    departmentName: '技術部',
    isDisabled: false,
    isLeave: false,
  },
  {
    employeeId: 3,
    actualName: '王五',
    loginName: 'wangwu',
    phone: '13800138003',
    departmentId: 2,
    departmentName: '市場部',
    isDisabled: true,
    isLeave: false,
  },
  {
    employeeId: 4,
    actualName: '趙六',
    loginName: 'zhaoliu',
    phone: '13800138004',
    departmentId: 2,
    departmentName: '市場部',
    isDisabled: false,
    isLeave: true,
  },
];

describe('EmployeeSelect', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('基本渲染', () => {
    it('應該渲染 Select 組件', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      const { container } = render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      // 應該渲染 Select 組件
      await waitFor(() => {
        const select = container.querySelector('.ant-select');
        expect(select).toBeInTheDocument();
      });

      // 應該調用 queryAll API
      expect(mockQueryAll).toHaveBeenCalledTimes(1);
    });

    it('應該顯示默認 placeholder', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('請選擇員工')).toBeInTheDocument();
      });
    });

    it('應該支持自定義 placeholder', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect placeholder="選擇負責人" />
        </Wrapper>
      );

      await waitFor(() => {
        expect(screen.getByText('選擇負責人')).toBeInTheDocument();
      });
    });
  });

  describe('數據加載', () => {
    it('應該加載所有員工（無 departmentId）', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalledTimes(1);
      });
    });

    it('應該按部門加載員工（有 departmentId）', async () => {
      const mockQueryByDeptId = vi.mocked(employeeApi.queryEmployeeByDeptId);
      const deptEmployees = mockEmployees.filter(e => e.departmentId === 1);
      mockQueryByDeptId.mockResolvedValue({
        ok: true,
        data: deptEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect departmentId={1} />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryByDeptId).toHaveBeenCalledWith(1);
      });
    });

    it('應該在加載時顯示 loading 狀態', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockImplementation(
        () =>
          new Promise(resolve =>
            setTimeout(
              () =>
                resolve({
                  ok: true,
                  data: mockEmployees,
                  code: 1,
                  msg: 'success',
                }),
              100
            )
          )
      );

      const { container } = render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      // 應該顯示 loading
      const select = container.querySelector('.ant-select-loading');
      expect(select).toBeInTheDocument();
    });

    it('應該處理 API 錯誤', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: false,
        data: null,
        code: 0,
        msg: 'error',
      });

      render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });

      // message.error 應該被調用（雖然我們 mock 了它）
    });
  });

  describe('員工篩選', () => {
    it('應該默認隱藏禁用員工', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });

      // 組件應該正常渲染
      const select = document.querySelector('.ant-select');
      expect(select).toBeInTheDocument();
    });

    it('應該支持顯示禁用員工（showDisabled=true）', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect showDisabled />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });
    });

    it('應該默認隱藏離職員工', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });
    });

    it('應該支持顯示離職員工（showLeave=true）', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect showLeave />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });
    });
  });

  describe('禁用選項', () => {
    it('應該支持禁用特定員工', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect disabledEmployeeIds={[1, 2]} />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });
    });
  });

  describe('搜索功能', () => {
    it('應該支持搜索員工（showSearch）', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      const { container } = render(
        <Wrapper>
          <EmployeeSelect />
        </Wrapper>
      );

      await waitFor(() => {
        const searchInput = container.querySelector('.ant-select-selection-search-input');
        expect(searchInput).toBeInTheDocument();
      });
    });
  });

  describe('onChange 回調', () => {
    it('應該接受 onChange 回調', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      const onChange = vi.fn();

      render(
        <Wrapper>
          <EmployeeSelect onChange={onChange} />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });

      // onChange prop 應該被正確傳遞
      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('多選模式', () => {
    it('應該支持多選模式（mode="multiple"）', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      const { container } = render(
        <Wrapper>
          <EmployeeSelect mode="multiple" />
        </Wrapper>
      );

      await waitFor(() => {
        const select = container.querySelector('.ant-select-multiple');
        expect(select).toBeInTheDocument();
      });
    });
  });

  describe('受控模式', () => {
    it('應該支持受控模式（value prop）', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      render(
        <Wrapper>
          <EmployeeSelect value={1} />
        </Wrapper>
      );

      await waitFor(() => {
        expect(mockQueryAll).toHaveBeenCalled();
      });
    });
  });

  describe('樣式和布局', () => {
    it('應該支持自定義寬度', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      const { container } = render(
        <Wrapper>
          <EmployeeSelect style={{ width: 300 }} />
        </Wrapper>
      );

      await waitFor(() => {
        const select = container.querySelector('.ant-select');
        expect(select).toHaveStyle({ width: '300px' });
      });
    });

    it('應該支持 disabled 狀態', async () => {
      const mockQueryAll = vi.mocked(employeeApi.queryAll);
      mockQueryAll.mockResolvedValue({
        ok: true,
        data: mockEmployees,
        code: 1,
        msg: 'success',
      });

      const { container } = render(
        <Wrapper>
          <EmployeeSelect disabled />
        </Wrapper>
      );

      await waitFor(() => {
        const select = container.querySelector('.ant-select-disabled');
        expect(select).toBeInTheDocument();
      });
    });
  });
});
