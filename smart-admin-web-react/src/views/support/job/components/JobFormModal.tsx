/**
 * Job Form Modal
 * 定時任務表單彈窗（新增/編輯）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/job/components/job-form-modal.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Form, Input, InputNumber, Switch, Radio, message } from 'antd';
import { jobApi } from '@/api/support/jobApi';
import { JOB_VALIDATION, JOB_TRIGGER_TYPE_LABELS } from '@/constants/support/jobConst';
import type { JobVO, JobFormData, JobAddForm, JobUpdateForm } from '../types';
import { JobTriggerTypeEnum } from '../types';

const { TextArea } = Input;

interface JobFormModalProps {
  onSuccess?: () => void;
}

const JobFormModal = forwardRef<{ show: (rowData?: JobVO) => void }, JobFormModalProps>(
  ({ onSuccess }, ref) => {
    const [form] = Form.useForm<JobFormData>();
    const [visible, setVisible] = useState(false);
    const [isEdit, setIsEdit] = useState(false);
    const [loading, setLoading] = useState(false);
    const [triggerType, setTriggerType] = useState<string>(JobTriggerTypeEnum.CRON);

    useImperativeHandle(ref, () => ({
      show: (rowData?: JobVO) => {
        setIsEdit(!!rowData?.jobId);
        if (rowData) {
          // 編輯模式：回填數據
          const formData: JobFormData = {
            jobId: rowData.jobId,
            jobName: rowData.jobName,
            jobClass: rowData.jobClass,
            triggerType: rowData.triggerType,
            triggerValue: rowData.triggerValue,
            param: rowData.param,
            enabledFlag: rowData.enabledFlag,
            remark: rowData.remark,
            sort: rowData.sort,
          };
          form.setFieldsValue(formData);
          setTriggerType(rowData.triggerType);
        } else {
          // 新增模式：初始化表單
          form.resetFields();
          setTriggerType(JobTriggerTypeEnum.CRON);
        }
        setVisible(true);
      },
    }));

    const handleOk = async () => {
      try {
        const values = await form.validateFields();

        // 驗證觸發時間是否已填寫
        if (!values.triggerValue) {
          message.error('請填寫觸發時間');
          return;
        }

        setLoading(true);

        if (isEdit && values.jobId) {
          // 編輯
          const updateForm: JobUpdateForm = {
            jobId: values.jobId,
            jobName: values.jobName,
            jobClass: values.jobClass,
            triggerType: values.triggerType,
            triggerValue: values.triggerValue,
            param: values.param,
            enabledFlag: values.enabledFlag,
            remark: values.remark,
            sort: values.sort,
          };
          await jobApi.updateJob(updateForm);
          message.success('更新成功');
        } else {
          // 新增
          const addForm: JobAddForm = {
            jobName: values.jobName,
            jobClass: values.jobClass,
            triggerType: values.triggerType,
            triggerValue: values.triggerValue,
            param: values.param,
            enabledFlag: values.enabledFlag,
            remark: values.remark,
            sort: values.sort,
          };
          await jobApi.addJob(addForm);
          message.success('添加成功');
        }

        setVisible(false);
        form.resetFields();
        onSuccess?.();
      } catch (error) {
        console.error('表單驗證失敗:', error);
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
        title={isEdit ? '編輯' : '添加'}
        open={visible}
        onOk={handleOk}
        onCancel={handleCancel}
        confirmLoading={loading}
        width={650}
        okText="確認"
        cancelText="取消"
      >
        <Form form={form} labelCol={{ span: 4 }} initialValues={{ enabledFlag: false }}>
          <Form.Item name="jobId" hidden>
            <Input />
          </Form.Item>

          <Form.Item
            label="任務名稱"
            name="jobName"
            rules={[{ required: true, message: '請輸入任務名稱' }]}
          >
            <Input
              placeholder="請輸入任務名稱"
              maxLength={JOB_VALIDATION.NAME_MAX_LENGTH}
              showCount
            />
          </Form.Item>

          <Form.Item label="任務描述" name="remark">
            <TextArea
              placeholder="（可選）請輸入任務備註描述"
              maxLength={JOB_VALIDATION.REMARK_MAX_LENGTH}
              showCount
              autoSize={{ minRows: 2, maxRows: 4 }}
            />
          </Form.Item>

          <Form.Item label="排序" name="sort" rules={[{ required: true, message: '請輸入排序' }]}>
            <InputNumber
              placeholder="值越小越靠前"
              min={-99999999}
              max={99999999}
              precision={0}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            label="執行類"
            name="jobClass"
            rules={[{ required: true, message: '請輸入執行類' }]}
          >
            <TextArea
              placeholder="示例：net.lab1024.sa.base.module.support.job.sample.SmartJobSample1"
              maxLength={JOB_VALIDATION.CLASS_MAX_LENGTH}
              showCount
              autoSize={{ minRows: 2, maxRows: 4 }}
            />
          </Form.Item>

          <Form.Item label="任務參數" name="param">
            <TextArea
              placeholder="（可選）請輸入任務執行參數"
              maxLength={JOB_VALIDATION.PARAM_MAX_LENGTH}
              showCount
              autoSize={{ minRows: 3, maxRows: 6 }}
            />
          </Form.Item>

          <Form.Item
            label="觸發類型"
            name="triggerType"
            rules={[{ required: true, message: '請選擇觸發類型' }]}
          >
            <Radio.Group onChange={e => setTriggerType(e.target.value)}>
              <Radio.Button value={JobTriggerTypeEnum.CRON}>
                {JOB_TRIGGER_TYPE_LABELS.CRON}
              </Radio.Button>
              <Radio.Button value={JobTriggerTypeEnum.FIXED_DELAY}>
                {JOB_TRIGGER_TYPE_LABELS.FIXED_DELAY}
              </Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item label="觸發時間" name="triggerValue">
            {triggerType === JobTriggerTypeEnum.CRON ? (
              <Input
                placeholder="示例：10 15 0/1 * * *"
                maxLength={JOB_VALIDATION.TRIGGER_VALUE_MAX_LENGTH}
                showCount
              />
            ) : (
              <InputNumber
                placeholder="秒"
                min={1}
                max={100000000}
                precision={0}
                addonBefore="每隔"
                addonAfter="秒"
                style={{ width: '100%' }}
              />
            )}
          </Form.Item>

          <Form.Item label="是否開啟" name="enabledFlag" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    );
  }
);

JobFormModal.displayName = 'JobFormModal';

export default JobFormModal;
