/**
 * Goods Catalog - Tree Table
 * 商品目錄 - 樹形表格
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/catalog/goods-catalog.vue
 *       Vue 版本 smart-admin-web/src/views/business/erp/catalog/components/category-tree-table.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useEffect, useRef } from 'react';
import { Card, Button, Table, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { usePrivilege } from '@/hooks/usePrivilege';
import { categoryApi } from '@/api/business/categoryApi';
import { CATEGORY_PERMISSION } from '@/constants/business/categoryConst';
import type { CategoryVO } from '@/views/business/category/types';
import { CategoryTypeEnum } from '@/views/business/category/types';
import CategoryFormModal from '@/views/business/category/components/CategoryFormModal';

/**
 * 商品目錄頁面（樹形表格）
 */
export default function GoodsCatalog() {
  const hasAddPrivilege = usePrivilege(CATEGORY_PERMISSION.ADD);
  const hasAddChildPrivilege = usePrivilege(CATEGORY_PERMISSION.ADD_CHILD);
  const hasUpdatePrivilege = usePrivilege(CATEGORY_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(CATEGORY_PERMISSION.DELETE);

  // State
  const [tableData, setTableData] = useState<CategoryVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
  const formModalRef = useRef<{ show: (parentId?: number, rowData?: CategoryVO) => void }>(null);

  // 固定使用商品分類類型
  const categoryType = CategoryTypeEnum.GOODS;

  /**
   * 加載分類樹
   */
  const loadCategoryTree = async (expandParentId?: number) => {
    setLoading(true);
    try {
      const res = await categoryApi.queryCategoryTree({ categoryType });
      if (res.data) {
        setTableData(res.data);

        // 如果有新增的父級分類，自動展開
        if (expandParentId) {
          setExpandedRowKeys(prev => {
            if (!prev.includes(expandParentId)) {
              return [...prev, expandParentId];
            }
            return prev;
          });
        }
      }
    } catch (error) {
      message.error('加載商品分類失敗');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 添加分類（根分類）
   */
  const handleAdd = () => {
    formModalRef.current?.show();
  };

  /**
   * 添加子分類
   */
  const handleAddChild = (parentId: number) => {
    formModalRef.current?.show(parentId);
  };

  /**
   * 編輯分類
   */
  const handleEdit = (record: CategoryVO) => {
    formModalRef.current?.show(undefined, record);
  };

  /**
   * 刪除分類
   */
  const handleDelete = (categoryId: number) => {
    Modal.confirm({
      title: '提示',
      content: '確定要刪除當前分類嗎？',
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await categoryApi.deleteCategory(categoryId);
          message.success('刪除成功');
          loadCategoryTree();
        } catch (error) {
          message.error('刪除失敗');
        }
      },
    });
  };

  /**
   * 表格列定義
   */
  const columns: ColumnsType<CategoryVO> = [
    {
      title: '商品分類',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 300,
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 8 }}>
          {hasAddChildPrivilege && (
            <Button type="link" size="small" onClick={() => handleAddChild(record.categoryId)}>
              增加子分類
            </Button>
          )}
          {hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              編輯
            </Button>
          )}
          {hasDeletePrivilege && (
            <Button type="link" size="small" danger onClick={() => handleDelete(record.categoryId)}>
              刪除
            </Button>
          )}
        </div>
      ),
    },
  ];

  /**
   * 處理表格展開/收起
   */
  const handleExpand = (expanded: boolean, record: CategoryVO) => {
    const keys = [...expandedRowKeys];
    if (expanded) {
      keys.push(record.categoryId);
    } else {
      const index = keys.indexOf(record.categoryId);
      if (index > -1) {
        keys.splice(index, 1);
      }
    }
    setExpandedRowKeys(keys);
  };

  /**
   * 表單提交成功回調
   */
  const handleFormSuccess = (parentId?: number) => {
    loadCategoryTree(parentId);
  };

  // 初始加載
  useEffect(() => {
    loadCategoryTree();
  }, []);

  return (
    <Card
      size="small"
      bordered={false}
      title="商品目錄"
      extra={
        hasAddPrivilege && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建
          </Button>
        )
      }
    >
      <Table
        rowKey="categoryId"
        columns={columns}
        dataSource={tableData}
        loading={loading}
        pagination={false}
        scroll={{ x: 600 }}
        size="small"
        bordered
        expandable={{
          expandedRowKeys,
          onExpand: handleExpand,
          defaultExpandAllRows: false,
        }}
      />

      {/* 表單模態框 - 復用現有的 CategoryFormModal */}
      <CategoryFormModal
        ref={formModalRef}
        categoryType={categoryType}
        onSuccess={handleFormSuccess}
      />
    </Card>
  );
}
