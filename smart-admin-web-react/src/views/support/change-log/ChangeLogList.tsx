/**
 * Change Log List
 *
 * Corresponds to Vue's support/change-log/change-log-list.vue (326L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker, Tag, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { changeLogApi } from '@/api/support/change-log-api';
import type { ChangeLogVO } from '@/api/support/change-log-api';
import type { ColumnsType } from 'antd/es/table';
import type { DateRangeValue } from '@/types/date-range.types';
import ChangeLogForm from './ChangeLogForm';
import ChangeLogModal from './ChangeLogModal';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const typeMap: Record<number, { color: string; text: string }> = {
  1: { color: 'blue', text: '重大更新' },
  2: { color: 'green', text: '功能更新' },
  3: { color: 'orange', text: 'Bug修复' },
};

const ChangeLogList: React.FC = () => {
  const [data, setData] = useState<ChangeLogVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [type, setType] = useState<number | undefined>();
  const [dateRange, setDateRange] = useState<DateRangeValue>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const [formOpen, setFormOpen] = useState(false);
  const [currentLog, setCurrentLog] = useState<ChangeLogVO | undefined>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLog, setDetailLog] = useState<ChangeLogVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await changeLogApi.queryPage({
        keywords, type,
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
  }, [keywords, type, dateRange]);

  useEffect(() => { queryList(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleReset = () => { setKeywords(''); setType(undefined); setDateRange(null); setPageNum(1); queryList(1); };

  const handleAdd = () => { setCurrentLog(undefined); setFormOpen(true); };
  const handleEdit = (record: ChangeLogVO) => { setCurrentLog(record); setFormOpen(true); };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该更新日志么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await changeLogApi.delete(id);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) { message.warning('请选择要删除的记录'); return; }
    Modal.confirm({
      title: '提示', content: `确定要批量删除选中的 ${selectedRowKeys.length} 条记录么？`,
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await changeLogApi.batchDelete(selectedRowKeys as number[]);
        message.success('删除成功');
        setSelectedRowKeys([]);
        queryList(pageNum);
      },
    });
  };

  const handleViewDetail = (record: ChangeLogVO) => { setDetailLog(record); setDetailOpen(true); };

  const columns: ColumnsType<ChangeLogVO> = [
    { title: '版本', dataIndex: 'updateVersion', width: 100, render: (text, record) => <a onClick={() => handleViewDetail(record)}>{text}</a> },
    { title: '更新类型', dataIndex: 'type', width: 100, render: (val) => { const info = typeMap[val]; return info ? <Tag color={info.color}>{info.text}</Tag> : val; } },
    { title: '发布人', dataIndex: 'publishAuthor', width: 100 },
    { title: '发布日期', dataIndex: 'publicDate', width: 120 },
    { title: '更新内容', dataIndex: 'content', ellipsis: true },
    { title: '跳转链接', dataIndex: 'link', width: 120, ellipsis: true, render: (val) => val ? <a href={val} target="_blank" rel="noopener noreferrer">查看</a> : '-' },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作', width: 140, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.changeLogId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="关键词" allowClear />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
        <Button danger onClick={handleBatchDelete}>批量删除</Button>
      </Space>
      <Table
        rowKey="changeLogId" columns={columns} dataSource={data} loading={loading} size="small"
        rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <ChangeLogForm open={formOpen} changeLog={currentLog} onCancel={() => setFormOpen(false)} onSuccess={() => { setFormOpen(false); queryList(pageNum); }} />
      <ChangeLogModal open={detailOpen} changeLog={detailLog} onClose={() => setDetailOpen(false)} />
    </Card>
  );
};

export default ChangeLogList;
