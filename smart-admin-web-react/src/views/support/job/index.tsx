/**
 * Job Management Page
 * 定時任務管理頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/job/job-list.vue
 * 後端接口：AdminSmartJobController.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import React, { useRef, useState } from 'react';
import {
  Form,
  Input,
  Select,
  Button,
  Table,
  Space,
  Tag,
  Switch,
  Modal,
  message,
  Tooltip,
  Tabs,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  CheckOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useTable } from '@/hooks/useTable';
import { usePrivilege } from '@/hooks/usePrivilege';
import { jobApi } from '@/api/support/jobApi';
import {
  JOB_PERMISSION,
  JOB_TRIGGER_TYPE_LABELS,
  JOB_TRIGGER_TYPE_COLORS,
  JOB_TABLE_COLUMNS_WIDTH,
} from '@/constants/support/jobConst';
import type { JobVO, JobQueryForm, JobEnabledUpdateForm } from './types';
import JobFormModal from './components/JobFormModal';
import JobExecuteModal from './components/JobExecuteModal';
import JobLogDrawer from './components/JobLogDrawer';

const JobManagement: React.FC = () => {
  const [form] = Form.useForm();
  const formModalRef = useRef<{ show: (rowData?: JobVO) => void }>(null);
  const executeModalRef = useRef<{ show: (rowData: JobVO) => void }>(null);
  const hasQueryPrivilege = usePrivilege(JOB_PERMISSION.QUERY);
  const hasAddPrivilege = usePrivilege(JOB_PERMISSION.ADD);
  const hasUpdatePrivilege = usePrivilege(JOB_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(JOB_PERMISSION.DELETE);
  const hasExecutePrivilege = usePrivilege(JOB_PERMISSION.EXECUTE);
  const hasUpdateEnabledPrivilege = usePrivilege(JOB_PERMISSION.UPDATE_ENABLED);
  const hasLogQueryPrivilege = usePrivilege(JOB_PERMISSION.LOG_QUERY);

  // Tab狀態
  const [activeTab, setActiveTab] = useState<'active' | 'deleted'>('active');

  // 執行記錄Drawer狀態
  const [logDrawerVisible, setLogDrawerVisible] = useState(false);
  const [currentJob, setCurrentJob] = useState<{ jobId: number; jobName: string } | undefined>();

  const { tableData, loading, pagination, query, reset, setQueryForm } = useTable<JobVO, JobQueryForm>({
    defaultQueryForm: {
      searchWord: undefined,
      triggerType: undefined,
      enabledFlag: undefined,
      deletedFlag: false,
    },
    pagination: { pageNum: 1, pageSize: 10 },
    queryApi: jobApi.queryJob,
    autoQuery: true,
  });

  // 處理執行類簡化顯示（只顯示類名最後部分）
  const handleJobClass = (jobClass: string): string => {
    const parts = jobClass.split('.');
    return parts[parts.length - 1];
  };

  // 處理執行結果顯示（截取前 400 字符）
  const handleExecuteResult = (result?: string): string => {
    if (!result) return '';
    const maxLength = 400;
    return result.length > maxLength ? result.substring(0, maxLength) + ' ...' : result;
  };

  // 處理啟用狀態切換
  const handleEnabledUpdate = async (checked: boolean, record: JobVO) => {
    try {
      const updateForm: JobEnabledUpdateForm = {
        jobId: record.jobId,
        enabledFlag: checked,
      };
      await jobApi.updateJobEnabled(updateForm);

      // 重新查詢任務詳情以獲取最新的 nextJobExecuteTimeList
      const res = await jobApi.queryJobInfo(record.jobId);
      if (res.ok && res.data) {
        query();
      }

      message.success('更新成功');
    } catch (error) {
      message.error('更新失敗');
      query();
    }
  };

  // 處理刪除
  const handleDelete = (record: JobVO) => {
    Modal.confirm({
      title: '警告',
      content: `確定要刪除【${record.jobName}】任務嗎？`,
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await jobApi.deleteJob(record.jobId);
        message.success('刪除成功');
        query();
      },
    });
  };

  // 處理查詢
  const handleSearch = () => {
    const values = form.getFieldsValue();
    const deletedFlag = activeTab === 'deleted';
    setQueryForm((prev) => ({
      ...prev,
      searchWord: values.searchWord,
      triggerType: values.triggerType,
      enabledFlag: values.enabledFlag,
      deletedFlag,
      pageNum: 1,
    }));
    setTimeout(() => query(), 0);
  };

  // 處理重置
  const handleReset = () => {
    form.resetFields();
    const deletedFlag = activeTab === 'deleted';
    reset();
    setQueryForm((prev) => ({ ...prev, deletedFlag }));
    setTimeout(() => query(), 0);
  };

  // 處理查看執行記錄
  const handleViewLog = (record: JobVO) => {
    setCurrentJob({ jobId: record.jobId, jobName: record.jobName });
    setLogDrawerVisible(true);
  };

  // 處理Tab切換
  const handleTabChange = (key: string) => {
    setActiveTab(key as 'active' | 'deleted');
    form.resetFields();
    const deletedFlag = key === 'deleted';
    reset();
    setQueryForm((prev) => ({ ...prev, deletedFlag }));
    setTimeout(() => query(), 0);
  };

  // 處理新增
  const handleAdd = () => {
    formModalRef.current?.show();
  };

  // 處理編輯
  const handleEdit = (record: JobVO) => {
    formModalRef.current?.show(record);
  };

  // 處理立即執行
  const handleExecute = (record: JobVO) => {
    executeModalRef.current?.show(record);
  };

  // 表格列配置
  const columns: ColumnsType<JobVO> = [
    {
      title: 'ID',
      dataIndex: 'jobId',
      width: JOB_TABLE_COLUMNS_WIDTH.jobId,
    },
    {
      title: '任務名稱',
      dataIndex: 'jobName',
      width: JOB_TABLE_COLUMNS_WIDTH.jobName,
      ellipsis: true,
    },
    {
      title: '執行類',
      dataIndex: 'jobClass',
      width: JOB_TABLE_COLUMNS_WIDTH.jobClass,
      ellipsis: true,
      render: (text: string) => (
        <Tooltip title={text}>
          <span>{handleJobClass(text)}</span>
        </Tooltip>
      ),
    },
    {
      title: '觸發類型',
      dataIndex: 'triggerType',
      width: JOB_TABLE_COLUMNS_WIDTH.triggerType,
      render: (text: string) => {
        const color =
          JOB_TRIGGER_TYPE_COLORS[text as keyof typeof JOB_TRIGGER_TYPE_COLORS] || 'default';
        const label =
          JOB_TRIGGER_TYPE_LABELS[text as keyof typeof JOB_TRIGGER_TYPE_LABELS] || text;
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: '觸發配置',
      dataIndex: 'triggerValue',
      width: JOB_TABLE_COLUMNS_WIDTH.triggerValue,
    },
    {
      title: '上次執行',
      dataIndex: 'lastJob',
      width: JOB_TABLE_COLUMNS_WIDTH.lastJob,
      render: (_, record) => {
        if (!record.lastJobLog) return null;
        const { successFlag, executeStartTime, executeResult } = record.lastJobLog;
        return (
          <Tooltip title={handleExecuteResult(executeResult)}>
            <Space>
              {successFlag ? (
                <CheckOutlined style={{ color: '#39c710' }} />
              ) : (
                <WarningOutlined style={{ color: '#f50' }} />
              )}
              <span>{executeStartTime}</span>
            </Space>
          </Tooltip>
        );
      },
    },
    {
      title: '下次執行',
      dataIndex: 'nextJob',
      width: JOB_TABLE_COLUMNS_WIDTH.nextJob,
      render: (_, record) => {
        if (!record.enabledFlag || !record.nextJobExecuteTimeList?.length) return null;
        return (
          <Tooltip
            title={
              <>
                <div>下次執行（預估時間）</div>
                {record.nextJobExecuteTimeList.map((time) => (
                  <div key={time}>{time}</div>
                ))}
              </>
            }
          >
            <span>{record.nextJobExecuteTimeList[0]}</span>
          </Tooltip>
        );
      },
    },
    {
      title: '啟用狀態',
      dataIndex: 'enabledFlag',
      width: JOB_TABLE_COLUMNS_WIDTH.enabledFlag,
      render: (value: boolean, record) => (
        <Switch
          checked={value}
          checkedChildren="已啟用"
          unCheckedChildren="已禁用"
          onChange={(checked) => handleEnabledUpdate(checked, record)}
          loading={record.enabledLoading}
          disabled={!hasUpdateEnabledPrivilege}
        />
      ),
    },
    {
      title: '執行參數',
      dataIndex: 'param',
      width: JOB_TABLE_COLUMNS_WIDTH.param,
      ellipsis: true,
    },
    {
      title: '任務描述',
      dataIndex: 'remark',
      width: JOB_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: JOB_TABLE_COLUMNS_WIDTH.sort,
    },
    {
      title: '更新人',
      dataIndex: 'updateName',
      width: JOB_TABLE_COLUMNS_WIDTH.updateName,
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      width: JOB_TABLE_COLUMNS_WIDTH.updateTime,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 260,
      render: (_, record) => (
        <Space size="small">
          {hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              編輯
            </Button>
          )}
          {hasExecutePrivilege && (
            <Button type="link" size="small" onClick={() => handleExecute(record)}>
              執行
            </Button>
          )}
          {hasLogQueryPrivilege && (
            <Button type="link" size="small" onClick={() => handleViewLog(record)}>
              執行記錄
            </Button>
          )}
          {hasDeletePrivilege && (
            <Button type="link" size="small" danger onClick={() => handleDelete(record)}>
              刪除
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="job-management">
      <Tabs activeKey={activeTab} onChange={handleTabChange}>
        <Tabs.TabPane tab="有效任務" key="active">
          <div className="search-form">
            <Form form={form} layout="inline">
              <Form.Item label="關鍵字" name="searchWord">
                <Input placeholder="請輸入關鍵字" style={{ width: 200 }} maxLength={30} />
              </Form.Item>
              <Form.Item label="觸發類型" name="triggerType">
                <Select placeholder="請選擇觸發類型" allowClear style={{ width: 155 }}>
                  {Object.entries(JOB_TRIGGER_TYPE_LABELS).map(([value, label]) => (
                    <Select.Option key={value} value={value}>
                      {label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item label="狀態" name="enabledFlag">
                <Select placeholder="請選擇狀態" allowClear style={{ width: 150 }}>
                  <Select.Option value={true}>開啟</Select.Option>
                  <Select.Option value={false}>停止</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item>
                <Space>
                  {hasQueryPrivilege && (
                    <>
                      <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                        查詢
                      </Button>
                      <Button icon={<ReloadOutlined />} onClick={handleReset}>
                        重置
                      </Button>
                    </>
                  )}
                  {hasAddPrivilege && (
                    <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                      添加任務
                    </Button>
                  )}
                </Space>
              </Form.Item>
            </Form>
          </div>

          <Table
            rowKey="jobId"
            columns={columns}
            dataSource={tableData}
            loading={loading}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 條`,
              onChange: (page, pageSize) => {
                const values = form.getFieldsValue();
                const deletedFlag = activeTab === 'deleted';
                setQueryForm((prev) => ({
                  ...prev,
                  ...values,
                  deletedFlag,
                  pageNum: page,
                  pageSize,
                }));
                setTimeout(() => query(), 0);
              },
            }}
            scroll={{ x: 1800 }}
            size="small"
            bordered
          />
        </Tabs.TabPane>

        <Tabs.TabPane tab="已刪除任務" key="deleted">
          <div className="search-form">
            <Form form={form} layout="inline">
              <Form.Item label="關鍵字" name="searchWord">
                <Input placeholder="請輸入關鍵字" style={{ width: 200 }} maxLength={30} />
              </Form.Item>
              <Form.Item label="觸發類型" name="triggerType">
                <Select placeholder="請選擇觸發類型" allowClear style={{ width: 155 }}>
                  {Object.entries(JOB_TRIGGER_TYPE_LABELS).map(([value, label]) => (
                    <Select.Option key={value} value={value}>
                      {label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item label="狀態" name="enabledFlag">
                <Select placeholder="請選擇狀態" allowClear style={{ width: 150 }}>
                  <Select.Option value={true}>開啟</Select.Option>
                  <Select.Option value={false}>停止</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item>
                <Space>
                  {hasQueryPrivilege && (
                    <>
                      <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                        查詢
                      </Button>
                      <Button icon={<ReloadOutlined />} onClick={handleReset}>
                        重置
                      </Button>
                    </>
                  )}
                </Space>
              </Form.Item>
            </Form>
          </div>

          <Table
            rowKey="jobId"
            columns={columns}
            dataSource={tableData}
            loading={loading}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 條`,
              onChange: (page, pageSize) => {
                const values = form.getFieldsValue();
                const deletedFlag = activeTab === 'deleted';
                setQueryForm((prev) => ({
                  ...prev,
                  ...values,
                  deletedFlag,
                  pageNum: page,
                  pageSize,
                }));
                setTimeout(() => query(), 0);
              },
            }}
            scroll={{ x: 1800 }}
            size="small"
            bordered
          />
        </Tabs.TabPane>
      </Tabs>

      <JobFormModal ref={formModalRef} onSuccess={query} />
      <JobExecuteModal ref={executeModalRef} onSuccess={query} />
      <JobLogDrawer
        visible={logDrawerVisible}
        jobId={currentJob?.jobId}
        jobName={currentJob?.jobName}
        onClose={() => setLogDrawerVisible(false)}
      />
    </div>
  );
};

export default JobManagement;
