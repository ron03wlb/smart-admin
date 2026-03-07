/**
 * Home Notice Component
 *
 * Displays announcement or notification list.
 * Corresponds to Vue's home-notice.vue
 */
import React, { useState, useEffect } from 'react';
import { Card, List, Badge, Typography, Tooltip, Space } from 'antd';
import { BellOutlined, NotificationOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface NoticeItem {
  id: string;
  title: string;
  publishDate: string;
  readFlag: boolean;
}

interface HomeNoticeProps {
  title: string;
  noticeTypeId: number; // 1=announcement, 2=notification
}

// Sample data (to be replaced with real API)
const SAMPLE_ANNOUNCEMENTS: NoticeItem[] = [
  { id: '1', title: 'SmartAdmin React v1.0 正式發佈', publishDate: '2026-03-06', readFlag: false },
  { id: '2', title: '系統將於週末進行維護升級', publishDate: '2026-03-05', readFlag: true },
  { id: '3', title: '新增 ECharts 圖表組件支援', publishDate: '2026-03-04', readFlag: false },
  { id: '4', title: '員工管理模組功能優化完成', publishDate: '2026-03-03', readFlag: true },
  { id: '5', title: '權限系統架構設計文檔更新', publishDate: '2026-03-02', readFlag: true },
  { id: '6', title: '前端技術棧遷移計畫公告', publishDate: '2026-03-01', readFlag: false },
];

const SAMPLE_NOTIFICATIONS: NoticeItem[] = [
  { id: '1', title: '你有一條新的待辦事項需要處理', publishDate: '2026-03-06', readFlag: false },
  { id: '2', title: '代碼審查請求：員工列表頁面重構', publishDate: '2026-03-05', readFlag: false },
  { id: '3', title: '部署通知：測試環境已更新至最新版本', publishDate: '2026-03-04', readFlag: true },
  { id: '4', title: '會議提醒：Sprint 回顧會議', publishDate: '2026-03-03', readFlag: true },
  { id: '5', title: '安全掃描報告已生成', publishDate: '2026-03-02', readFlag: true },
  { id: '6', title: '資料庫備份任務執行完成', publishDate: '2026-03-01', readFlag: true },
];

const HomeNotice: React.FC<HomeNoticeProps> = ({ title, noticeTypeId }) => {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<NoticeItem[]>([]);

  useEffect(() => {
    // Simulate API call with sample data
    const timer = setTimeout(() => {
      setData(noticeTypeId === 1 ? SAMPLE_ANNOUNCEMENTS : SAMPLE_NOTIFICATIONS);
      setLoading(false);
    }, 300);
    return () => clearTimeout(timer);
  }, [noticeTypeId]);

  const icon = noticeTypeId === 1
    ? <NotificationOutlined style={{ color: '#1890ff' }} />
    : <BellOutlined style={{ color: '#fa8c16' }} />;

  return (
    <Card
      title={
        <Space size={8}>
          {icon}
          <span>{title}</span>
        </Space>
      }
      size="small"
      style={{ height: '100%' }}
      extra={<a style={{ fontSize: 12 }}>更多</a>}
    >
      <List
        loading={loading}
        size="small"
        dataSource={data}
        style={{ height: 150, overflowY: 'auto' }}
        renderItem={(item) => (
          <List.Item
            style={{ padding: '4px 0', cursor: 'pointer' }}
          >
            <Space size={8} style={{ width: '100%', justifyContent: 'space-between' }}>
              <Space size={4}>
                <Badge status={item.readFlag ? 'default' : 'error'} />
                <Tooltip title={item.title}>
                  <Text
                    ellipsis
                    style={{ maxWidth: 200, fontSize: 13 }}
                  >
                    {item.title}
                  </Text>
                </Tooltip>
              </Space>
              <Text type="secondary" style={{ fontSize: 12, flexShrink: 0 }}>
                {item.publishDate}
              </Text>
            </Space>
          </List.Item>
        )}
      />
    </Card>
  );
};

export default HomeNotice;
