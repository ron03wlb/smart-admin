/**
 * Header Message Component
 *
 * Corresponds to Vue's header-message.vue (262 lines)
 * Shows unread message count badge and message popover.
 */
import React, { useState, useEffect, useCallback } from 'react';
import { Badge, Popover, List, Button, Empty } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '@/api/support/message-api';
import type { MessageVO } from '@/types/message.types';

function timeago(dateStr: string): string {
  const now = Date.now();
  const date = new Date(dateStr).getTime();
  const diff = now - date;
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (diff < minute) return '刚刚';
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`;
  if (diff < day) return `${Math.floor(diff / hour)}小时前`;
  return dateStr.slice(0, 10);
}

const HeaderMessage: React.FC = () => {
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);
  const [messages, setMessages] = useState<MessageVO[]>([]);
  const [open, setOpen] = useState(false);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const res = await messageApi.getUnreadCount();
      if (res.code === 1) {
        setUnreadCount(res.data ?? 0);
      }
    } catch {
      // ignore
    }
  }, []);

  const fetchMessages = useCallback(async () => {
    try {
      const res = await messageApi.queryMessage({
        pageNum: 1,
        pageSize: 3,
        readFlag: false,
      });
      if (res.code === 1) {
        setMessages(res.data?.list ?? []);
      }
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    fetchUnreadCount();
  }, [fetchUnreadCount]);

  const handleOpenChange = (visible: boolean) => {
    setOpen(visible);
    if (visible) {
      fetchMessages();
    }
  };

  const content = (
    <div style={{ width: 300 }}>
      {messages.length > 0 ? (
        <List
          size="small"
          dataSource={messages}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={item.title}
                description={timeago(item.createTime)}
              />
            </List.Item>
          )}
        />
      ) : (
        <Empty description="暂无未读消息" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
      <div style={{ textAlign: 'center', paddingTop: 8 }}>
        <Button type="link" onClick={() => { setOpen(false); navigate('/support/message'); }}>
          查看更多
        </Button>
      </div>
    </div>
  );

  return (
    <Popover
      content={content}
      title="消息通知"
      trigger="click"
      open={open}
      onOpenChange={handleOpenChange}
      placement="bottomRight"
    >
      <Badge count={unreadCount} overflowCount={99} size="small">
        <BellOutlined style={{ fontSize: 18, cursor: 'pointer' }} />
      </Badge>
    </Popover>
  );
};

export default HeaderMessage;
