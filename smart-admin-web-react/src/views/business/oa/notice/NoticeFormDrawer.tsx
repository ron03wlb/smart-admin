/**
 * Notice Form Drawer
 *
 * Corresponds to Vue's business/oa/notice/components/notice-form-drawer.vue (307L)
 * Simplified: uses TextArea instead of rich text editor
 */
import React, { useEffect, useState } from 'react';
import { Drawer, Form, Input, Select, Radio, DatePicker, Button, Space, message } from 'antd';
import RichTextEditor from '@/components/RichTextEditor/RichTextEditor';
import { noticeApi } from '@/api/business/oa/notice-api';
import type { NoticeVO, NoticeTypeVO } from '@/api/business/oa/notice-api';
import dayjs from 'dayjs';

interface Props {
  open: boolean;
  notice?: NoticeVO;
  noticeTypes: NoticeTypeVO[];
  onClose: () => void;
  onSuccess: () => void;
}

const NoticeFormDrawer: React.FC<Props> = ({ open, notice, noticeTypes, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!notice;

  useEffect(() => {
    if (open) {
      if (notice) {
        noticeApi.getUpdateVO(notice.noticeId).then((res) => {
          if (res.code === 1 && res.data) {
            form.setFieldsValue({
              ...res.data,
              publishTime: res.data.publishTime ? dayjs(res.data.publishTime) : undefined,
            });
          }
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ allVisibleFlag: true, scheduledPublishFlag: false });
      }
    }
  }, [open, notice, form]);

  const handleSave = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const payload = {
        ...values,
        publishTime: values.publishTime?.format('YYYY-MM-DD HH:mm:ss'),
        contentText: values.contentHtml?.replace(/<[^>]+>/g, '') || '',
      };
      if (isEdit) {
        await noticeApi.update({ ...payload, noticeId: notice!.noticeId });
      } else {
        await noticeApi.add(payload);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  const scheduledPublishFlag = Form.useWatch('scheduledPublishFlag', form);

  return (
    <Drawer
      title={isEdit ? '编辑通知公告' : '新建通知公告'}
      open={open} onClose={onClose} width={800}
      extra={<Space><Button onClick={onClose}>取消</Button><Button type="primary" onClick={handleSave} loading={loading}>保存</Button></Space>}
    >
      <Form form={form} labelCol={{ span: 4 }}>
        <Form.Item label="标题" name="title" rules={[{ required: true, message: '请输入标题' }]}>
          <Input placeholder="请输入标题" maxLength={200} />
        </Form.Item>
        <Form.Item label="分类" name="noticeTypeId" rules={[{ required: true, message: '请选择分类' }]}>
          <Select placeholder="请选择分类" options={noticeTypes.map((t) => ({ label: t.noticeTypeName, value: t.noticeTypeId }))} />
        </Form.Item>
        <Form.Item label="作者" name="author" rules={[{ required: true, message: '请输入作者' }]}>
          <Input placeholder="请输入作者" maxLength={50} />
        </Form.Item>
        <Form.Item label="来源" name="source">
          <Input placeholder="请输入来源" maxLength={100} />
        </Form.Item>
        <Form.Item label="文号" name="documentNumber">
          <Input placeholder="请输入文号" maxLength={100} />
        </Form.Item>
        <Form.Item label="可见范围" name="allVisibleFlag" rules={[{ required: true }]}>
          <Radio.Group>
            <Radio value={true}>全部可见</Radio>
            <Radio value={false}>部分可见</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="定时发布" name="scheduledPublishFlag">
          <Radio.Group>
            <Radio value={false}>立即发布</Radio>
            <Radio value={true}>定时发布</Radio>
          </Radio.Group>
        </Form.Item>
        {scheduledPublishFlag && (
          <Form.Item label="发布时间" name="publishTime" rules={[{ required: true, message: '请选择发布时间' }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        )}
        <Form.Item label="内容" name="contentHtml" rules={[{ required: true, message: '请输入内容' }]}>
          <RichTextEditor placeholder="请输入公告内容" />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default NoticeFormDrawer;
