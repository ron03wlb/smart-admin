/**
 * Home Page - Dashboard
 * 首頁儀表板
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/index.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Row, Col } from 'antd';
import HomeHeader from './components/HomeHeader';
import HomeNotice from './components/HomeNotice';
import OfficialAccountCard from './components/OfficialAccountCard';
import ChangelogCard from './components/ChangelogCard';
import ToBeDoneCard from './components/ToBeDoneCard';
import './index.css';

const HomePage: React.FC = () => {
  return (
    <div className="home-page">
      {/* 頂部用戶信息 */}
      <Row>
        <Col span={24}>
          <HomeHeader />
        </Col>
      </Row>

      {/* 下方左右佈局 */}
      <Row gutter={[10, 10]} style={{ marginTop: 10 }}>
        {/* 左側 */}
        <Col span={16}>
          <Row gutter={[10, 10]}>
            {/* 公告信息 */}
            <Col span={12}>
              <HomeNotice title="公告" noticeTypeId={1} />
            </Col>

            {/* 通知信息 */}
            <Col span={12}>
              <HomeNotice title="通知" noticeTypeId={2} />
            </Col>

            {/* 圖表組件 - Phase 2 實現 */}
            <Col span={12}>
              <div style={{ height: 300, background: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                Pie Chart (Phase 2)
              </div>
            </Col>

            <Col span={12}>
              <div style={{ height: 300, background: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                Category Chart (Phase 2)
              </div>
            </Col>

            <Col span={24}>
              <div style={{ height: 300, background: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                Gradient Chart (Phase 2)
              </div>
            </Col>
          </Row>
        </Col>

        {/* 右側 */}
        <Col span={8}>
          <Row gutter={[10, 10]}>
            {/* 聯繫我們 */}
            <Col span={24}>
              <OfficialAccountCard />
            </Col>

            {/* 更新日誌 */}
            <Col span={24}>
              <ChangelogCard />
            </Col>

            {/* 待辦事項 */}
            <Col span={24}>
              <ToBeDoneCard />
            </Col>
          </Row>
        </Col>
      </Row>
    </div>
  );
};

export default HomePage;
