/**
 * Account Center Component
 * 個人中心 - 個人信息編輯
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/account/components/center/index.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useEffect } from 'react';
import { Form, Input, Select, Button, message } from 'antd';
import { employeeApi } from '@/api/system/employeeApi';
import { useAppSelector } from '@/store/hooks';

const { TextArea } = Input;

/**
 * 性別選項
 */
const GENDER_OPTIONS = [
  { value: 1, label: '男' },
  { value: 2, label: '女' },
];

const Center: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  // 從 Redux 獲取當前用戶信息
  const employeeId = useAppSelector(state => state.user.employeeId);

  /**
   * 獲取員工信息
   */
  const getEmployeeInfo = async () => {
    if (!employeeId) {
      return;
    }

    try {
      setLoading(true);
      const result = await employeeApi.getEmployee(Number(employeeId));
      form.setFieldsValue({
        loginName: result.data.loginName,
        departmentId: result.data.departmentId,
        actualName: result.data.actualName,
        gender: result.data.gender,
        phone: result.data.phone,
        email: result.data.email,
        positionId: result.data.positionId,
        remark: result.data.remark,
      });
    } catch (error) {
      console.error('獲取員工信息失敗:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    getEmployeeInfo();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * 提交更新
   */
  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      setLoading(true);
      await employeeApi.updateEmployee({
        employeeId: Number(employeeId),
        ...values,
      });
      message.success('個人信息更新成功');
      getEmployeeInfo();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('請檢查表單必填項');
      } else {
        console.error('更新個人信息失敗:', error);
        message.error('更新失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 20 }}>個人中心</div>

      <div style={{ maxWidth: 600 }}>
        <Form form={form} layout="vertical">
          <Form.Item label="登錄賬號" name="loginName">
            <Input placeholder="登錄賬號" disabled />
          </Form.Item>

          <Form.Item label="部門" name="departmentId">
            <Input placeholder="部門" disabled />
          </Form.Item>

          <Form.Item
            label="員工名稱"
            name="actualName"
            rules={[{ required: true, message: '請輸入員工名稱' }]}
          >
            <Input placeholder="請輸入員工名稱" />
          </Form.Item>

          <Form.Item label="性別" name="gender">
            <Select placeholder="請選擇性別" options={GENDER_OPTIONS} />
          </Form.Item>

          <Form.Item
            label="手機號碼"
            name="phone"
            rules={[
              { required: true, message: '請輸入手機號碼' },
              { pattern: /^1[3-9]\d{9}$/, message: '請輸入有效的手機號碼' },
            ]}
          >
            <Input placeholder="請輸入手機號碼" />
          </Form.Item>

          <Form.Item
            label="郵箱"
            name="email"
            rules={[{ type: 'email', message: '請輸入有效的郵箱地址' }]}
          >
            <Input placeholder="請輸入郵箱" />
          </Form.Item>

          <Form.Item label="職務" name="positionId">
            <Input placeholder="職務" />
          </Form.Item>

          <Form.Item label="備註" name="remark">
            <TextArea rows={4} placeholder="請輸入備註" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" onClick={handleSubmit} loading={loading}>
              更新個人信息
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
};

export default Center;
