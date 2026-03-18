/**
 * CategoryTreeSelect Component
 * 分類樹選擇器組件
 *
 * 參考：Vue 版本 smart-admin-web/src/components/business/category-tree-select/index.vue
 *
 * 功能：
 * - 從後端 API 獲取分類樹數據
 * - 根據 categoryType 動態查詢數據
 * - 支持 Ant Design TreeSelect 所有屬性
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { useState, useEffect } from 'react';
import { TreeSelect, TreeSelectProps, message } from 'antd';
import { queryCategoryTree, CategoryTreeNode } from '@/api/business/categoryApi';

export interface CategoryTreeSelectProps extends Omit<TreeSelectProps, 'treeData'> {
  /**
   * 分類類型
   */
  categoryType?: number;

  /**
   * 寬度
   */
  width?: string | number;
}

/**
 * CategoryTreeSelect 組件
 */
function CategoryTreeSelect(props: CategoryTreeSelectProps) {
  const {
    categoryType,
    width = '100%',
    placeholder = '請選擇',
    allowClear = true,
    treeDefaultExpandAll = true,
    ...restProps
  } = props;

  const [treeData, setTreeData] = useState<CategoryTreeNode[]>([]);
  const [loading, setLoading] = useState(false);

  /**
   * 查詢分類樹數據
   */
  const fetchCategoryTree = async () => {
    if (!categoryType) {
      setTreeData([]);
      return;
    }

    setLoading(true);
    try {
      const response = await queryCategoryTree({ categoryType });
      if (response.ok && response.data) {
        setTreeData(response.data);
      } else {
        message.error(response.msg || '查詢分類樹失敗');
        setTreeData([]);
      }
    } catch (error) {
      console.error('查詢分類樹失敗:', error);
      message.error('查詢分類樹失敗，請稍後重試');
      setTreeData([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 監聽 categoryType 變化
   */
  useEffect(() => {
    fetchCategoryTree();
  }, [categoryType]);

  return (
    <TreeSelect
      {...restProps}
      style={{ width, ...restProps.style }}
      dropdownStyle={{ maxHeight: 400, overflowX: 'auto', ...restProps.dropdownStyle }}
      treeData={treeData}
      placeholder={placeholder}
      allowClear={allowClear}
      treeDefaultExpandAll={treeDefaultExpandAll}
      loading={loading}
      // TreeSelect 需要 treeData 中的節點有 value, title, key 屬性
      // CategoryTreeNode 已經包含這些屬性
      fieldNames={{
        label: 'title',
        value: 'value',
        children: 'children',
      }}
    />
  );
}

export default CategoryTreeSelect;
