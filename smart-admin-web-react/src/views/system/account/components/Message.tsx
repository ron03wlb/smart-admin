/**
 * Account Message Component
 * 個人中心 - 我的消息
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const Message: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Result
      status="info"
      title="我的消息"
      subTitle="請前往消息管理頁面查看完整消息列表"
      extra={
        <Button type="primary" onClick={() => navigate('/support/message')}>
          前往消息管理
        </Button>
      }
    />
  );
};

export default Message;
