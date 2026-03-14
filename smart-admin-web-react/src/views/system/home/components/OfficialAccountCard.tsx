/**
 * Official Account Card - 聯繫我們卡片
 * 顯示聯繫方式或二維碼
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/components/official-account-card.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result } from 'antd';
import { WechatOutlined } from '@ant-design/icons';
import DefaultHomeCard from './DefaultHomeCard';
import './OfficialAccountCard.css';

const OfficialAccountCard: React.FC = () => {
  return (
    <DefaultHomeCard icon="SmileOutlined" title="聯繫我們">
      <div className="official-account-box">
        <Result
          icon={<WechatOutlined style={{ fontSize: 48, color: '#52c41a' }} />}
          title="SmartAdmin React"
          subTitle="歡迎聯繫我們獲取更多信息"
        />
      </div>
    </DefaultHomeCard>
  );
};

export default OfficialAccountCard;
