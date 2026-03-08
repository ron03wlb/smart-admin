/**
 * Notice Employee Detail
 *
 * Corresponds to Vue's business/oa/notice/notice-employee-detail.vue (145L)
 */
import React, { useEffect, useState } from 'react';
import { Card, Typography, Divider, Space, Spin } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { noticeApi } from '@/api/business/oa/notice-api';
import { sanitizeHtml } from '@/utils/sanitize';
import type { NoticeVO } from '@/api/business/oa/notice-api';

const { Title, Text } = Typography;

const NoticeEmployeeDetail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const noticeId = Number(searchParams.get('noticeId'));
  const [notice, setNotice] = useState<NoticeVO | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (noticeId) {
      setLoading(true);
      noticeApi.employeeView(noticeId).then((res) => {
        if (res.code === 1 && res.data) setNotice(res.data);
      }).finally(() => setLoading(false));
    }
  }, [noticeId]);

  if (loading || !notice) return <Card><Spin /></Card>;

  return (
    <Card>
      <Title level={3} style={{ textAlign: 'center' }}>{notice.title}</Title>
      <div style={{ textAlign: 'center', marginBottom: 16 }}>
        <Space split={<Divider type="vertical" />}>
          <Text type="secondary">作者：{notice.author}</Text>
          <Text type="secondary">来源：{notice.source || '-'}</Text>
          <Text type="secondary">发布时间：{notice.publishTime}</Text>
          <Text type="secondary">浏览量：{notice.pageViewCount}</Text>
        </Space>
      </div>
      <Divider />
      <div dangerouslySetInnerHTML={{ __html: sanitizeHtml(notice.contentHtml || '') }} style={{ lineHeight: 1.8, minHeight: 300 }} />
    </Card>
  );
};

export default NoticeEmployeeDetail;
