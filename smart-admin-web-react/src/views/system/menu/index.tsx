/**
 * Menu Management Page
 * 菜單管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Card, Typography } from 'antd';

const { Title, Paragraph } = Typography;

export default function MenuPage() {
  return (
    <div>
      <Card>
        <Title level={2}>菜單管理</Title>
        <Paragraph>
          此頁面由<strong>動態路由系統</strong>自動加載。
        </Paragraph>
        <Paragraph>
          路由路徑：<code>/system/menu</code>
        </Paragraph>
        <Paragraph type="secondary">
          完整的菜單樹管理功能將在後續階段實現。
        </Paragraph>
      </Card>
    </div>
  );
}
