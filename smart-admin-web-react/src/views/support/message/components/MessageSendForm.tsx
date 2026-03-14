/**
 * Message Send Form
 * 發送消息表單模態框
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/message/components/message-send-form.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useRef, useImperativeHandle, forwardRef } from 'react';
import { Modal, Form, Input, Button, Select, Space, message } from 'antd';
import { messageApi } from '@/api/support/messageApi';
import { MESSAGE_TYPE_OPTIONS } from '@/constants/support/messageConst';
import MessageReceiverModal, { MessageReceiverModalRef } from './MessageReceiverModal';

const { TextArea } = Input;

/**
 * 發送消息表單
 */
interface SendFormData {
  title?: string;
  receiverUserIdList?: number[];
  content?: string;
  messageType?: number;
  receiverUserType: number; // 1 = ADMIN_EMPLOYEE
}

/**
 * Props
 */
interface MessageSendFormProps {
  onSuccess: () => void;
}

/**
 * Ref Methods
 */
export interface MessageSendFormRef {
  show: () => void;
}

const MessageSendForm = forwardRef<MessageSendFormRef, MessageSendFormProps>(({ onSuccess }, ref) => {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selectedNames, setSelectedNames] = useState<string>('');
  const [form] = Form.useForm<SendFormData>();
  const receiverModalRef = useRef<MessageReceiverModalRef>(null);

  /**
   * 顯示模態框
   */
  const show = () => {
    form.resetFields();
    form.setFieldsValue({
      receiverUserType: 1, // ADMIN_EMPLOYEE
    });
    setSelectedNames('');
    setVisible(true);
  };

  /**
   * 關閉模態框
   */
  const handleCancel = () => {
    form.resetFields();
    setVisible(false);
  };

  /**
   * 選擇接收人
   */
  const handleSelectReceiver = () => {
    const currentIds = form.getFieldValue('receiverUserIdList');
    receiverModalRef.current?.showModal(currentIds);
  };

  /**
   * 接收人選擇確認回調
   */
  const handleReceiverConfirm = (employeeIds: number[], employeeNames: string[]) => {
    form.setFieldsValue({ receiverUserIdList: employeeIds });
    setSelectedNames(employeeNames.join(', '));
  };

  /**
   * 提交表單
   */
  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();

      // 構建消息列表（每個接收人一條消息）
      const messageList = values.receiverUserIdList!.map((userId) => ({
        title: values.title!,
        receiverUserId: userId,
        content: values.content!,
        messageType: values.messageType!,
        receiverUserType: values.receiverUserType,
      }));

      setLoading(true);
      await messageApi.sendMessages(messageList);
      message.success('發送成功');
      setVisible(false);
      onSuccess();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('請檢查表單必填項');
      } else {
        console.error('發送消息失敗:', error);
        message.error('發送失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 暴露方法給父組件
   */
  useImperativeHandle(ref, () => ({
    show,
  }));

  return (
    <>
      <Modal
        title="發送消息"
        open={visible}
        onCancel={handleCancel}
        width={700}
        maskClosable={false}
        destroyOnClose
        footer={
          <Space>
            <Button onClick={handleCancel}>取消</Button>
            <Button type="primary" onClick={handleSubmit} loading={loading}>
              發送
            </Button>
          </Space>
        }
      >
        <Form form={form} labelCol={{ span: 5 }} wrapperCol={{ span: 16 }}>
          <Form.Item
            label="標題"
            name="title"
            rules={[{ required: true, message: '標題必填' }]}
          >
            <Input placeholder="請輸入標題" />
          </Form.Item>

          <Form.Item
            label="接收人"
            name="receiverUserIdList"
            rules={[{ required: true, message: '接收人必填' }]}
          >
            <div>
              <Button type="primary" onClick={handleSelectReceiver}>
                選擇接收人
              </Button>
              {selectedNames && (
                <div style={{ marginTop: 8, color: '#666' }}>已選擇：{selectedNames}</div>
              )}
            </div>
          </Form.Item>

          <Form.Item
            label="消息類型"
            name="messageType"
            rules={[{ required: true, message: '消息類型必填' }]}
          >
            <Select placeholder="請選擇消息類型" options={MESSAGE_TYPE_OPTIONS} />
          </Form.Item>

          <Form.Item
            label="推送內容"
            name="content"
            rules={[{ required: true, message: '推送內容必填' }]}
          >
            <TextArea rows={4} placeholder="請輸入推送內容" />
          </Form.Item>

          {/* Hidden field for receiverUserType */}
          <Form.Item name="receiverUserType" hidden>
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      {/* 接收人選擇模態框 */}
      <MessageReceiverModal ref={receiverModalRef} onConfirm={handleReceiverConfirm} />
    </>
  );
});

MessageSendForm.displayName = 'MessageSendForm';

export default MessageSendForm;
