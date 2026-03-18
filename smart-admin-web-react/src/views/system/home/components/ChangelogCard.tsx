/**
 * Changelog Card - 更新日誌卡片
 * 顯示系統更新日誌列表
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/components/changelog-card.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Empty, Badge } from 'antd';
import { changeLogApi } from '@/api/support/changeLogApi';
import type { ChangeLogVO } from '@/views/support/change-log/types';
import { CHANGE_LOG_TYPE_LABELS } from '@/constants/support/changeLogConst';
import DefaultHomeCard from './DefaultHomeCard';
import './ChangelogCard.css';

const ChangelogCard: React.FC = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<ChangeLogVO[]>([]);

  /**
   * 查詢更新日誌列表
   */
  const queryChangeLog = async () => {
    try {
      const result = await changeLogApi.queryPage({
        pageNum: 1,
        pageSize: 8,
        searchCount: false,
      });
      setData(result.data.list || []);
    } catch (error) {
      console.error('查詢更新日誌失敗:', error);
    }
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    queryChangeLog();
  }, []);

  /**
   * 查看更多
   */
  const handleMore = () => {
    navigate('/support/change-log');
  };

  /**
   * 查看詳情
   */
  const handleDetail = (item: ChangeLogVO) => {
    // 可以打開 Modal 顯示詳情，這裡簡化為導航到列表頁
    navigate('/support/change-log');
  };

  return (
    <DefaultHomeCard icon="FlagOutlined" title="更新日誌" extra="更多" onExtraClick={handleMore}>
      {data.length === 0 ? (
        <Empty />
      ) : (
        <ul className="changelog-list">
          {data.map((item, index) => (
            <li key={index} className="un-read">
              <a className="content" onClick={() => handleDetail(item)}>
                <Badge status="processing" />
                {CHANGE_LOG_TYPE_LABELS[item.type as keyof typeof CHANGE_LOG_TYPE_LABELS]}：{item.updateVersion} 版本
              </a>
              <span className="time">{item.publicDate}</span>
            </li>
          ))}
        </ul>
      )}
    </DefaultHomeCard>
  );
};

export default ChangelogCard;
