/**
 * Role Management Page
 * 角色管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { useEffect, useState } from 'react';
import { Button, Card, Col, Form, Input, message, Modal, Row, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, SearchOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, RoleQueryForm } from './types';
import { ROLE_PERMISSION, ROLE_TABLE_COLUMNS_WIDTH } from '@/constants/system/roleConst';
import PrivilegeButton from '@/components/PrivilegeButton';
import RoleFormModal from './components/RoleFormModal';
import RoleMenuModal from './components/RoleMenuModal';
import RoleEmployeeDrawer from './components/RoleEmployeeDrawer';
import RoleDataScopeModal from './components/RoleDataScopeModal';

const { confirm } = Modal;

export default function RolePage() {
  const [queryForm] = Form.useForm<RoleQueryForm>();
  const [loading, setLoading] = useState(false);
  const [roleList, setRoleList] = useState<RoleVO[]>([]);
  const [filteredRoleList, setFilteredRoleList] = useState<RoleVO[]>([]);

  // Form Modal state
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<RoleVO | undefined>(undefined);

  // Role Menu Modal state
  const [menuModalVisible, setMenuModalVisible] = useState(false);
  const [menuModalRole, setMenuModalRole] = useState<RoleVO | undefined>(undefined);

  // Role Employee Drawer state
  const [employeeDrawerVisible, setEmployeeDrawerVisible] = useState(false);
  const [employeeDrawerRole, setEmployeeDrawerRole] = useState<RoleVO | undefined>(undefined);

  // Role Data Scope Modal state
  const [dataScopeModalVisible, setDataScopeModalVisible] = useState(false);
  const [dataScopeModalRole, setDataScopeModalRole] = useState<RoleVO | undefined>(undefined);

  /**
   * 查詢角色列表
   */
  const fetchRoleList = async () => {
    try {
      setLoading(true);
      const res = await roleApi.queryAll();
      if (res.ok && res.data) {
        setRoleList(res.data);
        setFilteredRoleList(res.data);
      }
    } catch (error) {
      message.error('查詢角色列表失敗');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 搜尋處理
   */
  const handleSearch = () => {
    const values = queryForm.getFieldsValue();
    const { keywords } = values;

    if (!keywords || keywords.trim() === '') {
      setFilteredRoleList(roleList);
      return;
    }

    const keyword = keywords.toLowerCase();
    const filtered = roleList.filter(role => {
      return (
        role.roleName?.toLowerCase().includes(keyword) ||
        role.roleCode?.toLowerCase().includes(keyword) ||
        role.remark?.toLowerCase().includes(keyword)
      );
    });

    setFilteredRoleList(filtered);
  };

  /**
   * 重置搜尋
   */
  const handleReset = () => {
    queryForm.resetFields();
    setFilteredRoleList(roleList);
  };

  /**
   * 顯示新增表單
   */
  const handleAdd = () => {
    setFormInitialData(undefined);
    setFormModalVisible(true);
  };

  /**
   * 顯示編輯表單
   */
  const handleEdit = (record: RoleVO) => {
    setFormInitialData(record);
    setFormModalVisible(true);
  };

  /**
   * 刪除角色
   */
  const handleDelete = (record: RoleVO) => {
    confirm({
      title: '刪除角色',
      icon: <ExclamationCircleOutlined />,
      content: `確定要刪除角色「${record.roleName}」嗎？`,
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await roleApi.deleteRole(record.roleId);
          if (res.ok) {
            message.success('刪除成功');
            fetchRoleList();
          }
        } catch (error) {
          message.error('刪除失敗');
          console.error(error);
        }
      },
    });
  };

  /**
   * Form Modal 成功回調
   */
  const handleFormSuccess = () => {
    setFormModalVisible(false);
    setFormInitialData(undefined);
    fetchRoleList();
  };

  /**
   * Form Modal 取消回調
   */
  const handleFormCancel = () => {
    setFormModalVisible(false);
    setFormInitialData(undefined);
  };

  /**
   * 打開菜單權限 Modal
   */
  const handleOpenMenuModal = (record: RoleVO) => {
    setMenuModalRole(record);
    setMenuModalVisible(true);
  };

  /**
   * 菜單權限 Modal 成功回調
   */
  const handleMenuModalSuccess = () => {
    setMenuModalVisible(false);
    setMenuModalRole(undefined);
  };

  /**
   * 菜單權限 Modal 取消回調
   */
  const handleMenuModalCancel = () => {
    setMenuModalVisible(false);
    setMenuModalRole(undefined);
  };

  /**
   * 打開員工管理 Drawer
   */
  const handleOpenEmployeeDrawer = (record: RoleVO) => {
    setEmployeeDrawerRole(record);
    setEmployeeDrawerVisible(true);
  };

  /**
   * 員工管理 Drawer 關閉回調
   */
  const handleEmployeeDrawerClose = () => {
    setEmployeeDrawerVisible(false);
    setEmployeeDrawerRole(undefined);
  };

  /**
   * 打開數據範圍 Modal
   */
  const handleOpenDataScopeModal = (record: RoleVO) => {
    setDataScopeModalRole(record);
    setDataScopeModalVisible(true);
  };

  /**
   * 數據範圍 Modal 成功回調
   */
  const handleDataScopeModalSuccess = () => {
    setDataScopeModalVisible(false);
    setDataScopeModalRole(undefined);
  };

  /**
   * 數據範圍 Modal 取消回調
   */
  const handleDataScopeModalCancel = () => {
    setDataScopeModalVisible(false);
    setDataScopeModalRole(undefined);
  };

  useEffect(() => {
    fetchRoleList();
  }, []);

  /**
   * 表格列定義
   */
  const columns: ColumnsType<RoleVO> = [
    {
      title: '角色名稱',
      dataIndex: 'roleName',
      key: 'roleName',
      width: ROLE_TABLE_COLUMNS_WIDTH.roleName,
    },
    {
      title: '角色編碼',
      dataIndex: 'roleCode',
      key: 'roleCode',
      width: ROLE_TABLE_COLUMNS_WIDTH.roleCode,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: ROLE_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: ROLE_TABLE_COLUMNS_WIDTH.createTime,
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: ROLE_TABLE_COLUMNS_WIDTH.updateTime,
    },
    {
      title: '操作',
      key: 'operate',
      width: 280,
      fixed: 'right',
      render: (_: unknown, record: RoleVO) => (
        <Space size="small" wrap>
          <PrivilegeButton
            type="link"
            size="small"
            privilege={ROLE_PERMISSION.MENU_UPDATE}
            onClick={() => handleOpenMenuModal(record)}
          >
            菜單權限
          </PrivilegeButton>
          <PrivilegeButton
            type="link"
            size="small"
            privilege={ROLE_PERMISSION.EMPLOYEE_VIEW}
            onClick={() => handleOpenEmployeeDrawer(record)}
          >
            員工管理
          </PrivilegeButton>
          <PrivilegeButton
            type="link"
            size="small"
            privilege={ROLE_PERMISSION.DATA_SCOPE_UPDATE}
            onClick={() => handleOpenDataScopeModal(record)}
          >
            數據範圍
          </PrivilegeButton>
          <PrivilegeButton
            type="link"
            size="small"
            privilege={ROLE_PERMISSION.UPDATE}
            onClick={() => handleEdit(record)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            type="link"
            size="small"
            danger
            privilege={ROLE_PERMISSION.DELETE}
            onClick={() => handleDelete(record)}
          >
            刪除
          </PrivilegeButton>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        {/* 搜尋表單 */}
        <Form form={queryForm} layout="inline" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 16]} style={{ width: '100%' }}>
            <Col>
              <Form.Item name="keywords" label="關鍵字">
                <Input
                  placeholder="角色名稱/角色編碼/備註"
                  allowClear
                  style={{ width: 280 }}
                  onPressEnter={handleSearch}
                />
              </Form.Item>
            </Col>
            <Col>
              <Space size="small">
                <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                  搜尋
                </Button>
                <Button onClick={handleReset}>重置</Button>
              </Space>
            </Col>
          </Row>
        </Form>

        {/* 操作按鈕 */}
        <div style={{ marginBottom: 16 }}>
          <PrivilegeButton
            type="primary"
            icon={<PlusOutlined />}
            privilege={ROLE_PERMISSION.ADD}
            onClick={handleAdd}
          >
            新增角色
          </PrivilegeButton>
        </div>

        {/* 表格 */}
        <Table
          rowKey="roleId"
          columns={columns}
          dataSource={filteredRoleList}
          loading={loading}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 條`,
            defaultPageSize: 10,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* Form Modal */}
      <RoleFormModal
        visible={formModalVisible}
        onCancel={handleFormCancel}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />

      {/* Menu Permission Modal */}
      <RoleMenuModal
        visible={menuModalVisible}
        onCancel={handleMenuModalCancel}
        onSuccess={handleMenuModalSuccess}
        role={menuModalRole}
      />

      {/* Employee Management Drawer */}
      <RoleEmployeeDrawer
        visible={employeeDrawerVisible}
        onClose={handleEmployeeDrawerClose}
        role={employeeDrawerRole}
      />

      {/* Data Scope Modal */}
      <RoleDataScopeModal
        visible={dataScopeModalVisible}
        onCancel={handleDataScopeModalCancel}
        onSuccess={handleDataScopeModalSuccess}
        role={dataScopeModalRole}
      />
    </div>
  );
}
