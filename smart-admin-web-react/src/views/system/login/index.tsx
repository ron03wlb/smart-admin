/**
 * Login Page (Placeholder)
 * 登錄頁面（占位符）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/login/login.vue (314 行)
 * 將在 Week 2 Day 1-2 完整實現
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { Card, Typography } from 'antd';

const { Title } = Typography;

/**
 * 登錄頁面占位符
 * Week 2 Day 1-2 將實現完整的登錄表單、驗證碼、MFA 等功能
 */
export default function LoginPage() {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      <Card style={{ width: 400, textAlign: 'center' }}>
        <Title level={2}>SmartAdmin React</Title>
        <Title level={4} type="secondary">
          登錄頁面（占位符）
        </Title>
        <p>Week 2 Day 1-2 將實現完整登錄功能</p>
      </Card>
    </div>
  );
}
