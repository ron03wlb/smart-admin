/**
 * Enterprise Employee List
 *
 * Corresponds to Vue's business/oa/enterprise/components/enterprise-employee-list.vue (292L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Table, Input, Button, Space, Modal, message } from 'antd';
import { enterpriseEmployeeApi } from '@/api/business/oa/enterprise-api';
import type { EnterpriseEmployeeVO } from '@/api/business/oa/enterprise-api';
import type { ColumnsType } from 'antd/es/table';

const PAGE_SIZE = 10;

interface Props {
  enterpriseId: number;
}

const EnterpriseEmployeeList: React.FC<Props> = ({ enterpriseId }) => {
  const [data, setData] = useState<EnterpriseEmployeeVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keywords, setKeywords] = useState('');

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await enterpriseEmployeeApi.queryPage({ enterpriseId, keywords, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [enterpriseId, keywords]);

  useEffect(() => { queryList(1); }, [enterpriseId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleRemove = (employeeId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要移除该员工么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await enterpriseEmployeeApi.delete({ enterpriseId, employeeId });
        message.success('移除成功');
        queryList(pageNum);
      },
    });
  };

  const columns: ColumnsType<EnterpriseEmployeeVO> = [
    { title: '姓名', dataIndex: 'actualName', width: 120 },
    { title: '部门', dataIndex: 'departmentName', width: 150 },
    { title: '手机号', dataIndex: 'phone', width: 130 },
    {
      title: '操作', width: 80, align: 'center',
      render: (_, record) => <a style={{ color: '#ff4d4f' }} onClick={() => handleRemove(record.employeeId)}>移除</a>,
    },
  ];

  return (
    <>
      <Space style={{ marginBottom: 16 }}>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="姓名/手机号" allowClear />
        <Button type="primary" onClick={() => { setPageNum(1); queryList(1); }}>查询</Button>
      </Space>
      <Table
        rowKey="employeeId" columns={columns} dataSource={data} loading={loading} size="small"
        pagination={{ current: pageNum, pageSize: PAGE_SIZE, total, showTotal: (t) => `共${t}条`, onChange: (page) => { setPageNum(page); queryList(page); } }}
      />
    </>
  );
};

export default EnterpriseEmployeeList;
