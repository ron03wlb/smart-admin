/**
 * Login Fail List
 *
 * Corresponds to Vue's support/login-fail/login-fail-list.vue (251L)
 * Displays failed login attempts with lock status and batch unlock.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker, Tag, Modal, message, Radio } from 'antd';
import { loginFailApi } from '@/api/support/login-fail-api';
import type { LoginFailVO } from '@/api/support/login-fail-api';
import type { ColumnsType } from 'antd/es/table';
import type { DateRangeValue } from '@/types/date-range.types';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const LoginFailList: React.FC = () => {
  const [data, setData] = useState<LoginFailVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loginName, setLoginName] = useState('');
  const [lockFlag, setLockFlag] = useState<boolean | undefined>();
  const [dateRange, setDateRange] = useState<DateRangeValue>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await loginFailApi.queryPage({
        loginName,
        lockFlag,
        loginLockBeginTimeBegin: dateRange?.[0]?.format('YYYY-MM-DD'),
        loginLockBeginTimeEnd: dateRange?.[1]?.format('YYYY-MM-DD'),
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
  }, [loginName, lockFlag, dateRange]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryList(1);
  };

  const handleReset = () => {
    setLoginName('');
    setLockFlag(undefined);
    setDateRange(null);
    setPageNum(1);
    queryList(1);
  };

  const handleBatchUnlock = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要解锁的记录');
      return;
    }
    Modal.confirm({
      title: '提示',
      content: `确定要解锁选中的 ${selectedRowKeys.length} 条记录么？`,
      okText: '确定',
      cancelText: '取消',
      async onOk() {
        await loginFailApi.batchDelete(selectedRowKeys);
        message.success('解锁成功');
        setSelectedRowKeys([]);
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<LoginFailVO> = [
    { title: '登录名', dataIndex: 'loginName', width: 150 },
    { title: '失败次数', dataIndex: 'loginFailCount', width: 100, align: 'center' },
    {
      title: '锁定状态',
      dataIndex: 'lockFlag',
      width: 100,
      align: 'center',
      render: (val) => <Tag color={val ? 'error' : 'success'}>{val ? '已锁定' : '正常'}</Tag>,
    },
    { title: '锁定时间', dataIndex: 'loginLockBeginTime', width: 180 },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 160 }}
          value={loginName}
          onChange={(e) => setLoginName(e.target.value)}
          placeholder="登录名"
          allowClear
        />
        <Radio.Group value={lockFlag} onChange={(e) => setLockFlag(e.target.value)}>
          <Radio.Button value={undefined}>全部</Radio.Button>
          <Radio.Button value={true}>已锁定</Radio.Button>
          <Radio.Button value={false}>未锁定</Radio.Button>
        </Radio.Group>
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button danger onClick={handleBatchUnlock} disabled={selectedRowKeys.length === 0}>
          批量解锁
        </Button>
      </Space>

      <Table
        rowKey="loginFailId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as number[]),
        }}
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

export default LoginFailList;
