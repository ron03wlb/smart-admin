/**
 * Account Notice Component
 * 個人中心 - 通知公告
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const Notice: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Result
      status="info"
      title="通知公告"
      subTitle="請前往公告管理頁面查看完整公告列表"
      extra={
        <Button type="primary" onClick={() => navigate('/business/notice')}>
          前往公告管理
        </Button>
      }
    />
  );
};

export default Notice;
