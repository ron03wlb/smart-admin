/**
 * Cache List
 *
 * Corresponds to Vue's support/cache/cache-list.vue (112L)
 * Displays all Caffeine cache names with actions to view keys and clear cache.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Space, Modal, Alert, message } from 'antd';
import { cacheApi } from '@/api/support/cache-api';
import type { ColumnsType } from 'antd/es/table';

interface CacheItem {
  key: string;
}

const CacheList: React.FC = () => {
  const [data, setData] = useState<CacheItem[]>([]);
  const [loading, setLoading] = useState(false);

  const queryList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await cacheApi.getAllCacheNames();
      if (res.code === 1 && res.data) {
        setData(res.data.map((name) => ({ key: name })));
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    queryList();
  }, [queryList]);

  const handleViewKeys = async (cacheName: string) => {
    const res = await cacheApi.getKeys(cacheName);
    if (res.code === 1 && res.data) {
      Modal.info({
        title: `缓存 [${cacheName}] 的所有Key`,
        width: 600,
        content: (
          <div style={{ maxHeight: 400, overflow: 'auto', whiteSpace: 'pre-wrap' }}>
            {res.data.length > 0 ? res.data.join('\n') : '暂无缓存Key'}
          </div>
        ),
      });
    }
  };

  const handleRemove = (cacheName: string) => {
    Modal.confirm({
      title: '提示',
      content: `确定要清除缓存 [${cacheName}] 么？`,
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      async onOk() {
        await cacheApi.remove(cacheName);
        message.success('清除成功');
        queryList();
      },
    });
  };

  const columns: ColumnsType<CacheItem> = [
    { title: '缓存名称', dataIndex: 'key' },
    {
      title: '操作',
      width: 200,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleViewKeys(record.key)}>查看Key</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleRemove(record.key)}>清除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Alert
        type="info"
        showIcon
        message="说明：使用 Spring Cache + Caffeine 本地缓存，以下为所有缓存名称"
        style={{ marginBottom: 16 }}
      />
      <Table rowKey="key" columns={columns} dataSource={data} loading={loading} size="small" pagination={false} />
    </Card>
  );
};

export default CacheList;
