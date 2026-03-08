/**
 * Login Log List (Admin View)
 *
 * Corresponds to Vue's support/login-log/login-log-list.vue (239L)
 * Displays all user login logs with search and date range filter.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker, Tag } from 'antd';
import { loginLogApi } from '@/api/support/login-log-api';
import type { LoginLogVO } from '@/api/support/login-log-api';
import type { ColumnsType } from 'antd/es/table';
import type { DateRangeValue } from '@/types/date-range.types';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const loginResultMap: Record<number, { color: string; text: string }> = {
  1: { color: 'success', text: '登录成功' },
  2: { color: 'error', text: '登录失败' },
  3: { color: 'processing', text: '退出登录' },
};

const LoginLogList: React.FC = () => {
  const [data, setData] = useState<LoginLogVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [userName, setUserName] = useState('');
  const [ip, setIp] = useState('');
  const [dateRange, setDateRange] = useState<DateRangeValue>(null);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await loginLogApi.queryList({
        userName,
        ip,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
        pageNum: page,
        pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [userName, ip, dateRange]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryList(1);
  };

  const handleReset = () => {
    setUserName('');
    setIp('');
    setDateRange(null);
    setPageNum(1);
    queryList(1);
  };

  const columns: ColumnsType<LoginLogVO> = [
    { title: '用户', dataIndex: 'userName', width: 120 },
    { title: 'IP', dataIndex: 'loginIp', width: 150 },
    { title: 'IP归属', dataIndex: 'loginIpRegion', width: 120 },
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
    { title: '时间', dataIndex: 'createTime', width: 180 },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 160 }}
          value={userName}
          onChange={(e) => setUserName(e.target.value)}
          placeholder="用户名"
          allowClear
        />
        <Input
          style={{ width: 160 }}
          value={ip}
          onChange={(e) => setIp(e.target.value)}
          placeholder="IP"
          allowClear
        />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
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
          onChange: (page) => { setPageNum(page); queryList(page); },
        }}
      />
    </Card>
  );
};

export default LoginLogList;
