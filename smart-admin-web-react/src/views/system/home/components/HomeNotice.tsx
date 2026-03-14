/**
 * Home Notice - 首頁通知公告組件
 * 顯示通知公告列表
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/home-notice.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Spin, Empty, Badge, Tooltip } from 'antd';
import { noticeApi } from '@/api/business/noticeApi';
import type { NoticeVO } from '@/views/business/notice/types';
import DefaultHomeCard from './DefaultHomeCard';
import './HomeNotice.css';

interface HomeNoticeProps {
  title?: string;
  noticeTypeId: number;
}

const HomeNotice: React.FC<HomeNoticeProps> = ({ title = '通知公告', noticeTypeId }) => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<NoticeVO[]>([]);

  /**
   * 查詢通知公告列表
   */
  const queryNoticeList = async () => {
    setLoading(true);
    try {
      const result = await noticeApi.queryEmployeeNotice({
        noticeTypeId,
        pageNum: 1,
        pageSize: 6,
        searchCount: false,
      });
      setData(result.data.list || []);
    } catch (error) {
      console.error('查詢通知公告失敗:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    queryNoticeList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [noticeTypeId]);

  /**
   * 查看更多
   */
  const handleMore = () => {
    navigate('/business/notice');
  };

  /**
   * 進入詳情
   */
  const handleDetail = (noticeId: number) => {
    navigate(`/business/notice/detail?noticeId=${noticeId}`);
  };

  return (
    <DefaultHomeCard icon="SoundOutlined" title={title} extra="更多" onExtraClick={handleMore}>
      <Spin spinning={loading}>
        <div className="home-notice-content-wrapper">
          {data.length === 0 ? (
            <Empty />
          ) : (
            <ul>
              {data.map((item, index) => (
                <li key={index} className={item.viewFlag ? 'read' : 'un-read'}>
                  <Tooltip placement="top" title={item.title}>
                    <a className="content" onClick={() => handleDetail(item.noticeId)}>
                      <Badge status={item.viewFlag ? 'default' : 'error'} />
                      {item.title}
                    </a>
                  </Tooltip>
                  <span className="time">{item.publishDate}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </Spin>
    </DefaultHomeCard>
  );
};

export default HomeNotice;
