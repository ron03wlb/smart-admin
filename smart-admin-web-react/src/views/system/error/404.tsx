/**
 * 404 Not Found Page
 * 404 頁面不存在
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const NotFoundPage: React.FC = () => {
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate('/home');
  };

  return (
    <Result
      status="404"
      title="對不起，您訪問的內容不存在！"
      extra={
        <Button type="primary" onClick={handleGoHome}>
          返回首頁
        </Button>
      }
    />
  );
};

export default NotFoundPage;
