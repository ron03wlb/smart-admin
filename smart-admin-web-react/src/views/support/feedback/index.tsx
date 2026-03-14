/**
 * Feedback List Page
 * 意見反饋列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import React, { useEffect } from 'react';
import { Form, Input, Button, Table, Card, DatePicker, Space, Image, Tag } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { feedbackApi } from '@/api/support/feedbackApi';
import { useTable } from '@/hooks/useTable';
import { formatDateTime } from '@/utils/date';
import { FEEDBACK_PERMISSION, FEEDBACK_TABLE_COLUMNS_WIDTH } from '@/constants/support/feedbackConst';
import { usePrivilege } from '@/hooks/usePrivilege';
import type { FeedbackVO, FeedbackQueryForm } from './types';

const { RangePicker } = DatePicker;

/**
 * 意見反饋列表組件
 */
const FeedbackList: React.FC = () => {
  const [form] = Form.useForm<FeedbackQueryForm>();
  const hasQueryPermission = usePrivilege(FEEDBACK_PERMISSION.QUERY);

  // 使用 useTable Hook
  const {
    data: tableData,
    loading: tableLoading,
    pagination,
    handleTableChange,
    refreshTable,
  } = useTable<FeedbackVO, FeedbackQueryForm>(
    feedbackApi.queryPage,
    form,
  );

  // 初始化時加載數據
  useEffect(() => {
    if (hasQueryPermission) {
      refreshTable();
    }
  }, [hasQueryPermission, refreshTable]);

  // 處理搜索
  const handleSearch = () => {
    refreshTable();
  };

  // 處理重置
  const handleReset = () => {
    form.resetFields();
    refreshTable();
  };

  // 處理日期範圍變化
  const handleDateRangeChange = (_dates: any, dateStrings: [string, string]) => {
    form.setFieldsValue({
      startDate: dateStrings[0],
      endDate: dateStrings[1],
    });
  };

  // 表格列定義
  const columns: ColumnsType<FeedbackVO> = [
    {
      title: '編號',
      dataIndex: 'feedbackId',
      key: 'feedbackId',
      width: FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackId,
    },
    {
      title: '反饋內容',
      dataIndex: 'feedbackContent',
      key: 'feedbackContent',
      width: FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackContent,
      ellipsis: true,
    },
    {
      title: '反饋圖片',
      dataIndex: 'feedbackAttachment',
      key: 'feedbackAttachment',
      width: FEEDBACK_TABLE_COLUMNS_WIDTH.feedbackAttachment,
      render: (attachment: string) => {
        if (!attachment) return '-';
        try {
          const attachments = JSON.parse(attachment);
          if (Array.isArray(attachments) && attachments.length > 0) {
            return (
              <Image.PreviewGroup>
                {attachments.map((item: any, index: number) => (
                  <Image
                    key={index}
                    width={50}
                    height={50}
                    src={item.url}
                    alt={`attachment-${index}`}
                    style={{ marginRight: 4 }}
                  />
                ))}
              </Image.PreviewGroup>
            );
          }
        } catch {
          // JSON parse 失敗，忽略
        }
        return '-';
      },
    },
    {
      title: '反饋人',
      dataIndex: 'userName',
      key: 'userName',
      width: FEEDBACK_TABLE_COLUMNS_WIDTH.userName,
    },
    {
      title: '反饋人類型',
      dataIndex: 'userType',
      key: 'userType',
      width: FEEDBACK_TABLE_COLUMNS_WIDTH.userType,
      render: (userType: number) => {
        // UserTypeEnum: 1-員工, 2-學生, 3-其他
        const typeMap: Record<number, { text: string; color: string }> = {
          1: { text: '員工', color: 'blue' },
          2: { text: '學生', color: 'green' },
          3: { text: '其他', color: 'default' },
        };
        const type = typeMap[userType] || { text: '未知', color: 'default' };
        return <Tag color={type.color}>{type.text}</Tag>;
      },
    },
    {
      title: '反饋時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: FEEDBACK_TABLE_COLUMNS_WIDTH.createTime,
      render: (createTime: string) => formatDateTime(createTime),
    },
  ];

  if (!hasQueryPermission) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          您沒有權限查看意見反饋列表
        </div>
      </Card>
    );
  }

  return (
    <div className="feedback-list">
      {/* 搜索表單 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={form} layout="inline">
          <Form.Item
            name="searchWord"
            label="關鍵字"
            style={{ marginBottom: 16 }}
          >
            <Input
              placeholder="反饋內容/創建人"
              allowClear
              style={{ width: 240 }}
            />
          </Form.Item>

          <Form.Item
            label="創建日期"
            style={{ marginBottom: 16 }}
          >
            <RangePicker
              format="YYYY-MM-DD"
              onChange={handleDateRangeChange}
              style={{ width: 240 }}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 16 }}>
            <Space>
              <Button
                type="primary"
                icon={<SearchOutlined />}
                onClick={handleSearch}
              >
                查詢
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleReset}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {/* 數據表格 */}
      <Card size="small">
        <Table
          rowKey="feedbackId"
          columns={columns}
          dataSource={tableData}
          loading={tableLoading}
          pagination={pagination}
          onChange={handleTableChange}
          size="small"
          bordered
        />
      </Card>
    </div>
  );
};

export default FeedbackList;
