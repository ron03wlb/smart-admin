/**
 * Job List
 *
 * Corresponds to Vue's support/job/job-list.vue (379L)
 * Scheduled task management with tabs for active/deleted jobs.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Tabs, Input, Button, Space, Select, Switch, Tag, Modal, message, Tooltip } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { jobApi } from '@/api/support/job-api';
import type { JobVO } from '@/api/support/job-api';
import type { ColumnsType } from 'antd/es/table';
import JobFormModal from './JobFormModal';
import JobLogListModal from './JobLogListModal';
import DeletedJobList from './DeletedJobList';

const PAGE_SIZE = 10;

const triggerTypeMap: Record<string, { color: string; text: string }> = {
  CRON: { color: 'blue', text: 'CRON' },
  FIXED_DELAY: { color: 'green', text: '固定延迟' },
};

const JobList: React.FC = () => {
  const [data, setData] = useState<JobVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [triggerType, setTriggerType] = useState<string | undefined>();
  const [enabledFlag, setEnabledFlag] = useState<boolean | undefined>();
  const [formOpen, setFormOpen] = useState(false);
  const [currentJob, setCurrentJob] = useState<JobVO | undefined>();
  const [logOpen, setLogOpen] = useState(false);
  const [logJobId, setLogJobId] = useState<number>(0);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await jobApi.query({
        keywords, triggerType, enabledFlag, deletedFlag: false,
        pageNum: page, pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keywords, triggerType, enabledFlag]);

  useEffect(() => { queryList(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleReset = () => { setKeywords(''); setTriggerType(undefined); setEnabledFlag(undefined); setPageNum(1); queryList(1); };

  const handleAdd = () => { setCurrentJob(undefined); setFormOpen(true); };
  const handleEdit = (record: JobVO) => { setCurrentJob(record); setFormOpen(true); };

  const handleToggleEnabled = async (record: JobVO) => {
    await jobApi.updateEnabled({ jobId: record.jobId, enabledFlag: !record.enabledFlag });
    message.success('操作成功');
    queryList(pageNum);
  };

  const handleExecute = (record: JobVO) => {
    Modal.confirm({
      title: '执行任务',
      content: `确定要立即执行任务 [${record.jobName}] 么？`,
      okText: '确定', cancelText: '取消',
      async onOk() {
        await jobApi.execute({ jobId: record.jobId, param: record.param });
        message.success('执行成功');
      },
    });
  };

  const handleDelete = (jobId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该任务么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await jobApi.delete(jobId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const handleViewLog = (jobId: number) => { setLogJobId(jobId); setLogOpen(true); };

  const columns: ColumnsType<JobVO> = [
    { title: '任务名称', dataIndex: 'jobName', width: 160 },
    { title: '任务类', dataIndex: 'jobClass', ellipsis: true, width: 200, render: (text) => <Tooltip title={text}><span>{text}</span></Tooltip> },
    { title: '触发类型', dataIndex: 'triggerType', width: 100, render: (val) => { const info = triggerTypeMap[val]; return info ? <Tag color={info.color}>{info.text}</Tag> : val; } },
    { title: '触发配置', dataIndex: 'triggerValue', width: 140 },
    {
      title: '上次执行',
      width: 80,
      align: 'center',
      render: (_, record) => record.lastJobLog ? (
        <Tag color={record.lastJobLog.successFlag ? 'success' : 'error'}>
          {record.lastJobLog.successFlag ? '成功' : '失败'}
        </Tag>
      ) : '-',
    },
    { title: '启用', dataIndex: 'enabledFlag', width: 70, align: 'center', render: (val, record) => <Switch checked={val} size="small" onChange={() => handleToggleEnabled(record)} /> },
    { title: '排序', dataIndex: 'sort', width: 60 },
    {
      title: '操作', width: 220, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a onClick={() => handleExecute(record)}>执行</a>
          <a onClick={() => handleViewLog(record.jobId)}>日志</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.jobId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Tabs items={[
        {
          key: 'active',
          label: '有效任务',
          children: (
            <>
              <Space style={{ marginBottom: 16 }} wrap>
                <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="任务名/任务类" allowClear />
                <Select style={{ width: 120 }} value={triggerType} onChange={setTriggerType} placeholder="触发类型" allowClear
                  options={[{ label: 'CRON', value: 'CRON' }, { label: '固定延迟', value: 'FIXED_DELAY' }]} />
                <Button type="primary" onClick={handleSearch}>查询</Button>
                <Button onClick={handleReset}>重置</Button>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
              </Space>
              <Table rowKey="jobId" columns={columns} dataSource={data} loading={loading} size="small"
                pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
              />
            </>
          ),
        },
        { key: 'deleted', label: '已删除任务', children: <DeletedJobList /> },
      ]} />
      <JobFormModal open={formOpen} job={currentJob} onCancel={() => setFormOpen(false)} onSuccess={() => { setFormOpen(false); queryList(pageNum); }} />
      <JobLogListModal open={logOpen} jobId={logJobId} onClose={() => setLogOpen(false)} />
    </Card>
  );
};

export default JobList;
