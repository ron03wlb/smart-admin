/**
 * Job Execute Modal
 * 立即執行任務彈窗
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/job/components/job-form-modal.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import React, { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, Alert, message } from 'antd';
import { jobApi } from '@/api/support/jobApi';
import { JOB_VALIDATION } from '@/constants/support/jobConst';
import type { JobVO, JobExecuteForm } from '../types';

const { TextArea } = Input;

interface JobExecuteModalProps {
  onSuccess?: () => void;
}

const JobExecuteModal = forwardRef<{ show: (rowData: JobVO) => void }, JobExecuteModalProps>(
  ({ onSuccess }, ref) => {
    const [form] = Form.useForm();
    const [visible, setVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [jobData, setJobData] = useState<JobVO | null>(null);

    useImperativeHandle(ref, () => ({
      show: (rowData: JobVO) => {
        setJobData(rowData);
        form.setFieldsValue({
          jobName: rowData.jobName,
          jobClass: rowData.jobClass,
          param: rowData.param || '',
        });
        setVisible(true);
      },
    }));

    const handleOk = async () => {
      if (!jobData) return;

      try {
        setLoading(true);
        const values = form.getFieldsValue();

        const executeForm: JobExecuteForm = {
          jobId: jobData.jobId,
          updateName: undefined, // 後端會自動取得當前用戶
        };

        await jobApi.executeJob(executeForm);

        // 延遲 2 秒後再提示刷新（讓任務有時間執行）
        await new Promise((resolve) => setTimeout(resolve, 2000));

        message.success('執行成功');
        setVisible(false);
        form.resetFields();
        onSuccess?.();
      } catch (error) {
        console.error('執行任務失敗:', error);
      } finally {
        setLoading(false);
      }
    };

    const handleCancel = () => {
      setVisible(false);
      form.resetFields();
    };

    return (
      <Modal
        title="執行任務"
        open={visible}
        onOk={handleOk}
        onCancel={handleCancel}
        confirmLoading={loading}
        width={650}
        okText="執行"
        cancelText="取消"
      >
        <br />
        <Alert
          type="info"
          showIcon
          message="點擊【執行】後會按照【任務參數】，無論任務是否開啟，都會立即執行。"
          style={{ marginLeft: 25 }}
        />
        <br />
        <Form form={form} labelCol={{ span: 4 }}>
          <Form.Item label="任務名稱" name="jobName">
            <Input disabled />
          </Form.Item>

          <Form.Item label="任務類名" name="jobClass">
            <TextArea disabled autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>

          <Form.Item label="任務參數" name="param">
            <TextArea
              placeholder="（可選）請輸入任務執行參數"
              maxLength={JOB_VALIDATION.PARAM_MAX_LENGTH}
              showCount
              autoSize={{ minRows: 3, maxRows: 6 }}
            />
          </Form.Item>
        </Form>
      </Modal>
    );
  }
);

JobExecuteModal.displayName = 'JobExecuteModal';

export default JobExecuteModal;
