/**
 * Serial Number List
 *
 * Corresponds to Vue's support/serial-number/serial-number-list.vue (147L)
 * Displays serial number configurations with generate and record actions.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Space, Alert } from 'antd';
import { serialNumberApi } from '@/api/support/serial-number-api';
import type { SerialNumberVO } from '@/api/support/serial-number-api';
import type { ColumnsType } from 'antd/es/table';
import SerialNumberGenerateFormModal from './SerialNumberGenerateFormModal';
import SerialNumberRecordList from './SerialNumberRecordList';

const SerialNumberList: React.FC = () => {
  const [data, setData] = useState<SerialNumberVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [generateOpen, setGenerateOpen] = useState(false);
  const [recordOpen, setRecordOpen] = useState(false);
  const [currentSN, setCurrentSN] = useState<SerialNumberVO | undefined>();

  const queryList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await serialNumberApi.getAll();
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

  const handleGenerate = (record: SerialNumberVO) => {
    setCurrentSN(record);
    setGenerateOpen(true);
  };

  const handleViewRecords = (record: SerialNumberVO) => {
    setCurrentSN(record);
    setRecordOpen(true);
  };

  const columns: ColumnsType<SerialNumberVO> = [
    { title: '业务名称', dataIndex: 'businessName', width: 180 },
    { title: '格式', dataIndex: 'format', ellipsis: true },
    { title: '规则类型', dataIndex: 'ruleType', width: 100 },
    { title: '初始值', dataIndex: 'initNumber', width: 80 },
    { title: '最后生成', dataIndex: 'lastNumber', width: 150 },
    { title: '最后时间', dataIndex: 'lastTime', width: 180 },
    {
      title: '操作',
      width: 160,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleGenerate(record)}>生成</a>
          <a onClick={() => handleViewRecords(record)}>记录</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Alert
        type="info"
        showIcon
        message="说明：单据序列号生成器，支持内存/Redis/MySQL锁三种实现方式"
        style={{ marginBottom: 16 }}
      />
      <Table
        rowKey="serialNumberId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={false}
      />

      {currentSN && (
        <>
          <SerialNumberGenerateFormModal
            open={generateOpen}
            serialNumber={currentSN}
            onCancel={() => setGenerateOpen(false)}
            onSuccess={() => { setGenerateOpen(false); queryList(); }}
          />
          <SerialNumberRecordList
            open={recordOpen}
            serialNumberId={currentSN.serialNumberId}
            onCancel={() => setRecordOpen(false)}
          />
        </>
      )}
    </Card>
  );
};

export default SerialNumberList;
