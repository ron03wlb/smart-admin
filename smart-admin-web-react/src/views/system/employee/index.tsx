/**
 * Employee Management Page
 * 員工管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Card, Typography } from 'antd';

const { Title, Paragraph } = Typography;

/**
 * 員工管理頁面
 * 動態路由測試頁面
 */
export default function EmployeePage() {
  return (
    <div>
      <Card>
        <Title level={2}>員工管理</Title>
        <Paragraph>
          此頁面由<strong>動態路由系統</strong>自動加載。
        </Paragraph>
        <Paragraph>
          路由路徑：<code>/system/employee</code>
        </Paragraph>
        <Paragraph type="secondary">完整的 CRUD 功能將在後續階段實現。</Paragraph>
      </Card>
    </div>
  );
}
