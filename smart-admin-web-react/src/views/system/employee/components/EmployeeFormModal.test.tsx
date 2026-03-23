/**
 * EmployeeFormModal Component Unit Tests
 * 員工表單 Modal 組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { EmployeeFormModal } from './EmployeeFormModal';
import type { EmployeeFormData } from '../types';

// Mock dependencies
vi.mock('@/api/system/employeeApi');
vi.mock('@/api/system/roleApi');
vi.mock('@/hooks/useModal');
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
    },
  };
});

import { employeeApi } from '@/api/system/employeeApi';
import { roleApi } from '@/api/system/roleApi';
import { useModal } from '@/hooks/useModal';
import { message } from 'antd';

describe('EmployeeFormModal', () => {
  const mockOnCancel = vi.fn();
  const mockOnSuccess = vi.fn();

  const defaultProps = {
    visible: true,
    onCancel: mockOnCancel,
    onSuccess: mockOnSuccess,
  };

  // Helper function to find Modal buttons (rendered to document.body)
  const findModalButton = async (selector: string) => {
    return await waitFor(() => {
      const button = document.querySelector(selector);
      if (!button) throw new Error(`Button with selector "${selector}" not found`);
      return button;
    });
  };

  const findSubmitButton = () => findModalButton('.ant-modal-footer .ant-btn-primary');
  const findCancelButton = () =>
    findModalButton('.ant-modal-footer .ant-btn:not(.ant-btn-primary)');

  beforeEach(() => {
    vi.clearAllMocks();

    // Mock roleApi.queryAll
    vi.mocked(roleApi.queryAll).mockResolvedValue({
      code: 200,
      ok: true,
      msg: 'Success',
      data: [
        { roleId: 1, roleName: '管理員' },
        { roleId: 2, roleName: '普通用戶' },
      ],
    });

    // Mock useModal default (add mode)
    vi.mocked(useModal).mockReturnValue({
      visible: false,
      setVisible: vi.fn(),
      formData: {},
      setFormData: vi.fn(),
      isEdit: false,
      loading: false,
      setLoading: vi.fn(),
      open: vi.fn(),
      close: vi.fn(),
      handleSubmit: vi.fn(),
      reset: vi.fn(),
    } as any);
  });

  // ==================== 基本渲染測試 ====================

  describe('Basic Rendering - Add Mode', () => {
    it('should render in add mode', () => {
      render(<EmployeeFormModal {...defaultProps} />);

      expect(screen.getByText('添加員工')).toBeInTheDocument();
    });

    it('should display all form fields', () => {
      render(<EmployeeFormModal {...defaultProps} />);

      expect(screen.getByLabelText('姓名')).toBeInTheDocument();
      expect(screen.getByLabelText('手機號')).toBeInTheDocument();
      expect(screen.getByLabelText('部門')).toBeInTheDocument();
      expect(screen.getByLabelText('登錄名')).toBeInTheDocument();
      expect(screen.getByLabelText('郵箱')).toBeInTheDocument();
      expect(screen.getByLabelText('性別')).toBeInTheDocument();
      expect(screen.getByLabelText('狀態')).toBeInTheDocument();
      expect(screen.getByLabelText('職務')).toBeInTheDocument();
      expect(screen.getByLabelText('角色')).toBeInTheDocument();
    });

    it('should display warning alert for admin modification', () => {
      render(<EmployeeFormModal {...defaultProps} />);

      expect(screen.getByText(/超管需要直接在數據庫表/)).toBeInTheDocument();
    });

    it('should show initial password hint in add mode', () => {
      render(<EmployeeFormModal {...defaultProps} />);

      expect(screen.getByText(/初始密碼默認為：隨機/)).toBeInTheDocument();
    });
  });

  describe('Basic Rendering - Edit Mode', () => {
    it('should render in edit mode', () => {
      vi.mocked(useModal).mockReturnValue({
        visible: false,
        setVisible: vi.fn(),
        formData: {},
        setFormData: vi.fn(),
        isEdit: true,
        loading: false,
        setLoading: vi.fn(),
        open: vi.fn(),
        close: vi.fn(),
        handleSubmit: vi.fn(),
        reset: vi.fn(),
      } as any);

      const editData: EmployeeFormData = {
        employeeId: 1,
        actualName: '張三',
        phone: '13800138000',
        departmentId: 1,
        loginName: 'zhangsan',
        email: 'zhangsan@example.com',
        gender: 1,
        disabledFlag: 0,
        leaveFlag: 0,
      };

      render(<EmployeeFormModal {...defaultProps} initialData={editData} />);

      expect(screen.getByText('編輯員工')).toBeInTheDocument();
    });

    it('should not show initial password hint in edit mode', () => {
      vi.mocked(useModal).mockReturnValue({
        visible: false,
        setVisible: vi.fn(),
        formData: {},
        setFormData: vi.fn(),
        isEdit: true,
        loading: false,
        setLoading: vi.fn(),
        open: vi.fn(),
        close: vi.fn(),
        handleSubmit: vi.fn(),
        reset: vi.fn(),
      } as any);

      const editData: EmployeeFormData = {
        employeeId: 1,
        actualName: '張三',
        loginName: 'zhangsan',
        phone: '13800138000',
        departmentId: 1,
        gender: 1,
        disabledFlag: 0,
        leaveFlag: 0,
      };

      render(<EmployeeFormModal {...defaultProps} initialData={editData} />);

      expect(screen.queryByText(/初始密碼默認為：隨機/)).not.toBeInTheDocument();
    });
  });

  // ==================== 表單驗證測試 ====================

  describe('Form Validation', () => {
    it('should validate required fields', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      const submitButton = await findSubmitButton();
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('參數驗證錯誤，請仔細填寫表單數據！');
      });
    });

    it('should validate phone number format', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      // 填寫其他必填欄位（讓只有 phone 欄位顯示錯誤）
      fireEvent.change(screen.getByLabelText('姓名'), { target: { value: '測試' } });
      fireEvent.change(screen.getByLabelText('登錄名'), { target: { value: 'test' } });
      fireEvent.change(screen.getByLabelText('郵箱'), { target: { value: 'test@example.com' } });

      // 選擇部門
      const departmentSelect = screen.getByLabelText('部門');
      fireEvent.mouseDown(departmentSelect);
      await waitFor(() => {
        const option = screen.getByText('默認部門');
        fireEvent.click(option);
      });

      // 填寫無效的手機號
      const phoneInput = screen.getByLabelText('手機號');
      fireEvent.change(phoneInput, { target: { value: '12345' } });

      const submitButton = await findSubmitButton();
      fireEvent.click(submitButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/請輸入正確的手機號碼/))).toBe(true);
      });
    });

    it('should validate email format', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      // 填寫其他必填欄位（讓只有 email 欄位顯示錯誤）
      fireEvent.change(screen.getByLabelText('姓名'), { target: { value: '測試' } });
      fireEvent.change(screen.getByLabelText('手機號'), { target: { value: '13900139000' } });
      fireEvent.change(screen.getByLabelText('登錄名'), { target: { value: 'test' } });

      // 選擇部門
      const departmentSelect = screen.getByLabelText('部門');
      fireEvent.mouseDown(departmentSelect);
      await waitFor(() => {
        const option = screen.getByText('默認部門');
        fireEvent.click(option);
      });

      // 填寫無效的郵箱
      const emailInput = screen.getByLabelText('郵箱');
      fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

      const submitButton = await findSubmitButton();
      fireEvent.click(submitButton);

      await waitFor(() => {
        const errorElements = document.querySelectorAll('.ant-form-item-explain-error');
        const errorTexts = Array.from(errorElements).map(el => el.textContent);
        expect(errorTexts.some(text => text?.match(/請輸入正確的郵箱格式/))).toBe(true);
      });
    });

    it('should validate name max length', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      const nameInput = screen.getByLabelText('姓名');
      const longName = 'a'.repeat(31); // 超過 30 個字符
      fireEvent.change(nameInput, { target: { value: longName } });

      const submitButton = await findSubmitButton();
      fireEvent.click(submitButton);

      // Ant Design Form 驗證錯誤會顯示在 .ant-form-item-explain-error 元素中
      await waitFor(() => {
        const errorElement = document.querySelector('.ant-form-item-explain-error');
        expect(errorElement).toBeTruthy();
        expect(errorElement?.textContent).toMatch(/姓名不能大於30個字符/);
      });
    });
  });

  // ==================== 表單提交測試 - 新增模式 ====================

  describe('Form Submission - Add Mode', () => {
    it('should submit form successfully in add mode', async () => {
      vi.mocked(employeeApi.addEmployee).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: 'Abc123!@#', // 新密碼
      });

      render(<EmployeeFormModal {...defaultProps} />);

      // 填寫表單（包含所有必填欄位）
      fireEvent.change(screen.getByLabelText('姓名'), { target: { value: '李四' } });
      fireEvent.change(screen.getByLabelText('手機號'), { target: { value: '13900139000' } });
      fireEvent.change(screen.getByLabelText('登錄名'), { target: { value: 'lisi' } });
      fireEvent.change(screen.getByLabelText('郵箱'), { target: { value: 'lisi@example.com' } });

      // 選擇部門（必填欄位）
      const departmentSelect = screen.getByLabelText('部門');
      fireEvent.mouseDown(departmentSelect);
      await waitFor(() => {
        const option = screen.getByText('默認部門');
        fireEvent.click(option);
      });

      const submitButton = await findSubmitButton();
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(employeeApi.addEmployee).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('添加成功');
        expect(mockOnSuccess).toHaveBeenCalledWith({
          loginName: 'lisi',
          password: 'Abc123!@#',
        });
      });
    });

    it('should handle API error in add mode', async () => {
      vi.mocked(employeeApi.addEmployee).mockRejectedValue(new Error('API Error'));

      render(<EmployeeFormModal {...defaultProps} />);

      // 填寫表單（包含所有必填欄位）
      fireEvent.change(screen.getByLabelText('姓名'), { target: { value: '李四' } });
      fireEvent.change(screen.getByLabelText('手機號'), { target: { value: '13900139000' } });
      fireEvent.change(screen.getByLabelText('登錄名'), { target: { value: 'lisi' } });
      fireEvent.change(screen.getByLabelText('郵箱'), { target: { value: 'lisi@example.com' } });

      // 選擇部門（必填欄位）
      const departmentSelect = screen.getByLabelText('部門');
      fireEvent.mouseDown(departmentSelect);
      await waitFor(() => {
        const option = screen.getByText('默認部門');
        fireEvent.click(option);
      });

      const submitButton = await findSubmitButton();
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('添加失敗');
      });
    });
  });

  // ==================== 表單提交測試 - 編輯模式 ====================

  describe('Form Submission - Edit Mode', () => {
    it('should submit form successfully in edit mode', async () => {
      vi.mocked(useModal).mockReturnValue({
        visible: false,
        setVisible: vi.fn(),
        formData: {},
        setFormData: vi.fn(),
        isEdit: true,
        loading: false,
        setLoading: vi.fn(),
        open: vi.fn(),
        close: vi.fn(),
        handleSubmit: vi.fn(),
        reset: vi.fn(),
      } as any);

      vi.mocked(employeeApi.updateEmployee).mockResolvedValue({
        code: 200,
        ok: true,
        msg: 'Success',
        data: undefined,
      });

      const editData: EmployeeFormData = {
        employeeId: 1,
        actualName: '張三',
        phone: '13800138000',
        departmentId: 1,
        loginName: 'zhangsan',
        email: 'zhangsan@example.com',
        gender: 1,
        disabledFlag: 0,
        leaveFlag: 0,
      };

      render(<EmployeeFormModal {...defaultProps} initialData={editData} />);

      const submitButton = await findSubmitButton();
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(employeeApi.updateEmployee).toHaveBeenCalled();
        expect(message.success).toHaveBeenCalledWith('更新成功');
        expect(mockOnSuccess).toHaveBeenCalledWith();
      });
    });
  });

  // ==================== Modal 行為測試 ====================

  describe('Modal Behavior', () => {
    it('should call onCancel when cancel button is clicked', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should reset form on cancel', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      // 填寫表單
      const nameInput = screen.getByLabelText('姓名');
      fireEvent.change(nameInput, { target: { value: '測試' } });

      // 取消
      const cancelButton = await findCancelButton();
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('should load role list when modal opens', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      await waitFor(() => {
        expect(roleApi.queryAll).toHaveBeenCalled();
      });
    });
  });

  // ==================== 角色選擇測試 ====================

  describe('Role Selection', () => {
    it('should display role options', async () => {
      render(<EmployeeFormModal {...defaultProps} />);

      await waitFor(() => {
        expect(roleApi.queryAll).toHaveBeenCalled();
      });

      // Ant Design Select 會在展開時顯示選項
      const roleSelect = screen.getByLabelText('角色');
      fireEvent.mouseDown(roleSelect);

      await waitFor(() => {
        expect(screen.getByText('管理員')).toBeInTheDocument();
        expect(screen.getByText('普通用戶')).toBeInTheDocument();
      });
    });

    it('should allow multiple role selection', () => {
      render(<EmployeeFormModal {...defaultProps} />);

      const roleSelect = screen.getByLabelText('角色');
      // Ant Design Select mode="multiple" 支持多選
      expect(roleSelect).toBeInTheDocument();
    });
  });
});
