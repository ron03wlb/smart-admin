/**
 * Category Management - Tree Table
 * 分類管理 - 樹形表格
 *
 * 參考：Vue 版本 smart-admin-web/src/views/business/erp/catalog/components/category-tree-table.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { useState, useEffect, useRef } from 'react';
import { Card, Button, Table, Modal, message, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { usePrivilege } from '@/hooks/usePrivilege';
import { categoryApi } from '@/api/business/categoryApi';
import { CATEGORY_PERMISSION, CATEGORY_TYPE_LABELS, DISABLED_STATUS_LABELS } from '@/constants/business/categoryConst';
import type { CategoryVO, CategoryTypeEnum } from './types';
import CategoryFormModal from './components/CategoryFormModal';

/**
 * 分類管理頁面（樹形表格）
 */
export default function CategoryManagement() {
  const hasAddPrivilege = usePrivilege(CATEGORY_PERMISSION.ADD);
  const hasAddChildPrivilege = usePrivilege(CATEGORY_PERMISSION.ADD_CHILD);
  const hasUpdatePrivilege = usePrivilege(CATEGORY_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(CATEGORY_PERMISSION.DELETE);

  // State
  const [tableData, setTableData] = useState<CategoryVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [categoryType] = useState<CategoryTypeEnum>(1); // 默認商品分類
  const formModalRef = useRef<{ show: (parentId?: number, rowData?: CategoryVO) => void }>(null);

  /**
   * 加載分類樹
   */
  const loadCategoryTree = async () => {
    setLoading(true);
    try {
      const res = await categoryApi.queryCategoryTree({ categoryType });
      if (res.data) {
        setTableData(res.data);
      }
    } catch (error) {
      message.error('加載分類數據失敗');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 添加分類
   */
  const handleAdd = (parentId?: number) => {
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
      title: '確認刪除？',
      content: '刪除後無法恢復，請確認是否刪除該分類。',
      okText: '確認',
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
      title: '分類名稱',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 200,
    },
    {
      title: '分類類型',
      dataIndex: 'categoryType',
      key: 'categoryType',
      width: 100,
      render: (categoryType: number) => CATEGORY_TYPE_LABELS[categoryType as keyof typeof CATEGORY_TYPE_LABELS] || '-',
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: 80,
      render: (sort?: number) => sort ?? '-',
    },
    {
      title: '狀態',
      dataIndex: 'disabledFlag',
      key: 'disabledFlag',
      width: 80,
      render: (disabledFlag: boolean) => (
        <Tag color={disabledFlag ? 'red' : 'green'}>
          {DISABLED_STATUS_LABELS[disabledFlag ? 'true' : 'false']}
        </Tag>
      ),
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: 200,
      render: (remark?: string) => remark || '-',
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 8 }}>
          {hasAddChildPrivilege && (
            <Button type="link" size="small" onClick={() => handleAdd(record.categoryId)}>
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

  // 初始加載
  useEffect(() => {
    loadCategoryTree();
  }, [categoryType]);

  return (
    <Card
      size="small"
      bordered={false}
      title="分類管理"
      extra={
        hasAddPrivilege && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>
            新建分類
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
        scroll={{ x: 1000 }}
        size="small"
        bordered
      />

      {/* 表單模態框 */}
      <CategoryFormModal ref={formModalRef} categoryType={categoryType} onSuccess={loadCategoryTree} />
    </Card>
  );
}
