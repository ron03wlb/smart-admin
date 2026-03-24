/**
 * ChangeLog Management Page
 * 系統更新日誌管理頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/change-log/change-log-list.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import React, { useState, useRef } from 'react';
import { Button, Form, Input, DatePicker, Row, Col, Table, Tag, Modal, Space, message } from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { RangePickerProps } from 'antd/es/date-picker';
import dayjs from 'dayjs';

import { useTable } from '@/hooks/useTable';
import { changeLogApi } from '@/api/support/changeLogApi';
import {
  CHANGE_LOG_PERMISSION,
  CHANGE_LOG_TYPE_LABELS,
  CHANGE_LOG_TYPE_COLORS,
  CHANGE_LOG_TABLE_COLUMNS_WIDTH,
} from '@/constants/support/changeLogConst';
import type { ChangeLogVO, ChangeLogQueryForm } from './types';
import ChangeLogFormModal from './components/ChangeLogFormModal';
import ChangeLogDetailModal from './components/ChangeLogDetailModal';
import PrivilegeButton from '@/components/PrivilegeButton';
import SmartEnumSelect from '@/components/common/SmartEnumSelect';
import TableOperator from '@/components/common/TableOperator';

const { RangePicker } = DatePicker;

/**
 * 日期範圍預設值
 */
const rangePresets: RangePickerProps['presets'] = [
  { label: '最近 7 天', value: [dayjs().subtract(7, 'd'), dayjs()] },
  { label: '最近 14 天', value: [dayjs().subtract(14, 'd'), dayjs()] },
  { label: '最近 30 天', value: [dayjs().subtract(30, 'd'), dayjs()] },
  { label: '最近 90 天', value: [dayjs().subtract(90, 'd'), dayjs()] },
];

const ChangeLogManagement: React.FC = () => {
  const [form] = Form.useForm();
  const formModalRef = useRef<{ show: (rowData?: ChangeLogVO) => void }>(null);
  const detailModalRef = useRef<{ show: (record: ChangeLogVO) => void }>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // 使用 useTable Hook
  const { tableData, loading, pagination, query, reset, setQueryForm } = useTable<
    ChangeLogVO,
    ChangeLogQueryForm
  >({
    defaultQueryForm: {
      type: undefined,
      keyword: undefined,
      publicDateBegin: undefined,
      publicDateEnd: undefined,
      createTime: undefined,
      link: undefined,
    },
    pagination: {
      pageNum: 1,
      pageSize: 10,
    },
    queryApi: changeLogApi.queryPage,
    autoQuery: true,
  });

  /**
   * 搜索
   */
  const handleSearch = () => {
    const values = form.getFieldsValue();
    setQueryForm(prev => ({
      ...prev,
      type: values.type,
      keyword: values.keyword,
      publicDateBegin: values.publicDate?.[0]
        ? dayjs(values.publicDate[0]).format('YYYY-MM-DD')
        : undefined,
      publicDateEnd: values.publicDate?.[1]
        ? dayjs(values.publicDate[1]).format('YYYY-MM-DD')
        : undefined,
      createTime: values.createTime ? dayjs(values.createTime).format('YYYY-MM-DD') : undefined,
      link: undefined,
      pageNum: 1, // Reset to first page on search
    }));
    query();
  };

  /**
   * 重置
   */
  const handleReset = () => {
    form.resetFields();
    reset();
  };

  /**
   * 顯示新增/編輯表單
   */
  const handleShowForm = (record?: ChangeLogVO) => {
    formModalRef.current?.show(record);
  };

  /**
   * 顯示詳情 Modal
   */
  const handleShowDetail = (record: ChangeLogVO) => {
    detailModalRef.current?.show(record);
  };

  /**
   * 刪除
   */
  const handleDelete = (record: ChangeLogVO) => {
    Modal.confirm({
      title: '提示',
      content: '確定要刪除選中的更新日誌嗎？',
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await changeLogApi.delete(record.changeLogId);
          message.success('刪除成功');
          query();
        } catch (error) {
          console.error('Delete failed:', error);
        }
      },
    });
  };

  /**
   * 批量刪除
   */
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請至少選擇一條記錄');
      return;
    }

    Modal.confirm({
      title: '提示',
      content: `確定要批量刪除選中的 ${selectedRowKeys.length} 條更新日誌嗎？`,
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await changeLogApi.batchDelete(selectedRowKeys as number[]);
          message.success('批量刪除成功');
          setSelectedRowKeys([]);
          query();
        } catch (error) {
          console.error('Batch delete failed:', error);
        }
      },
    });
  };

  /**
   * 表格列定義
   */
  const columns: ColumnsType<ChangeLogVO> = [
    {
      title: '版本',
      dataIndex: 'updateVersion',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.updateVersion,
      ellipsis: true,
      render: (text: string, record: ChangeLogVO) => (
        <Button type="link" onClick={() => handleShowDetail(record)}>
          {text}
        </Button>
      ),
    },
    {
      title: '更新類型',
      dataIndex: 'type',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.type,
      render: (type: number) => (
        <Tag
          color={CHANGE_LOG_TYPE_COLORS[type as keyof typeof CHANGE_LOG_TYPE_COLORS]}
          icon={<CheckCircleOutlined />}
        >
          {CHANGE_LOG_TYPE_LABELS[type as keyof typeof CHANGE_LOG_TYPE_LABELS]}
        </Tag>
      ),
    },
    {
      title: '發布人',
      dataIndex: 'publishAuthor',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.publishAuthor,
      ellipsis: true,
    },
    {
      title: '發布日期',
      dataIndex: 'publicDate',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.publicDate,
      ellipsis: true,
    },
    {
      title: '更新內容',
      dataIndex: 'content',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.content,
      ellipsis: true,
    },
    {
      title: '跳轉鏈接',
      dataIndex: 'link',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.link,
      ellipsis: true,
      render: (link?: string) =>
        link ? (
          <a href={link} target="_blank" rel="noreferrer">
            {link}
          </a>
        ) : (
          '-'
        ),
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.createTime,
      ellipsis: true,
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.updateTime,
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: CHANGE_LOG_TABLE_COLUMNS_WIDTH.action,
      fixed: 'right',
      render: (_: any, record: ChangeLogVO) => (
        <Space>
          <PrivilegeButton
            privilege={CHANGE_LOG_PERMISSION.UPDATE}
            type="link"
            onClick={() => handleShowForm(record)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            privilege={CHANGE_LOG_PERMISSION.DELETE}
            type="link"
            danger
            onClick={() => handleDelete(record)}
          >
            刪除
          </PrivilegeButton>
        </Space>
      ),
    },
  ];

  /**
   * 表格行選擇
   */
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(newSelectedRowKeys);
    },
  };

  return (
    <div className="change-log-management">
      {/* 查詢表單 */}
      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col>
            <Form.Item label="更新類型" name="type">
              <SmartEnumSelect
                enumName="CHANGE_LOG_TYPE_ENUM"
                placeholder="更新類型"
                style={{ width: 200 }}
                allowClear
              />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item label="關鍵字" name="keyword">
              <Input placeholder="關鍵字" style={{ width: 200 }} allowClear />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item label="發布日期" name="publicDate">
              <RangePicker presets={rangePresets} style={{ width: 240 }} />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item label="創建時間" name="createTime">
              <DatePicker style={{ width: 150 }} />
            </Form.Item>
          </Col>
          <Col>
            <Form.Item>
              <Space>
                <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                  查詢
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleReset}>
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Col>
        </Row>
      </Form>

      {/* 操作按鈕行 */}
      <Row justify="space-between" style={{ marginBottom: 16 }}>
        <Col>
          <Space>
            <PrivilegeButton
              privilege={CHANGE_LOG_PERMISSION.ADD}
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => handleShowForm()}
            >
              新建
            </PrivilegeButton>
            <PrivilegeButton
              privilege={CHANGE_LOG_PERMISSION.BATCH_DELETE}
              danger
              icon={<DeleteOutlined />}
              disabled={selectedRowKeys.length === 0}
              onClick={handleBatchDelete}
            >
              批量刪除
            </PrivilegeButton>
          </Space>
        </Col>
        <Col>
          <TableOperator onRefresh={query} showRefresh />
        </Col>
      </Row>

      {/* 表格 */}
      <Table
        rowKey="changeLogId"
        columns={columns}
        dataSource={tableData}
        loading={loading}
        pagination={pagination}
        rowSelection={rowSelection}
        scroll={{ x: 1500 }}
        size="small"
        bordered
      />

      {/* 表單 Modal */}
      <ChangeLogFormModal ref={formModalRef} onSuccess={query} />

      {/* 詳情 Modal */}
      <ChangeLogDetailModal ref={detailModalRef} />
    </div>
  );
};

export default ChangeLogManagement;
