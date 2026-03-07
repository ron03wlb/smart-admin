/**
 * Message Receiver Modal
 *
 * Corresponds to Vue's support/message/components/message-receiver-modal.vue
 * Displays employee list for selecting message receivers.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Modal, Table, Input, Button, Space } from 'antd';
import { employeeApi } from '@/api/system/employee-api';
import type { ColumnsType } from 'antd/es/table';

interface EmployeeItem {
  employeeId: number;
  actualName: string;
  phone?: string;
  loginName?: string;
  departmentName?: string;
}

interface Props {
  open: boolean;
  selectedIds: number[];
  onCancel: () => void;
  onSelect: (employees: { employeeId: number; actualName: string }[]) => void;
}

const PAGE_SIZE = 10;

const MessageReceiverModal: React.FC<Props> = ({ open, selectedIds, onCancel, onSelect }) => {
  const [data, setData] = useState<EmployeeItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await employeeApi.query({
        keywords,
        pageNum: page,
        pageSize: PAGE_SIZE,
      } as any);
      if (res.code === 1 && res.data) {
        setData((res.data.list || []) as unknown as EmployeeItem[]);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keywords]);

  useEffect(() => {
    if (open) {
      setSelectedRowKeys([...selectedIds]);
      queryList(1);
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleOk = () => {
    const selected = data
      .filter((d) => selectedRowKeys.includes(d.employeeId))
      .map((d) => ({ employeeId: d.employeeId, actualName: d.actualName }));
    onSelect(selected);
  };

  const columns: ColumnsType<EmployeeItem> = [
    { title: '姓名', dataIndex: 'actualName', width: 120 },
    { title: '手机号', dataIndex: 'phone', width: 140 },
    { title: '部门', dataIndex: 'departmentName', ellipsis: true },
  ];

  return (
    <Modal title="选择接收人" open={open} onOk={handleOk} onCancel={onCancel} width={600} destroyOnClose>
      <Space style={{ marginBottom: 12 }}>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="姓名/手机号" allowClear />
        <Button type="primary" onClick={() => { setPageNum(1); queryList(1); }}>搜索</Button>
      </Space>
      <Table
        rowKey="employeeId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as number[]),
        }}
        pagination={{
          current: pageNum, pageSize: PAGE_SIZE, total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryList(page); },
        }}
      />
    </Modal>
  );
};

export default MessageReceiverModal;
