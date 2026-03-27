/**
 * Brand List Page
 * 品牌列表頁面
 *
 * 參考：後端 BrandController.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import { useState, useRef } from 'react';
import { Form, Input, Select, Button, Table, Space, Tag, Modal, message } from 'antd';
import { PlusOutlined, DeleteOutlined, ReloadOutlined, EditOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useTable } from '@/hooks/useTable';
import { formatDateTime } from '@/utils/date';
import { brandApi } from './brandApi';
import BrandFormModal from './components/BrandFormModal';
import type { BrandVO, BrandQueryForm } from './types';
import { BrandStatusEnum } from './types';
import {
  BRAND_PERMISSIONS,
  BRAND_STATUS_LABELS,
  BRAND_STATUS_TAG_COLORS,
  BRAND_STATUS_OPTIONS,
  BRAND_COLUMN_WIDTHS,
} from './brandConst';

const BrandList: React.FC = () => {
  const [form] = Form.useForm<BrandQueryForm>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const formModalRef = useRef<any>(null);

  // Table hook
  const { tableData, loading, pagination, queryTable, setTableData } = useTable<BrandVO>({
    queryApi: brandApi.queryBrand,
    form,
  });

  /**
   * Search
   * 搜索
   */
  const handleSearch = () => {
    queryTable();
  };

  /**
   * Reset
   * 重置
   */
  const handleReset = () => {
    form.resetFields();
    queryTable();
  };

  /**
   * Add
   * 新增
   */
  const handleAdd = () => {
    formModalRef.current?.open();
  };

  /**
   * Edit
   * 編輯
   */
  const handleEdit = (record: BrandVO) => {
    formModalRef.current?.open(record);
  };

  /**
   * Delete
   * 刪除
   */
  const handleDelete = (brandId: number) => {
    Modal.confirm({
      title: '確認刪除',
      content: '確定要刪除這個品牌嗎？',
      okText: '確定',
      cancelText: '取消',
      onOk: async () => {
        const result = await brandApi.batchDelete([brandId]);
        if (result.ok) {
          message.success('刪除成功');
          queryTable();
        }
      },
    });
  };

  /**
   * Batch Delete
   * 批量刪除
   */
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要刪除的品牌');
      return;
    }

    Modal.confirm({
      title: '確認刪除',
      content: `確定要刪除選中的 ${selectedRowKeys.length} 個品牌嗎？`,
      okText: '確定',
      cancelText: '取消',
      onOk: async () => {
        const result = await brandApi.batchDelete(selectedRowKeys);
        if (result.ok) {
          message.success('批量刪除成功');
          setSelectedRowKeys([]);
          queryTable();
        }
      },
    });
  };

  /**
   * Refresh
   * 刷新
   */
  const handleRefresh = () => {
    setSelectedRowKeys([]);
    queryTable();
  };

  /**
   * Form Modal Success Callback
   * 表單Modal成功回調
   */
  const handleFormSuccess = () => {
    queryTable();
  };

  /**
   * Table Columns
   * 表格列定義
   */
  const columns: ColumnsType<BrandVO> = [
    {
      title: '品牌ID',
      dataIndex: 'brandId',
      key: 'brandId',
      width: BRAND_COLUMN_WIDTHS.brandId,
      fixed: 'left',
    },
    {
      title: '品牌名稱',
      dataIndex: 'brandName',
      key: 'brandName',
      width: BRAND_COLUMN_WIDTHS.brandName,
      fixed: 'left',
    },
    {
      title: '品牌Logo',
      dataIndex: 'brandLogo',
      key: 'brandLogo',
      width: BRAND_COLUMN_WIDTHS.brandLogo,
      render: (logo: string) =>
        logo ? <img src={logo} alt="logo" style={{ width: 40, height: 40, objectFit: 'contain' }} /> : '-',
    },
    {
      title: '品牌描述',
      dataIndex: 'description',
      key: 'description',
      width: BRAND_COLUMN_WIDTHS.description,
      ellipsis: true,
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: BRAND_COLUMN_WIDTHS.sort,
    },
    {
      title: '狀態',
      dataIndex: 'status',
      key: 'status',
      width: BRAND_COLUMN_WIDTHS.status,
      render: (status: BrandStatusEnum) => (
        <Tag color={BRAND_STATUS_TAG_COLORS[status]}>{BRAND_STATUS_LABELS[status]}</Tag>
      ),
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: BRAND_COLUMN_WIDTHS.updateTime,
      render: (time: string) => formatDateTime(time),
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: BRAND_COLUMN_WIDTHS.createTime,
      render: (time: string) => formatDateTime(time),
    },
    {
      title: '操作',
      key: 'actions',
      width: BRAND_COLUMN_WIDTHS.actions,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            編輯
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.brandId)}
          >
            刪除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="brand-list-container">
      {/* Search Form */}
      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item name="keyword" label="關鍵詞">
          <Input placeholder="請輸入品牌名稱" style={{ width: 200 }} allowClear />
        </Form.Item>

        <Form.Item name="status" label="狀態">
          <Select placeholder="請選擇狀態" style={{ width: 120 }} options={BRAND_STATUS_OPTIONS} allowClear />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" onClick={handleSearch}>
              查詢
            </Button>
            <Button onClick={handleReset}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      {/* Action Buttons */}
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建品牌
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={handleBatchDelete}
            disabled={selectedRowKeys.length === 0}
          >
            批量刪除
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
            刷新
          </Button>
        </Space>
      </div>

      {/* Data Table */}
      <Table
        rowKey="brandId"
        columns={columns}
        dataSource={tableData}
        loading={loading}
        pagination={pagination}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as number[]),
        }}
        scroll={{ x: 1400 }}
      />

      {/* Form Modal */}
      <BrandFormModal ref={formModalRef} onSuccess={handleFormSuccess} />
    </div>
  );
};

export default BrandList;
