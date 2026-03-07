/**
 * Account Message Inbox
 *
 * Corresponds to Vue's account/components/message/index.vue (171L)
 * Displays user's personal messages with filter and detail view.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, Input, Button, DatePicker, Select, Space, Drawer, Typography } from 'antd';
import { messageApi } from '@/api/support/message-api';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

interface MessageVO {
  messageId: number;
  title: string;
  messageType?: number;
  messageTypeName?: string;
  content: string;
  readFlag: boolean;
  createTime: string;
  sendUserName?: string;
}

const AccountMessage: React.FC = () => {
  const [data, setData] = useState<MessageVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [readFlag, setReadFlag] = useState<boolean | undefined>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailMessage, setDetailMessage] = useState<MessageVO | null>(null);

  const queryMessages = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await messageApi.queryMessage({
        pageNum: page,
        pageSize: PAGE_SIZE,
        keywords,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
        readFlag,
      } as any);
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keywords, dateRange, readFlag]);

  useEffect(() => {
    queryMessages(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryMessages(1);
  };

  const handleViewDetail = async (record: MessageVO) => {
    setDetailMessage(record);
    setDetailOpen(true);
    if (!record.readFlag) {
      await messageApi.read(record.messageId);
      queryMessages(pageNum);
    }
  };

  const columns: ColumnsType<MessageVO> = [
    {
      title: '标题',
      dataIndex: 'title',
      render: (text, record) => (
        <a
          onClick={() => handleViewDetail(record)}
          style={{ fontWeight: record.readFlag ? 'normal' : 'bold' }}
        >
          {record.messageTypeName ? `【${record.messageTypeName}】` : ''}{text}
        </a>
      ),
    },
    {
      title: '状态',
      dataIndex: 'readFlag',
      width: 80,
      render: (val) => (val ? '已读' : '未读'),
    },
    { title: '时间', dataIndex: 'createTime', width: 180 },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 200 }}
          value={keywords}
          onChange={(e) => setKeywords(e.target.value)}
          placeholder="搜索标题/内容"
          allowClear
        />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Select
          style={{ width: 100 }}
          value={readFlag}
          onChange={setReadFlag}
          placeholder="状态"
          allowClear
          options={[
            { label: '已读', value: true },
            { label: '未读', value: false },
          ]}
        />
        <Button type="primary" onClick={handleSearch}>查询</Button>
      </Space>

      <Table
        rowKey="messageId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryMessages(page); },
        }}
      />

      <Drawer
        title={detailMessage?.title}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={800}
      >
        {detailMessage && (
          <div>
            <Typography.Text type="secondary">
              {detailMessage.messageTypeName && `类型：${detailMessage.messageTypeName} | `}
              时间：{detailMessage.createTime}
            </Typography.Text>
            <div style={{ marginTop: 16, whiteSpace: 'pre-wrap' }}>
              {detailMessage.content}
            </div>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default AccountMessage;
