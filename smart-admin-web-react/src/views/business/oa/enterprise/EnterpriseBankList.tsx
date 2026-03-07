/**
 * Enterprise Bank List
 *
 * Corresponds to Vue's business/oa/enterprise/components/enterprise-bank-list.vue (249L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { bankApi } from '@/api/business/oa/enterprise-api';
import type { BankVO } from '@/api/business/oa/enterprise-api';
import type { ColumnsType } from 'antd/es/table';
import EnterpriseBankOperateModal from './EnterpriseBankOperateModal';

const PAGE_SIZE = 10;

interface Props {
  enterpriseId: number;
}

const EnterpriseBankList: React.FC<Props> = ({ enterpriseId }) => {
  const [data, setData] = useState<BankVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [formOpen, setFormOpen] = useState(false);
  const [currentBank, setCurrentBank] = useState<BankVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await bankApi.pageQuery({ enterpriseId, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [enterpriseId]);

  useEffect(() => { queryList(1); }, [enterpriseId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAdd = () => { setCurrentBank(undefined); setFormOpen(true); };
  const handleEdit = (record: BankVO) => { setCurrentBank(record); setFormOpen(true); };

  const handleDelete = (bankId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该银行信息么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await bankApi.delete(bankId);
        message.success('删除成功');
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<BankVO> = [
    { title: '开户行', dataIndex: 'bankName', width: 150 },
    { title: '户名', dataIndex: 'accountName', width: 150 },
    { title: '账号', dataIndex: 'accountNumber', width: 200 },
    { title: '对公', dataIndex: 'publicFlag', width: 70, align: 'center', render: (val) => <Tag color={val ? 'blue' : 'default'}>{val ? '对公' : '对私'}</Tag> },
    { title: '状态', dataIndex: 'disabledFlag', width: 80, align: 'center', render: (val) => <Tag color={val ? 'red' : 'green'}>{val ? '禁用' : '正常'}</Tag> },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '操作', width: 120, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.bankId)}>删除</a>
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
        rowKey="bankId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
      <EnterpriseBankOperateModal open={formOpen} bank={currentBank} enterpriseId={enterpriseId} onCancel={() => setFormOpen(false)} onSuccess={() => { setFormOpen(false); queryList(pageNum); }} />
    </>
  );
};

export default EnterpriseBankList;
