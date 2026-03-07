/**
 * Notice View Record List
 *
 * Corresponds to Vue's business/oa/notice/components/notice-view-record-list.vue (164L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table } from 'antd';
import { noticeApi } from '@/api/business/oa/notice-api';
import type { NoticeViewRecordVO } from '@/api/business/oa/notice-api';
import type { ColumnsType } from 'antd/es/table';

const PAGE_SIZE = 10;

interface Props {
  noticeId: number;
}

const NoticeViewRecordList: React.FC<Props> = ({ noticeId }) => {
  const [data, setData] = useState<NoticeViewRecordVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);

  const queryList = useCallback(async (page: number) => {
    if (!noticeId) return;
    setLoading(true);
    try {
      const res = await noticeApi.queryViewRecord({ noticeId, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [noticeId]);

  useEffect(() => { queryList(1); }, [noticeId]); // eslint-disable-line react-hooks/exhaustive-deps

  const columns: ColumnsType<NoticeViewRecordVO> = [
    { title: '员工', dataIndex: 'employeeName', width: 120 },
    { title: '部门', dataIndex: 'departmentName', width: 150 },
    { title: '浏览次数', dataIndex: 'pageViewCount', width: 80, align: 'center' },
    { title: '首次IP', dataIndex: 'firstIp', width: 130 },
    { title: '首次时间', dataIndex: 'firstTime', width: 170 },
    { title: '最近IP', dataIndex: 'lastIp', width: 130 },
    { title: '最近时间', dataIndex: 'lastTime', width: 170 },
  ];

  return (
    <Table
      rowKey="employeeId" columns={columns} dataSource={data} loading={loading} size="small"
      pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
    />
  );
};

export default NoticeViewRecordList;
