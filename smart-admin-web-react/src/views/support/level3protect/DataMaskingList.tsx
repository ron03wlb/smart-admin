/**
 * Data Masking Demo List
 *
 * Corresponds to Vue's support/level3protect/data-masking-list.vue
 * Displays PII masking demo data.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Button, Alert } from 'antd';
import { dataMaskingApi } from '@/api/support/level3protect-api';
import type { DataMaskingDemoVO } from '@/api/support/level3protect-api';
import type { ColumnsType } from 'antd/es/table';

const DataMaskingList: React.FC = () => {
  const [data, setData] = useState<DataMaskingDemoVO[]>([]);
  const [loading, setLoading] = useState(false);

  const queryList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await dataMaskingApi.query();
      if (res.code === 1 && res.data) {
        setData(res.data);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    queryList();
  }, [queryList]);

  const columns: ColumnsType<DataMaskingDemoVO> = [
    { title: '用户ID', dataIndex: 'userId', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 140 },
    { title: '身份证', dataIndex: 'idCard', width: 180 },
    { title: '密码', dataIndex: 'password', width: 100 },
    { title: '邮箱', dataIndex: 'email', width: 180 },
    { title: '车牌号', dataIndex: 'carLicense', width: 120 },
    { title: '银行卡', dataIndex: 'bankCard', width: 180 },
    { title: '地址', dataIndex: 'address', ellipsis: true },
  ];

  return (
    <Card>
      <Alert
        type="info"
        showIcon
        message="数据脱敏演示：后端使用 @DataMasking 注解自动对敏感字段进行脱敏处理"
        style={{ marginBottom: 16 }}
      />
      <Button type="primary" onClick={queryList} style={{ marginBottom: 16 }}>查询</Button>
      <Table
        rowKey="userId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={false}
      />
    </Card>
  );
};

export default DataMaskingList;
