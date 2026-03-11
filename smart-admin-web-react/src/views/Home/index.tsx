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
        <p>✅ Week 1: 專案初始化 + HTTP 客戶端 + Redux Store + 權限系統 + 路由</p>
        <p>✅ Week 2: 登錄頁面（表單驗證 + MFA）+ userSlice + 首頁 + 動態路由</p>
        <p>✅ Week 3: 通用組件庫（10 組件，188 單元測試）</p>
        <p>✅ Week 4: Redux Slices（4 slices，91 測試）+ i18n + Keep-alive</p>
        <p style={{ marginTop: 16, fontWeight: 'bold', color: '#52c41a' }}>
          🎉 M2 里程碑達成！累計 279 測試，覆蓋率 &gt; 80%
        </p>
        <p style={{ marginTop: 8 }}>⏳ 下一步：Phase 3 組件遷移（準備中）</p>
      </Card>
    </div>
  );
}
