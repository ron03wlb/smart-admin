/**
 * Deleted Job List
 *
 * Corresponds to Vue's support/job/components/deleted-job-list.vue (287L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, Input, Button, Space, Tag } from 'antd';
import { jobApi } from '@/api/support/job-api';
import type { JobVO } from '@/api/support/job-api';
import type { ColumnsType } from 'antd/es/table';
import JobLogListModal from './JobLogListModal';

const PAGE_SIZE = 10;

const DeletedJobList: React.FC = () => {
  const [data, setData] = useState<JobVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [logOpen, setLogOpen] = useState(false);
  const [logJobId, setLogJobId] = useState<number>(0);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await jobApi.query({ keywords, deletedFlag: true, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keywords]);

  useEffect(() => { queryList(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const columns: ColumnsType<JobVO> = [
    { title: '任务名称', dataIndex: 'jobName', width: 160 },
    { title: '任务类', dataIndex: 'jobClass', ellipsis: true },
    { title: '触发类型', dataIndex: 'triggerType', width: 100, render: (val) => <Tag>{val}</Tag> },
    { title: '触发配置', dataIndex: 'triggerValue', width: 140 },
    { title: '排序', dataIndex: 'sort', width: 60 },
    {
      title: '操作', width: 80, align: 'center',
      render: (_, record) => <a onClick={() => { setLogJobId(record.jobId); setLogOpen(true); }}>日志</a>,
    },
  ];

  return (
    <>
      <Space style={{ marginBottom: 16 }}>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="任务名/任务类" allowClear />
        <Button type="primary" onClick={() => { setPageNum(1); queryList(1); }}>查询</Button>
      </Space>
      <Table rowKey="jobId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <JobLogListModal open={logOpen} jobId={logJobId} onClose={() => setLogOpen(false)} />
    </>
  );
};

export default DeletedJobList;
