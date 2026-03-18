/**
 * Position Management Page
 * 職位管理頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/position/
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
  Modal,
  message,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { TableColumnsType } from 'antd';
import { useTable } from '@/hooks/useTable';
import { usePrivilege } from '@/hooks/usePrivilege';
import PrivilegeButton from '@/components/PrivilegeButton';
import { positionApi } from '@/api/system/positionApi';
import type { PositionVO, PositionQueryForm, PositionFormData } from './types';
import {
  POSITION_PERMISSION,
  POSITION_TABLE_COLUMNS_WIDTH,
} from '@/constants/system/positionConst';
import { formatDateTime } from '@/utils/date';
import { PositionFormModal } from './components/PositionFormModal';

/**
 * 職位管理頁面
 */
export default function PositionPage() {
  const [form] = Form.useForm();
  const hasAddPrivilege = usePrivilege(POSITION_PERMISSION.ADD);
  const hasUpdatePrivilege = usePrivilege(POSITION_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(POSITION_PERMISSION.DELETE);
  const hasBatchDeletePrivilege = usePrivilege(POSITION_PERMISSION.BATCH_DELETE);

  // ==================== Modal State ====================

  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<PositionFormData | undefined>();

  // ==================== Table State Management ====================

  const {
    tableData,
    loading,
    pagination,
    queryForm,
    setQueryForm,
    selectedRowKeys,
    query,
    reset,
    handleTableChange,
    handleRowSelectionChange,
  } = useTable<PositionVO, PositionQueryForm>({
    defaultQueryForm: {
      keywords: undefined,
    },
    pagination: { pageNum: 1, pageSize: 10 },
    queryApi: positionApi.queryPage,
    autoQuery: true,
  });

  // ==================== Search Operations ====================

  /**
   * 處理查詢
   */
  const handleQuery = () => {
    form.validateFields().then((values) => {
      setQueryForm({ keywords: values.keywords?.trim() });
      query();
    });
  };

  /**
   * 處理重置
   */
  const handleReset = () => {
    form.resetFields();
    setQueryForm({ keywords: undefined });
    reset();
  };

  // ==================== Table Columns Definition ====================

  const columns: TableColumnsType<PositionVO> = [
    {
      title: '職位名稱',
      dataIndex: 'positionName',
      width: POSITION_TABLE_COLUMNS_WIDTH.positionName,
      ellipsis: true,
    },
    {
      title: '職級',
      dataIndex: 'positionLevel',
      width: POSITION_TABLE_COLUMNS_WIDTH.positionLevel,
      ellipsis: true,
      render: (positionLevel: string) => positionLevel || '-',
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: POSITION_TABLE_COLUMNS_WIDTH.sort,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      width: POSITION_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
      render: (remark: string) => remark || '-',
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      width: POSITION_TABLE_COLUMNS_WIDTH.createTime,
      render: (createTime: string) => formatDateTime(createTime),
    },
    {
      title: '操作',
      key: 'operate',
      width: POSITION_TABLE_COLUMNS_WIDTH.operate,
      fixed: 'right',
      render: (_: any, record: PositionVO) => (
        <Space size="small">
          {hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              編輯
            </Button>
          )}
          {hasDeletePrivilege && (
            <Button
              type="link"
              size="small"
              danger
              onClick={() => handleDelete(record.positionId)}
            >
              刪除
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // ==================== CRUD Operations ====================

  /**
   * 處理新增職位
   */
  const handleAdd = () => {
    setFormInitialData(undefined);
    setFormModalVisible(true);
  };

  /**
   * 處理編輯職位
   */
  const handleEdit = (record: PositionVO) => {
    setFormInitialData(record as PositionFormData);
    setFormModalVisible(true);
  };

  /**
   * 表單提交成功回調
   */
  const handleFormSuccess = () => {
    setFormModalVisible(false);
    query(); // 刷新列表
  };

  /**
   * 處理刪除職位
   */
  const handleDelete = (positionId: number) => {
    Modal.confirm({
      title: '提示',
      icon: <ExclamationCircleOutlined />,
      content: '確定要刪除該職位嗎？',
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await positionApi.deletePosition(positionId);
          message.success('刪除成功');
          query();
        } catch (error) {
          message.error('刪除失敗');
        }
      },
    });
  };

  /**
   * 處理批量刪除
   */
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要刪除的職位');
      return;
    }

    Modal.confirm({
      title: '提示',
      icon: <ExclamationCircleOutlined />,
      content: `確定要批量刪除這 ${selectedRowKeys.length} 個職位嗎？`,
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await positionApi.batchDeletePosition(selectedRowKeys as number[]);
          message.success('批量刪除成功');
          query();
        } catch (error) {
          message.error('批量刪除失敗');
        }
      },
    });
  };

  // ==================== Render ====================

  return (
    <div className="position-page">
      {/* 查詢表單 */}
      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item name="keywords" label="關鍵字查詢">
          <Input
            placeholder="關鍵字查詢"
            allowClear
            style={{ width: 200 }}
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
          </Space>
        </Form.Item>
      </Form>

      {/* 主表格卡片 */}
      <Card size="small" bordered={false}>
        {/* 表格操作按鈕 */}
        <Row justify="space-between" style={{ marginBottom: 16 }}>
          <Col>
            <Space>
              <PrivilegeButton
                privilege={POSITION_PERMISSION.ADD}
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleAdd}
              >
                新建
              </PrivilegeButton>
              <PrivilegeButton
                privilege={POSITION_PERMISSION.BATCH_DELETE}
                type="primary"
                danger
                icon={<DeleteOutlined />}
                onClick={handleBatchDelete}
                disabled={selectedRowKeys.length === 0}
              >
                批量刪除
              </PrivilegeButton>
            </Space>
          </Col>
        </Row>

        {/* 表格 */}
        <Table
          rowKey="positionId"
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
          rowSelection={{
            selectedRowKeys,
            onChange: handleRowSelectionChange,
          }}
          scroll={{ x: 1200 }}
          size="small"
          bordered
        />
      </Card>

      {/* 新增/編輯 Modal */}
      <PositionFormModal
        visible={formModalVisible}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />
    </div>
  );
}
