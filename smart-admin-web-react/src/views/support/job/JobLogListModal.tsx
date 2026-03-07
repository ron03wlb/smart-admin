/**
 * Job Log List Modal (Drawer)
 *
 * Corresponds to Vue's support/job/components/job-log-list-modal.vue
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Drawer, Table, Input, Button, Space, DatePicker, Tag } from 'antd';
import { jobApi } from '@/api/support/job-api';
import type { JobLogVO } from '@/api/support/job-api';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

interface Props {
  open: boolean;
  jobId: number;
  onClose: () => void;
}

const JobLogListModal: React.FC<Props> = ({ open, jobId, onClose }) => {
  const [data, setData] = useState<JobLogVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [successFilter, setSuccessFilter] = useState<boolean | undefined>();
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const queryList = useCallback(async (page: number) => {
    if (!jobId) return;
    setLoading(true);
    try {
      const res = await jobApi.queryLog({
        jobId, keywords, successFlag: successFilter,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
        pageNum: page, pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [jobId, keywords, successFilter, dateRange]);

  useEffect(() => {
    if (open) { setPageNum(1); setKeywords(''); setSuccessFilter(undefined); setDateRange(null); queryList(1); }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const columns: ColumnsType<JobLogVO> = [
    { title: '执行人', dataIndex: 'createName', width: 80 },
    { title: '参数', dataIndex: 'param', ellipsis: true, width: 150 },
    { title: '开始时间', dataIndex: 'executeStartTime', width: 170 },
    { title: '结束时间', dataIndex: 'executeEndTime', width: 170 },
    { title: '耗时(ms)', dataIndex: 'executeTimeMillis', width: 90 },
    {
      title: '结果', dataIndex: 'successFlag', width: 80, align: 'center',
      render: (val) => <Tag color={val ? 'success' : 'error'}>{val ? '成功' : '失败'}</Tag>,
    },
    { title: '结果描述', dataIndex: 'executeResult', ellipsis: true },
    { title: 'IP', dataIndex: 'ip', width: 120 },
  ];

  return (
    <Drawer title="执行日志" open={open} onClose={onClose} width={1000}>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 160 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="关键词" allowClear />
        <Button type={successFilter === true ? 'primary' : 'default'} onClick={() => setSuccessFilter(successFilter === true ? undefined : true)}>成功</Button>
        <Button type={successFilter === false ? 'primary' : 'default'} danger onClick={() => setSuccessFilter(successFilter === false ? undefined : false)}>失败</Button>
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Button type="primary" onClick={() => { setPageNum(1); queryList(1); }}>查询</Button>
      </Space>
      <Table rowKey="jobLogId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
    </Drawer>
  );
};

export default JobLogListModal;
