/**
 * Config Management Page
 * 配置管理頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/config/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-12
 */

import { useState } from 'react';
import {
  Card,
  Form,
  Input,
  Button,
  Table,
  Space,
  Row,
  Col,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { TableColumnsType } from 'antd';
import { useTable } from '@/hooks/useTable';
import { usePrivilege } from '@/hooks/usePrivilege';
import PrivilegeButton from '@/components/PrivilegeButton';
import { configApi } from '@/api/support/configApi';
import type { ConfigVO, ConfigQueryForm, ConfigFormData } from './types';
import {
  CONFIG_PERMISSION,
  CONFIG_TABLE_COLUMNS_WIDTH,
} from '@/constants/support/configConst';
import { formatDateTime } from '@/utils/date';
import { ConfigFormModal } from './components/ConfigFormModal';

/**
 * 配置管理頁面
 */
export default function ConfigPage() {
  const [form] = Form.useForm();
  const hasAddPrivilege = usePrivilege(CONFIG_PERMISSION.ADD);
  const hasUpdatePrivilege = usePrivilege(CONFIG_PERMISSION.UPDATE);

  // ==================== Modal State ====================

  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<ConfigFormData | undefined>();

  // ==================== Table State Management ====================

  const {
    tableData,
    loading,
    pagination,
    queryForm,
    setQueryForm,
    query,
    reset,
    handleTableChange,
  } = useTable<ConfigVO, ConfigQueryForm>({
    defaultQueryForm: {
      configKey: undefined,
    },
    pagination: { pageNum: 1, pageSize: 10 },
    queryApi: configApi.queryPage,
    autoQuery: true,
  });

  // ==================== Search Operations ====================

  /**
   * 處理查詢
   */
  const handleQuery = () => {
    form.validateFields().then((values) => {
      setQueryForm({ configKey: values.configKey?.trim() });
      query();
    });
  };

  /**
   * 處理重置
   */
  const handleReset = () => {
    form.resetFields();
    setQueryForm({ configKey: undefined });
    reset();
  };

  // ==================== Table Columns Definition ====================

  const columns: TableColumnsType<ConfigVO> = [
    {
      title: 'ID',
      dataIndex: 'configId',
      width: CONFIG_TABLE_COLUMNS_WIDTH.configId,
    },
    {
      title: '參數 Key',
      dataIndex: 'configKey',
      width: CONFIG_TABLE_COLUMNS_WIDTH.configKey,
      ellipsis: true,
    },
    {
      title: '參數名稱',
      dataIndex: 'configName',
      width: CONFIG_TABLE_COLUMNS_WIDTH.configName,
      ellipsis: true,
    },
    {
      title: '參數值',
      dataIndex: 'configValue',
      width: CONFIG_TABLE_COLUMNS_WIDTH.configValue,
      ellipsis: true,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      width: CONFIG_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
      render: (remark: string) => remark || '-',
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      width: CONFIG_TABLE_COLUMNS_WIDTH.createTime,
      render: (createTime: string) => formatDateTime(createTime),
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      width: CONFIG_TABLE_COLUMNS_WIDTH.updateTime,
      render: (updateTime: string) => formatDateTime(updateTime),
    },
    {
      title: '操作',
      key: 'operate',
      width: CONFIG_TABLE_COLUMNS_WIDTH.operate,
      fixed: 'right',
      render: (_: any, record: ConfigVO) => (
        <Space size="small">
          {hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              編輯
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // ==================== CRUD Operations ====================

  /**
   * 處理新增配置
   */
  const handleAdd = () => {
    setFormInitialData(undefined);
    setFormModalVisible(true);
  };

  /**
   * 處理編輯配置
   */
  const handleEdit = (record: ConfigVO) => {
    setFormInitialData(record as ConfigFormData);
    setFormModalVisible(true);
  };

  /**
   * 表單提交成功回調
   */
  const handleFormSuccess = () => {
    setFormModalVisible(false);
    query(); // 刷新列表
  };

  // ==================== Render ====================

  return (
    <div className="config-page">
      {/* 查詢表單 */}
      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item name="configKey" label="參數 Key">
          <Input
            placeholder="請輸入 Key"
            allowClear
            style={{ width: 300 }}
            onPressEnter={handleQuery}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" icon={<SearchOutlined />} onClick={handleQuery}>
              查詢
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
            <PrivilegeButton
              privilege={CONFIG_PERMISSION.ADD}
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
            >
              新建
            </PrivilegeButton>
          </Space>
        </Form.Item>
      </Form>

      {/* 主表格卡片 */}
      <Card size="small" bordered={false}>
        {/* 表格 */}
        <Table
          rowKey="configId"
          columns={columns}
          dataSource={tableData}
          loading={loading}
          pagination={{
            current: pagination.pageNum,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 條`,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1200 }}
          size="small"
          bordered
        />
      </Card>

      {/* 新增/編輯 Modal */}
      <ConfigFormModal
        visible={formModalVisible}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />
    </div>
  );
}
