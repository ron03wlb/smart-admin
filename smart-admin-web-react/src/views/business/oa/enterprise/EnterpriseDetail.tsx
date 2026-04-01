/**
 * Enterprise Detail
 *
 * Corresponds to Vue's business/oa/enterprise/enterprise-detail.vue (128L)
 */
import React, { useEffect, useState } from 'react';
import { Card, Tabs, Descriptions, Tag, Spin } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { enterpriseApi } from '@/api/business/oa/enterprise-api';
import type { EnterpriseVO } from '@/api/business/oa/enterprise-api';
import EnterpriseEmployeeList from './EnterpriseEmployeeList';
import EnterpriseBankList from './EnterpriseBankList';
import EnterpriseInvoiceList from './EnterpriseInvoiceList';

const EnterpriseDetail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const enterpriseId = Number(searchParams.get('enterpriseId'));
  const [enterprise, setEnterprise] = useState<EnterpriseVO | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (enterpriseId) {
      setLoading(true);
      enterpriseApi.detail(enterpriseId).then((res) => {
        if (res.code === 1 && res.data) setEnterprise(res.data);
      }).finally(() => setLoading(false));
    }
  }, [enterpriseId]);

  if (loading || !enterprise) return <Card><Spin /></Card>;

  return (
    <Card>
      <Descriptions title="企业信息" bordered column={2} size="small" style={{ marginBottom: 24 }}>
        <Descriptions.Item label="企业名称">{enterprise.enterpriseName}</Descriptions.Item>
        <Descriptions.Item label="信用代码">{enterprise.unifiedSocialCreditCode}</Descriptions.Item>
        <Descriptions.Item label="联系人">{enterprise.contact}</Descriptions.Item>
        <Descriptions.Item label="联系电话">{enterprise.contactPhone}</Descriptions.Item>
        <Descriptions.Item label="邮箱">{enterprise.email}</Descriptions.Item>
        <Descriptions.Item label="状态"><Tag color={enterprise.disabledFlag ? 'red' : 'green'}>{enterprise.disabledFlag ? '禁用' : '正常'}</Tag></Descriptions.Item>
        <Descriptions.Item label="地址" span={2}>{[enterprise.provinceName, enterprise.cityName, enterprise.districtName, enterprise.address].filter(Boolean).join(' ')}</Descriptions.Item>
      </Descriptions>
      <Tabs items={[
        { key: 'employee', label: '企业员工', children: <EnterpriseEmployeeList enterpriseId={enterpriseId} /> },
        { key: 'bank', label: '银行信息', children: <EnterpriseBankList enterpriseId={enterpriseId} /> },
        { key: 'invoice', label: '发票信息', children: <EnterpriseInvoiceList enterpriseId={enterpriseId} /> },
      ]} />
    </Card>
  );
};

export default EnterpriseDetail;
