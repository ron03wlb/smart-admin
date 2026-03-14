/**
 * Employee Form Modal Component
 * 員工表單 Modal 組件（新增/編輯）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/employee/components/employee-form-modal/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Alert,
  message,
  Row,
  Col,
} from 'antd';
import { useModal } from '@/hooks/useModal';
import { employeeApi } from '@/api/system/employeeApi';
import { roleApi } from '@/api/system/roleApi';
import type { EmployeeFormData, GenderEnum } from '../types';
import { EMPLOYEE_VALIDATION } from '@/constants/system/employeeConst';

const { Option } = Select;

/**
 * 新增員工成功回調數據
 */
export interface AddEmployeeSuccessData {
  loginName: string;
  password: string;
}

/**
 * 員工表單 Modal Props
 */
export interface EmployeeFormModalProps {
  /**
   * Modal 顯示狀態（由父組件控制）
   */
  visible: boolean;
  /**
   * 關閉 Modal 的回調
   */
  onCancel: () => void;
  /**
   * 提交成功後的回調
   * @param data 新員工的登錄名和密碼（僅新增時有值）
   */
  onSuccess: (data?: AddEmployeeSuccessData) => void;
  /**
   * 初始數據（編輯模式時傳入）
   */
  initialData?: EmployeeFormData;
}

/**
 * 員工表單 Modal 組件
 */
export const EmployeeFormModal: React.FC<EmployeeFormModalProps> = ({
  visible,
  onCancel,
  onSuccess,
  initialData,
}) => {
  const [form] = Form.useForm<EmployeeFormData>();
  const [loading, setLoading] = useState(false);
  const [roleList, setRoleList] = useState<any[]>([]);

  // ==================== Modal Mode Detection ====================

  const { isEditMode } = useModal({
    editIdField: 'employeeId',
    recordData: initialData,
  });

  // ==================== Load Role List ====================

  /**
   * 加載角色列表
   */
  const loadRoleList = async () => {
    try {
      const response = await roleApi.queryAll();
      if (response.ok) {
        setRoleList(response.data || []);
      }
    } catch (error) {
      console.error('加載角色列表失敗:', error);
    }
  };

  // ==================== Form Initialization ====================

  useEffect(() => {
    if (visible) {
      loadRoleList();

      if (initialData) {
        // 編輯模式：填充表單數據
        form.setFieldsValue(initialData);
      } else {
        // 新增模式：重置表單並設置默認值
        form.resetFields();
        form.setFieldsValue({
          gender: 1, // 默認性別：男
          disabledFlag: 0, // 默認狀態：啟用
          leaveFlag: 0, // 默認在職狀態：在職
        });
      }
    }
  }, [visible, initialData, form]);

  // ==================== Form Submission ====================

  /**
   * 處理表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEditMode) {
        // 編輯模式
        await employeeApi.updateEmployee({
          ...values,
          employeeId: initialData!.employeeId!,
        });
        message.success('更新成功');
        onSuccess();
      } else {
        // 新增模式
        const response = await employeeApi.addEmployee(values);
        message.success('添加成功');
        // 傳遞登錄名和密碼給父組件
        onSuccess({
          loginName: values.loginName,
          password: response.data,
        });
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據！');
      } else {
        message.error(isEditMode ? '更新失敗' : '添加失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 處理取消操作
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  // ==================== Render ====================

  return (
    <Modal
      title={isEditMode ? '編輯員工' : '添加員工'}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      okText={isEditMode ? '更新' : '保存'}
      cancelText="取消"
      destroyOnClose
    >
      <Alert
        message="超管需要直接在數據庫表 t_employee 修改"
        type="error"
        closable
        style={{ marginBottom: 16 }}
      />

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          gender: 1,
          disabledFlag: 0,
          leaveFlag: 0,
        }}
      >
        <Form.Item
          label="姓名"
          name="actualName"
          rules={[
            { required: true, message: '姓名不能為空' },
            { max: EMPLOYEE_VALIDATION.NAME_MAX_LENGTH, message: `姓名不能大於${EMPLOYEE_VALIDATION.NAME_MAX_LENGTH}個字符` },
          ]}
        >
          <Input placeholder="請輸入姓名" />
        </Form.Item>

        <Form.Item
          label="手機號"
          name="phone"
          rules={[
            { required: true, message: '手機號不能為空' },
            { pattern: EMPLOYEE_VALIDATION.PHONE_REGEX, message: '請輸入正確的手機號碼' },
          ]}
        >
          <Input placeholder="請輸入手機號" />
        </Form.Item>

        <Form.Item
          label="部門"
          name="departmentId"
          rules={[{ required: true, message: '部門不能為空' }]}
        >
          <Select placeholder="請選擇部門">
            {/* TODO: 集成 DepartmentTreeSelect 組件 */}
            <Option value={1}>默認部門</Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="登錄名"
          name="loginName"
          rules={[
            { required: true, message: '登錄賬號不能為空' },
            { max: EMPLOYEE_VALIDATION.LOGIN_NAME_MAX_LENGTH, message: `登錄賬號不能大於${EMPLOYEE_VALIDATION.LOGIN_NAME_MAX_LENGTH}個字符` },
          ]}
          extra={!isEditMode && <span style={{ color: '#8c8c8c' }}>初始密碼默認為：隨機</span>}
        >
          <Input placeholder="請輸入登錄名" />
        </Form.Item>

        <Form.Item
          label="郵箱"
          name="email"
          rules={[
            { required: true, message: '請輸入郵箱' },
            { pattern: EMPLOYEE_VALIDATION.EMAIL_REGEX, message: '請輸入正確的郵箱格式' },
          ]}
        >
          <Input placeholder="請輸入郵箱" />
        </Form.Item>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="性別"
              name="gender"
              rules={[{ required: true, message: '性別不能為空' }]}
            >
              <Select placeholder="請選擇性別">
                <Option value={1}>男</Option>
                <Option value={2}>女</Option>
              </Select>
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label="狀態"
              name="disabledFlag"
              rules={[{ required: true, message: '狀態不能為空' }]}
            >
              <Select placeholder="請選擇狀態">
                <Option value={0}>啟用</Option>
                <Option value={1}>禁用</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          label="職務"
          name="positionId"
        >
          <Select placeholder="請選擇職務" allowClear>
            {/* TODO: 集成 PositionSelect 組件 */}
            <Option value={1}>默認職務</Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="角色"
          name="roleIdList"
        >
          <Select
            mode="multiple"
            placeholder="請選擇角色"
            optionFilterProp="children"
            allowClear
          >
            {roleList.map((role) => (
              <Option key={role.roleId} value={role.roleId}>
                {role.roleName}
              </Option>
            ))}
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};
