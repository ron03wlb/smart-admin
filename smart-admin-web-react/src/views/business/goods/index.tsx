/**
 * Goods Management Page
 * 商品管理頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  message,
  Modal,
  Radio,
  Row,
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
  DeleteOutlined,
} from '@ant-design/icons';
import { goodsApi } from '@/api/business/goodsApi';
import type { GoodsVO, GoodsQueryForm, GoodsStatusEnum } from './types';
import {
  GOODS_PERMISSION,
  GOODS_TABLE_COLUMNS_WIDTH,
  GOODS_STATUS_LABELS,
  GOODS_STATUS_COLORS,
  SHELVES_FLAG_LABELS,
} from '@/constants/business/goodsConst';
import PrivilegeButton from '@/components/PrivilegeButton';
import GoodsFormDrawer from './components/GoodsFormDrawer';

const { confirm } = Modal;

export default function GoodsPage() {
  const [queryForm] = Form.useForm<GoodsQueryForm>();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<GoodsVO[]>([]);
  const [total, setTotal] = useState(0);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);

  // Pagination state
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Form Drawer state
  const [formDrawerVisible, setFormDrawerVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<GoodsVO | undefined>(undefined);

  /**
   * 查詢商品列表
   */
  const fetchGoodsList = async () => {
    try {
      setLoading(true);
      const values = queryForm.getFieldsValue();

      const params: GoodsQueryForm = {
        ...values,
        pageNum,
        pageSize,
      };

      const res = await goodsApi.queryGoodsList(params);
      if (res.ok && res.data) {
        setDataSource(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } catch (error) {
      message.error('查詢商品列表失敗');
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
    fetchGoodsList();
  };

  /**
   * 重置搜尋
   */
  const handleReset = () => {
    queryForm.resetFields();
    setPageNum(1);
    setPageSize(10);
    fetchGoodsList();
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
  const handleEdit = (record: GoodsVO) => {
    setFormInitialData(record);
    setFormDrawerVisible(true);
  };

  /**
   * 刪除商品
   */
  const handleDelete = (record: GoodsVO) => {
    confirm({
      title: '刪除商品',
      icon: <ExclamationCircleOutlined />,
      content: `確定要刪除商品「${record.goodsName}」嗎？`,
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await goodsApi.deleteGoods(record.goodsId);
          if (res.ok) {
            message.success('刪除成功');
            fetchGoodsList();
          }
        } catch (error) {
          message.error('刪除失敗');
          console.error(error);
        }
      },
    });
  };

  /**
   * 批量刪除
   */
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要刪除的商品');
      return;
    }

    confirm({
      title: '批量刪除商品',
      icon: <ExclamationCircleOutlined />,
      content: `確定要刪除選中的 ${selectedRowKeys.length} 個商品嗎？`,
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await goodsApi.batchDelete(selectedRowKeys);
          if (res.ok) {
            message.success('批量刪除成功');
            setSelectedRowKeys([]);
            fetchGoodsList();
          }
        } catch (error) {
          message.error('批量刪除失敗');
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
    fetchGoodsList();
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
    fetchGoodsList();
  }, [pageNum, pageSize]);

  /**
   * 表格列定義
   */
  const columns: ColumnsType<GoodsVO> = [
    {
      title: '商品名稱',
      dataIndex: 'goodsName',
      key: 'goodsName',
      width: GOODS_TABLE_COLUMNS_WIDTH.goodsName,
    },
    {
      title: '商品分類',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: GOODS_TABLE_COLUMNS_WIDTH.categoryName,
    },
    {
      title: '商品狀態',
      dataIndex: 'goodsStatus',
      key: 'goodsStatus',
      width: GOODS_TABLE_COLUMNS_WIDTH.goodsStatus,
      render: (status: GoodsStatusEnum) => (
        <Tag color={GOODS_STATUS_COLORS[status]}>{GOODS_STATUS_LABELS[status]}</Tag>
      ),
    },
    {
      title: '產地',
      dataIndex: 'place',
      key: 'place',
      width: GOODS_TABLE_COLUMNS_WIDTH.place,
    },
    {
      title: '價格',
      dataIndex: 'price',
      key: 'price',
      width: GOODS_TABLE_COLUMNS_WIDTH.price,
      render: (price: number) => `¥${price.toFixed(2)}`,
    },
    {
      title: '上架狀態',
      dataIndex: 'shelvesFlag',
      key: 'shelvesFlag',
      width: GOODS_TABLE_COLUMNS_WIDTH.shelvesFlag,
      render: (shelvesFlag: boolean) => (
        <Tag color={shelvesFlag ? 'green' : 'default'}>
          {SHELVES_FLAG_LABELS[String(shelvesFlag) as 'true' | 'false']}
        </Tag>
      ),
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: GOODS_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: GOODS_TABLE_COLUMNS_WIDTH.createTime,
    },
    {
      title: '操作',
      key: 'operate',
      width: GOODS_TABLE_COLUMNS_WIDTH.operate,
      fixed: 'right',
      render: (_: unknown, record: GoodsVO) => (
        <Space size="small">
          <PrivilegeButton
            type="link"
            size="small"
            privilege={GOODS_PERMISSION.UPDATE}
            onClick={() => handleEdit(record)}
          >
            編輯
          </PrivilegeButton>
          <PrivilegeButton
            type="link"
            size="small"
            danger
            privilege={GOODS_PERMISSION.DELETE}
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
              <Form.Item name="searchWord" label="商品名稱">
                <Input
                  placeholder="請輸入商品名稱"
                  allowClear
                  style={{ width: 200 }}
                  onPressEnter={handleSearch}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="shelvesFlag" label="快速篩選">
                <Radio.Group buttonStyle="solid">
                  <Radio.Button value={undefined}>全部</Radio.Button>
                  <Radio.Button value={true}>上架</Radio.Button>
                  <Radio.Button value={false}>下架</Radio.Button>
                </Radio.Group>
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
          <Space size="small">
            <PrivilegeButton
              type="primary"
              icon={<PlusOutlined />}
              privilege={GOODS_PERMISSION.ADD}
              onClick={handleAdd}
            >
              新增商品
            </PrivilegeButton>
            <PrivilegeButton
              danger
              icon={<DeleteOutlined />}
              privilege={GOODS_PERMISSION.BATCH_DELETE}
              onClick={handleBatchDelete}
              disabled={selectedRowKeys.length === 0}
            >
              批量刪除
            </PrivilegeButton>
          </Space>
        </div>

        {/* 表格 */}
        <Table
          rowKey="goodsId"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: keys => setSelectedRowKeys(keys as number[]),
          }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 條`,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: handlePageChange,
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* Form Drawer */}
      <GoodsFormDrawer
        visible={formDrawerVisible}
        onCancel={handleFormCancel}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />
    </div>
  );
}
