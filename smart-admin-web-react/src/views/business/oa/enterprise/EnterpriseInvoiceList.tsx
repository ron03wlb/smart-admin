/**
 * Enterprise Invoice List
 *
 * Corresponds to Vue's business/oa/enterprise/components/enterprise-invoice-list.vue (253L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { invoiceApi } from '@/api/business/oa/enterprise-api';
import type { InvoiceVO } from '@/api/business/oa/enterprise-api';
import type { ColumnsType } from 'antd/es/table';
import EnterpriseInvoiceOperateModal from './EnterpriseInvoiceOperateModal';

const PAGE_SIZE = 10;

interface Props {
  enterpriseId: number;
}

const EnterpriseInvoiceList: React.FC<Props> = ({ enterpriseId }) => {
  const [data, setData] = useState<InvoiceVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [formOpen, setFormOpen] = useState(false);
  const [currentInvoice, setCurrentInvoice] = useState<InvoiceVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await invoiceApi.pageQuery({ enterpriseId, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [enterpriseId]);

  useEffect(() => { queryList(1); }, [enterpriseId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAdd = () => { setCurrentInvoice(undefined); setFormOpen(true); };
  const handleEdit = (record: InvoiceVO) => { setCurrentInvoice(record); setFormOpen(true); };

  const handleDelete = (invoiceId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该发票信息么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await invoiceApi.delete(invoiceId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<InvoiceVO> = [
    { title: '发票抬头', dataIndex: 'invoiceHeads', width: 200 },
    { title: '纳税人识别号', dataIndex: 'taxpayerIdentificationNumber', width: 200 },
    { title: '账号', dataIndex: 'accountNumber', width: 180 },
    { title: '开户行', dataIndex: 'bankName', width: 150 },
    { title: '状态', dataIndex: 'disabledFlag', width: 80, align: 'center', render: (val) => <Tag color={val ? 'red' : 'green'}>{val ? '禁用' : '正常'}</Tag> },
    {
      title: '操作', width: 120, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.invoiceId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
      </Space>
      <Table
        rowKey="invoiceId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <EnterpriseInvoiceOperateModal open={formOpen} invoice={currentInvoice} enterpriseId={enterpriseId} onCancel={() => setFormOpen(false)} onSuccess={() => { setFormOpen(false); queryList(pageNum); }} />
    </>
  );
};

export default EnterpriseInvoiceList;
