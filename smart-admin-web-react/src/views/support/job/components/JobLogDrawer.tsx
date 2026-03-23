/**
 * Job Log Drawer
 * 任務執行記錄Drawer
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/job/components/job-log-list-modal.vue
 * 參考：DictDataDrawer.tsx (主從表關聯模式)
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-16
 */

import { useState, useEffect } from 'react';
import { Drawer, Table, Form, Input, Select, DatePicker, Button, Space, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined, CheckOutlined, WarningOutlined } from '@ant-design/icons';
import { jobApi } from '@/api/support/jobApi';
import type { JobLogVO, JobLogQueryForm } from '../types';
import type { PageResult } from '@/api/types/response';

const { RangePicker } = DatePicker;

/**
 * JobLogDrawer Props
 */
export interface JobLogDrawerProps {
  /** Drawer 可見性 */
  visible: boolean;
  /** 任務 ID */
  jobId?: number;
  /** 任務名稱 */
  jobName?: string;
  /** 關閉回調 */
  onClose: () => void;
}

/**
 * 執行記錄列寬度配置
 */
const JOB_LOG_TABLE_COLUMNS_WIDTH = {
  createName: 100,
  param: 80,
  executeStartTime: 200,
  executeTimeMillis: 100,
  successFlag: 80,
  executeResult: 100,
  ip: 110,
  processId: 60,
  programPath: 150,
} as const;

/**
 * Job Log Drawer Component
 */
const JobLogDrawer: React.FC<JobLogDrawerProps> = ({ visible, jobId, jobName, onClose }) => {
  const [form] = Form.useForm();

  // 表格數據狀態
  const [tableData, setTableData] = useState<JobLogVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  // 查詢表單狀態
  const [queryForm, setQueryForm] = useState<Partial<JobLogQueryForm>>({
    searchWord: undefined,
    successFlag: undefined,
    startTime: undefined,
    endTime: undefined,
  });

  /**
   * 查詢執行記錄
   */
  const queryJobLog = async () => {
    if (!jobId) {
      message.warning('任務ID不能為空');
      return;
    }

    setLoading(true);
    try {
      const params: JobLogQueryForm = {
        jobId,
        searchWord: queryForm.searchWord,
        successFlag: queryForm.successFlag,
        startTime: queryForm.startTime,
        endTime: queryForm.endTime,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      };

      const response = await jobApi.queryJobLog(params);
      if (response.ok && response.data) {
        const result: PageResult<JobLogVO> = response.data;
        setTableData(result.list || []);
        setPagination({
          ...pagination,
          total: result.total || 0,
        });
      } else {
        message.error(response.msg || '查詢執行記錄失敗');
      }
    } catch (error) {
      message.error('查詢執行記錄失敗');
      console.error('Query job log error:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 重置查詢表單
   */
  const handleReset = () => {
    form.resetFields();
    setQueryForm({
      searchWord: undefined,
      successFlag: undefined,
      startTime: undefined,
      endTime: undefined,
    });
    setPagination({
      ...pagination,
      current: 1,
    });
  };

  /**
   * 分頁變化處理
   */
  const handleTableChange = (newPagination: any) => {
    setPagination({
      current: newPagination.current,
      pageSize: newPagination.pageSize,
      total: pagination.total,
    });
  };

  /**
   * 當 visible 或 jobId 變化時，重新查詢
   */
  useEffect(() => {
    if (visible && jobId) {
      queryJobLog();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, jobId, pagination.current, pagination.pageSize]);

  /**
   * 當查詢條件變化時，重置到第一頁並查詢
   */
  useEffect(() => {
    if (visible && jobId && pagination.current === 1) {
      queryJobLog();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryForm]);

  /**
   * 表格列定義
   */
  const columns: ColumnsType<JobLogVO> = [
    {
      title: '執行人',
      dataIndex: 'createName',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.createName,
      ellipsis: true,
    },
    {
      title: '執行參數',
      dataIndex: 'param',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.param,
      ellipsis: true,
      render: (text: string) => text || '-',
    },
    {
      title: '執行時間',
      dataIndex: 'executeStartTime',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.executeStartTime,
      render: (_, record) => (
        <>
          <div>
            <Tag color="green">始</Tag>
            {record.executeStartTime || '-'}
          </div>
          <div style={{ marginTop: 5 }}>
            <Tag color="blue">終</Tag>
            {record.executeEndTime || '-'}
          </div>
        </>
      ),
    },
    {
      title: '執行用時',
      dataIndex: 'executeTimeMillis',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.executeTimeMillis,
      render: (value: number) => (value !== undefined ? `${value} ms` : '-'),
    },
    {
      title: '結果',
      dataIndex: 'successFlag',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.successFlag,
      render: (value: number) =>
        value === 1 ? (
          <div style={{ color: '#39c710' }}>
            <CheckOutlined /> 成功
          </div>
        ) : (
          <div style={{ color: '#f50' }}>
            <WarningOutlined /> 失敗
          </div>
        ),
    },
    {
      title: '執行結果',
      dataIndex: 'executeResult',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.executeResult,
      ellipsis: true,
      render: (text: string) => text || '-',
    },
    {
      title: 'IP',
      dataIndex: 'ip',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.ip,
      render: (text: string) => text || '-',
    },
    {
      title: '進程ID',
      dataIndex: 'processId',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.processId,
      render: (text: string) => text || '-',
    },
    {
      title: '程序目錄',
      dataIndex: 'programPath',
      width: JOB_LOG_TABLE_COLUMNS_WIDTH.programPath,
      ellipsis: true,
      render: (text: string) => text || '-',
    },
  ];

  return (
    <Drawer
      title={`執行記錄 - ${jobName || ''}`}
      open={visible}
      width={1000}
      onClose={onClose}
      destroyOnClose
    >
      {/* 查詢表單 */}
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={queryJobLog}>
        <Form.Item name="searchWord" label="關鍵字">
          <Input
            placeholder="請輸入關鍵字"
            value={queryForm.searchWord}
            onChange={e => setQueryForm({ ...queryForm, searchWord: e.target.value })}
            maxLength={30}
            allowClear
            style={{ width: 200 }}
          />
        </Form.Item>

        <Form.Item name="successFlag" label="執行結果">
          <Select
            placeholder="請選擇"
            value={queryForm.successFlag}
            onChange={value => setQueryForm({ ...queryForm, successFlag: value })}
            allowClear
            style={{ width: 120 }}
          >
            <Select.Option value={1}>成功</Select.Option>
            <Select.Option value={0}>失敗</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item name="dateRange" label="執行時間">
          <RangePicker
            onChange={(_, dateStrings) => {
              setQueryForm({
                ...queryForm,
                startTime: dateStrings[0] || undefined,
                endTime: dateStrings[1] || undefined,
              });
            }}
            style={{ width: 260 }}
          />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" icon={<SearchOutlined />} onClick={queryJobLog}>
              查詢
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      {/* 數據表格 */}
      <Table
        rowKey="logId"
        columns={columns}
        dataSource={tableData}
        loading={loading}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: total => `共 ${total} 條`,
        }}
        onChange={handleTableChange}
        scroll={{ x: 1200 }}
        size="small"
        bordered
      />
    </Drawer>
  );
};

export default JobLogDrawer;
