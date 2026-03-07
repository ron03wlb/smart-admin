/**
 * Config List
 *
 * Corresponds to Vue's support/config/config-list.vue (181L)
 * System configuration parameters with CRUD operations.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { configApi } from '@/api/support/config-api';
import type { ConfigVO } from '@/api/support/config-api';
import type { ColumnsType } from 'antd/es/table';
import ConfigFormModal from './ConfigFormModal';

const PAGE_SIZE = 10;

const ConfigList: React.FC = () => {
  const [data, setData] = useState<ConfigVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [configKey, setConfigKey] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [currentConfig, setCurrentConfig] = useState<ConfigVO | undefined>();

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await configApi.queryList({ configKey, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [configKey]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryList(1);
  };

  const handleReset = () => {
    setConfigKey('');
    setPageNum(1);
    queryList(1);
  };

  const handleAdd = () => {
    setCurrentConfig(undefined);
    setModalOpen(true);
  };

  const handleEdit = (record: ConfigVO) => {
    setCurrentConfig(record);
    setModalOpen(true);
  };

  const handleSuccess = () => {
    setModalOpen(false);
    queryList(pageNum);
  };

  const columns: ColumnsType<ConfigVO> = [
    { title: '参数Key', dataIndex: 'configKey', width: 200 },
    { title: '参数名称', dataIndex: 'configName', width: 180 },
    { title: '参数值', dataIndex: 'configValue', ellipsis: true },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    {
      title: '操作',
      width: 80,
      align: 'center',
      render: (_, record) => <a onClick={() => handleEdit(record)}>编辑</a>,
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 200 }}
          value={configKey}
          onChange={(e) => setConfigKey(e.target.value)}
          placeholder="参数Key"
          allowClear
        />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
      </Space>

      <Table
        rowKey="configId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryList(page); },
        }}
      />

      <ConfigFormModal
        open={modalOpen}
        config={currentConfig}
        onCancel={() => setModalOpen(false)}
        onSuccess={handleSuccess}
      />
    </Card>
  );
};

export default ConfigList;
