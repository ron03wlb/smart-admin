/**
 * Role Employee List
 *
 * Corresponds to Vue's role/components/role-employee-list/index.vue
 * Displays employees assigned to a role with search, batch operations.
 */
import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Table, Input, Button, Tag, Modal, message, Space } from 'antd';
import { roleApi } from '@/api/system/role-api';
import type { RoleEmployeeVO } from '@/types/role.types';
import type { ColumnsType } from 'antd/es/table';
import { RoleContext } from '../index';
import EmployeeTableSelectModal from '@/components/system/employee-table-select-modal/EmployeeTableSelectModal';

const PAGE_SIZE = 10;

const RoleEmployeeList: React.FC = () => {
  const { selectedRoleId } = useContext(RoleContext);
  const [tableData, setTableData] = useState<RoleEmployeeVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [selectModalOpen, setSelectModalOpen] = useState(false);
  const [existingEmployeeIds, setExistingEmployeeIds] = useState<number[]>([]);

  const queryRoleEmployee = useCallback(
    async (page?: number) => {
      if (!selectedRoleId) return;
      setLoading(true);
      try {
        const res = await roleApi.queryEmployee({
          roleId: selectedRoleId,
          keywords,
          pageNum: page ?? pageNum,
          pageSize: PAGE_SIZE,
        });
        if (res.code === 1 && res.data) {
          setTableData(res.data.list || []);
          setTotal(res.data.total || 0);
        }
      } finally {
        setLoading(false);
      }
    },
    [selectedRoleId, keywords, pageNum]
  );

  useEffect(() => {
    if (selectedRoleId) {
      setPageNum(1);
      queryRoleEmployee(1);
    }
  }, [selectedRoleId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryRoleEmployee(1);
  };

  const handleReset = () => {
    setKeywords('');
    setPageNum(1);
    // Need to query with empty keywords
    setTimeout(() => queryRoleEmployee(1), 0);
  };

  const handleRemove = (employeeId: number) => {
    Modal.confirm({
      title: '提示',
      content: '确定要删除该角色成员么？',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      async onOk() {
        await roleApi.removeEmployee(employeeId, selectedRoleId!);
        message.success('移除成功');
        queryRoleEmployee();
      },
    });
  };

  const handleBatchRemove = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的角色成员');
      return;
    }
    Modal.confirm({
      title: '提示',
      content: '确定移除这些角色成员吗？',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      async onOk() {
        await roleApi.batchRemoveEmployee({
          employeeIdList: selectedRowKeys,
          roleId: selectedRoleId!,
        });
        message.success('移除成功');
        setSelectedRowKeys([]);
        queryRoleEmployee();
      },
    });
  };

  const handleAddEmployee = async () => {
    if (!selectedRoleId) return;
    const res = await roleApi.getAllEmployeeByRoleId(selectedRoleId);
    const ids = (res.data || []).map((e) => e.employeeId);
    setExistingEmployeeIds(ids);
    setSelectModalOpen(true);
  };

  const handleSelectEmployee = async (employeeIds: number[]) => {
    if (!selectedRoleId) return;
    await roleApi.batchAddEmployee({
      employeeIdList: employeeIds,
      roleId: selectedRoleId,
    });
    message.success('添加成功');
    setSelectModalOpen(false);
    queryRoleEmployee();
  };

  const columns: ColumnsType<RoleEmployeeVO> = [
    { title: '姓名', dataIndex: 'actualName' },
    { title: '手机号', dataIndex: 'phone' },
    { title: '登录账号', dataIndex: 'loginName' },
    { title: '部门', dataIndex: 'departmentName' },
    {
      title: '状态',
      dataIndex: 'disabledFlag',
      render: (val) => <Tag color={val ? 'error' : 'processing'}>{val ? '禁用' : '启用'}</Tag>,
    },
    {
      title: '操作',
      width: 60,
      render: (_, record) => (
        <a onClick={() => handleRemove(record.employeeId)}>移除</a>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '20px 0' }}>
        <Space>
          <span>关键字：</span>
          <Input
            style={{ width: 250 }}
            value={keywords}
            onChange={(e) => setKeywords(e.target.value)}
            placeholder="姓名/手机号/登录账号"
            onPressEnter={handleSearch}
          />
          {selectedRoleId && <Button type="primary" onClick={handleSearch}>搜索</Button>}
          {selectedRoleId && <Button onClick={handleReset}>重置</Button>}
        </Space>
        <Space>
          {selectedRoleId && <Button type="primary" onClick={handleAddEmployee}>添加员工</Button>}
          {selectedRoleId && (
            <Button type="primary" danger onClick={handleBatchRemove}>批量移除</Button>
          )}
        </Space>
      </div>

      <Table
        rowKey="employeeId"
        columns={columns}
        dataSource={tableData}
        loading={loading}
        size="small"
        scroll={{ y: 400 }}
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => {
            setPageNum(page);
            queryRoleEmployee(page);
          },
        }}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as number[]),
        }}
      />

      <EmployeeTableSelectModal
        open={selectModalOpen}
        selectedEmployeeIds={existingEmployeeIds}
        onCancel={() => setSelectModalOpen(false)}
        onSelect={handleSelectEmployee}
      />
    </div>
  );
};

export default RoleEmployeeList;
