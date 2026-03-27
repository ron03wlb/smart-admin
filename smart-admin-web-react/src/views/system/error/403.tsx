/**
 * 403 Forbidden Page
 * 403 無權限頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/40X/403.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const Forbidden403: React.FC = () => {
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

export default Forbidden403;
