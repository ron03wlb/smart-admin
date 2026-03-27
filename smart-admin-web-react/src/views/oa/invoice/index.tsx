/**
 * Invoice List Page
 * 發票信息列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import React, { useRef, useState } from 'react';
import { Button, Form, Input, Space, Table, Tag, Popconfirm, DatePicker, Select, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { InvoiceVO, InvoiceQueryForm } from './types';
import { invoiceApi } from './invoiceApi';
import { INVOICE_PERMISSIONS, INVOICE_COLUMN_WIDTHS, DISABLED_FLAG_OPTIONS } from './invoiceConst';
import InvoiceFormModal from './components/InvoiceFormModal';
import type { InvoiceFormModalRef } from './components/InvoiceFormModal';
import { useTable } from '@/hooks/useTable';
import PrivilegeButton from '@/components/common/PrivilegeButton';
import { usePrivilege } from '@/hooks/usePrivilege';

const { RangePicker } = DatePicker;

const InvoiceList: React.FC = () => {
  const [form] = Form.useForm<InvoiceQueryForm>();
  const formModalRef = useRef<InvoiceFormModalRef>(null);
  const hasAddPrivilege = usePrivilege(INVOICE_PERMISSIONS.ADD);
  const hasUpdatePrivilege = usePrivilege(INVOICE_PERMISSIONS.UPDATE);
  const hasDeletePrivilege = usePrivilege(INVOICE_PERMISSIONS.DELETE);

  const { tableData, loading, pagination, queryTable, setTableData } = useTable<InvoiceVO>({
    queryApi: invoiceApi.queryByPage,
    form,
  });

  /**
   * 處理新建
   */
  const handleAdd = () => {
    formModalRef.current?.open();
  };

  /**
   * 處理編輯
   */
  const handleEdit = (record: InvoiceVO) => {
    formModalRef.current?.open(record);
  };

  /**
   * 處理刪除
   */
  const handleDelete = async (invoiceId: number) => {
    const result = await invoiceApi.deleteInvoice(invoiceId);
    if (result.ok) {
      message.success('刪除成功');
      queryTable();
    }
  };

  /**
   * 處理刷新
   */
  const handleRefresh = () => {
    queryTable();
  };

  /**
   * 處理表單提交成功
   */
  const handleFormSuccess = () => {
    queryTable();
  };

  /**
   * 處理日期範圍變化
   */
  const handleDateRangeChange = (dates: any) => {
    if (dates) {
      form.setFieldsValue({
        startTime: dates[0].format('YYYY-MM-DD'),
        endTime: dates[1].format('YYYY-MM-DD'),
      });
    } else {
      form.setFieldsValue({
        startTime: undefined,
        endTime: undefined,
      });
    }
  };

  /**
   * 表格列定義
   */
  const columns: ColumnsType<InvoiceVO> = [
    {
      title: '序號',
      dataIndex: 'index',
      key: 'index',
      width: INVOICE_COLUMN_WIDTHS.INDEX,
      fixed: 'left',
      render: (_text, _record, index) => {
        return (pagination.current - 1) * pagination.pageSize + index + 1;
      },
    },
    {
      title: '開票抬頭',
      dataIndex: 'invoiceHeads',
      key: 'invoiceHeads',
      width: INVOICE_COLUMN_WIDTHS.INVOICE_HEADS,
      ellipsis: true,
    },
    {
      title: '納稅人識別號',
      dataIndex: 'taxpayerIdentificationNumber',
      key: 'taxpayerIdentificationNumber',
      width: INVOICE_COLUMN_WIDTHS.TAXPAYER_ID,
      ellipsis: true,
    },
    {
      title: '銀行賬戶',
      dataIndex: 'accountNumber',
      key: 'accountNumber',
      width: INVOICE_COLUMN_WIDTHS.ACCOUNT_NUMBER,
      ellipsis: true,
    },
    {
      title: '開戶行',
      dataIndex: 'bankName',
      key: 'bankName',
      width: INVOICE_COLUMN_WIDTHS.BANK_NAME,
      ellipsis: true,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: INVOICE_COLUMN_WIDTHS.REMARK,
      ellipsis: true,
    },
    {
      title: '企業名稱',
      dataIndex: 'enterpriseName',
      key: 'enterpriseName',
      width: INVOICE_COLUMN_WIDTHS.ENTERPRISE,
      ellipsis: true,
    },
    {
      title: '禁用狀態',
      dataIndex: 'disabledFlag',
      key: 'disabledFlag',
      width: INVOICE_COLUMN_WIDTHS.DISABLED_FLAG,
      render: (disabledFlag: boolean) => (
        <Tag color={disabledFlag ? 'red' : 'green'}>{disabledFlag ? '禁用' : '啟用'}</Tag>
      ),
    },
    {
      title: '創建人',
      dataIndex: 'createUserName',
      key: 'createUserName',
      width: INVOICE_COLUMN_WIDTHS.CREATE_USER,
      ellipsis: true,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: INVOICE_COLUMN_WIDTHS.CREATE_TIME,
      render: (createTime: string) => (createTime ? dayjs(createTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: INVOICE_COLUMN_WIDTHS.UPDATE_TIME,
      render: (updateTime: string) => (updateTime ? dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: INVOICE_COLUMN_WIDTHS.ACTIONS,
      fixed: 'right',
      render: (_text, record) => (
        <Space size="small">
          {hasUpdatePrivilege && (
            <PrivilegeButton
              type="link"
              size="small"
              icon={<EditOutlined />}
              privilege={INVOICE_PERMISSIONS.UPDATE}
              onClick={() => handleEdit(record)}
            >
              編輯
            </PrivilegeButton>
          )}
          {hasDeletePrivilege && (
            <Popconfirm
              title="確定要刪除這條發票信息嗎？"
              onConfirm={() => handleDelete(record.invoiceId)}
              okText="確定"
              cancelText="取消"
            >
              <PrivilegeButton
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                privilege={INVOICE_PERMISSIONS.DELETE}
              >
                刪除
              </PrivilegeButton>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* 搜索表單 */}
      <Form
        form={form}
        layout="inline"
        onFinish={queryTable}
        style={{ marginBottom: 16 }}
        initialValues={{
          pageNum: 1,
          pageSize: 10,
        }}
      >
        <Form.Item name="keywords" label="關鍵字">
          <Input placeholder="開票抬頭/納稅人識別號/銀行賬戶" style={{ width: 250 }} allowClear />
        </Form.Item>

        <Form.Item name="enterpriseId" label="企業">
          <Input placeholder="請輸入企業ID" style={{ width: 150 }} allowClear />
        </Form.Item>

        <Form.Item label="創建時間">
          <RangePicker onChange={handleDateRangeChange} />
        </Form.Item>

        <Form.Item name="disabledFlag" label="狀態">
          <Select placeholder="請選擇" style={{ width: 120 }} allowClear>
            {DISABLED_FLAG_OPTIONS.map((option) => (
              <Select.Option key={String(option.value)} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item name="pageNum" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="pageSize" hidden>
          <Input />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查詢
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      {/* 操作按鈕 */}
      <Space style={{ marginBottom: 16 }}>
        {hasAddPrivilege && (
          <PrivilegeButton
            type="primary"
            icon={<PlusOutlined />}
            privilege={INVOICE_PERMISSIONS.ADD}
            onClick={handleAdd}
          >
            新建
          </PrivilegeButton>
        )}
        <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
          刷新
        </Button>
      </Space>

      {/* 表格 */}
      <Table<InvoiceVO>
        columns={columns}
        dataSource={tableData}
        loading={loading}
        rowKey="invoiceId"
        scroll={{ x: 1800 }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 條`,
        }}
      />

      {/* 表單模態框 */}
      <InvoiceFormModal ref={formModalRef} onSuccess={handleFormSuccess} />
    </div>
  );
};

export default InvoiceList;
