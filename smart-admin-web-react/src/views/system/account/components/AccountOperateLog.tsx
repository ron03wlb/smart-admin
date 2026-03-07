/**
 * Account Operate Log
 *
 * Corresponds to Vue's account/components/operate-log/index.vue (193L)
 * Displays current user's operation history.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, DatePicker, Button, Tag, Space } from 'antd';
import { operateLogApi } from '@/api/support/operate-log-api';
import type { OperateLogVO } from '@/api/support/operate-log-api';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const AccountOperateLog: React.FC = () => {
  const [data, setData] = useState<OperateLogVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [successFilter, setSuccessFilter] = useState<boolean | undefined>();

  const queryLogs = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await operateLogApi.queryListLogin({
        pageNum: page,
        pageSize: PAGE_SIZE,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
        successFlag: successFilter,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [dateRange, successFilter]);

  useEffect(() => {
    queryLogs(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryLogs(1);
  };

  const columns: ColumnsType<OperateLogVO> = [
    { title: '模块', dataIndex: 'module', width: 120 },
    { title: '操作内容', dataIndex: 'content', ellipsis: true },
    { title: 'IP归属', dataIndex: 'ipRegion', width: 120 },
    { title: '时间', dataIndex: 'createTime', width: 180 },
    {
      title: '结果',
      dataIndex: 'successFlag',
      width: 80,
      render: (val) => (
        <Tag color={val ? 'success' : 'error'}>{val ? '成功' : '失败'}</Tag>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Button type={successFilter === true ? 'primary' : 'default'} onClick={() => setSuccessFilter(true)}>成功</Button>
        <Button type={successFilter === false ? 'primary' : 'default'} danger onClick={() => setSuccessFilter(false)}>失败</Button>
        <Button onClick={() => setSuccessFilter(undefined)}>全部</Button>
        <Button type="primary" onClick={handleSearch}>查询</Button>
      </Space>
      <Table
        rowKey="operateLogId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryLogs(page); },
        }}
      />
    </div>
  );
};

export default AccountOperateLog;
