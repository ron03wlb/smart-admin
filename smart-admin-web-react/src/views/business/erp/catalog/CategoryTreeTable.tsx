/**
 * Category Tree Table
 *
 * Corresponds to Vue's business/erp/catalog/components/category-tree-table.vue (164L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Button, Space, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { categoryApi } from '@/api/business/erp/catalog-api';
import type { CategoryVO } from '@/api/business/erp/catalog-api';
import type { ColumnsType } from 'antd/es/table';
import CategoryFormModal from './CategoryFormModal';

interface Props {
  categoryType: number;
}

const CategoryTreeTable: React.FC<Props> = ({ categoryType }) => {
  const [data, setData] = useState<CategoryVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [editCategory, setEditCategory] = useState<CategoryVO | undefined>();
  const [parentId, setParentId] = useState<number>(0);

  const loadTree = useCallback(async () => {
    setLoading(true);
    try {
      const res = await categoryApi.queryTree({ categoryType });
      if (res.code === 1 && res.data) {
        setData(res.data);
        const keys = res.data.map((item) => item.categoryId);
        setExpandedRowKeys(keys);
      }
    } finally {
      setLoading(false);
    }
  }, [categoryType]);

  useEffect(() => { loadTree(); }, [loadTree]);

  const handleAddRoot = () => { setEditCategory(undefined); setParentId(0); setFormOpen(true); };
  const handleAddChild = (parentCategoryId: number) => { setEditCategory(undefined); setParentId(parentCategoryId); setFormOpen(true); };
  const handleEdit = (record: CategoryVO) => { setEditCategory(record); setParentId(record.parentId); setFormOpen(true); };

  const handleDelete = (categoryId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该分类么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await categoryApi.delete(categoryId);
        message.success('删除成功');
        loadTree();
      },
    });
  };

  const columns: ColumnsType<CategoryVO> = [
    { title: '分类名称', dataIndex: 'categoryName' },
    { title: '排序', dataIndex: 'sort', width: 80 },
    {
      title: '操作', width: 220, align: 'center',
      render: (_, record) => (
        <Space size="small">
          <a onClick={() => handleAddChild(record.categoryId)}>添加子分类</a>
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.categoryId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddRoot}>添加分类</Button>
      </Space>
      <Table
        rowKey="categoryId" columns={columns} dataSource={data} loading={loading} size="small"
        expandable={{ expandedRowKeys, onExpandedRowsChange: (keys) => setExpandedRowKeys(keys as React.Key[]), childrenColumnName: 'children' }}
        pagination={false}
      />
      <CategoryFormModal
        open={formOpen}
        category={editCategory}
        categoryType={categoryType}
        parentId={parentId}
        onCancel={() => setFormOpen(false)}
        onSuccess={() => { setFormOpen(false); loadTree(); }}
      />
    </Card>
  );
};

export default CategoryTreeTable;
