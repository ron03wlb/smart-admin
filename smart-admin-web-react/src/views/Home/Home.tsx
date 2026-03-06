/**
 * 首頁（臨時）
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { selectEmployeeName, selectAdministratorFlag, logout } from '@/store/slices/userSlice';
import { PrivilegeButton } from '@/components/framework/privilege';

export default function Home() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const employeeName = useAppSelector(selectEmployeeName);
  const isAdmin = useAppSelector(selectAdministratorFlag);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/');
  };

  return (
    <div style={{ padding: '40px', textAlign: 'center' }}>
      <h1>SmartAdmin React 首頁</h1>
      <p>歡迎，{employeeName}！</p>
      {isAdmin && <p style={{ color: '#ff4d4f' }}>（超級管理員）</p>}

      <div style={{ marginTop: '32px' }}>
        <PrivilegeButton
          permissionCode="system:user:add"
          type="primary"
          style={{ marginRight: '12px' }}
        >
          新增用戶（測試權限按鈕）
        </PrivilegeButton>

        <Button onClick={handleLogout}>
          登出
        </Button>
      </div>
    </div>
  );
}
