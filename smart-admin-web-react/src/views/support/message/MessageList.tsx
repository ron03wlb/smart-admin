/**
 * Message List (Admin View)
 *
 * Corresponds to Vue's support/message/message-list.vue (240L)
 * Admin view for managing and sending messages.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker, Modal, message } from 'antd';
import { messageApi } from '@/api/support/message-api';
import type { MessageVO } from '@/types/message.types';
import type { ColumnsType } from 'antd/es/table';
import MessageSendForm from './MessageSendForm';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

const MessageList: React.FC = () => {
  const [data, setData] = useState<MessageVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [sendFormOpen, setSendFormOpen] = useState(false);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await messageApi.query({
        keywords,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
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
  }, [keywords, dateRange]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleReset = () => { setKeywords(''); setDateRange(null); setPageNum(1); queryList(1); };

  const handleDelete = (messageId: number) => {
    Modal.confirm({
      title: '提示',
      content: '确定要删除该消息么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await messageApi.delete(messageId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<MessageVO> = [
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '类型', dataIndex: 'messageTypeName', width: 100 },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '已读', dataIndex: 'readFlag', width: 60, render: (val) => val ? '是' : '否' },
    { title: '时间', dataIndex: 'createTime', width: 180 },
    {
      title: '操作',
      width: 60,
      align: 'center',
      render: (_, record) => (
        <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.messageId)}>删除</a>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="标题/内容" allowClear />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" onClick={() => setSendFormOpen(true)}>发送消息</Button>
      </Space>

      <Table
        rowKey="messageId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={{
          current: pageNum, pageSize: PAGE_SIZE, total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryList(page); },
        }}
      />

      <MessageSendForm
        open={sendFormOpen}
        onCancel={() => setSendFormOpen(false)}
        onSuccess={() => { setSendFormOpen(false); queryList(pageNum); }}
      />
    </Card>
  );
};

export default MessageList;
