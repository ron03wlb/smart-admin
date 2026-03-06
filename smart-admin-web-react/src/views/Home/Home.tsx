/**
 * 首頁組件（純內容組件）
 *
 * 注意：此組件現在被 MainLayout 包裹，不需要處理佈局邏輯。
 * MainLayout 已經提供了 Header、Sidebar、Breadcrumb 等佈局元素。
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 * @updated 2026-03-06
 */
import { Button, Space, Typography, Divider } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { selectEmployeeName, selectAdministratorFlag, logout } from '@/store/slices/userSlice';
import { PrivilegeButton } from '@/components/framework/privilege';

const { Title, Paragraph, Text } = Typography;

/**
 * 首頁組件
 *
 * 職責：
 * - 顯示歡迎信息
 * - 提供快捷操作（測試權限按鈕、登出按鈕）
 * - 未來將整合 Dashboard 儀表板（Task 10）
 */
export default function Home() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const employeeName = useAppSelector(selectEmployeeName);
  const isAdmin = useAppSelector(selectAdministratorFlag);

  /**
   * 登出處理
   */
  const handleLogout = () => {
    dispatch(logout());
    navigate('/');
  };

  return (
    <div style={{ textAlign: 'center', padding: '40px 20px' }}>
      <Title level={2}>SmartAdmin React 首頁</Title>

      <Paragraph style={{ fontSize: '16px', marginTop: '24px' }}>
        歡迎，<Text strong>{employeeName}</Text>！
        {isAdmin && <Text type="danger">（超級管理員）</Text>}
      </Paragraph>

      <Divider />

      <Space size="middle" style={{ marginTop: '32px' }}>
        <PrivilegeButton
          permissionCode="system:user:add"
          type="primary"
        >
          新增用戶（測試權限按鈕）
        </PrivilegeButton>

        <Button onClick={handleLogout}>登出</Button>
      </Space>

      <Paragraph style={{ marginTop: '48px', color: '#8c8c8c' }}>
        註：Dashboard 儀表板將在 Phase 1 Task 10 實施
      </Paragraph>
    </div>
  );
}
