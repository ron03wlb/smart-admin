/**
 * Help Doc Catalog Tree
 *
 * Corresponds to Vue's support/help-doc/management/components/help-doc-catalog-tree.vue (353L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Tree, Input, Button, Space, Dropdown, Modal, message } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { helpDocCatalogApi } from '@/api/support/help-doc-api';
import type { HelpDocCatalogVO } from '@/api/support/help-doc-api';
import type { DataNode } from 'antd/es/tree';
import HelpDocCatalogFormModal from './HelpDocCatalogFormModal';

interface Props {
  onSelect?: (catalogId: number | undefined) => void;
}

const buildTreeData = (list: HelpDocCatalogVO[]): DataNode[] =>
  list.map((item) => ({
    key: item.helpDocCatalogId,
    title: item.name,
    children: item.children ? buildTreeData(item.children) : [],
  }));

const HelpDocCatalogTree: React.FC<Props> = ({ onSelect }) => {
  const [treeData, setTreeData] = useState<DataNode[]>([]);
  const [catalogList, setCatalogList] = useState<HelpDocCatalogVO[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [searchValue, setSearchValue] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editCatalog, setEditCatalog] = useState<HelpDocCatalogVO | undefined>();
  const [parentId, setParentId] = useState<number>(0);

  const loadTree = useCallback(async () => {
    const res = await helpDocCatalogApi.getAll();
    if (res.code === 1 && res.data) {
      setCatalogList(res.data);
      const tree = buildTreeData(res.data);
      setTreeData(tree);
      setExpandedKeys(tree.map((n) => n.key));
    }
  }, []);

  useEffect(() => { loadTree(); }, [loadTree]);

  const handleSelect = (keys: React.Key[]) => {
    setSelectedKeys(keys);
    onSelect?.(keys.length > 0 ? (keys[0] as number) : undefined);
  };

  const handleAddRoot = () => { setEditCatalog(undefined); setParentId(0); setFormOpen(true); };

  const handleAddChild = (parentCatalogId: number) => {
    setEditCatalog(undefined);
    setParentId(parentCatalogId);
    setFormOpen(true);
  };

  const handleEditCatalog = (catalog: HelpDocCatalogVO) => {
    setEditCatalog(catalog);
    setParentId(catalog.parentId);
    setFormOpen(true);
  };

  const handleDeleteCatalog = (catalogId: number) => {
    Modal.confirm({
      title: '提示', content: '确定要删除该目录么？',
      okText: '确定', okType: 'danger', cancelText: '取消',
      async onOk() {
        await helpDocCatalogApi.delete(catalogId);
        message.success('删除成功');
        loadTree();
      },
    });
  };

  const findCatalog = (id: number, list: HelpDocCatalogVO[]): HelpDocCatalogVO | undefined => {
    for (const item of list) {
      if (item.helpDocCatalogId === id) return item;
      if (item.children) {
        const found = findCatalog(id, item.children);
        if (found) return found;
      }
    }
    return undefined;
  };

  const titleRender = (node: DataNode) => {
    const catalog = findCatalog(node.key as number, catalogList);
    return (
      <Dropdown
        trigger={['contextMenu']}
        menu={{
          items: [
            { key: 'addChild', label: '添加子目录', onClick: () => handleAddChild(node.key as number) },
            { key: 'edit', label: '编辑', onClick: () => catalog && handleEditCatalog(catalog) },
            { key: 'delete', label: '删除', danger: true, onClick: () => handleDeleteCatalog(node.key as number) },
          ],
        }}
      >
        <span>{node.title as string}</span>
      </Dropdown>
    );
  };

  return (
    <div>
      <Space style={{ marginBottom: 8 }}>
        <Input size="small" style={{ width: 140 }} value={searchValue} onChange={(e) => setSearchValue(e.target.value)} placeholder="搜索目录" allowClear />
        <Button size="small" icon={<PlusOutlined />} onClick={handleAddRoot}>新建</Button>
        <Button size="small" icon={<ReloadOutlined />} onClick={loadTree} />
      </Space>
      <Tree
        treeData={treeData}
        expandedKeys={expandedKeys}
        selectedKeys={selectedKeys}
        onExpand={setExpandedKeys}
        onSelect={handleSelect}
        titleRender={titleRender}
        blockNode
      />
      <HelpDocCatalogFormModal
        open={formOpen}
        catalog={editCatalog}
        parentId={parentId}
        catalogList={catalogList}
        onCancel={() => setFormOpen(false)}
        onSuccess={() => { setFormOpen(false); loadTree(); }}
      />
    </div>
  );
};

export default HelpDocCatalogTree;
