/**
 * ChangeLog Form Modal
 * 系統更新日誌表單 Modal
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/change-log/change-log-form.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, DatePicker, message } from 'antd';
import dayjs from 'dayjs';

import { changeLogApi } from '@/api/support/changeLogApi';
import { CHANGE_LOG_VALIDATION } from '@/constants/support/changeLogConst';
import type { ChangeLogVO, ChangeLogFormData, ChangeLogAddForm, ChangeLogUpdateForm } from '../types';
import SmartEnumSelect from '@/components/common/SmartEnumSelect';

const { TextArea } = Input;

interface ChangeLogFormModalProps {
  onSuccess: () => void;
}

const ChangeLogFormModal = forwardRef<{ show: (rowData?: ChangeLogVO) => void }, ChangeLogFormModalProps>(
  ({ onSuccess }, ref) => {
    const [form] = Form.useForm<ChangeLogFormData>();
    const [visible, setVisible] = useState(false);
    const [isEdit, setIsEdit] = useState(false);
    const [loading, setLoading] = useState(false);

    /**
     * 顯示 Modal
     */
    useImperativeHandle(ref, () => ({
      show: (rowData?: ChangeLogVO) => {
        setIsEdit(!!rowData?.changeLogId);

        if (rowData) {
          // 編輯模式 - 回填數據
          form.setFieldsValue({
            ...rowData,
            publicDate: rowData.publicDate ? dayjs(rowData.publicDate) : undefined,
          });
        } else {
          // 新增模式 - 重置表單
          form.resetFields();
        }

        setVisible(true);
      },
    }));

    /**
     * 關閉 Modal
     */
    const handleClose = () => {
      setVisible(false);
      form.resetFields();
    };

    /**
     * 提交表單
     */
    const handleSubmit = async () => {
      try {
        const values = await form.validateFields();
        setLoading(true);

        const params: ChangeLogAddForm | ChangeLogUpdateForm = {
          ...values,
          publicDate: dayjs(values.publicDate).format('YYYY-MM-DD'),
        };

        if (isEdit) {
          // 編輯
          await changeLogApi.update(params as ChangeLogUpdateForm);
          message.success('更新成功');
        } else {
          // 新增
          await changeLogApi.add(params as ChangeLogAddForm);
          message.success('新增成功');
        }

        handleClose();
        onSuccess();
      } catch (error: any) {
        if (error.errorFields) {
          message.error('請檢查表單填寫是否正確');
        } else {
          console.error('Submit failed:', error);
        }
      } finally {
        setLoading(false);
      }
    };

    return (
      <Modal
        title={isEdit ? '編輯更新日誌' : '新增更新日誌'}
        open={visible}
        onOk={handleSubmit}
        onCancel={handleClose}
        confirmLoading={loading}
        width={600}
        destroyOnClose
        maskClosable={false}
      >
        <Form
          form={form}
          labelCol={{ span: 5 }}
          wrapperCol={{ span: 19 }}
          autoComplete="off"
        >
          <Form.Item name="changeLogId" hidden>
            <Input />
          </Form.Item>

          <Form.Item
            label="版本"
            name="updateVersion"
            rules={[
              { required: true, message: '請輸入版本號' },
              { max: CHANGE_LOG_VALIDATION.VERSION_MAX_LENGTH, message: `版本號最多 ${CHANGE_LOG_VALIDATION.VERSION_MAX_LENGTH} 個字符` },
            ]}
          >
            <Input placeholder="請輸入版本號，例如：v1.0.0" />
          </Form.Item>

          <Form.Item
            label="更新類型"
            name="type"
            rules={[{ required: true, message: '請選擇更新類型' }]}
          >
            <SmartEnumSelect
              enumName="CHANGE_LOG_TYPE_ENUM"
              placeholder="請選擇更新類型"
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            label="發布人"
            name="publishAuthor"
            rules={[
              { required: true, message: '請輸入發布人' },
              { max: CHANGE_LOG_VALIDATION.AUTHOR_MAX_LENGTH, message: `發布人最多 ${CHANGE_LOG_VALIDATION.AUTHOR_MAX_LENGTH} 個字符` },
            ]}
          >
            <Input placeholder="請輸入發布人" />
          </Form.Item>

          <Form.Item
            label="發布日期"
            name="publicDate"
            rules={[{ required: true, message: '請選擇發布日期' }]}
          >
            <DatePicker style={{ width: '100%' }} placeholder="請選擇發布日期" />
          </Form.Item>

          <Form.Item
            label="跳轉鏈接"
            name="link"
            rules={[
              { type: 'url', message: '請輸入有效的 URL' },
              { max: CHANGE_LOG_VALIDATION.LINK_MAX_LENGTH, message: `跳轉鏈接最多 ${CHANGE_LOG_VALIDATION.LINK_MAX_LENGTH} 個字符` },
            ]}
          >
            <Input placeholder="請輸入跳轉鏈接（可選）" />
          </Form.Item>

          <Form.Item
            label="更新內容"
            name="content"
            rules={[
              { required: true, message: '請輸入更新內容' },
              { max: CHANGE_LOG_VALIDATION.CONTENT_MAX_LENGTH, message: `更新內容最多 ${CHANGE_LOG_VALIDATION.CONTENT_MAX_LENGTH} 個字符` },
            ]}
          >
            <TextArea
              rows={15}
              placeholder="請輸入更新內容，支持 Markdown 格式"
              showCount
              maxLength={CHANGE_LOG_VALIDATION.CONTENT_MAX_LENGTH}
            />
          </Form.Item>
        </Form>
      </Modal>
    );
  }
);

ChangeLogFormModal.displayName = 'ChangeLogFormModal';

export default ChangeLogFormModal;
