/**
 * Message Send Form Modal
 *
 * Corresponds to Vue's support/message/components/message-send-form.vue
 */
import React, { useState } from 'react';
import { Modal, Form, Input, Button, message, Tag } from 'antd';
import { messageApi } from '@/api/support/message-api';
import MessageReceiverModal from './MessageReceiverModal';

interface ReceiverItem {
  employeeId: number;
  actualName: string;
}

interface Props {
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

const MessageSendForm: React.FC<Props> = ({ open, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [receivers, setReceivers] = useState<ReceiverItem[]>([]);
  const [receiverModalOpen, setReceiverModalOpen] = useState(false);

  const handleSelectReceivers = (selected: ReceiverItem[]) => {
    setReceivers(selected);
    setReceiverModalOpen(false);
  };

  const handleRemoveReceiver = (employeeId: number) => {
    setReceivers(receivers.filter((r) => r.employeeId !== employeeId));
  };

  const handleOk = async () => {
    const values = await form.validateFields();
    if (receivers.length === 0) {
      message.warning('请选择接收人');
      return;
    }
    setLoading(true);
    try {
      await messageApi.sendMessages({
        title: values.title,
        content: values.content,
        receiverUserIdList: receivers.map((r) => r.employeeId),
      });
      message.success('发送成功');
      form.resetFields();
      setReceivers([]);
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Modal title="发送消息" open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={600} destroyOnClose>
        <Form form={form} labelCol={{ span: 4 }}>
          <Form.Item label="标题" name="title" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="请输入消息标题" />
          </Form.Item>
          <Form.Item label="接收人" required>
            <div>
              <Button size="small" onClick={() => setReceiverModalOpen(true)} style={{ marginBottom: 8 }}>选择接收人</Button>
              <div>
                {receivers.map((r) => (
                  <Tag key={r.employeeId} closable onClose={() => handleRemoveReceiver(r.employeeId)}>
                    {r.actualName}
                  </Tag>
                ))}
              </div>
            </div>
          </Form.Item>
          <Form.Item label="内容" name="content" rules={[{ required: true, message: '请输入消息内容' }]}>
            <Input.TextArea rows={4} placeholder="请输入消息内容" />
          </Form.Item>
        </Form>
      </Modal>

      <MessageReceiverModal
        open={receiverModalOpen}
        selectedIds={receivers.map((r) => r.employeeId)}
        onCancel={() => setReceiverModalOpen(false)}
        onSelect={handleSelectReceivers}
      />
    </>
  );
};

export default MessageSendForm;
