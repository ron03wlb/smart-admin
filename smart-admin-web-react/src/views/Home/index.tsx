/**
 * Home Page (Placeholder)
 * 首頁（占位符）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/home/home.vue
 * 將在 Week 2 Day 4 完整實現
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Card, Typography, Row, Col, Statistic } from 'antd';
import { UserOutlined, ShoppingOutlined, DollarOutlined } from '@ant-design/icons';

const { Title } = Typography;

/**
 * 首頁占位符
 * Week 2 Day 4 將實現完整的首頁儀表板、統計數據等功能
 */
export default function HomePage() {
  return (
    <div>
      <Title level={2}>歡迎使用 SmartAdmin React</Title>

      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="用戶總數"
              value={1128}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="商品總數"
              value={935}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="總銷售額"
              value={98500}
              prefix={<DollarOutlined />}
              valueStyle={{ color: '#cf1322' }}
              precision={2}
            />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 24 }}>
        <Title level={4}>Phase 1 進度</Title>
        <p>✅ Week 1 Day 1: 專案初始化與基礎配置</p>
        <p>✅ Week 1 Day 2: HTTP 客戶端 + API 層</p>
        <p>✅ Week 1 Day 3: Redux Store 基礎架構</p>
        <p>✅ Week 1 Day 4: 權限系統 POC</p>
        <p>⏳ Week 1 Day 5: 路由基礎架構（進行中）</p>
      </Card>
    </div>
  );
}
