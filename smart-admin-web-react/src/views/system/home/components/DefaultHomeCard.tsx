/**
 * Default Home Card - 通用卡片容器組件
 * 首頁卡片容器，支持標題、圖標、額外鏈接
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/components/default-home-card.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Card } from 'antd';
import * as AntdIcons from '@ant-design/icons';
import './DefaultHomeCard.css';

interface DefaultHomeCardProps {
  icon?: string;
  title?: string;
  extra?: string;
  onExtraClick?: () => void;
  children?: React.ReactNode;
}

const DefaultHomeCard: React.FC<DefaultHomeCardProps> = ({ icon, title, extra, onExtraClick, children }) => {
  // 動態獲取 Ant Design 圖標
  const IconComponent = icon ? (AntdIcons as any)[icon] : null;

  const cardTitle = (
    <div className="default-home-card-title">
      {IconComponent && <IconComponent style={{ fontSize: 18 }} />}
      <span className="title-text">{title}</span>
    </div>
  );

  const cardExtra = extra ? (
    <a onClick={onExtraClick}>{extra}</a>
  ) : null;

  return (
    <div className="default-home-card-container">
      <Card size="small" title={cardTitle} extra={cardExtra}>
        {children}
      </Card>
    </div>
  );
};

export default DefaultHomeCard;
