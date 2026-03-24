/**
 * Cache List Page
 * 緩存列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState } from 'react';
import { Card, Alert, Table, Button, Modal, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { cacheApi } from '@/api/support/cacheApi';
import { usePrivilege } from '@/hooks/usePrivilege';
import { CACHE_PERMISSION, CACHE_TABLE_COLUMNS_WIDTH } from '@/constants/support/cacheConst';
import type { CacheItem } from './types';

const CacheListPage: React.FC = () => {
  const hasDeletePrivilege = usePrivilege(CACHE_PERMISSION.DELETE);
  const hasKeysPrivilege = usePrivilege(CACHE_PERMISSION.KEYS);

  const [tableData, setTableData] = useState<CacheItem[]>([]);
  const [tableLoading, setTableLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setTableLoading(true);
      const res = await cacheApi.getAllCacheNames();
      setTableData(res.data.map(key => ({ key })));
    } catch (error) {
      console.error('Failed to fetch cache names:', error);
    } finally {
      setTableLoading(false);
    }
  };

  const handleRemove = async (cacheName: string) => {
    try {
      await cacheApi.remove(cacheName);
      message.success('刪除成功');
      fetchData();
    } catch (error) {
      console.error('Failed to remove cache:', error);
    }
  };

  const handleGetAllKeys = async (cacheName: string) => {
    try {
      const res = await cacheApi.getKeys(cacheName);
      Modal.info({
        title: `所有Key: ${cacheName}`,
        content: (
          <div>
            <p>{res.data.join(', ')}</p>
          </div>
        ),
        onOk: () => {
          fetchData();
        },
      });
    } catch (error) {
      console.error('Failed to get cache keys:', error);
    }
  };

  const columns: ColumnsType<CacheItem> = [
    {
      title: 'Key',
      dataIndex: 'key',
      key: 'key',
      width: CACHE_TABLE_COLUMNS_WIDTH.key,
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      fixed: 'right',
      width: CACHE_TABLE_COLUMNS_WIDTH.action,
      render: (_: any, record: CacheItem) => (
        <div>
          {hasDeletePrivilege && (
            <Button type="link" size="small" onClick={() => handleRemove(record.key)}>
              清除
            </Button>
          )}
          {hasKeysPrivilege && (
            <Button type="link" size="small" onClick={() => handleGetAllKeys(record.key)}>
              獲取所有key
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <Card size="small" bordered={false} hoverable>
      <Alert
        message={<h4>緩存介紹：</h4>}
        description={
          <pre>
            {`簡介：SmartAdmin使用的是SpringCache進行管理緩存，SpringCache有多種實現方式，本項目默認採用的是caffeine。
Caffeine：
- Caffeine是一個進程內部緩存框架，使用了Java 8最新的[StampedLock]樂觀鎖技術，極大提高緩存並發吞吐量，一個高性能的 Java 緩存庫，被稱為最快緩存。
其他：
· 對於分布式、集群等應用實現方式可以改為 Redis、CouchBase等`}
          </pre>
        }
        type="info"
        style={{ marginBottom: 16 }}
      />

      <Table
        rowKey="key"
        columns={columns}
        dataSource={tableData}
        loading={tableLoading}
        pagination={false}
        size="small"
        bordered
      />
    </Card>
  );
};

export default CacheListPage;
