/**
 * Heart Beat List
 *
 * Corresponds to Vue's support/heart-beat/heart-beat-list.vue (180L)
 * Displays running Java service heartbeat information.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker, Alert } from 'antd';
import { heartBeatApi } from '@/api/support/heart-beat-api';
import type { HeartBeatVO } from '@/api/support/heart-beat-api';
import type { ColumnsType } from 'antd/es/table';
import type { DateRangeValue } from '@/types/date-range.types';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const HeartBeatList: React.FC = () => {
  const [data, setData] = useState<HeartBeatVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [dateRange, setDateRange] = useState<DateRangeValue>(null);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await heartBeatApi.queryList({
        keywords,
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
  }, [keywords, dateRange]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryList(1);
  };

  const handleReset = () => {
    setKeywords('');
    setDateRange(null);
    setPageNum(1);
    queryList(1);
  };

  const columns: ColumnsType<HeartBeatVO> = [
    { title: '项目路径', dataIndex: 'projectPath', ellipsis: true },
    { title: '服务IP', dataIndex: 'serverIp', width: 150 },
    { title: '进程号', dataIndex: 'processNo', width: 100 },
    { title: '进程启动时间', dataIndex: 'processStartTime', width: 180 },
    { title: '心跳时间', dataIndex: 'heartBeatTime', width: 180 },
  ];

  return (
    <Card>
      <Alert
        type="info"
        showIcon
        message="说明：心跳服务用于监控正在运行的Java服务，定时上报心跳信息"
        style={{ marginBottom: 16 }}
      />
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 200 }}
          value={keywords}
          onChange={(e) => setKeywords(e.target.value)}
          placeholder="项目路径/IP"
          allowClear
        />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      <Table
        rowKey="heartBeatId"
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

export default HeartBeatList;
