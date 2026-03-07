/**
 * Help Doc List Table
 *
 * Corresponds to Vue's support/help-doc/management/components/help-doc-list.vue (272L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, Input, Button, Space, DatePicker, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { helpDocApi } from '@/api/support/help-doc-api';
import type { HelpDocVO } from '@/api/support/help-doc-api';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import HelpDocFormDrawer from './HelpDocFormDrawer';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

interface Props {
  helpDocCatalogId?: number;
}

const HelpDocList: React.FC<Props> = ({ helpDocCatalogId }) => {
  const [data, setData] = useState<HelpDocVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [currentDoc, setCurrentDoc] = useState<HelpDocVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await helpDocApi.query({
        helpDocCatalogId, keywords,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
        pageNum: page, pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [helpDocCatalogId, keywords, dateRange]);

  useEffect(() => { setPageNum(1); queryList(1); }, [helpDocCatalogId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleAdd = () => { setCurrentDoc(undefined); setDrawerOpen(true); };
  const handleEdit = (record: HelpDocVO) => { setCurrentDoc(record); setDrawerOpen(true); };

  const handleDelete = (helpDocId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该文档么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await helpDocApi.delete(helpDocId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<HelpDocVO> = [
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '目录', dataIndex: 'helpDocCatalogName', width: 120 },
    { title: '作者', dataIndex: 'author', width: 100 },
    { title: '排序', dataIndex: 'sort', width: 60 },
    { title: '浏览量', dataIndex: 'pageViewCount', width: 80, align: 'center' },
    { title: '用户浏览', dataIndex: 'userViewCount', width: 80, align: 'center' },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作', width: 120, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.helpDocId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="标题/作者" allowClear />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val as any)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
      </Space>
      <Table
        rowKey="helpDocId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <HelpDocFormDrawer open={drawerOpen} helpDoc={currentDoc} onClose={() => setDrawerOpen(false)} onSuccess={() => { setDrawerOpen(false); queryList(pageNum); }} />
    </>
  );
};

export default HelpDocList;
