/**
 * Operate Log List Page
 * 操作日誌列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import React, { useEffect, useMemo, useRef } from 'react';
import { Form, Input, Button, Table, Card, DatePicker, Space, Tag, Radio, Typography } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { UAParser } from 'ua-parser-js';
import { operateLogApi } from '@/api/support/operateLogApi';
import { useTable } from '@/hooks/useTable';
import { formatDateTime } from '@/utils/date';
import {
  OPERATE_LOG_PERMISSION,
  OPERATE_LOG_TABLE_COLUMNS_WIDTH,
  SUCCESS_FLAG_ENUM,
} from '@/constants/support/operateLogConst';
import { usePrivilege } from '@/hooks/usePrivilege';
import type { OperateLogVO, OperateLogQueryForm } from './types';
import OperateLogDetailModal from './components/OperateLogDetailModal';

const { RangePicker } = DatePicker;

/**
 * 操作日誌列表組件
 */
const OperateLogList: React.FC = () => {
  const [form] = Form.useForm<OperateLogQueryForm>();
  const hasQueryPermission = usePrivilege(OPERATE_LOG_PERMISSION.QUERY);
  const hasDetailPermission = usePrivilege(OPERATE_LOG_PERMISSION.DETAIL);

  // DetailModal ref
  const detailModalRef = useRef<{ show: (id: number) => void }>(null);

  // 使用 useTable Hook
  const { tableData, loading, pagination, query, reset, setQueryForm } = useTable<
    OperateLogVO,
    OperateLogQueryForm
  >({
    defaultQueryForm: {
      searchWord: undefined,
      successFlag: undefined,
      startDate: undefined,
      endDate: undefined,
    },
    pagination: { pageNum: 1, pageSize: 10 },
    queryApi: operateLogApi.queryPage,
    autoQuery: false,
  });

  // 解析 UserAgent 並解析 response
  const parsedTableData = useMemo(() => {
    return tableData.map(log => {
      const parsed = { ...log };

      // 解析 UserAgent
      if (log.userAgent) {
        const parser = UAParser(log.userAgent);
        const browser = parser.browser;
        const os = parser.os;
        const device = parser.device;

        parsed.browser = browser?.name || '';
        parsed.os = os?.name || '';
        parsed.device = device?.vendor && device?.model ? `${device.vendor} ${device.model}` : '';
      }

      // 解析 response JSON
      if (log.response) {
        try {
          parsed.response = JSON.parse(log.response) as any;
        } catch {
          // 解析失敗保持原值
        }
      }

      return parsed;
    });
  }, [tableData]);

  // 初始化時加載數據
  useEffect(() => {
    if (hasQueryPermission) {
      query();
    }
  }, [hasQueryPermission, query]);

  // 處理搜索
  const handleSearch = () => {
    const values = form.getFieldsValue();
    setQueryForm(prev => ({
      ...prev,
      searchWord: values.searchWord,
      successFlag: values.successFlag,
      startDate: values.startDate,
      endDate: values.endDate,
      pageNum: 1,
    }));
    setTimeout(() => query(), 0);
  };

  // 處理重置
  const handleReset = () => {
    form.resetFields();
    reset();
    setTimeout(() => query(), 0);
  };

  // 處理日期範圍變化
  const handleDateRangeChange = (_dates: any, dateStrings: [string, string]) => {
    form.setFieldsValue({
      startDate: dateStrings[0],
      endDate: dateStrings[1],
    });
  };

  // 處理成功狀態變化
  const handleSuccessFlagChange = (e: any) => {
    const successFlag = e.target.value;
    form.setFieldsValue({ successFlag });
    setQueryForm(prev => ({ ...prev, successFlag, pageNum: 1 }));
    setTimeout(() => query(), 0);
  };

  // 顯示詳情
  const handleShowDetail = (operateLogId: number) => {
    detailModalRef.current?.show(operateLogId);
  };

  // 表格列定義
  const columns: ColumnsType<OperateLogVO> = [
    {
      title: '用戶',
      dataIndex: 'operateUserName',
      key: 'operateUserName',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.operateUserName,
      ellipsis: true,
    },
    {
      title: '類型',
      dataIndex: 'operateUserType',
      key: 'operateUserType',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.operateUserType,
      ellipsis: true,
      render: (userType: number) => {
        const typeMap: Record<number, { text: string; color: string }> = {
          1: { text: '員工', color: 'blue' },
          2: { text: '學生', color: 'green' },
          3: { text: '其他', color: 'default' },
        };
        const type = typeMap[userType] || { text: '未知', color: 'default' };
        return <Tag color={type.color}>{type.text}</Tag>;
      },
    },
    {
      title: '操作模塊',
      dataIndex: 'module',
      key: 'module',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.module,
      ellipsis: true,
    },
    {
      title: '操作內容',
      dataIndex: 'content',
      key: 'content',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.content,
      ellipsis: true,
    },
    {
      title: '請求路徑',
      dataIndex: 'url',
      key: 'url',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.url,
      ellipsis: true,
    },
    {
      title: '返回結果',
      dataIndex: 'response',
      key: 'response',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.response,
      ellipsis: true,
      render: (response: any) => {
        if (!response) return '-';
        if (typeof response === 'object') {
          const msg = response.msg || '-';
          const isOk = response.ok === true;
          return <Typography.Text type={isOk ? 'success' : 'warning'}>{msg}</Typography.Text>;
        }
        return response;
      },
    },
    {
      title: 'IP地區',
      dataIndex: 'ipRegion',
      key: 'ipRegion',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.ipRegion,
      ellipsis: true,
    },
    {
      title: '客戶端',
      dataIndex: 'userAgent',
      key: 'userAgent',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.userAgent,
      ellipsis: true,
      render: (_: string, record: OperateLogVO) => {
        const parts = [record.os, record.browser, record.device].filter(Boolean);
        return parts.join(' / ') || '-';
      },
    },
    {
      title: '操作時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.createTime,
      render: (createTime: string) => formatDateTime(createTime),
    },
    {
      title: '狀態',
      dataIndex: 'successFlag',
      key: 'successFlag',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.successFlag,
      render: (successFlag: number) => {
        const status =
          successFlag === SUCCESS_FLAG_ENUM.SUCCESS.value
            ? SUCCESS_FLAG_ENUM.SUCCESS
            : SUCCESS_FLAG_ENUM.FAILURE;
        return <Tag color={status.color}>{status.label}</Tag>;
      },
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      fixed: 'right',
      width: OPERATE_LOG_TABLE_COLUMNS_WIDTH.action,
      render: (_: any, record: OperateLogVO) => (
        <Button
          type="link"
          size="small"
          disabled={!hasDetailPermission}
          onClick={() => handleShowDetail(record.operateLogId)}
        >
          詳情
        </Button>
      ),
    },
  ];

  if (!hasQueryPermission) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '50px 0' }}>您沒有權限查看操作日誌列表</div>
      </Card>
    );
  }

  return (
    <div className="operate-log-list">
      {/* 搜索表單 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={form} layout="inline">
          <Form.Item name="keywords" label="操作關鍵字" style={{ marginBottom: 16 }}>
            <Input placeholder="模塊/操作內容" allowClear style={{ width: 150 }} />
          </Form.Item>

          <Form.Item name="requestKeywords" label="請求關鍵字" style={{ marginBottom: 16 }}>
            <Input
              placeholder="請求地址/請求方法/請求參數/返回結果"
              allowClear
              style={{ width: 270 }}
            />
          </Form.Item>

          <Form.Item name="userName" label="用戶名稱" style={{ marginBottom: 16 }}>
            <Input placeholder="用戶名稱" allowClear style={{ width: 100 }} />
          </Form.Item>

          <Form.Item label="請求時間" style={{ marginBottom: 16 }}>
            <RangePicker
              format="YYYY-MM-DD"
              onChange={handleDateRangeChange}
              style={{ width: 240 }}
            />
          </Form.Item>

          <Form.Item name="successFlag" label="狀態" style={{ marginBottom: 16 }}>
            <Radio.Group onChange={handleSuccessFlagChange} buttonStyle="solid">
              <Radio.Button value={undefined}>全部</Radio.Button>
              <Radio.Button value={1}>成功</Radio.Button>
              <Radio.Button value={0}>失敗</Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item style={{ marginBottom: 16 }}>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                查詢
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {/* 數據表格 */}
      <Card size="small">
        <Table
          rowKey="operateLogId"
          columns={columns}
          dataSource={parsedTableData}
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 條`,
            onChange: (page, pageSize) => {
              const values = form.getFieldsValue();
              setQueryForm(prev => ({
                ...prev,
                ...values,
                pageNum: page,
                pageSize,
              }));
              setTimeout(() => query(), 0);
            },
          }}
          size="small"
          bordered
          scroll={{ x: 1500 }}
        />
      </Card>

      {/* 詳情 Modal */}
      <OperateLogDetailModal ref={detailModalRef} />
    </div>
  );
};

export default OperateLogList;
