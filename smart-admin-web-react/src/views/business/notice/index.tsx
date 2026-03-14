/**
 * Notice Management Page
 * 通知公告管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { noticeApi } from '@/api/business/noticeApi';
import type { NoticeVO, NoticeQueryForm, NoticeTypeVO } from './types';
import {
  NOTICE_PERMISSION,
  NOTICE_TABLE_COLUMNS_WIDTH,
  VISIBLE_FLAG_LABELS,
} from '@/constants/business/noticeConst';
import { PrivilegeButton } from '@/components/PrivilegeButton';
import NoticeFormDrawer from './components/NoticeFormDrawer';

const { confirm } = Modal;

export default function NoticePage() {
  const [queryForm] = Form.useForm<NoticeQueryForm>();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<NoticeVO[]>([]);
  const [total, setTotal] = useState(0);
  const [noticeTypeList, setNoticeTypeList] = useState<NoticeTypeVO[]>([]);

  // Pagination state
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Form Drawer state
  const [formDrawerVisible, setFormDrawerVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<NoticeVO | undefined>(undefined);

  /**
   * 查詢通知類型列表
   */
  const fetchNoticeTypeList = async () => {
    try {
      const res = await noticeApi.getAllNoticeTypeList();
      if (res.ok && res.data) {
        setNoticeTypeList(res.data);
      }
    } catch (error) {
      console.error('查詢通知類型列表失敗', error);
    }
  };

  /**
   * 查詢通知公告列表
   */
  const fetchNoticeList = async () => {
    try {
      setLoading(true);
      const values = queryForm.getFieldsValue();

      const params: NoticeQueryForm = {
        ...values,
        pageNum,
        pageSize,
      };

      const res = await noticeApi.queryNotice(params);
      if (res.ok && res.data) {
        setDataSource(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } catch (error) {
      message.error('查詢通知公告列表失敗');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 搜尋處理
   */
  const handleSearch = () => {
    setPageNum(1); // Reset to first page
    fetchNoticeList();
  };

  /**
   * 重置搜尋
   */
  const handleReset = () => {
    queryForm.resetFields();
    setPageNum(1);
    setPageSize(10);
    fetchNoticeList();
  };

  /**
   * 顯示新增表單
   */
  const handleAdd = () => {
    setFormInitialData(undefined);
    setFormDrawerVisible(true);
  };

  /**
   * 顯示編輯表單
   */
  const handleEdit = (record: NoticeVO) => {
    setFormInitialData(record);
    setFormDrawerVisible(true);
  };

  /**
   * 刪除通知公告
   */
  const handleDelete = (record: NoticeVO) => {
    confirm({
      title: '刪除通知公告',
      icon: <ExclamationCircleOutlined />,
      content: `確定要刪除通知公告「${record.title}」嗎？`,
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await noticeApi.deleteNotice(record.noticeId);
          if (res.ok) {
            message.success('刪除成功');
            fetchNoticeList();
          }
        } catch (error) {
          message.error('刪除失敗');
          console.error(error);
        }
      },
    });
  };

  /**
   * Form Drawer 成功回調
   */
  const handleFormSuccess = () => {
    setFormDrawerVisible(false);
    setFormInitialData(undefined);
    fetchNoticeList();
  };

  /**
   * Form Drawer 取消回調
   */
  const handleFormCancel = () => {
    setFormDrawerVisible(false);
    setFormInitialData(undefined);
  };

  /**
   * 分頁變化處理
   */
  const handlePageChange = (page: number, size: number) => {
    setPageNum(page);
    setPageSize(size);
  };

  useEffect(() => {
    fetchNoticeTypeList();
  }, []);

  useEffect(() => {
    fetchNoticeList();
  }, [pageNum, pageSize]);

  /**
   * 表格列定義
   */
  const columns: ColumnsType<NoticeVO> = [
    {
      title: '公告標題',
      dataIndex: 'title',
      key: 'title',
      width: NOTICE_TABLE_COLUMNS_WIDTH.title,
      ellipsis: true,
    },
    {
      title: '分類',
      dataIndex: 'noticeTypeName',
      key: 'noticeTypeName',
      width: NOTICE_TABLE_COLUMNS_WIDTH.noticeTypeName,
    },
    {
      title: '文號',
      dataIndex: 'documentNumber',
      key: 'documentNumber',
      width: NOTICE_TABLE_COLUMNS_WIDTH.documentNumber,
      render: (text: string) => text || '無',
    },
    {
      title: '作者',
      dataIndex: 'author',
      key: 'author',
      width: NOTICE_TABLE_COLUMNS_WIDTH.author,
    },
    {
      title: '來源',
      dataIndex: 'source',
      key: 'source',
      width: NOTICE_TABLE_COLUMNS_WIDTH.source,
    },
    {
      title: '可見範圍',
      dataIndex: 'allVisibleFlag',
      key: 'allVisibleFlag',
      width: NOTICE_TABLE_COLUMNS_WIDTH.allVisibleFlag,
      render: (allVisibleFlag: boolean) => (
        <Tag color={allVisibleFlag ? 'green' : 'blue'}>
          {VISIBLE_FLAG_LABELS[String(allVisibleFlag) as 'true' | 'false']}
        </Tag>
      ),
    },
    {
      title: '發布時間',
      dataIndex: 'publishTime',
      key: 'publishTime',
      width: NOTICE_TABLE_COLUMNS_WIDTH.publishTime,
    },
    {
      title: '創建人',
      dataIndex: 'createUserName',
      key: 'createUserName',
      width: NOTICE_TABLE_COLUMNS_WIDTH.createUserName,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: NOTICE_TABLE_COLUMNS_WIDTH.createTime,
    },
    {
      title: '操作',
      key: 'operate',
      width: NOTICE_TABLE_COLUMNS_WIDTH.operate,
      fixed: 'right',
      render: (_: unknown, record: NoticeVO) => (
        <Space size="small">
          <PrivilegeButton
            type="link"
            size="small"
            permission={NOTICE_PERMISSION.UPDATE}
            onClick={() => handleEdit(record)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            type="link"
            size="small"
            danger
            permission={NOTICE_PERMISSION.DELETE}
            onClick={() => handleDelete(record)}
          >
            刪除
          </PrivilegeButton>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        {/* 搜尋表單 */}
        <Form form={queryForm} layout="inline" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 16]} style={{ width: '100%' }}>
            <Col>
              <Form.Item name="noticeTypeId" label="分類">
                <Select
                  placeholder="請選擇分類"
                  allowClear
                  style={{ width: 120 }}
                  options={noticeTypeList.map((item) => ({
                    label: item.noticeTypeName,
                    value: item.noticeTypeId,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="keywords" label="關鍵字">
                <Input
                  placeholder="標題、作者、來源"
                  allowClear
                  style={{ width: 250 }}
                  onPressEnter={handleSearch}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="documentNumber" label="文號">
                <Input
                  placeholder="請輸入文號"
                  allowClear
                  style={{ width: 150 }}
                  onPressEnter={handleSearch}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="createUserName" label="創建人">
                <Input
                  placeholder="請輸入創建人"
                  allowClear
                  style={{ width: 120 }}
                  onPressEnter={handleSearch}
                />
              </Form.Item>
            </Col>
            <Col>
              <Space size="small">
                <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                  搜尋
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleReset}>
                  重置
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>

        {/* 操作按鈕 */}
        <div style={{ marginBottom: 16 }}>
          <PrivilegeButton
            type="primary"
            icon={<PlusOutlined />}
            permission={NOTICE_PERMISSION.ADD}
            onClick={handleAdd}
          >
            新增通知公告
          </PrivilegeButton>
        </div>

        {/* 表格 */}
        <Table
          rowKey="noticeId"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 條`,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: handlePageChange,
          }}
          scroll={{ x: 1600 }}
        />
      </Card>

      {/* Form Drawer */}
      <NoticeFormDrawer
        visible={formDrawerVisible}
        onCancel={handleFormCancel}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
        noticeTypeList={noticeTypeList}
      />
    </div>
  );
}
