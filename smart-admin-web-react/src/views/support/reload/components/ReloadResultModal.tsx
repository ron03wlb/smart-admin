/**
 * Reload Result Modal
 * Reload 結果 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Button, Table, Tag } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { reloadApi } from '@/api/support/reloadApi';
import type { ReloadResultVO } from '../types';

export interface ReloadResultModalRef {
  showModal: (tag: string) => void;
}

const ReloadResultModal = forwardRef<ReloadResultModalRef>((_, ref) => {
  const [visible, setVisible] = useState(false);
  const [tableData, setTableData] = useState<ReloadResultVO[]>([]);
  const [tableLoading, setTableLoading] = useState(false);
  const [queryTag, setQueryTag] = useState('');

  useImperativeHandle(ref, () => ({
    showModal: (tag: string) => {
      setQueryTag(tag);
      setVisible(true);
      fetchData(tag);
    },
  }));

  const fetchData = async (tag: string) => {
    try {
      setTableLoading(true);
      const res = await reloadApi.queryReloadResult(tag);
      const dataWithId = res.data.map((item, index) => ({
        ...item,
        id: index + 1,
      }));
      setTableData(dataWithId);
    } catch (error) {
      console.error('Failed to fetch reload result:', error);
    } finally {
      setTableLoading(false);
    }
  };

  const handleRefresh = () => {
    fetchData(queryTag);
  };

  const handleCancel = () => {
    setVisible(false);
  };

  const columns: ColumnsType<ReloadResultVO> = [
    {
      title: '標籤',
      dataIndex: 'tag',
      key: 'tag',
    },
    {
      title: '參數',
      dataIndex: 'args',
      key: 'args',
    },
    {
      title: '運行結果',
      dataIndex: 'result',
      key: 'result',
      render: (result: boolean) => (
        <Tag color={result ? 'success' : 'error'}>
          {result ? '成功' : '失敗'}
        </Tag>
      ),
    },
    {
      title: '異常',
      dataIndex: 'exception',
      key: 'exception',
      ellipsis: true,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
    },
  ];

  return (
    <Modal
      open={visible}
      title="reload結果列表"
      width="60%"
      footer={null}
      onCancel={handleCancel}
    >
      <Button
        type="primary"
        size="small"
        icon={<ReloadOutlined />}
        onClick={handleRefresh}
        style={{ marginBottom: 10 }}
      >
        刷新
      </Button>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={tableData}
        loading={tableLoading}
        pagination={false}
        size="small"
        bordered
        scroll={{ y: 350 }}
        expandable={{
          expandedRowRender: (record) => (
            <pre style={{ margin: 0, fontSize: 12 }}>{record.exception}</pre>
          ),
          rowExpandable: (record) => !!record.exception,
        }}
      />
    </Modal>
  );
});

ReloadResultModal.displayName = 'ReloadResultModal';

export default ReloadResultModal;
