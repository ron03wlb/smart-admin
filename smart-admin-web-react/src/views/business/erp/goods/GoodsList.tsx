/**
 * Goods List
 *
 * Corresponds to Vue's business/erp/goods/goods-list.vue (529L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, Select, Radio, Tag, Modal, message, Tooltip } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { goodsApi } from '@/api/business/erp/goods-api';
import type { GoodsVO } from '@/api/business/erp/goods-api';
import type { ColumnsType } from 'antd/es/table';
import GoodsFormModal from './GoodsFormModal';

const PAGE_SIZE = 10;

const GoodsList: React.FC = () => {
  const [data, setData] = useState<GoodsVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [goodsName, setGoodsName] = useState('');
  const [goodsStatus, setGoodsStatus] = useState<number | undefined>();
  const [shelvesFlag, setShelvesFlag] = useState<boolean | undefined>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [currentGoods, setCurrentGoods] = useState<GoodsVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await goodsApi.query({
        goodsName: goodsName || undefined,
        goodsStatus,
        shelvesFlag,
        pageNum: page, pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [goodsName, goodsStatus, shelvesFlag]);

  useEffect(() => { queryList(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleReset = () => { setGoodsName(''); setGoodsStatus(undefined); setShelvesFlag(undefined); setPageNum(1); queryList(1); };

  const handleAdd = () => { setCurrentGoods(undefined); setFormOpen(true); };
  const handleEdit = (record: GoodsVO) => { setCurrentGoods(record); setFormOpen(true); };

  const handleDelete = (goodsId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该商品么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await goodsApi.delete(goodsId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) { message.warning('请选择要删除的记录'); return; }
    Modal.confirm({
      title: '提示', content: `确定要批量删除选中的 ${selectedRowKeys.length} 条记录么？`,
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await goodsApi.batchDelete(selectedRowKeys as number[]);
        message.success('删除成功');
        setSelectedRowKeys([]);
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<GoodsVO> = [
    { title: '分类', dataIndex: 'categoryName', width: 120 },
    { title: '商品名称', dataIndex: 'goodsName', width: 160, ellipsis: true, render: (text) => <Tooltip title={text}>{text}</Tooltip> },
    { title: '状态', dataIndex: 'goodsStatus', width: 80, align: 'center', render: (val) => <Tag color={val === 1 ? 'green' : 'red'}>{val === 1 ? '预约中' : '禁用'}</Tag> },
    { title: '产地', dataIndex: 'place', width: 100 },
    { title: '价格', dataIndex: 'price', width: 80, align: 'right' },
    { title: '上架', dataIndex: 'shelvesFlag', width: 70, align: 'center', render: (val) => <Tag color={val ? 'success' : 'default'}>{val ? '上架' : '下架'}</Tag> },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作', width: 120, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.goodsId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={goodsName} onChange={(e) => setGoodsName(e.target.value)} placeholder="商品名称" allowClear />
        <Select style={{ width: 120 }} value={goodsStatus} onChange={setGoodsStatus} placeholder="状态" allowClear
          options={[{ label: '预约中', value: 1 }, { label: '禁用', value: 2 }]} />
        <Radio.Group value={shelvesFlag} onChange={(e) => setShelvesFlag(e.target.value)}>
          <Radio.Button value={undefined}>全部</Radio.Button>
          <Radio.Button value={true}>上架</Radio.Button>
          <Radio.Button value={false}>下架</Radio.Button>
        </Radio.Group>
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
        <Button danger onClick={handleBatchDelete}>批量删除</Button>
      </Space>
      <Table
        rowKey="goodsId" columns={columns} dataSource={data} loading={loading} size="small"
        rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <GoodsFormModal open={formOpen} goods={currentGoods} onCancel={() => setFormOpen(false)} onSuccess={() => { setFormOpen(false); queryList(pageNum); }} />
    </Card>
  );
};

export default GoodsList;
