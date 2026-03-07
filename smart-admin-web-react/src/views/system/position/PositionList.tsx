/**
 * Position List Page
 *
 * Corresponds to Vue's views/system/position/position-list.vue
 */
import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Input, Space, Modal, Card, Form, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { positionApi } from '@/api/system/position-api';
import type { PositionVO, PositionQueryForm } from '@/types/position.types';
import PositionFormModal from './PositionFormModal';

const PositionList: React.FC = () => {
  const [data, setData] = useState<PositionVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<PositionQueryForm>({
    pageNum: 1,
    pageSize: 10,
    keywords: '',
  });
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<PositionVO | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await positionApi.queryPage(queryForm);
      if (res.code === 1 && res.data) {
        setData(res.data.list);
        setTotal(res.data.total);
      }
    } finally {
      setLoading(false);
    }
  }, [queryForm]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSearch = () => {
    setQueryForm((prev) => ({ ...prev, pageNum: 1 }));
  };

  const handleReset = () => {
    setQueryForm((prev) => ({ pageNum: 1, pageSize: prev.pageSize, keywords: '' }));
  };

  const handleAdd = () => {
    setCurrentRecord(null);
    setModalVisible(true);
  };

  const handleEdit = (record: PositionVO) => {
    setCurrentRecord(record);
    setModalVisible(true);
  };

  const handleDelete = (record: PositionVO) => {
    Modal.confirm({
      title: '提示',
      content: '确定要删除吗?',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        const res = await positionApi.delete(record.positionId);
        if (res.code === 1) {
          message.success('删除成功');
          fetchData();
        }
      },
    });
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的数据');
      return;
    }
    Modal.confirm({
      title: '提示',
      content: '确定要批量删除这些数据吗?',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        const res = await positionApi.batchDelete(selectedRowKeys as number[]);
        if (res.code === 1) {
          message.success('删除成功');
          setSelectedRowKeys([]);
          fetchData();
        }
      },
    });
  };

  const columns: ColumnsType<PositionVO> = [
    { title: '职务名称', dataIndex: 'positionName', ellipsis: true },
    { title: '职级', dataIndex: 'positionLevel', ellipsis: true },
    { title: '排序', dataIndex: 'sort', ellipsis: true },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', ellipsis: true },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 90,
      render: (_: unknown, record: PositionVO) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Button type="link" size="small" danger onClick={() => handleDelete(record)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Form layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="关键字查询">
          <Input
            style={{ width: 200 }}
            value={queryForm.keywords}
            onChange={(e) => setQueryForm((prev) => ({ ...prev, keywords: e.target.value }))}
            onPressEnter={handleSearch}
            placeholder="关键字查询"
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>查询</Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      <Card size="small" bordered={false} hoverable>
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
          <Button type="primary" danger icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0} onClick={handleBatchDelete}>
            批量删除
          </Button>
        </Space>

        <Table
          size="small"
          bordered
          dataSource={data}
          columns={columns}
          loading={loading}
          rowKey="positionId"
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          pagination={{
            current: queryForm.pageNum,
            pageSize: queryForm.pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共${t}条`,
            onChange: (page, size) => setQueryForm((prev) => ({ ...prev, pageNum: page, pageSize: size })),
          }}
        />
      </Card>

      <PositionFormModal
        visible={modalVisible}
        record={currentRecord}
        onCancel={() => setModalVisible(false)}
        onSuccess={() => {
          setModalVisible(false);
          fetchData();
        }}
      />
    </div>
  );
};

export default PositionList;
