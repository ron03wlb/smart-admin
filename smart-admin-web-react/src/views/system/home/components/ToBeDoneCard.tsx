/**
 * To Be Done Card - 待辦事項卡片（簡化版）
 * 顯示待辦工作列表
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/components/to-be-done-card/home-to-be-done.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result } from 'antd';
import { CheckCircleOutlined } from '@ant-design/icons';
import DefaultHomeCard from './DefaultHomeCard';
import './ToBeDoneCard.css';

const ToBeDoneCard: React.FC = () => {
  return (
    <DefaultHomeCard icon="StarTwoTone" title="待辦工作">
      <div className="to-be-done-box">
        <Result
          icon={<CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />}
          title="待辦事項管理"
          subTitle="完整功能開發中，敬請期待"
        />
      </div>
    </DefaultHomeCard>
  );
};

export default ToBeDoneCard;
