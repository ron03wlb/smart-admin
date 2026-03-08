/**
 * Notice List (Admin)
 *
 * Corresponds to Vue's business/oa/notice/notice-list.vue (358L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, Select, DatePicker, Tag, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { noticeApi, noticeTypeApi } from '@/api/business/oa/notice-api';
import type { NoticeVO, NoticeTypeVO } from '@/api/business/oa/notice-api';
import type { ColumnsType } from 'antd/es/table';
import type { DateRangeValue } from '@/types/date-range.types';
import NoticeFormDrawer from './NoticeFormDrawer';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const NoticeList: React.FC = () => {
  const [data, setData] = useState<NoticeVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [noticeTypeId, setNoticeTypeId] = useState<number | undefined>();
  const [dateRange, setDateRange] = useState<DateRangeValue>(null);
  const [noticeTypes, setNoticeTypes] = useState<NoticeTypeVO[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [currentNotice, setCurrentNotice] = useState<NoticeVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await noticeApi.query({
        keywords, noticeTypeId,
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
  }, [keywords, noticeTypeId, dateRange]);

  const loadNoticeTypes = useCallback(async () => {
    const res = await noticeTypeApi.getAll();
    if (res.code === 1 && res.data) setNoticeTypes(res.data);
  }, []);

  useEffect(() => { queryList(1); loadNoticeTypes(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleReset = () => { setKeywords(''); setNoticeTypeId(undefined); setDateRange(null); setPageNum(1); queryList(1); };

  const handleAdd = () => { setCurrentNotice(undefined); setFormOpen(true); };
  const handleEdit = (record: NoticeVO) => { setCurrentNotice(record); setFormOpen(true); };

  const handleDelete = (noticeId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该通知公告么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await noticeApi.delete(noticeId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<NoticeVO> = [
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '分类', dataIndex: 'noticeTypeName', width: 100 },
    { title: '作者', dataIndex: 'author', width: 100 },
    { title: '文号', dataIndex: 'documentNumber', width: 120 },
    { title: '发布时间', dataIndex: 'publishTime', width: 170 },
    { title: '浏览量', dataIndex: 'pageViewCount', width: 80, align: 'center' },
    { title: '范围', dataIndex: 'allVisibleFlag', width: 80, align: 'center', render: (val) => <Tag color={val ? 'blue' : 'orange'}>{val ? '全部' : '部分'}</Tag> },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作', width: 150, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a onClick={() => window.open(`#/business/oa/notice/detail?noticeId=${record.noticeId}`, '_self')}>详情</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.noticeId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="标题/文号" allowClear />
        <Select style={{ width: 120 }} value={noticeTypeId} onChange={setNoticeTypeId} placeholder="分类" allowClear
          options={noticeTypes.map((t) => ({ label: t.noticeTypeName, value: t.noticeTypeId }))} />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
      </Space>
      <Table
        rowKey="noticeId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <NoticeFormDrawer open={formOpen} notice={currentNotice} noticeTypes={noticeTypes} onClose={() => setFormOpen(false)} onSuccess={() => { setFormOpen(false); queryList(pageNum); }} />
    </Card>
  );
};

export default NoticeList;
