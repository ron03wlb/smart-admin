/**
 * Account LoginLog Component
 * 個人中心 - 登錄日誌
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const LoginLog: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Result
      status="info"
      title="登錄日誌"
      subTitle="請前往登錄日誌頁面查看完整登錄記錄"
      extra={
        <Button type="primary" onClick={() => navigate('/support/login-log')}>
          前往登錄日誌
        </Button>
      }
    />
  );
};

export default LoginLog;
