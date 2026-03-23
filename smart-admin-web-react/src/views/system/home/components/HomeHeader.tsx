/**
 * Home Header - 首頁用戶信息頭部
 * 顯示歡迎語、部門信息、上次登錄信息等
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/home-header.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Card, Typography, Row } from 'antd';
import { AlertOutlined, SmileOutlined } from '@ant-design/icons';
import { useAppSelector } from '@/store/hooks';
import { useMemo } from 'react';
import './HomeHeader.css';

const { Text } = Typography;

const HomeHeader: React.FC = () => {
  const employeeName = useAppSelector(state => state.user.employeeName);

  /**
   * 歡迎語（根據時間）
   */
  const welcomeSentence = useMemo(() => {
    const hour = new Date().getHours();
    let greeting = '';

    if (hour > 0 && hour <= 6) {
      greeting = '午夜好，';
    } else if (hour > 6 && hour <= 11) {
      greeting = '早上好，';
    } else if (hour > 11 && hour <= 14) {
      greeting = '中午好，';
    } else if (hour > 14 && hour <= 18) {
      greeting = '下午好，';
    } else {
      greeting = '晚上好，';
    }

    return greeting + (employeeName || '用戶');
  }, [employeeName]);

  /**
   * 上次登錄信息
   * TODO: 待後端提供 lastLoginTime, lastLoginUserAgent, lastLoginIp 等數據後完善
   */
  const lastLoginInfo = useMemo(() => {
    // 目前 UserState 中沒有上次登錄相關數據，暫時顯示歡迎消息
    return '歡迎使用 SmartAdmin 管理系統';
  }, []);

  /**
   * 當前日期信息
   */
  const dayInfo = useMemo(() => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
    const week = weekDays[now.getDay()];

    return `${year}-${month}-${day} 星期${week}`;
  }, []);

  /**
   * 心靈雞湯（隨機）
   */
  const heartSentence = useMemo(() => {
    const sentences = [
      '每一個不曾起舞的日子，都是對生命的辜負。',
      '世界上最快樂的事，莫過於為理想而奮鬥。',
      '生活不是等待暴風雨過去，而是要學會在雨中跳舞。',
      '成功不是將來才有的，而是從決定去做的那一刻起，持續累積而成。',
      '你只有非常努力，才能看起來毫不費力。',
    ];
    return sentences[Math.floor(Math.random() * sentences.length)];
  }, []);

  return (
    <Card className="home-header-card">
      <div className="home-header-page-header">
        {/* 標題與副標題 */}
        <div className="page-header-heading">
          <div className="page-header-title">{welcomeSentence}</div>
          <div className="page-header-subtitle">
            <Text type="secondary">登錄帳號： {employeeName || '未知'}</Text>
          </div>
        </div>

        {/* 額外信息（日期） */}
        <div className="page-header-extra">
          <Text type="secondary">{dayInfo}</Text>
        </div>
      </div>

      {/* 內容區 */}
      <Row className="home-header-content">
        <div className="left-content">
          <p className="last-login-info">
            <AlertOutlined /> {lastLoginInfo}
          </p>
          <a className="sentence" href="#" target="_blank" rel="noopener noreferrer">
            <SmileOutlined spin /> {heartSentence}
          </a>
        </div>
      </Row>
    </Card>
  );
};

export default HomeHeader;
