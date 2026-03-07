/**
 * Notice Detail (Admin)
 *
 * Corresponds to Vue's business/oa/notice/notice-detail.vue (144L)
 */
import React, { useEffect, useState } from 'react';
import { Card, Tabs, Descriptions, Tag, Spin, Typography, Divider } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { noticeApi } from '@/api/business/oa/notice-api';
import type { NoticeVO } from '@/api/business/oa/notice-api';
import NoticeViewRecordList from './NoticeViewRecordList';

const NoticeDetail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const noticeId = Number(searchParams.get('noticeId'));
  const [notice, setNotice] = useState<NoticeVO | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (noticeId) {
      setLoading(true);
      noticeApi.getUpdateVO(noticeId).then((res) => {
        if (res.code === 1 && res.data) setNotice(res.data);
      }).finally(() => setLoading(false));
    }
  }, [noticeId]);

  if (loading || !notice) return <Card><Spin /></Card>;

  return (
    <Card>
      <Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
        <Descriptions.Item label="标题" span={2}>{notice.title}</Descriptions.Item>
        <Descriptions.Item label="分类">{notice.noticeTypeName}</Descriptions.Item>
        <Descriptions.Item label="作者">{notice.author}</Descriptions.Item>
        <Descriptions.Item label="来源">{notice.source || '-'}</Descriptions.Item>
        <Descriptions.Item label="文号">{notice.documentNumber || '-'}</Descriptions.Item>
        <Descriptions.Item label="可见范围"><Tag color={notice.allVisibleFlag ? 'blue' : 'orange'}>{notice.allVisibleFlag ? '全部可见' : '部分可见'}</Tag></Descriptions.Item>
        <Descriptions.Item label="发布时间">{notice.publishTime || '-'}</Descriptions.Item>
      </Descriptions>

      <Tabs items={[
        {
          key: 'content',
          label: '公告内容',
          children: (
            <>
              <Typography.Title level={4} style={{ textAlign: 'center' }}>{notice.title}</Typography.Title>
              <Divider />
              <div dangerouslySetInnerHTML={{ __html: notice.contentHtml || '' }} style={{ lineHeight: 1.8 }} />
            </>
          ),
        },
        { key: 'records', label: '查看记录', children: <NoticeViewRecordList noticeId={noticeId} /> },
      ]} />
    </Card>
  );
};

export default NoticeDetail;
