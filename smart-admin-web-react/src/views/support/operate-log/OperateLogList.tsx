/**
 * Operate Log List (Admin View)
 *
 * Corresponds to Vue's support/operate-log/operate-log-list.vue (250L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker, Tag } from 'antd';
import { operateLogApi } from '@/api/support/operate-log-api';
import type { OperateLogVO } from '@/api/support/operate-log-api';
import type { ColumnsType } from 'antd/es/table';
import OperateLogDetailModal from './OperateLogDetailModal';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const OperateLogList: React.FC = () => {
  const [data, setData] = useState<OperateLogVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [userName, setUserName] = useState('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [successFilter, setSuccessFilter] = useState<boolean | undefined>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLog, setDetailLog] = useState<OperateLogVO | null>(null);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await operateLogApi.queryList({
        keywords,
        operateUserName: userName,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
        successFlag: successFilter,
        pageNum: page,
        pageSize: PAGE_SIZE,
      } as any);
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keywords, userName, dateRange, successFilter]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryList(1);
  };

  const handleReset = () => {
    setKeywords('');
    setUserName('');
    setDateRange(null);
    setSuccessFilter(undefined);
    setPageNum(1);
    queryList(1);
  };

  const handleViewDetail = async (record: OperateLogVO) => {
    const res = await operateLogApi.detail(record.operateLogId);
    if (res.code === 1 && res.data) {
      setDetailLog(res.data);
      setDetailOpen(true);
    }
  };

  const columns: ColumnsType<OperateLogVO> = [
    { title: '操作人', dataIndex: 'operateUserName', width: 100 },
    { title: '模块', dataIndex: 'module', width: 120 },
    { title: '操作内容', dataIndex: 'content', ellipsis: true },
    { title: 'URL', dataIndex: 'url', ellipsis: true, width: 200 },
    { title: 'IP', dataIndex: 'ip', width: 130 },
    {
      title: '结果',
      dataIndex: 'successFlag',
      width: 80,
      align: 'center',
      render: (val) => <Tag color={val ? 'success' : 'error'}>{val ? '成功' : '失败'}</Tag>,
    },
    { title: '时间', dataIndex: 'createTime', width: 180 },
    {
      title: '操作',
      width: 60,
      align: 'center',
      render: (_, record) => <a onClick={() => handleViewDetail(record)}>详情</a>,
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 160 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="模块/操作内容" allowClear />
        <Input style={{ width: 120 }} value={userName} onChange={(e) => setUserName(e.target.value)} placeholder="操作人" allowClear />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Button type={successFilter === true ? 'primary' : 'default'} onClick={() => setSuccessFilter(successFilter === true ? undefined : true)}>成功</Button>
        <Button type={successFilter === false ? 'primary' : 'default'} danger onClick={() => setSuccessFilter(successFilter === false ? undefined : false)}>失败</Button>
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
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
          onChange: (page) => { setPageNum(page); queryList(page); },
        }}
      />

      <OperateLogDetailModal
        open={detailOpen}
        log={detailLog}
        onClose={() => setDetailOpen(false)}
      />
    </Card>
  );
};

export default OperateLogList;
