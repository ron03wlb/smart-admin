/**
 * 404 Not Found Page
 * 404 不存在頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/40X/404.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const NotFound404: React.FC = () => {
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

export default NotFound404;
