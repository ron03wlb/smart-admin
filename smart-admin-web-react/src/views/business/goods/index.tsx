/**
 * Goods Management Page
 * 商品管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Card, Typography } from 'antd';

const { Title, Paragraph } = Typography;

export default function GoodsPage() {
  return (
    <div>
      <Card>
        <Title level={2}>商品管理</Title>
        <Paragraph>
          此頁面由<strong>動態路由系統</strong>自動加載。
        </Paragraph>
        <Paragraph>
          路由路徑：<code>/business/goods</code>
        </Paragraph>
        <Paragraph type="secondary">
          完整的商品 CRUD 功能將在 Phase 3 實現（參考 Vue 版本）。
        </Paragraph>
      </Card>
    </div>
  );
}
