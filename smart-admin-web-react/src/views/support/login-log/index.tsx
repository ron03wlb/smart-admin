/**
 * Login Log List Page
 * 登錄日誌列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import React, { useEffect, useMemo } from 'react';
import { Form, Input, Button, Table, Card, DatePicker, Space, Tag } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import UAParser from 'ua-parser-js';
import { loginLogApi } from '@/api/support/loginLogApi';
import { useTable } from '@/hooks/useTable';
import { formatDateTime } from '@/utils/date';
import {
  LOGIN_LOG_PERMISSION,
  LOGIN_LOG_TABLE_COLUMNS_WIDTH,
  LOGIN_RESULT_ENUM,
} from '@/constants/support/loginLogConst';
import { usePrivilege } from '@/hooks/usePrivilege';
import type { LoginLogVO, LoginLogQueryForm } from './types';

const { RangePicker } = DatePicker;

/**
 * 登錄日誌列表組件
 */
const LoginLogList: React.FC = () => {
  const [form] = Form.useForm<LoginLogQueryForm>();
  const hasQueryPermission = usePrivilege(LOGIN_LOG_PERMISSION.QUERY);

  // 使用 useTable Hook
  const {
    data: tableData,
    loading: tableLoading,
    pagination,
    handleTableChange,
    refreshTable,
  } = useTable<LoginLogVO, LoginLogQueryForm>(
    loginLogApi.queryPage,
    form,
  );

  // 解析 UserAgent 並添加到數據中
  const parsedTableData = useMemo(() => {
    return tableData.map((log) => {
      if (!log.userAgent) {
        return { ...log, browser: '', os: '', device: '' };
      }

      const parser = new UAParser(log.userAgent);
      const browser = parser.getBrowser();
      const os = parser.getOS();
      const device = parser.getDevice();

      return {
        ...log,
        browser: browser.name || '',
        os: os.name || '',
        device: device.vendor && device.model ? `${device.vendor} ${device.model}` : '',
      };
    });
  }, [tableData]);

  // 初始化時加載數據
  useEffect(() => {
    if (hasQueryPermission) {
      refreshTable();
    }
  }, [hasQueryPermission, refreshTable]);

  // 處理搜索
  const handleSearch = () => {
    refreshTable();
  };

  // 處理重置
  const handleReset = () => {
    form.resetFields();
    refreshTable();
  };

  // 處理日期範圍變化
  const handleDateRangeChange = (_dates: any, dateStrings: [string, string]) => {
    form.setFieldsValue({
      startDate: dateStrings[0],
      endDate: dateStrings[1],
    });
  };

  // 表格列定義
  const columns: ColumnsType<LoginLogVO> = [
    {
      title: '用戶ID',
      dataIndex: 'userId',
      key: 'userId',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.userId,
    },
    {
      title: '用戶名',
      dataIndex: 'userName',
      key: 'userName',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.userName,
      ellipsis: true,
    },
    {
      title: '類型',
      dataIndex: 'userType',
      key: 'userType',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.userType,
      render: (userType: number) => {
        // UserTypeEnum: 1-員工, 2-學生, 3-其他
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
      title: 'IP',
      dataIndex: 'loginIp',
      key: 'loginIp',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.loginIp,
      ellipsis: true,
    },
    {
      title: 'IP地區',
      dataIndex: 'loginIpRegion',
      key: 'loginIpRegion',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.loginIpRegion,
      ellipsis: true,
    },
    {
      title: '設備信息',
      dataIndex: 'userAgent',
      key: 'userAgent',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.userAgent,
      ellipsis: true,
      render: (_: string, record: LoginLogVO & { browser?: string; os?: string; device?: string }) => (
        <div>{`${record.browser || ''} / ${record.os || ''} / ${record.device || ''}`}</div>
      ),
    },
    {
      title: '結果',
      dataIndex: 'loginResult',
      key: 'loginResult',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.loginResult,
      render: (loginResult: number) => {
        const resultMap = {
          [LOGIN_RESULT_ENUM.LOGIN_SUCCESS.value]: LOGIN_RESULT_ENUM.LOGIN_SUCCESS,
          [LOGIN_RESULT_ENUM.LOGIN_FAIL.value]: LOGIN_RESULT_ENUM.LOGIN_FAIL,
          [LOGIN_RESULT_ENUM.LOGIN_OUT.value]: LOGIN_RESULT_ENUM.LOGIN_OUT,
        };
        const result = resultMap[loginResult];
        if (!result) return '-';
        return <Tag color={result.color}>{result.label}</Tag>;
      },
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
    },
    {
      title: '時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: LOGIN_LOG_TABLE_COLUMNS_WIDTH.createTime,
      render: (createTime: string) => formatDateTime(createTime),
    },
  ];

  if (!hasQueryPermission) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          您沒有權限查看登錄日誌列表
        </div>
      </Card>
    );
  }

  return (
    <div className="login-log-list">
      {/* 搜索表單 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={form} layout="inline">
          <Form.Item
            name="userName"
            label="用戶名稱"
            style={{ marginBottom: 16 }}
          >
            <Input
              placeholder="用戶名稱"
              allowClear
              style={{ width: 200 }}
            />
          </Form.Item>

          <Form.Item
            name="ip"
            label="用戶IP"
            style={{ marginBottom: 16 }}
          >
            <Input
              placeholder="IP"
              allowClear
              style={{ width: 150 }}
            />
          </Form.Item>

          <Form.Item
            label="時間"
            style={{ marginBottom: 16 }}
          >
            <RangePicker
              format="YYYY-MM-DD"
              onChange={handleDateRangeChange}
              style={{ width: 240 }}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 16 }}>
            <Space>
              <Button
                type="primary"
                icon={<SearchOutlined />}
                onClick={handleSearch}
              >
                查詢
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleReset}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {/* 數據表格 */}
      <Card size="small">
        <Table
          rowKey="loginLogId"
          columns={columns}
          dataSource={parsedTableData}
          loading={tableLoading}
          pagination={pagination}
          onChange={handleTableChange}
          size="small"
          bordered
        />
      </Card>
    </div>
  );
};

export default LoginLogList;
