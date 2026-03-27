/**
 * Bank List Page
 * 銀行信息列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

import React, { useRef, useState } from 'react';
import { Button, Form, Input, Space, Table, Tag, Popconfirm, DatePicker, Select, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { BankVO, BankQueryForm } from './types';
import { bankApi } from './bankApi';
import { BANK_PERMISSIONS, BANK_COLUMN_WIDTHS, BUSINESS_FLAG_OPTIONS, DISABLED_FLAG_OPTIONS } from './bankConst';
import BankFormModal from './components/BankFormModal';
import type { BankFormModalRef } from './components/BankFormModal';
import { useTable } from '@/hooks/useTable';
import PrivilegeButton from '@/components/common/PrivilegeButton';
import { usePrivilege } from '@/hooks/usePrivilege';

const { RangePicker } = DatePicker;

const BankList: React.FC = () => {
  const [form] = Form.useForm<BankQueryForm>();
  const formModalRef = useRef<BankFormModalRef>(null);
  const hasAddPrivilege = usePrivilege(BANK_PERMISSIONS.ADD);
  const hasUpdatePrivilege = usePrivilege(BANK_PERMISSIONS.UPDATE);
  const hasDeletePrivilege = usePrivilege(BANK_PERMISSIONS.DELETE);

  const { tableData, loading, pagination, queryTable, setTableData } = useTable<BankVO>({
    queryApi: bankApi.queryByPage,
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
  const handleEdit = (record: BankVO) => {
    formModalRef.current?.open(record);
  };

  /**
   * 處理刪除
   */
  const handleDelete = async (bankId: number) => {
    const result = await bankApi.deleteBank(bankId);
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
  const columns: ColumnsType<BankVO> = [
    {
      title: '序號',
      dataIndex: 'index',
      key: 'index',
      width: BANK_COLUMN_WIDTHS.INDEX,
      fixed: 'left',
      render: (_text, _record, index) => {
        return (pagination.current - 1) * pagination.pageSize + index + 1;
      },
    },
    {
      title: '開戶銀行',
      dataIndex: 'bankName',
      key: 'bankName',
      width: BANK_COLUMN_WIDTHS.BANK_NAME,
      ellipsis: true,
    },
    {
      title: '賬戶名稱',
      dataIndex: 'accountName',
      key: 'accountName',
      width: BANK_COLUMN_WIDTHS.ACCOUNT_NAME,
      ellipsis: true,
    },
    {
      title: '賬號',
      dataIndex: 'accountNumber',
      key: 'accountNumber',
      width: BANK_COLUMN_WIDTHS.ACCOUNT_NUMBER,
      ellipsis: true,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: BANK_COLUMN_WIDTHS.REMARK,
      ellipsis: true,
    },
    {
      title: '是否對公',
      dataIndex: 'businessFlag',
      key: 'businessFlag',
      width: BANK_COLUMN_WIDTHS.BUSINESS_FLAG,
      render: (businessFlag: boolean) => (
        <Tag color={businessFlag ? 'blue' : 'default'}>{businessFlag ? '對公' : '對私'}</Tag>
      ),
    },
    {
      title: '企業名稱',
      dataIndex: 'enterpriseName',
      key: 'enterpriseName',
      width: BANK_COLUMN_WIDTHS.ENTERPRISE,
      ellipsis: true,
    },
    {
      title: '禁用狀態',
      dataIndex: 'disabledFlag',
      key: 'disabledFlag',
      width: BANK_COLUMN_WIDTHS.DISABLED_FLAG,
      render: (disabledFlag: boolean) => (
        <Tag color={disabledFlag ? 'red' : 'green'}>{disabledFlag ? '禁用' : '啟用'}</Tag>
      ),
    },
    {
      title: '創建人',
      dataIndex: 'createUserName',
      key: 'createUserName',
      width: BANK_COLUMN_WIDTHS.CREATE_USER,
      ellipsis: true,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: BANK_COLUMN_WIDTHS.CREATE_TIME,
      render: (createTime: string) => (createTime ? dayjs(createTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: BANK_COLUMN_WIDTHS.UPDATE_TIME,
      render: (updateTime: string) => (updateTime ? dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: BANK_COLUMN_WIDTHS.ACTIONS,
      fixed: 'right',
      render: (_text, record) => (
        <Space size="small">
          {hasUpdatePrivilege && (
            <PrivilegeButton
              type="link"
              size="small"
              icon={<EditOutlined />}
              privilege={BANK_PERMISSIONS.UPDATE}
              onClick={() => handleEdit(record)}
            >
              編輯
            </PrivilegeButton>
          )}
          {hasDeletePrivilege && (
            <Popconfirm
              title="確定要刪除這條銀行信息嗎？"
              onConfirm={() => handleDelete(record.bankId)}
              okText="確定"
              cancelText="取消"
            >
              <PrivilegeButton
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                privilege={BANK_PERMISSIONS.DELETE}
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
          <Input placeholder="銀行名/賬戶名/賬號" style={{ width: 200 }} allowClear />
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
          <PrivilegeButton type="primary" icon={<PlusOutlined />} privilege={BANK_PERMISSIONS.ADD} onClick={handleAdd}>
            新建
          </PrivilegeButton>
        )}
        <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
          刷新
        </Button>
      </Space>

      {/* 表格 */}
      <Table<BankVO>
        columns={columns}
        dataSource={tableData}
        loading={loading}
        rowKey="bankId"
        scroll={{ x: 1800 }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 條`,
        }}
      />

      {/* 表單模態框 */}
      <BankFormModal ref={formModalRef} onSuccess={handleFormSuccess} />
    </div>
  );
};

export default BankList;
