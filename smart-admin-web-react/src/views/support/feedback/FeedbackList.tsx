/**
 * Feedback List
 *
 * Corresponds to Vue's support/feedback/feedback-list.vue (170L)
 * Displays user feedback with keyword search and date range filter.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker } from 'antd';
import { feedbackApi } from '@/api/support/feedback-api';
import type { FeedbackVO } from '@/api/support/feedback-api';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const FeedbackList: React.FC = () => {
  const [data, setData] = useState<FeedbackVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [searchWord, setSearchWord] = useState('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await feedbackApi.queryFeedback({
        searchWord,
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
  }, [searchWord, dateRange]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryList(1);
  };

  const handleReset = () => {
    setSearchWord('');
    setDateRange(null);
    setPageNum(1);
    queryList(1);
  };

  const columns: ColumnsType<FeedbackVO> = [
    { title: '反馈内容', dataIndex: 'feedbackContent', ellipsis: true },
    { title: '提交人', dataIndex: 'userName', width: 120 },
    { title: '时间', dataIndex: 'createTime', width: 180 },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 200 }}
          value={searchWord}
          onChange={(e) => setSearchWord(e.target.value)}
          placeholder="反馈内容/提交人"
          allowClear
        />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      <Table
        rowKey="feedbackId"
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

export default FeedbackList;
