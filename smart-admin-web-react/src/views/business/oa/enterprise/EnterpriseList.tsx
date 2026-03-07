/**
 * Enterprise List
 *
 * Corresponds to Vue's business/oa/enterprise/enterprise-list.vue (288L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, Tag, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { enterpriseApi } from '@/api/business/oa/enterprise-api';
import type { EnterpriseVO } from '@/api/business/oa/enterprise-api';
import type { ColumnsType } from 'antd/es/table';
import EnterpriseOperateModal from './EnterpriseOperateModal';

const PAGE_SIZE = 10;

const EnterpriseList: React.FC = () => {
  const [data, setData] = useState<EnterpriseVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [currentEnterprise, setCurrentEnterprise] = useState<EnterpriseVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await enterpriseApi.pageQuery({ keywords, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [keywords]);

  useEffect(() => { queryList(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleReset = () => { setKeywords(''); setPageNum(1); queryList(1); };
  const handleAdd = () => { setCurrentEnterprise(undefined); setFormOpen(true); };
  const handleEdit = (record: EnterpriseVO) => { setCurrentEnterprise(record); setFormOpen(true); };

  const handleDelete = (enterpriseId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该企业么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await enterpriseApi.delete(enterpriseId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<EnterpriseVO> = [
    { title: '企业名称', dataIndex: 'enterpriseName', width: 200, ellipsis: true },
    { title: '统一社会信用代码', dataIndex: 'unifiedSocialCreditCode', width: 200 },
    { title: '联系人', dataIndex: 'contactName', width: 100 },
    { title: '联系电话', dataIndex: 'contactPhone', width: 130 },
    { title: '状态', dataIndex: 'disabledFlag', width: 80, align: 'center', render: (val) => <Tag color={val ? 'red' : 'green'}>{val ? '禁用' : '正常'}</Tag> },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作', width: 180, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a onClick={() => window.open(`#/business/oa/enterprise/detail?enterpriseId=${record.enterpriseId}`, '_self')}>详情</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.enterpriseId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="企业名称" allowClear />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
      </Space>
      <Table
        rowKey="enterpriseId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <EnterpriseOperateModal open={formOpen} enterprise={currentEnterprise} onCancel={() => setFormOpen(false)} onSuccess={() => { setFormOpen(false); queryList(pageNum); }} />
    </Card>
  );
};

export default EnterpriseList;
