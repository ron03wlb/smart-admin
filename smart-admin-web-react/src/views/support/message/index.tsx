/**
 * Message List Page
 * 消息管理列表頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/message/message-list.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useRef, useEffect } from 'react';
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Row,
  Col,
  Table,
  Pagination,
  Space,
  Modal,
  message,
  DatePicker,
} from 'antd';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import { messageApi, type MessageQueryForm, type MessageVO } from '@/api/support/messageApi';
import { MESSAGE_TYPE_OPTIONS, getMessageTypeLabel } from '@/constants/support/messageConst';
import MessageSendForm, { MessageSendFormRef } from './components/MessageSendForm';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;

/**
 * 是否已讀選項
 */
const READ_FLAG_OPTIONS = [
  { value: true, label: '已讀' },
  { value: false, label: '未讀' },
];

const MessageListPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [tableData, setTableData] = useState<MessageVO[]>([]);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<MessageQueryForm>({
    searchWord: undefined,
    messageType: undefined,
    readFlag: undefined,
    startDate: undefined,
    endDate: undefined,
    pageNum: 1,
    pageSize: 10,
  });

  const sendFormRef = useRef<MessageSendFormRef>(null);

  /**
   * 查詢數據
   */
  const queryData = async () => {
    setLoading(true);
    try {
      const result = await messageApi.queryAdminMessage(queryForm);
      setTableData(result.data.list);
      setTotal(result.data.total);
    } catch (error) {
      console.error('查詢消息列表失敗:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 初始加載
   */
  useEffect(() => {
    queryData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryForm.pageNum, queryForm.pageSize]);

  /**
   * 搜索
   */
  const handleSearch = () => {
    setQueryForm((prev) => ({ ...prev, pageNum: 1 }));
    setTimeout(() => queryData(), 0);
  };

  /**
   * 重置
   */
  const handleReset = () => {
    const pageSize = queryForm.pageSize;
    setQueryForm({
      searchWord: undefined,
      messageType: undefined,
      readFlag: undefined,
      startDate: undefined,
      endDate: undefined,
      pageNum: 1,
      pageSize,
    });
    setTimeout(() => queryData(), 0);
  };

  /**
   * 日期範圍變更
   */
  const handleDateChange = (dates: any, dateStrings: [string, string]) => {
    setQueryForm((prev) => ({
      ...prev,
      startDate: dateStrings[0] || undefined,
      endDate: dateStrings[1] || undefined,
    }));
  };

  /**
   * 分頁變更
   */
  const handlePageChange = (page: number, pageSize: number) => {
    setQueryForm((prev) => ({ ...prev, pageNum: page, pageSize }));
  };

  /**
   * 顯示發送消息表單
   */
  const handleSendMessage = () => {
    sendFormRef.current?.show();
  };

  /**
   * 刪除消息
   */
  const handleDelete = (record: MessageVO) => {
    Modal.confirm({
      title: '提示',
      content: '確定要刪除嗎？',
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await messageApi.deleteMessage(record.messageId);
          message.success('刪除成功');
          queryData();
        } catch (error) {
          console.error('刪除消息失敗:', error);
          message.error('刪除失敗');
        }
      },
    });
  };

  /**
   * 表格列配置
   */
  const columns: ColumnsType<MessageVO> = [
    {
      title: '消息類型',
      dataIndex: 'messageType',
      key: 'messageType',
      width: 100,
      ellipsis: true,
      render: (value: number) => getMessageTypeLabel(value),
    },
    {
      title: '消息標題',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: '消息內容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: '接收人ID',
      dataIndex: 'receiverUserId',
      key: 'receiverUserId',
      width: 100,
      ellipsis: true,
    },
    {
      title: '已讀',
      dataIndex: 'readFlag',
      key: 'readFlag',
      width: 50,
      render: (value: boolean) => (value ? '已讀' : '未讀'),
    },
    {
      title: '已讀時間',
      dataIndex: 'readTime',
      key: 'readTime',
      width: 150,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 150,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 60,
      render: (_, record) => (
        <Button type="link" danger onClick={() => handleDelete(record)}>
          刪除
        </Button>
      ),
    },
  ];

  return (
    <div style={{ padding: '20px' }}>
      {/* 查詢表單 */}
      <Form layout="inline" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col>
            <Form.Item label="關鍵詞">
              <Input
                style={{ width: 150 }}
                placeholder="關鍵詞"
                value={queryForm.searchWord}
                onChange={(e) => setQueryForm((prev) => ({ ...prev, searchWord: e.target.value }))}
              />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item label="類型">
              <Select
                style={{ width: 150 }}
                placeholder="消息類型"
                allowClear
                options={MESSAGE_TYPE_OPTIONS}
                value={queryForm.messageType}
                onChange={(value) => setQueryForm((prev) => ({ ...prev, messageType: value }))}
              />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item label="是否已讀">
              <Select
                style={{ width: 120 }}
                placeholder="是否已讀"
                allowClear
                options={READ_FLAG_OPTIONS}
                value={queryForm.readFlag}
                onChange={(value) => setQueryForm((prev) => ({ ...prev, readFlag: value }))}
              />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item label="創建時間">
              <RangePicker
                style={{ width: 200 }}
                onChange={handleDateChange}
                value={
                  queryForm.startDate && queryForm.endDate
                    ? [dayjs(queryForm.startDate), dayjs(queryForm.endDate)]
                    : undefined
                }
              />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item>
              <Space>
                <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                  查詢
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleReset}>
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Col>
        </Row>
      </Form>

      {/* 表格卡片 */}
      <Card size="small" bordered={false} hoverable>
        {/* 表格操作欄 */}
        <Row justify="space-between" style={{ marginBottom: 16 }}>
          <Col>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleSendMessage}>
              發送消息
            </Button>
          </Col>
        </Row>

        {/* 表格 */}
        <Table
          size="small"
          rowKey="messageId"
          columns={columns}
          dataSource={tableData}
          loading={loading}
          bordered
          pagination={false}
        />

        {/* 分頁 */}
        <Row justify="end" style={{ marginTop: 16 }}>
          <Pagination
            showSizeChanger
            showQuickJumper
            current={queryForm.pageNum}
            pageSize={queryForm.pageSize}
            total={total}
            onChange={handlePageChange}
            showTotal={(total) => `共 ${total} 條`}
            pageSizeOptions={['10', '20', '30', '50']}
          />
        </Row>
      </Card>

      {/* 發送消息表單 */}
      <MessageSendForm ref={sendFormRef} onSuccess={queryData} />
    </div>
  );
};

export default MessageListPage;
