/**
 * Home Header Component
 *
 * Displays greeting, department, and last login information.
 * Corresponds to Vue's home-header.vue
 */
import React, { useMemo } from 'react';
import { Card, Row, Col, Typography, Space, Tag } from 'antd';
import {
  ClockCircleOutlined,
  DesktopOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons';
import { useAppSelector } from '@/store/hooks';
import { selectEmployeeName } from '@/store/slices/userSlice';

const { Text, Title } = Typography;

// Time-based greeting
function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 6) return '午夜好';
  if (hour < 12) return '早上好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
}

// Random motivational quotes
const QUOTES = [
  '世上無難事，只要肯放棄。',
  '如果你覺得自己整天累成狗，只因為狗都比你努力。',
  '失敗並不可怕，可怕的是你還相信這句話。',
  '努力不一定成功，但不努力一定很舒服。',
  '只要功夫深，鐵杵磨成繡花針。',
  '知識就是力量，法國就是培根。',
  '你的對手正在翻書，你的仇人正在磨刀。',
  '不怕萬人阻擋，只怕自己投降。',
];

const HomeHeader: React.FC = () => {
  const employeeName = useAppSelector(selectEmployeeName);

  const greeting = useMemo(() => getGreeting(), []);
  const quote = useMemo(() => QUOTES[Math.floor(Math.random() * QUOTES.length)], []);

  const today = useMemo(() => {
    const d = new Date();
    const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    return {
      date: `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`,
      week: weekDays[d.getDay()],
    };
  }, []);

  return (
    <Card style={{ marginBottom: 10 }}>
      <Row gutter={16} align="middle">
        <Col flex="auto">
          <Title level={4} style={{ marginBottom: 4 }}>
            {greeting}，{employeeName}
          </Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            {quote}
          </Text>
        </Col>
        <Col>
          <Space direction="vertical" size={4} style={{ textAlign: 'right' }}>
            <Space size={8}>
              <ClockCircleOutlined style={{ color: '#1890ff' }} />
              <Text type="secondary">{today.date} {today.week}</Text>
            </Space>
            <Space size={8}>
              <DesktopOutlined style={{ color: '#52c41a' }} />
              <Text type="secondary">
                {navigator.userAgent.includes('Chrome') ? 'Chrome' : 'Browser'}
              </Text>
            </Space>
            <Space size={8}>
              <EnvironmentOutlined style={{ color: '#fa8c16' }} />
              <Tag color="blue">本地開發</Tag>
            </Space>
          </Space>
        </Col>
      </Row>
    </Card>
  );
};

export default HomeHeader;
