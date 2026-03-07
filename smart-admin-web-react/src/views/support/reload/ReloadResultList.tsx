/**
 * Reload Result List Modal
 *
 * Corresponds to Vue's support/reload/components/reload-result-list.vue (105L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Modal, Table, Tag, Button } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { reloadApi } from '@/api/support/reload-api';
import type { ReloadResultVO } from '@/api/support/reload-api';
import type { ColumnsType } from 'antd/es/table';

interface Props {
  open: boolean;
  tag: string;
  onCancel: () => void;
}

const ReloadResultList: React.FC<Props> = ({ open, tag, onCancel }) => {
  const [data, setData] = useState<ReloadResultVO[]>([]);
  const [loading, setLoading] = useState(false);

  const queryResults = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reloadApi.queryReloadResult(tag);
      if (res.code === 1 && res.data) {
        setData(res.data);
      }
    } finally {
      setLoading(false);
    }
  }, [tag]);

  useEffect(() => {
    if (open) {
      queryResults();
    }
  }, [open, queryResults]);

  const columns: ColumnsType<ReloadResultVO> = [
    { title: 'Tag', dataIndex: 'tag', width: 160 },
    { title: 'Args', dataIndex: 'args', ellipsis: true },
    {
      title: '结果',
      dataIndex: 'result',
      width: 80,
      align: 'center',
      render: (val) => <Tag color={val ? 'success' : 'error'}>{val ? '成功' : '失败'}</Tag>,
    },
    { title: '异常', dataIndex: 'exception', ellipsis: true },
    { title: '时间', dataIndex: 'createTime', width: 180 },
  ];

  return (
    <Modal
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: 24 }}>
          <span>执行结果</span>
          <Button size="small" icon={<ReloadOutlined />} onClick={queryResults}>刷新</Button>
        </div>
      }
      open={open}
      onCancel={onCancel}
      footer={null}
      width={800}
      destroyOnClose
    >
      <Table
        rowKey={(_, i) => String(i)}
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={false}
        scroll={{ y: 350 }}
      />
    </Modal>
  );
};

export default ReloadResultList;
