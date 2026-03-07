/**
 * Dictionary Management
 *
 * Corresponds to Vue's support/dict/index.vue (260L)
 * Two-level CRUD: Dictionary list + Dictionary values drawer.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, Switch, Modal, message } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { dictApi } from '@/api/support/dict-api';
import type { DictVO } from '@/types/dict.types';
import type { ColumnsType } from 'antd/es/table';
import DictFormModal from './DictFormModal';
import DictDataModal from './DictDataModal';

const PAGE_SIZE = 10;

const DictIndex: React.FC = () => {
  const [data, setData] = useState<DictVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [searchWord, setSearchWord] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [currentDict, setCurrentDict] = useState<DictVO | undefined>();
  const [dataModalOpen, setDataModalOpen] = useState(false);
  const [dataDictId, setDataDictId] = useState<number>(0);
  const [dataDictCode, setDataDictCode] = useState('');

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await dictApi.queryPage({ searchWord, pageNum: page, pageSize: PAGE_SIZE });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [searchWord]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPageNum(1); queryList(1); };
  const handleReset = () => { setSearchWord(''); setPageNum(1); queryList(1); };

  const handleAdd = () => { setCurrentDict(undefined); setFormOpen(true); };
  const handleEdit = (record: DictVO) => { setCurrentDict(record); setFormOpen(true); };

  const handleToggleDisabled = async (dictId: number) => {
    await dictApi.updateDisabled(dictId);
    message.success('操作成功');
    queryList(pageNum);
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) { message.warning('请选择要删除的字典'); return; }
    Modal.confirm({
      title: '提示',
      content: `确定要删除选中的 ${selectedRowKeys.length} 个字典么？`,
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await dictApi.batchDelete(selectedRowKeys);
        message.success('删除成功');
        setSelectedRowKeys([]);
        queryList(pageNum);
      },
    });
  };

  const handleViewData = (record: DictVO) => {
    setDataDictId(record.dictId);
    setDataDictCode(record.dictCode);
    setDataModalOpen(true);
  };

  const columns: ColumnsType<DictVO> = [
    {
      title: '字典编码',
      dataIndex: 'dictCode',
      width: 200,
      render: (text, record) => <a onClick={() => handleViewData(record)}>{text}</a>,
    },
    { title: '字典名称', dataIndex: 'dictName', width: 180 },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'disabledFlag',
      width: 80,
      align: 'center',
      render: (val, record) => (
        <Switch checked={!val} size="small" onChange={() => handleToggleDisabled(record.dictId)} />
      ),
    },
    {
      title: '操作',
      width: 120,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a onClick={() => handleViewData(record)}>数据</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 200 }} value={searchWord} onChange={(e) => setSearchWord(e.target.value)} placeholder="编码/名称/备注" allowClear />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
        <Button danger icon={<DeleteOutlined />} onClick={handleBatchDelete} disabled={selectedRowKeys.length === 0}>批量删除</Button>
      </Space>

      <Table
        rowKey="dictId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        rowSelection={{ selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys as number[]) }}
        pagination={{
          current: pageNum, pageSize: PAGE_SIZE, total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryList(page); },
        }}
      />

      <DictFormModal
        open={formOpen}
        dict={currentDict}
        onCancel={() => setFormOpen(false)}
        onSuccess={() => { setFormOpen(false); queryList(pageNum); }}
      />

      <DictDataModal
        open={dataModalOpen}
        dictId={dataDictId}
        dictCode={dataDictCode}
        onClose={() => setDataModalOpen(false)}
      />
    </Card>
  );
};

export default DictIndex;
