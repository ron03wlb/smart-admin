/**
 * Login Fail List Page
 * 登錄失敗列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import React, { useEffect, useState } from 'react';
import {
  Form,
  Input,
  Button,
  Table,
  Card,
  DatePicker,
  Space,
  Tag,
  Radio,
  Modal,
  message,
} from 'antd';
import type { TableProps } from 'antd';
import { SearchOutlined, ReloadOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { loginFailApi } from '@/api/support/loginFailApi';
import { useTable } from '@/hooks/useTable';
import { formatDateTime } from '@/utils/date';
import {
  LOGIN_FAIL_PERMISSION,
  LOGIN_FAIL_TABLE_COLUMNS_WIDTH,
  LOCK_FLAG_ENUM,
} from '@/constants/support/loginFailConst';
import { usePrivilege } from '@/hooks/usePrivilege';
import type { LoginFailVO, LoginFailQueryForm } from './types';

const { RangePicker } = DatePicker;

/**
 * 登錄失敗列表組件
 */
const LoginFailList: React.FC = () => {
  const [form] = Form.useForm<LoginFailQueryForm>();
  const hasQueryPermission = usePrivilege(LOGIN_FAIL_PERMISSION.QUERY);

  // 批量選擇狀態
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // 使用 useTable Hook（默認查詢已鎖定記錄）
  const { tableData, loading, pagination, query, reset, setQueryForm } = useTable<
    LoginFailVO,
    LoginFailQueryForm
  >({
    defaultQueryForm: {
      loginName: undefined,
      lockFlag: 1,
      loginLockBeginTimeBegin: undefined,
      loginLockBeginTimeEnd: undefined,
    },
    pagination: { pageNum: 1, pageSize: 10 },
    queryApi: loginFailApi.queryPage,
    autoQuery: false,
  });

  // 初始化時加載數據
  useEffect(() => {
    if (hasQueryPermission) {
      form.setFieldsValue({ lockFlag: 1 }); // 設置默認值
      query();
    }
  }, [hasQueryPermission, query, form]);

  // 處理搜索
  const handleSearch = () => {
    const values = form.getFieldsValue();
    setQueryForm(prev => ({
      ...prev,
      loginName: values.loginName,
      lockFlag: values.lockFlag,
      loginLockBeginTimeBegin: values.loginLockBeginTimeBegin,
      loginLockBeginTimeEnd: values.loginLockBeginTimeEnd,
      pageNum: 1,
    }));
    setTimeout(() => query(), 0);
  };

  // 處理重置
  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({ lockFlag: undefined }); // 重置為全部
    reset();
    setTimeout(() => query(), 0);
  };

  // 處理日期範圍變化
  const handleDateRangeChange = (_dates: any, dateStrings: [string, string]) => {
    form.setFieldsValue({
      loginLockBeginTimeBegin: dateStrings[0],
      loginLockBeginTimeEnd: dateStrings[1],
    });
  };

  // 處理鎖定狀態變化
  const handleLockFlagChange = (e: any) => {
    const lockFlag = e.target.value;
    form.setFieldsValue({ lockFlag });
    setQueryForm(prev => ({ ...prev, lockFlag, pageNum: 1 }));
    setTimeout(() => query(), 0);
  };

  // 行選擇配置
  const rowSelection: TableProps<LoginFailVO>['rowSelection'] = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => {
      setSelectedRowKeys(keys);
    },
  };

  // 批量解除鎖定
  const handleBatchUnlock = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要解鎖的記錄');
      return;
    }

    Modal.confirm({
      title: '提示',
      content: '確定要批量解除鎖定這些數據嗎？',
      okText: '解鎖',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await loginFailApi.batchDelete(selectedRowKeys as number[]);
          message.success('解鎖成功');
          setSelectedRowKeys([]);
          query();
        } catch (error) {
          message.error('解鎖失敗');
        }
      },
    });
  };

  // 表格列定義
  const columns: ColumnsType<LoginFailVO> = [
    {
      title: '登錄名',
      dataIndex: 'loginName',
      key: 'loginName',
      width: LOGIN_FAIL_TABLE_COLUMNS_WIDTH.loginName,
      ellipsis: true,
    },
    {
      title: '用戶類型',
      dataIndex: 'userType',
      key: 'userType',
      width: LOGIN_FAIL_TABLE_COLUMNS_WIDTH.userType,
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
      title: '登錄失敗次數',
      dataIndex: 'loginFailCount',
      key: 'loginFailCount',
      width: LOGIN_FAIL_TABLE_COLUMNS_WIDTH.loginFailCount,
    },
    {
      title: '鎖定狀態',
      dataIndex: 'lockFlag',
      key: 'lockFlag',
      width: LOGIN_FAIL_TABLE_COLUMNS_WIDTH.lockFlag,
      render: (lockFlag: number) => {
        const lock =
          lockFlag === LOCK_FLAG_ENUM.LOCKED.value
            ? LOCK_FLAG_ENUM.LOCKED
            : LOCK_FLAG_ENUM.UNLOCKED;
        return <Tag color={lock.color}>{lock.label}</Tag>;
      },
    },
    {
      title: '鎖定開始時間',
      dataIndex: 'loginLockBeginTime',
      key: 'loginLockBeginTime',
      width: LOGIN_FAIL_TABLE_COLUMNS_WIDTH.loginLockBeginTime,
      render: (loginLockBeginTime: string) =>
        loginLockBeginTime ? formatDateTime(loginLockBeginTime) : '-',
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: LOGIN_FAIL_TABLE_COLUMNS_WIDTH.createTime,
      render: (createTime: string) => formatDateTime(createTime),
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: LOGIN_FAIL_TABLE_COLUMNS_WIDTH.updateTime,
      render: (updateTime: string) => formatDateTime(updateTime),
    },
  ];

  if (!hasQueryPermission) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '50px 0' }}>您沒有權限查看登錄失敗列表</div>
      </Card>
    );
  }

  return (
    <div className="login-fail-list">
      {/* 搜索表單 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={form} layout="inline">
          <Form.Item name="loginName" label="登錄名" style={{ marginBottom: 16 }}>
            <Input placeholder="登錄名" allowClear style={{ width: 300 }} />
          </Form.Item>

          <Form.Item name="lockFlag" label="快速篩選" style={{ marginBottom: 16 }}>
            <Radio.Group onChange={handleLockFlagChange} buttonStyle="solid">
              <Radio.Button value={undefined}>全部</Radio.Button>
              <Radio.Button value={1}>已鎖定</Radio.Button>
              <Radio.Button value={0}>未鎖定</Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item label="鎖定時間" style={{ marginBottom: 16 }}>
            <RangePicker
              format="YYYY-MM-DD"
              onChange={handleDateRangeChange}
              style={{ width: 220 }}
            />
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
        {/* 表格操作按鈕 */}
        <div style={{ marginBottom: 16 }}>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={handleBatchUnlock}
            disabled={selectedRowKeys.length === 0}
          >
            解除鎖定
          </Button>
        </div>

        <Table
          rowKey="loginFailId"
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
          rowSelection={rowSelection}
          size="small"
          bordered
        />
      </Card>
    </div>
  );
};

export default LoginFailList;
