/**
 * Notice Form Drawer Component
 * 通知公告表單 Drawer 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect } from 'react';
import { Drawer, Form, Input, message, Select } from 'antd';
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
   * 表單驗證規則
   */
  const rules = {
    title: [
      { required: true, message: '請輸入公告標題' },
      {
        max: NOTICE_VALIDATION.TITLE_MAX_LENGTH,
        message: `標題最多${NOTICE_VALIDATION.TITLE_MAX_LENGTH}個字符`,
      },
    ],
    noticeTypeId: [{ required: true, message: '請選擇分類' }],
    author: [
      { required: true, message: '請輸入作者' },
      {
        max: NOTICE_VALIDATION.AUTHOR_MAX_LENGTH,
        message: `作者最多${NOTICE_VALIDATION.AUTHOR_MAX_LENGTH}個字符`,
      },
    ],
    source: [
      { required: true, message: '請輸入來源' },
      {
        max: NOTICE_VALIDATION.SOURCE_MAX_LENGTH,
        message: `來源最多${NOTICE_VALIDATION.SOURCE_MAX_LENGTH}個字符`,
      },
    ],
    allVisibleFlag: [{ required: true, message: '請選擇可見範圍' }],
    contentHtml: [{ required: true, message: '請輸入公告內容' }],
    documentNumber: [
      {
        max: NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH,
        message: `文號最多${NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH}個字符`,
      },
    ],
  };

  /**
   * 表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit && initialData) {
        // 編輯模式
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
        // 新增模式
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
        message.error(error.message || (isEdit ? '更新失敗' : '新增失敗'));
      }
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Drawer 關閉處理
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  /**
   * 初始化表單數據（編輯模式）
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
        contentHtml: '', // TODO: 需要從API獲取完整內容
      });
    } else if (visible && !initialData) {
      form.resetFields();
      // 設置默認值
      form.setFieldsValue({
        allVisibleFlag: 1, // 默認全部可見
      });
    }
  }, [visible, initialData, form]);

  return (
    <Drawer
      title={isEdit ? '編輯通知公告' : '新增通知公告'}
      open={visible}
      onClose={handleCancel}
      width={800}
      destroyOnClose
      footer={
        <div style={{ textAlign: 'right' }}>
          <button
            type="button"
            onClick={handleCancel}
            style={{
              marginRight: 8,
              padding: '4px 15px',
              border: '1px solid #d9d9d9',
              borderRadius: '2px',
              background: '#fff',
              cursor: 'pointer',
            }}
          >
            取消
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={loading}
            style={{
              padding: '4px 15px',
              border: 'none',
              borderRadius: '2px',
              background: '#1890ff',
              color: '#fff',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? '提交中...' : '提交'}
          </button>
        </div>
      }
    >
      <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
        <Form.Item label="公告標題" name="title" rules={rules.title}>
          <Input
            placeholder="請輸入公告標題"
            maxLength={NOTICE_VALIDATION.TITLE_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="分類" name="noticeTypeId" rules={rules.noticeTypeId}>
          <Select
            placeholder="請選擇分類"
            options={noticeTypeList.map((item) => ({
              label: item.noticeTypeName,
              value: item.noticeTypeId,
            }))}
          />
        </Form.Item>

        <Form.Item
          label="文號"
          name="documentNumber"
          rules={rules.documentNumber}
          tooltip="例如：1024創新實驗室發〔2022〕字第36號"
        >
          <Input
            placeholder="請輸入文號（可選）"
            maxLength={NOTICE_VALIDATION.DOCUMENT_NUMBER_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="作者" name="author" rules={rules.author}>
          <Input
            placeholder="請輸入作者"
            maxLength={NOTICE_VALIDATION.AUTHOR_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="來源" name="source" rules={rules.source}>
          <Input
            placeholder="請輸入來源"
            maxLength={NOTICE_VALIDATION.SOURCE_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="可見範圍" name="allVisibleFlag" rules={rules.allVisibleFlag}>
          <Select placeholder="請選擇可見範圍">
            <Select.Option value={1}>全部可見</Select.Option>
            <Select.Option value={0}>部分可見</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="公告內容"
          name="contentHtml"
          rules={rules.contentHtml}
          tooltip="TODO: 應使用富文本編輯器組件"
        >
          <Input.TextArea
            placeholder="請輸入公告內容（暫用純文本，後續應使用富文本編輯器）"
            rows={8}
          />
        </Form.Item>
      </Form>
    </Drawer>
  );
}
