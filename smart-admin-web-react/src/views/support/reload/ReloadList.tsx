/**
 * Reload List
 *
 * Corresponds to Vue's support/reload/reload-list.vue (138L)
 * Displays hot-reload configurations with execute and view results actions.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Space, Alert } from 'antd';
import { reloadApi } from '@/api/support/reload-api';
import type { ReloadVO } from '@/api/support/reload-api';
import type { ColumnsType } from 'antd/es/table';
import DoReloadFormModal from './DoReloadFormModal';
import ReloadResultList from './ReloadResultList';

const ReloadList: React.FC = () => {
  const [data, setData] = useState<ReloadVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [reloadOpen, setReloadOpen] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);
  const [currentReload, setCurrentReload] = useState<ReloadVO | undefined>();

  const queryList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reloadApi.queryList();
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

  const handleExecute = (record: ReloadVO) => {
    setCurrentReload(record);
    setReloadOpen(true);
  };

  const handleViewResult = (record: ReloadVO) => {
    setCurrentReload(record);
    setResultOpen(true);
  };

  const columns: ColumnsType<ReloadVO> = [
    { title: 'Tag', dataIndex: 'tag', width: 200 },
    { title: 'Identification', dataIndex: 'identification', ellipsis: true },
    { title: 'Args', dataIndex: 'args', ellipsis: true },
    { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    {
      title: '操作',
      width: 160,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleExecute(record)}>执行</a>
          <a onClick={() => handleViewResult(record)}>结果</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Alert
        type="info"
        showIcon
        message="说明：Reload服务用于热加载/更新部分程序配置，无需重启进程"
        style={{ marginBottom: 16 }}
      />
      <Table
        rowKey="tag"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={false}
      />

      {currentReload && (
        <>
          <DoReloadFormModal
            open={reloadOpen}
            reload={currentReload}
            onCancel={() => setReloadOpen(false)}
            onSuccess={() => { setReloadOpen(false); queryList(); }}
          />
          <ReloadResultList
            open={resultOpen}
            tag={currentReload.tag}
            onCancel={() => setResultOpen(false)}
          />
        </>
      )}
    </Card>
  );
};

export default ReloadList;
