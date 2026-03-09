/**
 * File Management Page
 * 文件管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Card, Typography } from 'antd';

const { Title, Paragraph } = Typography;

export default function FilePage() {
  return (
    <div>
      <Card>
        <Title level={2}>文件管理</Title>
        <Paragraph>
          此頁面由<strong>動態路由系統</strong>自動加載。
        </Paragraph>
        <Paragraph>
          路由路徑：<code>/support/file</code>
        </Paragraph>
        <Paragraph type="secondary">
          完整的文件上傳、下載、預覽功能將在後續階段實現。
        </Paragraph>
      </Card>
    </div>
  );
}
