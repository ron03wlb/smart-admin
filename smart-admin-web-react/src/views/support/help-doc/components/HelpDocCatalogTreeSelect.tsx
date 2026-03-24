/**
 * Help Doc Catalog Tree Select
 * 幫助文檔目錄下拉選擇器
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState, forwardRef, useImperativeHandle } from 'react';
import { TreeSelect } from 'antd';
import { helpDocCatalogApi } from '@/api/support/helpDocCatalogApi';
import type { HelpDocCatalogVO } from '../types';

export interface HelpDocCatalogTreeSelectProps {
  value?: number;
  onChange?: (value: number) => void;
  multiple?: boolean;
  style?: React.CSSProperties;
}

export interface HelpDocCatalogTreeSelectRef {
  queryCatalogTree: () => Promise<void>;
}

/**
 * 構建目錄樹
 */
function buildHelpDocCatalogTree(data: HelpDocCatalogVO[], parentId: number): HelpDocCatalogVO[] {
  let children = data.filter(e => e.parentId === parentId) || [];
  children = children.sort((a, b) => a.sort - b.sort);
  children.forEach(e => {
    e.children = buildHelpDocCatalogTree(data, e.helpDocCatalogId);
  });
  updateHelpDocCatalogPreIdAndNextId(children);
  return children;
}

/**
 * 更新樹的前置ID和後置ID
 */
function updateHelpDocCatalogPreIdAndNextId(data: HelpDocCatalogVO[]) {
  for (let index = 0; index < data.length; index++) {
    if (index === 0) {
      data[index].nextId = data.length > 1 ? data[1].helpDocCatalogId : undefined;
      continue;
    }

    if (index === data.length - 1) {
      data[index].preId = data[index - 1].helpDocCatalogId;
      data[index].nextId = undefined;
      continue;
    }

    data[index].preId = data[index - 1].helpDocCatalogId;
    data[index].nextId = data[index + 1].helpDocCatalogId;
  }
}

const HelpDocCatalogTreeSelect = forwardRef<
  HelpDocCatalogTreeSelectRef,
  HelpDocCatalogTreeSelectProps
>(({ value, onChange, multiple = false, style }, ref) => {
  const [treeData, setTreeData] = useState<HelpDocCatalogVO[]>([]);

  useEffect(() => {
    queryCatalogTree();
  }, []);

  const queryCatalogTree = async () => {
    try {
      const res = await helpDocCatalogApi.getAll();
      const children = buildHelpDocCatalogTree(res.data, 0);
      setTreeData(children);
    } catch (error) {
      console.error('Failed to fetch catalog tree:', error);
    }
  };

  useImperativeHandle(ref, () => ({
    queryCatalogTree,
  }));

  return (
    <TreeSelect
      value={value}
      onChange={onChange}
      treeData={treeData}
      fieldNames={{ label: 'name', value: 'helpDocCatalogId', children: 'children' }}
      showSearch
      style={style || { width: '100%' }}
      dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
      placeholder="請選擇目錄"
      allowClear
      treeDefaultExpandAll
      multiple={multiple}
      treeNodeFilterProp="name"
    />
  );
});

HelpDocCatalogTreeSelect.displayName = 'HelpDocCatalogTreeSelect';

export default HelpDocCatalogTreeSelect;
