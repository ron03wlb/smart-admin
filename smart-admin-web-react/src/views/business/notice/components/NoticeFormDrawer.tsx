/**
 * Notice Form Drawer Component
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect } from 'react';
import { Drawer, Form, Input, message, Select, Button, Space } from 'antd';
import { noticeApi } from '@/api/business/noticeApi';
import type {
  NoticeVO,
  NoticeAddForm,
  NoticeUpdateForm,
  NoticeFormData,
  NoticeTypeVO,
} from '../types';
import { useModal } from '@/hooks/useModal';
import { NOTICE_VALIDATION } from '@/constants/business/noticeConst';
import RichTextEditor from '@/components/RichTextEditor/RichTextEditor';

interface NoticeFormDrawerProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  initialData?: NoticeVO;
  noticeTypeList: NoticeTypeVO[];
}

export default function NoticeFormDrawer({
  visible,
  onCancel,
  onSuccess,
  initialData,
  noticeTypeList,
}: NoticeFormDrawerProps) {
  const [form] = Form.useForm<NoticeFormData>();
  const [loading, setLoading] = React.useState(false);

  const { isEdit } = useModal<NoticeVO>({
    defaultFormData: initialData,
  });

  /**
   * Validation rules
   */
  const rules = {
    title: [
      { required: true, message: '请输入公告标题' },
      {
        max: NOTICE_VALIDATION.TITLE_MAX_LENGTH,
        message: `标题最多${NOTICE_VALIDATION.TITLE_MAX_LENGTH}个字符`,
      },
    ],
    noticeTypeId: [{ required: true, message: '请选择分类' }],
    author: [
      { required: true, message: '请输入作者' },
      {
        max: NOTICE_VALIDATION.AUTHOR_MAX_LENGTH,
        message: `作者最多${NOTICE_VALIDATION.AUTHOR_MAX_LENGTH}个字符`,
      },
    ],
    source: [
      { required: true, message: '请输入来源' },
      {
        max: NOTICE_VALIDATION.SOURCE_MAX_LENGTH,
        message: `来源最多${NOTICE_VALIDATION.SOURCE_MAX_LENGTH}个字符`,
      },
    ],
    allVisibleFlag: [{ required: true, message: '请选择可见范围' }],
    contentHtml: [{ required: true, message: '请输入公告内容' }],
    documentNumber: [
      {
        max: NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH,
        message: `文号最多${NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH}个字符`,
      },
    ],
  };

  /**
   * Form submit
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit && initialData) {
        const updateForm: NoticeUpdateForm = {
          noticeId: initialData.noticeId,
          title: values.title!,
          noticeTypeId: values.noticeTypeId!,
          documentNumber: values.documentNumber,
          author: values.author!,
          source: values.source!,
          allVisibleFlag: values.allVisibleFlag!,
          publishTime: values.publishTime,
          contentHtml: values.contentHtml!,
        };

        const res = await noticeApi.updateNotice(updateForm);
        if (res.ok) {
          message.success('更新成功');
          onSuccess();
        }
      } else {
        const addForm: NoticeAddForm = {
          title: values.title!,
          noticeTypeId: values.noticeTypeId!,
          documentNumber: values.documentNumber,
          author: values.author!,
          source: values.source!,
          allVisibleFlag: values.allVisibleFlag!,
          publishTime: values.publishTime,
          contentHtml: values.contentHtml!,
        };

        const res = await noticeApi.addNotice(addForm);
        if (res.ok) {
          message.success('新增成功');
          onSuccess();
        }
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || (isEdit ? '更新失败' : '新增失败'));
      }
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  /**
   * Initialize form data (edit mode)
   */
  useEffect(() => {
    if (visible && initialData) {
      form.setFieldsValue({
        title: initialData.title,
        noticeTypeId: initialData.noticeTypeId,
        documentNumber: initialData.documentNumber,
        author: initialData.author,
        source: initialData.source,
        allVisibleFlag: initialData.allVisibleFlag ? 1 : 0,
        publishTime: initialData.publishTime,
        contentHtml: '', // Content loaded from detail API in parent or via separate call
      });
    } else if (visible && !initialData) {
      form.resetFields();
      form.setFieldsValue({
        allVisibleFlag: 1,
      });
    }
  }, [visible, initialData, form]);

  return (
    <Drawer
      title={isEdit ? '编辑通知公告' : '新增通知公告'}
      open={visible}
      onClose={handleCancel}
      width={800}
      destroyOnClose
      extra={
        <Space>
          <Button onClick={handleCancel}>取消</Button>
          <Button type="primary" onClick={handleSubmit} loading={loading}>
            提交
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
        <Form.Item label="公告标题" name="title" rules={rules.title}>
          <Input placeholder="请输入公告标题" maxLength={NOTICE_VALIDATION.TITLE_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="分类" name="noticeTypeId" rules={rules.noticeTypeId}>
          <Select
            placeholder="请选择分类"
            options={noticeTypeList.map(item => ({
              label: item.noticeTypeName,
              value: item.noticeTypeId,
            }))}
          />
        </Form.Item>

        <Form.Item
          label="文号"
          name="documentNumber"
          rules={rules.documentNumber}
          tooltip="例如：1024创新实验室发〔2022〕字第36号"
        >
          <Input
            placeholder="请输入文号（可选）"
            maxLength={NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="作者" name="author" rules={rules.author}>
          <Input placeholder="请输入作者" maxLength={NOTICE_VALIDATION.AUTHOR_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="来源" name="source" rules={rules.source}>
          <Input placeholder="请输入来源" maxLength={NOTICE_VALIDATION.SOURCE_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="可见范围" name="allVisibleFlag" rules={rules.allVisibleFlag}>
          <Select placeholder="请选择可见范围">
            <Select.Option value={1}>全部可见</Select.Option>
            <Select.Option value={0}>部分可见</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item label="公告内容" name="contentHtml" rules={rules.contentHtml}>
          <RichTextEditor placeholder="请输入公告内容" />
        </Form.Item>
      </Form>
    </Drawer>
  );
}
