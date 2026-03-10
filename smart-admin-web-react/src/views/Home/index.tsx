/**
 * Home Page
 * 首頁（集成國際化）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/home/home.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { Card, Typography, Row, Col, Statistic } from 'antd';
import { UserOutlined, ShoppingOutlined, FileOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title } = Typography;

/**
 * 首頁
 * 展示系統概覽和 i18n 國際化功能
 */
export default function HomePage() {
  const { t } = useTranslation();

  return (
    <div>
      <Title level={2}>{t('menu.home')}</Title>

      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title={t('menu.system.user')}
              value={1128}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title={t('menu.business.goods')}
              value={935}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title={t('menu.support.file')}
              value={532}
              prefix={<FileOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 24 }}>
        <Title level={4}>Phase 1 POC 進度</Title>
        <p>✅ Week 1 Day 1: 專案初始化與基礎配置</p>
        <p>✅ Week 1 Day 2: HTTP 客戶端 + API 層</p>
        <p>✅ Week 1 Day 3: Redux Store 基礎架構</p>
        <p>✅ Week 1 Day 4: 權限系統 POC（usePrivilege Hook）</p>
        <p>✅ Week 1 Day 5: 路由基礎架構（BasicLayout + ProtectedRoute）</p>
        <p>✅ Week 2 Day 1: 登錄頁面 Part 1（表單驗證）</p>
        <p>✅ Week 2 Day 2: 登錄頁面 Part 2（MFA 支持）</p>
        <p>✅ Week 2 Day 3: userSlice 完整遷移（菜單樹構建 - 427 行）</p>
        <p>✅ Week 2 Day 4: 首頁 + 側邊欄菜單（動態菜單渲染）</p>
        <p>✅ Week 2 Day 5: 動態路由加載（React.lazy 懶加載）</p>
        <p>✅ Week 3: 通用組件庫建設（10+ 組件，188 單元測試）</p>
        <p>✅ Week 4 Day 1-3: Redux Slices（appConfig、role、tenant）</p>
        <p>✅ Week 4 Day 4: 國際化集成（react-i18next + Ant Design Locale）</p>
        <p>⏳ Week 4 Day 5: Keep-alive 緩存機制（進行中）</p>
      </Card>
    </div>
  );
}
