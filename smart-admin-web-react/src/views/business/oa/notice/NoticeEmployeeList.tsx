/**
 * Notice Employee List
 *
 * Corresponds to Vue's business/oa/notice/notice-employee-list.vue (215L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, Tag } from 'antd';
import { noticeApi } from '@/api/business/oa/notice-api';
import type { NoticeEmployeeVO } from '@/api/business/oa/notice-api';
import type { ColumnsType } from 'antd/es/table';

const PAGE_SIZE = 10;

const NoticeEmployeeList: React.FC = () => {
  const [data, setData] = useState<NoticeEmployeeVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await noticeApi.employeeQuery({ keywords, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keywords]);

  useEffect(() => { queryList(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const columns: ColumnsType<NoticeEmployeeVO> = [
    { title: '标题', dataIndex: 'title', ellipsis: true, render: (text, record) => <a onClick={() => window.open(`#/business/oa/notice/employee-detail?noticeId=${record.noticeId}`, '_self')}>{text}</a> },
    { title: '分类', dataIndex: 'noticeTypeName', width: 100 },
    { title: '作者', dataIndex: 'author', width: 100 },
    { title: '发布时间', dataIndex: 'publishTime', width: 170 },
    { title: '状态', dataIndex: 'readFlag', width: 80, align: 'center', render: (val) => <Tag color={val ? 'green' : 'orange'}>{val ? '已读' : '未读'}</Tag> },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }}>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="标题关键词" allowClear />
        <Button type="primary" onClick={() => { setPageNum(1); queryList(1); }}>查询</Button>
      </Space>
      <Table
        rowKey="noticeId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
    </Card>
  );
};

export default NoticeEmployeeList;
