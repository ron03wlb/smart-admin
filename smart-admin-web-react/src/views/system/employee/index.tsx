/**
 * Employee Management Page
 * 員工管理頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/employee/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { useState } from 'react';
import { Card, Input, Button, Table, Space, Radio, Tag, Typography, Modal, message } from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { TableColumnsType } from 'antd';
import { useTable } from '@/hooks/useTable';
import { usePrivilege } from '@/hooks/usePrivilege';
import PrivilegeButton from '@/components/PrivilegeButton';
import { employeeApi } from '@/api/system/employeeApi';
import type { EmployeeVO, EmployeeQueryForm, EmployeeFormData } from './types';
import {
  EMPLOYEE_PERMISSION,
  GENDER_LABELS,
  EMPLOYEE_TABLE_COLUMNS_WIDTH,
} from '@/constants/system/employeeConst';
import { EmployeeFormModal, type AddEmployeeSuccessData } from './components/EmployeeFormModal';
import { PasswordDisplayModal } from './components/PasswordDisplayModal';

const { Title } = Typography;
const { Search } = Input;

/**
 * 員工管理頁面
 */
export default function EmployeePage() {
  // const { t } = useTranslation();
  // const _hasAddPrivilege = usePrivilege(EMPLOYEE_PERMISSION.ADD);
  const hasUpdatePrivilege = usePrivilege(EMPLOYEE_PERMISSION.UPDATE);
  // const _hasDeletePrivilege = usePrivilege(EMPLOYEE_PERMISSION.DELETE);
  const hasResetPasswordPrivilege = usePrivilege(EMPLOYEE_PERMISSION.RESET_PASSWORD);
  const hasDisabledPrivilege = usePrivilege(EMPLOYEE_PERMISSION.DISABLED);

  // ==================== Modal State ====================

  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<EmployeeFormData | undefined>();
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);
  const [passwordData, setPasswordData] = useState({ loginName: '', password: '' });

  // ==================== Table State Management ====================

  const {
    tableData,
    loading,
    pagination,
    queryForm,
    setQueryForm,
    selectedRowKeys,
    query,
    reset,
    handleTableChange,
    handleRowSelectionChange,
  } = useTable<EmployeeVO, EmployeeQueryForm>({
    defaultQueryForm: {
      keyword: undefined,
      disabledFlag: false,
    },
    pagination: { pageNum: 1, pageSize: 10 },
    queryApi: employeeApi.queryEmployee,
    autoQuery: true,
  });

  // ==================== Search Operations ====================

  /**
   * 處理關鍵字搜索
   */
  const handleSearch = (value: string) => {
    setQueryForm({ keyword: value?.trim() });
    query();
  };

  /**
   * 處理狀態篩選變更
   */
  const handleStatusChange = (e: any) => {
    setQueryForm({ disabledFlag: e.target.value });
    query();
  };

  // ==================== Table Columns Definition ====================

  const columns: TableColumnsType<EmployeeVO> = [
    {
      title: '姓名',
      dataIndex: 'actualName',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.actualName,
      fixed: 'left',
    },
    {
      title: '性別',
      dataIndex: 'gender',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.gender,
      render: (gender: number) => GENDER_LABELS[gender as 1 | 2] || '-',
    },
    {
      title: '登錄賬號',
      dataIndex: 'loginName',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.loginName,
    },
    {
      title: '手機號',
      dataIndex: 'phone',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.phone,
    },
    {
      title: '郵箱',
      dataIndex: 'email',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.email,
      ellipsis: true,
    },
    {
      title: '超管',
      dataIndex: 'administratorFlag',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.administratorFlag,
      render: (administratorFlag: boolean) =>
        administratorFlag ? <Tag color="error">超管</Tag> : null,
    },
    {
      title: '狀態',
      dataIndex: 'disabledFlag',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.disabledFlag,
      render: (disabledFlag: boolean) => (
        <Tag color={disabledFlag ? 'error' : 'processing'}>{disabledFlag ? '禁用' : '啟用'}</Tag>
      ),
    },
    {
      title: '職務',
      dataIndex: 'positionName',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.positionName,
      ellipsis: true,
    },
    {
      title: '角色',
      dataIndex: 'roleNameList',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.roleNameList,
      render: (roleNameList: string) => roleNameList || '-',
    },
    {
      title: '部門',
      dataIndex: 'departmentName',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.departmentName,
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'operate',
      width: EMPLOYEE_TABLE_COLUMNS_WIDTH.operate,
      fixed: 'right',
      render: (_: any, record: EmployeeVO) => (
        <Space size="small">
          {hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              編輯
            </Button>
          )}
          {hasResetPasswordPrivilege && (
            <Button
              type="link"
              size="small"
              onClick={() => handleResetPassword(record.employeeId, record.loginName)}
            >
              重置密碼
            </Button>
          )}
          {hasDisabledPrivilege && (
            <Button
              type="link"
              size="small"
              onClick={() => handleToggleDisabled(record.employeeId, record.disabledFlag)}
            >
              {record.disabledFlag ? '啟用' : '禁用'}
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // ==================== CRUD Operations ====================

  /**
   * 處理新增員工
   */
  const handleAdd = () => {
    setFormInitialData(undefined);
    setFormModalVisible(true);
  };

  /**
   * 處理編輯員工
   */
  const handleEdit = (record: EmployeeVO) => {
    // 轉換 EmployeeVO 到 EmployeeFormData (boolean → number)
    const formData: EmployeeFormData = {
      employeeId: record.employeeId,
      actualName: record.actualName,
      phone: record.phone,
      departmentId: record.departmentId,
      loginName: record.loginName,
      email: record.email,
      gender: record.gender,
      disabledFlag: record.disabledFlag ? 1 : 0,
      leaveFlag: record.leaveFlag ? 1 : 0,
      positionId: record.positionId,
      roleIdList: record.roleIdList,
    };
    setFormInitialData(formData);
    setFormModalVisible(true);
  };

  /**
   * 表單提交成功回調
   */
  const handleFormSuccess = (data?: AddEmployeeSuccessData) => {
    setFormModalVisible(false);
    query(); // 刷新列表

    // 如果有數據（新增員工），顯示密碼 Modal
    if (data) {
      setPasswordData({
        loginName: data.loginName,
        password: data.password,
      });
      setPasswordModalVisible(true);
    }
  };

  /**
   * 處理批量刪除
   */
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要刪除的員工');
      return;
    }

    Modal.confirm({
      title: '確認刪除',
      icon: <ExclamationCircleOutlined />,
      content: `確定要刪除選中的 ${selectedRowKeys.length} 個員工嗎？`,
      okText: '確定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await employeeApi.batchDeleteEmployee(selectedRowKeys as number[]);
          message.success('刪除成功');
          query();
        } catch (error) {
          message.error('刪除失敗');
        }
      },
    });
  };

  /**
   * 處理重置密碼
   */
  const handleResetPassword = (employeeId: number, loginName: string) => {
    Modal.confirm({
      title: '確認重置密碼',
      icon: <ExclamationCircleOutlined />,
      content: `確定要重置員工 ${loginName} 的密碼嗎？`,
      okText: '確定',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await employeeApi.resetPassword(employeeId);
          // 顯示密碼 Modal
          setPasswordData({
            loginName,
            password: response.data,
          });
          setPasswordModalVisible(true);
          message.success('密碼重置成功');
        } catch (error) {
          message.error('密碼重置失敗');
        }
      },
    });
  };

  /**
   * 處理啟用/禁用員工
   */
  const handleToggleDisabled = (employeeId: number, currentDisabledFlag: boolean) => {
    const action = currentDisabledFlag ? '啟用' : '禁用';

    Modal.confirm({
      title: `確認${action}`,
      icon: <ExclamationCircleOutlined />,
      content: `確定要${action}該員工嗎？`,
      okText: '確定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await employeeApi.updateDisabled(employeeId);
          message.success(`${action}成功`);
          query();
        } catch (error) {
          message.error(`${action}失敗`);
        }
      },
    });
  };

  // ==================== Render ====================

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        {/* Header */}
        <div style={{ marginBottom: 16 }}>
          <Title level={5} style={{ marginBottom: 16 }}>
            員工管理
          </Title>

          {/* Search Bar */}
          <Space style={{ marginBottom: 16, width: '100%' }} wrap>
            <Radio.Group value={queryForm.disabledFlag} onChange={handleStatusChange}>
              <Radio.Button value={undefined}>全部</Radio.Button>
              <Radio.Button value={false}>啟用</Radio.Button>
              <Radio.Button value={true}>禁用</Radio.Button>
            </Radio.Group>

            <Search
              placeholder="姓名/手機號/登錄賬號"
              allowClear
              style={{ width: 300 }}
              onSearch={handleSearch}
              enterButton={
                <Button type="primary" icon={<SearchOutlined />}>
                  查詢
                </Button>
              }
            />

            <Button icon={<ReloadOutlined />} onClick={reset}>
              重置
            </Button>
          </Space>

          {/* Action Buttons */}
          <Space style={{ marginBottom: 16 }}>
            <PrivilegeButton
              privilege={EMPLOYEE_PERMISSION.ADD}
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
            >
              添加成員
            </PrivilegeButton>

            <PrivilegeButton privilege={EMPLOYEE_PERMISSION.DELETE} onClick={handleBatchDelete}>
              批量刪除
            </PrivilegeButton>
          </Space>
        </div>

        {/* Table */}
        <Table
          rowKey="employeeId"
          columns={columns}
          dataSource={tableData}
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 條`,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
          onChange={handleTableChange}
          rowSelection={{
            selectedRowKeys,
            onChange: handleRowSelectionChange,
          }}
          scroll={{ x: 1500 }}
          bordered
          size="small"
        />
      </Card>

      {/* 員工表單 Modal */}
      <EmployeeFormModal
        visible={formModalVisible}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />

      {/* 密碼顯示 Modal */}
      <PasswordDisplayModal
        visible={passwordModalVisible}
        loginName={passwordData.loginName}
        password={passwordData.password}
        onClose={() => setPasswordModalVisible(false)}
      />
    </div>
  );
}
