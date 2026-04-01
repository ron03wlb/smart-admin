/**
 * Dict Data Modal (Drawer)
 *
 * Corresponds to Vue's support/dict/components/dict-data-modal.vue (253L)
 * Displays and manages dictionary values in a drawer.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Drawer, Table, Input, Button, Space, Switch, Modal, message } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { dictApi } from '@/api/support/dict-api';
import type { DictDataVO } from '@/types/dict.types';
import type { ColumnsType } from 'antd/es/table';
import DictDataFormModal from './DictDataFormModal';

interface Props {
  open: boolean;
  dictId: number;
  dictCode: string;
  onClose: () => void;
}

const DictDataModal: React.FC<Props> = ({ open, dictId, dictCode, onClose }) => {
  const [allData, setAllData] = useState<DictDataVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [keywords, setKeywords] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [currentData, setCurrentData] = useState<DictDataVO | undefined>();

  const queryData = useCallback(async () => {
    if (!dictId) return;
    setLoading(true);
    try {
      const res = await dictApi.queryDictData(dictId);
      if (res.code === 1 && res.data) {
        setAllData(res.data);
      }
    } finally {
      setLoading(false);
    }
  }, [dictId]);

  useEffect(() => {
    if (open) {
      setKeywords('');
      setSelectedRowKeys([]);
      queryData();
    }
  }, [open, queryData]);

  const filteredData = keywords
    ? allData.filter((d) => d.dataLabel.includes(keywords) || d.dataValue.includes(keywords))
    : allData;

  const handleAdd = () => { setCurrentData(undefined); setFormOpen(true); };
  const handleEdit = (record: DictDataVO) => { setCurrentData(record); setFormOpen(true); };

  const handleToggleDisabled = async (dictDataId: number) => {
    await dictApi.updateDictDataDisabled(dictDataId);
    message.success('操作成功');
    queryData();
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) { message.warning('请选择要删除的数据'); return; }
    Modal.confirm({
      title: '提示',
      content: `确定要删除选中的 ${selectedRowKeys.length} 条数据么？`,
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await dictApi.batchDeleteDictData(selectedRowKeys);
        message.success('删除成功');
        setSelectedRowKeys([]);
        queryData();
      },
    });
  };

  const columns: ColumnsType<DictDataVO> = [
    { title: '显示文本', dataIndex: 'dataLabel', width: 150 },
    { title: '值', dataIndex: 'dataValue', width: 120 },
    { title: '排序', dataIndex: 'sortOrder', width: 80 },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'disabledFlag',
      width: 80,
      align: 'center',
      render: (val, record) => (
        <Switch checked={!val} size="small" onChange={() => handleToggleDisabled(record.dictDataId)} />
      ),
    },
    {
      title: '操作',
      width: 60,
      align: 'center',
      render: (_, record) => <a onClick={() => handleEdit(record)}>编辑</a>,
    },
  ];

  return (
    <Drawer
      title={`字典数据 [${dictCode}]`}
      open={open}
      onClose={onClose}
      width={800}
    >
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="文本/值" allowClear />
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
        <Button danger icon={<DeleteOutlined />} onClick={handleBatchDelete} disabled={selectedRowKeys.length === 0}>批量删除</Button>
      </Space>

      <Table
        rowKey="dictDataId"
        columns={columns}
        dataSource={filteredData}
        loading={loading}
        size="small"
        rowSelection={{ selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys as number[]) }}
        pagination={false}
      />

      <DictDataFormModal
        open={formOpen}
        dictId={dictId}
        dictData={currentData}
        onCancel={() => setFormOpen(false)}
        onSuccess={() => { setFormOpen(false); queryData(); }}
      />
    </Drawer>
  );
};

export default DictDataModal;
