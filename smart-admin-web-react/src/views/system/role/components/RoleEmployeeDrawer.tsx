/**
 * Role Employee Management Drawer Component
 * 角色員工管理 Drawer 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-23
 */

import React, { useEffect, useState, useCallback } from 'react';
import { Drawer, Table, Input, Button, Space, message, Modal, Tag } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, RoleEmployeeVO, RoleEmployeeQueryForm } from '../types';
import { PAGE_SIZE, PAGE_SIZE_OPTIONS } from '@/constants/common-const';
import EmployeeTableSelectModal from './EmployeeTableSelectModal';

interface RoleEmployeeDrawerProps {
  visible: boolean;
  onClose: () => void;
  role?: RoleVO;
}

export default function RoleEmployeeDrawer({ visible, onClose, role }: RoleEmployeeDrawerProps) {
  // 查詢表單
  const [queryForm, setQueryForm] = useState<RoleEmployeeQueryForm>({
    roleId: 0,
    keywords: '',
    pageNum: 1,
    pageSize: PAGE_SIZE,
  });

  // 表格數據
  const [loading, setLoading] = useState(false);
  const [tableData, setTableData] = useState<RoleEmployeeVO[]>([]);
  const [total, setTotal] = useState(0);

  // 選中的員工 IDs
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // 員工選擇 Modal 顯示狀態
  const [employeeSelectModalVisible, setEmployeeSelectModalVisible] = useState(false);

  /**
   * 查詢員工列表
   */
  const queryEmployeeList = useCallback(async () => {
    if (!role) return;

    try {
      setLoading(true);
      const params: RoleEmployeeQueryForm = {
        ...queryForm,
        roleId: role.roleId,
      };

      const res = await roleApi.queryRoleEmployee(params);
      if (res.ok && res.data) {
        setTableData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '查詢失敗');
      }
    } finally {
      setLoading(false);
    }
  }, [queryForm, role]);

  /**
   * 搜索
   */
  const handleSearch = () => {
    setQueryForm(prev => ({ ...prev, pageNum: 1 }));
  };

  /**
   * 重置搜索
   */
  const handleReset = () => {
    setQueryForm({
      roleId: role?.roleId || 0,
      keywords: '',
      pageNum: 1,
      pageSize: PAGE_SIZE,
    });
  };

  /**
   * 分頁變化
   */
  const handlePaginationChange = (page: number, pageSize: number) => {
    setQueryForm(prev => ({ ...prev, pageNum: page, pageSize }));
  };

  /**
   * 移除單個員工
   */
  const handleRemoveEmployee = (employeeId: number) => {
    if (!role) return;

    Modal.confirm({
      title: '確認移除',
      content: '確定要移除該角色成員嗎？',
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await roleApi.deleteEmployeeRole(employeeId, role.roleId);
          if (res.ok) {
            message.success('移除成功');
            queryEmployeeList();
          }
        } catch (error) {
          if (error instanceof Error) {
            message.error(error.message || '移除失敗');
          }
        }
      },
    });
  };

  /**
   * 批量移除員工
   */
  const handleBatchRemove = () => {
    if (!role) return;

    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要移除的員工');
      return;
    }

    Modal.confirm({
      title: '確認批量移除',
      content: `確定要移除這 ${selectedRowKeys.length} 個角色成員嗎？`,
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await roleApi.batchRemoveRoleEmployee({
            roleId: role.roleId,
            employeeIdList: selectedRowKeys.map(key => Number(key)),
          });

          if (res.ok) {
            message.success('移除成功');
            setSelectedRowKeys([]);
            queryEmployeeList();
          }
        } catch (error) {
          if (error instanceof Error) {
            message.error(error.message || '移除失敗');
          }
        }
      },
    });
  };

  /**
   * 打開員工選擇 Modal
   */
  const handleOpenEmployeeSelect = () => {
    setEmployeeSelectModalVisible(true);
  };

  /**
   * 批量添加員工
   */
  const handleAddEmployees = async (employeeIds: number[]) => {
    if (!role) return;

    try {
      const res = await roleApi.batchAddRoleEmployee({
        roleId: role.roleId,
        employeeIdList: employeeIds,
      });

      if (res.ok) {
        message.success(`成功添加 ${employeeIds.length} 名員工`);
        setEmployeeSelectModalVisible(false);
        queryEmployeeList();
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '添加失敗');
      }
    }
  };

  /**
   * 表格列定義
   */
  const columns: ColumnsType<RoleEmployeeVO> = [
    {
      title: '姓名',
      dataIndex: 'actualName',
      width: 100,
    },
    {
      title: '手機號',
      dataIndex: 'phone',
      width: 120,
    },
    {
      title: '登錄賬號',
      dataIndex: 'loginName',
      width: 120,
    },
    {
      title: '部門',
      dataIndex: 'departmentName',
      width: 150,
    },
    {
      title: '狀態',
      dataIndex: 'disabledFlag',
      width: 80,
      render: (disabled: boolean) => (
        <Tag color={disabled ? 'error' : 'processing'}>{disabled ? '禁用' : '啟用'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      fixed: 'right',
      render: (_: unknown, record: RoleEmployeeVO) => (
        <Button
          type="link"
          size="small"
          danger
          onClick={() => handleRemoveEmployee(record.employeeId)}
        >
          移除
        </Button>
      ),
    },
  ];

  /**
   * 表格分頁配置
   */
  const pagination: TablePaginationConfig = {
    current: queryForm.pageNum,
    pageSize: queryForm.pageSize,
    total,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: PAGE_SIZE_OPTIONS,
    showTotal: total => `共 ${total} 條`,
    onChange: handlePaginationChange,
  };

  /**
   * 行選擇配置
   */
  const rowSelection = {
    selectedRowKeys,
    onChange: (selectedKeys: React.Key[]) => {
      setSelectedRowKeys(selectedKeys);
    },
  };

  /**
   * Drawer 顯示時查詢數據
   */
  useEffect(() => {
    if (visible && role) {
      setQueryForm(prev => ({
        ...prev,
        roleId: role.roleId,
        pageNum: 1,
      }));
    }
  }, [visible, role]);

  /**
   * 查詢條件變化時自動查詢
   */
  useEffect(() => {
    if (visible && role && queryForm.roleId === role.roleId) {
      queryEmployeeList();
    }
  }, [queryForm, visible, role, queryEmployeeList]);

  return (
    <>
      <Drawer
        title={`角色員工列表 - ${role?.roleName || ''}`}
        placement="right"
        onClose={onClose}
        open={visible}
        width={900}
        destroyOnClose
      >
        {/* 搜索欄 */}
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            placeholder="姓名/手機號/登錄賬號"
            value={queryForm.keywords}
            onChange={e => setQueryForm(prev => ({ ...prev, keywords: e.target.value }))}
            onPressEnter={handleSearch}
            style={{ width: 250 }}
            allowClear
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>

        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenEmployeeSelect}>
            添加員工
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={handleBatchRemove}
            disabled={selectedRowKeys.length === 0}
          >
            批量移除 {selectedRowKeys.length > 0 && `(${selectedRowKeys.length})`}
          </Button>
        </Space>

        {/* 員工列表表格 */}
        <Table
          loading={loading}
          dataSource={tableData}
          columns={columns}
          pagination={pagination}
          rowSelection={rowSelection}
          rowKey="employeeId"
          scroll={{ x: 800, y: 'calc(100vh - 350px)' }}
          size="small"
          bordered
        />
      </Drawer>

      {/* 員工選擇 Modal */}
      <EmployeeTableSelectModal
        visible={employeeSelectModalVisible}
        onCancel={() => setEmployeeSelectModalVisible(false)}
        onConfirm={handleAddEmployees}
        excludeEmployeeIds={tableData.map(emp => emp.employeeId)}
      />
    </>
  );
}
