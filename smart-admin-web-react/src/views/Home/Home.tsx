/**
 * Home Page Component
 *
 * Dashboard layout with header, notices, charts, and utility cards.
 * Corresponds to Vue's system/home/index.vue
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │ HomeHeader (greeting, calendar, info)    │
 * ├──────────────────────┬──────────────────┤
 * │ Announcements        │ Changelog        │
 * │ Notifications        │ To-do tasks      │
 * │ Pie chart            │                  │
 * │ Bar chart            │                  │
 * │ Gradient chart (full width)             │
 * └──────────────────────┴──────────────────┘
 */
import { Row, Col } from 'antd';
import HomeHeader from './components/HomeHeader';
import HomeNotice from './components/HomeNotice';
import PieChart from './components/charts/PieChart';
import CategoryChart from './components/charts/CategoryChart';
import GradientChart from './components/charts/GradientChart';
import ToBeDoneCard from './components/ToBeDoneCard';
import ChangelogCard from './components/ChangelogCard';

export default function Home() {
  return (
    <div>
      {/* Header section */}
      <HomeHeader />

      {/* Main content grid */}
      <Row gutter={[10, 10]}>
        {/* Left section (16 cols / 67%) */}
        <Col xs={24} lg={16}>
          <Row gutter={[10, 10]}>
            {/* Announcements */}
            <Col xs={24} sm={12}>
              <HomeNotice title="公告" noticeTypeId={1} />
            </Col>
            {/* Notifications */}
            <Col xs={24} sm={12}>
              <HomeNotice title="通知" noticeTypeId={2} />
            </Col>
            {/* Pie chart */}
            <Col xs={24} sm={12}>
              <PieChart />
            </Col>
            {/* Bar chart */}
            <Col xs={24} sm={12}>
              <CategoryChart />
            </Col>
            {/* Gradient chart (full width) */}
            <Col span={24}>
              <GradientChart />
            </Col>
          </Row>
        </Col>

        {/* Right section (8 cols / 33%) */}
        <Col xs={24} lg={8}>
          <Row gutter={[10, 10]}>
            {/* Changelog */}
            <Col span={24}>
              <ChangelogCard />
            </Col>
            {/* To-do tasks */}
            <Col span={24}>
              <ToBeDoneCard />
            </Col>
          </Row>
        </Col>
      </Row>
    </div>
  );
}
