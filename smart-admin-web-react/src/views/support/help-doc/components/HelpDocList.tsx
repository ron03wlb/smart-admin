/**
 * Help Doc List
 * 幫助文檔列表
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState, useRef } from 'react';
import { Form, Input, Button, Card, Table, Space, DatePicker, Modal } from 'antd';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { helpDocApi } from '@/api/support/helpDocApi';
import { usePrivilege } from '@/hooks/usePrivilege';
import { HELP_DOC_PERMISSION, HELP_DOC_TABLE_COLUMNS_WIDTH } from '@/constants/support/helpDocConst';
import type { HelpDocVO, HelpDocQueryForm } from '../types';
import HelpDocFormDrawer, { type HelpDocFormDrawerRef } from './HelpDocFormDrawer';
import { Link } from 'react-router-dom';

const { RangePicker } = DatePicker;
const { confirm } = Modal;

export interface HelpDocListProps {
  helpDocCatalogId?: number | null;
}

const HelpDocList: React.FC<HelpDocListProps> = ({ helpDocCatalogId }) => {
  const [form] = Form.useForm<HelpDocQueryForm>();
  const hasQueryPrivilege = usePrivilege(HELP_DOC_PERMISSION.QUERY);
  const hasAddPrivilege = usePrivilege(HELP_DOC_PERMISSION.ADD);
  const hasUpdatePrivilege = usePrivilege(HELP_DOC_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(HELP_DOC_PERMISSION.DELETE);

  const [tableData, setTableData] = useState<HelpDocVO[]>([]);
  const [tableLoading, setTableLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [createDate, setCreateDate] = useState<[Dayjs, Dayjs] | null>(null);

  const helpDocFormDrawerRef = useRef<HelpDocFormDrawerRef>(null);

  useEffect(() => {
    form.setFieldsValue({
      helpDocCatalogId,
      pageNum: 1,
      pageSize: 10,
    });
    fetchData();
  }, [helpDocCatalogId]);

  const fetchData = async () => {
    if (!hasQueryPrivilege) {
      return;
    }

    try {
      setTableLoading(true);
      const values = form.getFieldsValue();
      const res = await helpDocApi.query({
        ...values,
        helpDocCatalogId: helpDocCatalogId || null,
      });

      setTableData(res.data.list);
      setTotal(res.data.total);
    } catch (error) {
      console.error('Failed to fetch help doc list:', error);
    } finally {
      setTableLoading(false);
    }
  };

  const handleSearch = () => {
    form.setFieldsValue({ pageNum: 1 });
    fetchData();
  };

  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({
      pageNum: 1,
      pageSize: 10,
      helpDocCatalogId,
    });
    setCreateDate(null);
    fetchData();
  };

  const handleTableChange = (page: number, pageSize: number) => {
    form.setFieldsValue({ pageNum: page, pageSize });
    fetchData();
  };

  const handleCreateDateChange = (dates: [Dayjs, Dayjs] | null) => {
    setCreateDate(dates);
    if (dates) {
      form.setFieldsValue({
        createTimeBegin: dates[0].format('YYYY-MM-DD'),
        createTimeEnd: dates[1].format('YYYY-MM-DD'),
      });
    } else {
      form.setFieldsValue({
        createTimeBegin: null,
        createTimeEnd: null,
      });
    }
  };

  const handleAddOrUpdate = (helpDocId?: number) => {
    helpDocFormDrawerRef.current?.showDrawer(helpDocId);
  };

  const handleDelete = (helpDocId: number) => {
    confirm({
      title: '提示',
      content: '確認刪除此數據嗎?',
      onOk: async () => {
        try {
          await helpDocApi.delete(helpDocId);
          fetchData();
        } catch (error) {
          console.error('Failed to delete help doc:', error);
        }
      },
    });
  };

  const columns: ColumnsType<HelpDocVO> = [
    {
      title: '標題',
      dataIndex: 'title',
      key: 'title',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.title,
      ellipsis: true,
      render: (text: string, record: HelpDocVO) => (
        <Link to={`/help-doc/detail?helpDocId=${record.helpDocId}`} target="_blank">
          {text}
        </Link>
      ),
    },
    {
      title: '目錄',
      dataIndex: 'helpDocCatalogName',
      key: 'helpDocCatalogName',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.helpDocCatalogName,
      ellipsis: true,
    },
    {
      title: '作者',
      dataIndex: 'author',
      key: 'author',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.author,
      ellipsis: true,
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.sort,
    },
    {
      title: '頁面瀏覽量',
      dataIndex: 'pageViewCount',
      key: 'pageViewCount',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.pageViewCount,
    },
    {
      title: '用戶瀏覽量',
      dataIndex: 'userViewCount',
      key: 'userViewCount',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.userViewCount,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.createTime,
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      fixed: 'right',
      width: HELP_DOC_TABLE_COLUMNS_WIDTH.action,
      render: (_: any, record: HelpDocVO) => (
        <div>
          {hasUpdatePrivilege && (
            <Button
              type="link"
              size="small"
              onClick={() => handleAddOrUpdate(record.helpDocId)}
            >
              編輯
            </Button>
          )}
          {hasDeletePrivilege && (
            <Button
              type="link"
              size="small"
              danger
              onClick={() => handleDelete(record.helpDocId)}
            >
              刪除
            </Button>
          )}
        </div>
      ),
    },
  ];

  if (!hasQueryPrivilege) {
    return null;
  }

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="關鍵字">
          <Input
            value={form.getFieldValue('keywords')}
            onChange={(e) => form.setFieldValue('keywords', e.target.value)}
            placeholder="標題、作者"
            style={{ width: 300 }}
          />
        </Form.Item>

        <Form.Item label="創建時間">
          <RangePicker
            value={createDate}
            onChange={handleCreateDateChange}
            style={{ width: 220 }}
          />
        </Form.Item>

        <Form.Item name="pageNum" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="pageSize" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="createTimeBegin" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="createTimeEnd" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="helpDocCatalogId" hidden>
          <Input />
        </Form.Item>

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
      </Form>

      <Card size="small" style={{ flex: 1 }}>
        <div style={{ marginBottom: 16 }}>
          {hasAddPrivilege && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => handleAddOrUpdate()}
            >
              新建
            </Button>
          )}
        </div>

        <Table
          rowKey="helpDocId"
          columns={columns}
          dataSource={tableData}
          loading={tableLoading}
          pagination={{
            current: form.getFieldValue('pageNum') || 1,
            pageSize: form.getFieldValue('pageSize') || 10,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共${total}條`,
            onChange: handleTableChange,
          }}
          size="small"
          bordered
          scroll={{ x: 1000 }}
        />
      </Card>

      <HelpDocFormDrawer ref={helpDocFormDrawerRef} onReloadList={fetchData} />
    </div>
  );
};

export default HelpDocList;
