/**
 * To Be Done Modal - 新增待辦Modal
 * 新增待辦事項
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-17
 */

import { useEffect } from 'react';
import { Modal, Form, Input, message } from 'antd';

export interface ToBeDoneModalProps {
  /** Modal 可見性 */
  visible: boolean;
  /** 關閉回調 */
  onClose: () => void;
  /** 提交回調 */
  onSubmit: (title: string) => void;
}

const ToBeDoneModal: React.FC<ToBeDoneModalProps> = ({ visible, onClose, onSubmit }) => {
  const [form] = Form.useForm();

  /**
   * Modal 關閉時重置表單
   */
  useEffect(() => {
    if (!visible) {
      form.resetFields();
    }
  }, [visible, form]);

  /**
   * 表單提交
   */
  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      onSubmit(values.title);
      message.success('待辦事項新增成功');
      onClose();
    } catch (error) {
      console.error('表單驗證失敗:', error);
    }
  };

  return (
    <Modal
      title="新增待辦事項"
      open={visible}
      onOk={handleOk}
      onCancel={onClose}
      width={500}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 20 }}>
        <Form.Item
          name="title"
          label="待辦事項"
          rules={[
            { required: true, message: '請輸入待辦事項' },
            { max: 100, message: '待辦事項不能超過100個字符' },
          ]}
        >
          <Input.TextArea
            placeholder="請輸入待辦事項"
            maxLength={100}
            rows={3}
            showCount
            autoFocus
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ToBeDoneModal;
