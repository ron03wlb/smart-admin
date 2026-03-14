/**
 * Account OperateLog Component
 * 個人中心 - 操作日誌
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const OperateLog: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Result
      status="info"
      title="操作日誌"
      subTitle="請前往操作日誌頁面查看完整操作記錄"
      extra={
        <Button type="primary" onClick={() => navigate('/support/operate-log')}>
          前往操作日誌
        </Button>
      }
    />
  );
};

export default OperateLog;
