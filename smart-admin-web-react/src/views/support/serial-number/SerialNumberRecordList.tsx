/**
 * Serial Number Record List Modal
 *
 * Corresponds to Vue's support/serial-number/components/serial-number-record-list.vue (114L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Modal, Table } from 'antd';
import { serialNumberApi } from '@/api/support/serial-number-api';
import type { SerialNumberRecordVO } from '@/api/support/serial-number-api';
import type { ColumnsType } from 'antd/es/table';

const PAGE_SIZE = 10;

interface Props {
  open: boolean;
  serialNumberId: number;
  onCancel: () => void;
}

const SerialNumberRecordList: React.FC<Props> = ({ open, serialNumberId, onCancel }) => {
  const [data, setData] = useState<SerialNumberRecordVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);

  const queryRecords = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await serialNumberApi.queryRecord({
        serialNumberId,
        pageNum: page,
        pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [serialNumberId]);

  useEffect(() => {
    if (open) {
      setPageNum(1);
      queryRecords(1);
    }
  }, [open, queryRecords]);

  const columns: ColumnsType<SerialNumberRecordVO> = [
    { title: '日期', dataIndex: 'recordDate', width: 120 },
    { title: '生成数量', dataIndex: 'count', width: 100 },
    { title: '最后生成', dataIndex: 'lastNumber', ellipsis: true },
    { title: '最后时间', dataIndex: 'lastTime', width: 180 },
  ];

  return (
    <Modal
      title="生成记录"
      open={open}
      onCancel={onCancel}
      footer={null}
      width={700}
      destroyOnClose
    >
      <Table
        rowKey={(r) => `${r.serialNumberId}-${r.recordDate}`}
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryRecords(page); },
        }}
      />
    </Modal>
  );
};

export default SerialNumberRecordList;
