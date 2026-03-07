/**
 * Changelog Card Component
 *
 * Displays recent system changelog entries.
 * Corresponds to Vue's changelog-card.vue
 */
import React from 'react';
import { Card, List, Tag, Typography, Space } from 'antd';
import { HistoryOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface ChangelogItem {
  id: string;
  type: string;
  version: string;
  publishDate: string;
}

// Sample data (to be replaced with real API)
const SAMPLE_CHANGELOG: ChangelogItem[] = [
  { id: '1', type: '新功能', version: 'v1.0.0', publishDate: '2026-03-06' },
  { id: '2', type: '優化', version: 'v0.9.5', publishDate: '2026-03-04' },
  { id: '3', type: '修復', version: 'v0.9.4', publishDate: '2026-03-02' },
  { id: '4', type: '新功能', version: 'v0.9.3', publishDate: '2026-02-28' },
  { id: '5', type: '優化', version: 'v0.9.2', publishDate: '2026-02-25' },
  { id: '6', type: '修復', version: 'v0.9.1', publishDate: '2026-02-20' },
];

const TYPE_COLORS: Record<string, string> = {
  '新功能': 'blue',
  '優化': 'green',
  '修復': 'orange',
};

const ChangelogCard: React.FC = () => {
  return (
    <Card
      title={
        <Space size={8}>
          <HistoryOutlined style={{ color: '#1890ff' }} />
          <span>更新日誌</span>
        </Space>
      }
      size="small"
      extra={<a style={{ fontSize: 12 }}>更多</a>}
      style={{ height: '100%' }}
    >
      <List
        size="small"
        dataSource={SAMPLE_CHANGELOG}
        style={{ height: 150, overflowY: 'auto' }}
        renderItem={(item) => (
          <List.Item style={{ padding: '4px 0' }}>
            <Space size={8} style={{ width: '100%', justifyContent: 'space-between' }}>
              <Space size={8}>
                <Tag color={TYPE_COLORS[item.type] || 'default'} style={{ margin: 0 }}>
                  {item.type}
                </Tag>
                <Text style={{ fontSize: 13 }}>{item.version}</Text>
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

export default ChangelogCard;
