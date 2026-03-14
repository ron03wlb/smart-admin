/**
 * 403 Forbidden Page
 * 403 無權訪問頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const ForbiddenPage: React.FC = () => {
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate('/home');
  };

  return (
    <Result
      status="403"
      title="對不起，您沒有權限訪問此內容"
      extra={
        <Button type="primary" onClick={handleGoHome}>
          返回首頁
        </Button>
      }
    />
  );
};

export default ForbiddenPage;
