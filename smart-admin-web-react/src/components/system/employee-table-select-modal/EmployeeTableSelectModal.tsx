/**
 * Employee Table Select Modal
 *
 * Corresponds to Vue's components/system/employee-table-select-modal/index.vue
 * Reusable modal for selecting employees with pagination, search and checkbox selection.
 * Already-selected employees are shown with disabled checkboxes.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Modal, Table, Input, Select, Button, Tag, Space, message } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { employeeApi } from '@/api/system/employee-api';
import DepartmentTreeSelect from '@/components/system/department-tree-select/DepartmentTreeSelect';
import type { ColumnsType } from 'antd/es/table';

interface EmployeeTableSelectModalProps {
  open: boolean;
  /** IDs of employees already assigned (shown with disabled checkboxes) */
  selectedEmployeeIds: number[];
  onCancel: () => void;
  /** Called with newly selected employee IDs (excludes already-assigned ones) */
  onSelect: (employeeIds: number[]) => void;
}

interface EmployeeRow {
  employeeId: number;
  actualName?: string;
  employeeName?: string;
  phone?: string;
  loginName?: string;
  departmentName?: string;
  disabledFlag?: boolean;
  gender?: number;
}

const PAGE_SIZE = 10;

const columns: ColumnsType<EmployeeRow> = [
  { title: '姓名', dataIndex: 'actualName', render: (text, record) => text || record.employeeName },
  { title: '手机号', dataIndex: 'phone' },
  { title: '登录账号', dataIndex: 'loginName' },
  {
    title: '状态',
    dataIndex: 'disabledFlag',
    render: (val) => <Tag color={val ? 'error' : 'processing'}>{val ? '禁用' : '启用'}</Tag>,
  },
];

const EmployeeTableSelectModal: React.FC<EmployeeTableSelectModalProps> = ({
  open,
  selectedEmployeeIds,
  onCancel,
  onSelect,
}) => {
  const [tableData, setTableData] = useState<EmployeeRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);

  const [keyword, setKeyword] = useState('');
  const [departmentId, setDepartmentId] = useState<number | undefined>();
  const [disabledFlag, setDisabledFlag] = useState<number | undefined>();
  const [pageNum, setPageNum] = useState(1);

  const queryEmployee = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await employeeApi.query({
        keyword,
        departmentId,
        pageNum: page,
        pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setTableData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keyword, departmentId]);

  useEffect(() => {
    if (open) {
      setSelectedRowKeys([...selectedEmployeeIds]);
      setKeyword('');
      setDepartmentId(undefined);
      setDisabledFlag(undefined);
      setPageNum(1);
      queryEmployee(1);
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryEmployee(1);
  };

  const handleReset = () => {
    setKeyword('');
    setDepartmentId(undefined);
    setDisabledFlag(undefined);
    setPageNum(1);
    queryEmployee(1);
  };

  const handlePageChange = (page: number) => {
    setPageNum(page);
    queryEmployee(page);
  };

  const handleOk = () => {
    const newIds = selectedRowKeys.filter((id) => !selectedEmployeeIds.includes(id));
    if (newIds.length === 0) {
      message.warning('请选择人员');
      return;
    }
    onSelect(newIds);
  };

  return (
    <Modal
      title="选择人员"
      open={open}
      width={900}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnClose
    >
      <Space style={{ marginBottom: 16 }} wrap>
        <span>关键字：</span>
        <Input
          style={{ width: 150 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="关键字"
        />
        <span>部门：</span>
        <DepartmentTreeSelect
          style={{ width: 200 }}
          value={departmentId as any}
          onChange={(val: any) => setDepartmentId(val ?? undefined)}
        />
        <span>状态：</span>
        <Select
          style={{ width: 120 }}
          value={disabledFlag}
          onChange={setDisabledFlag}
          placeholder="请选择状态"
          allowClear
          options={[
            { label: '启用', value: 0 },
            { label: '禁用', value: 1 },
          ]}
        />
        <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>查询</Button>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>

      <Table
        rowKey="employeeId"
        columns={columns}
        dataSource={tableData}
        loading={loading}
        size="small"
        scroll={{ y: 300 }}
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showSizeChanger: false,
          showQuickJumper: true,
          showTotal: (t) => `共${t}条`,
          onChange: handlePageChange,
        }}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as number[]),
          getCheckboxProps: (record) => ({
            disabled: selectedEmployeeIds.includes(record.employeeId),
          }),
        }}
      />
    </Modal>
  );
};

export default EmployeeTableSelectModal;
