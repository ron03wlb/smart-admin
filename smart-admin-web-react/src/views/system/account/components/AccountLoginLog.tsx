/**
 * Account Login Log
 *
 * Corresponds to Vue's account/components/login-log/index.vue (192L)
 * Displays current user's login history.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, DatePicker, Button, Tag, Space } from 'antd';
import { loginLogApi } from '@/api/support/login-log-api';
import type { LoginLogVO } from '@/api/support/login-log-api';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const loginResultMap: Record<number, { color: string; text: string }> = {
  1: { color: 'success', text: '登录成功' },
  2: { color: 'error', text: '登录失败' },
  3: { color: 'processing', text: '退出登录' },
};

const AccountLoginLog: React.FC = () => {
  const [data, setData] = useState<LoginLogVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const queryLogs = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await loginLogApi.queryListLogin({
        pageNum: page,
        pageSize: PAGE_SIZE,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  useEffect(() => {
    queryLogs(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryLogs(1);
  };

  const columns: ColumnsType<LoginLogVO> = [
    { title: '时间', dataIndex: 'createTime', width: 180 },
    { title: 'IP', dataIndex: 'loginIp', width: 150 },
    { title: 'IP归属', dataIndex: 'loginIpRegion', ellipsis: true },
    { title: '设备', dataIndex: 'userAgent', ellipsis: true },
    {
      title: '结果',
      dataIndex: 'loginResult',
      width: 100,
      render: (val) => {
        const info = loginResultMap[val];
        return info ? <Tag color={info.color}>{info.text}</Tag> : val;
      },
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
      </Space>
      <Table
        rowKey="loginLogId"
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

export default AccountLoginLog;
